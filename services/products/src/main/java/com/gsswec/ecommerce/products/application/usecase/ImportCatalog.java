package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.products.application.port.out.EventPublisher;
import com.gsswec.ecommerce.products.application.port.out.ImportJobRepository;
import com.gsswec.ecommerce.products.application.port.out.ProductCache;
import com.gsswec.ecommerce.products.application.port.out.ProductRepository;
import com.gsswec.ecommerce.products.domain.model.ImportJob;
import com.gsswec.ecommerce.products.domain.model.Product;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.product.ProductImportedEvent;
import com.gsswec.ecommerce.shared.util.ImportError;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ImportCatalog {

    private final ProductRepository products;
    private final ResolveCategory resolveCategory;
    private final ImportJobRepository jobs;
    private final CsvValidator validator;
    private final ProductCache cache;
    private final EventPublisher events;
    private final Clock clock;

    public ImportCatalog(
            ProductRepository products, ResolveCategory resolveCategory, ImportJobRepository jobs,
            CsvValidator validator, ProductCache cache, EventPublisher events, Clock clock) {
        this.products = products;
        this.resolveCategory = resolveCategory;
        this.jobs = jobs;
        this.validator = validator;
        this.cache = cache;
        this.events = events;
        this.clock = clock;
    }

    // Creates the PROCESSING job synchronously so the controller can return
    // 202 + importId immediately; the heavy work runs in process().
    public ImportJob start(UUID requestedBy) {
        return jobs.save(ImportJob.processing(UUID.randomUUID(), requestedBy));
    }

    @Async
    public void process(UUID jobId, byte[] csvBytes) {
        long startNanos = System.nanoTime();
        int imported = 0;
        int updated = 0;
        int skipped = 0;
        int total = 0;
        List<ImportError> errors = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true)
                .setTrim(true).build()
                .parse(new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8))) {

            for (CSVRecord record : parser) {
                if (isEmptyRow(record)) {
                    continue; // trailing/blank rows are not errors
                }
                total++;
                int rowNumber = (int) record.getRecordNumber();
                CsvValidator.Result result = validator.validate(rowNumber, toMap(record));
                if (!result.isValid()) {
                    errors.add(result.error());
                    skipped++;
                    continue;
                }
                if (upsert(result.row())) {
                    imported++;
                } else {
                    updated++;
                }
            }
        } catch (Exception e) {
            long ms = (System.nanoTime() - startNanos) / 1_000_000;
            jobs.save(jobs.findById(jobId).orElseThrow().failed(ms));
            return;
        }

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        ImportJob completed = jobs.findById(jobId).orElseThrow()
                .completed(total, imported, updated, skipped, errors, durationMs);
        jobs.save(completed);
        cache.evictAll();

        events.publish(StreamNames.PRODUCT_IMPORTED, new ProductImportedEvent(
                Events.base(StreamNames.PRODUCT_IMPORTED, clock),
                jobId, completed.requestedBy(), total, imported, updated, skipped, errors, durationMs));
    }

    // Upsert by SKU. Returns true if a new product was inserted, false if updated.
    private boolean upsert(CsvValidator.ValidRow row) {
        Integer categoryId = resolveCategory.toIdOrNull(row.category());
        Optional<Product> existing = products.findBySku(row.sku());
        if (existing.isPresent()) {
            Product merged = existing.get().withUpdates(
                    row.name(), row.description(), categoryId, row.price(), row.stock(),
                    row.weightKg(), row.imageUrl(), null);
            products.save(merged);
            return false;
        }
        products.save(Product.create(
                row.name(), row.sku(), row.description(), categoryId, row.price(), row.stock(),
                row.weightKg(), row.imageUrl()));
        return true;
    }

    private static Map<String, String> toMap(CSVRecord record) {
        Map<String, String> map = new LinkedHashMap<>();
        record.toMap().forEach((k, v) -> map.put(k == null ? "" : k.trim().toLowerCase(), v));
        return map;
    }

    private static boolean isEmptyRow(CSVRecord record) {
        for (String value : record) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
