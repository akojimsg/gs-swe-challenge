package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.shared.util.ImportError;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

// Validates and sanitises a single CSV row. Pure (no I/O); the most edge-case
// dense component in the service — see features/csv-import.md.
@Component
public class CsvValidator {

    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern PRICE_NOISE = Pattern.compile("[^0-9.\\-]");

    // Returns a ValidRow on success, or an ImportError describing the rejection.
    public Result validate(int rowNumber, Map<String, String> row) {
        String name = sanitise(row.get("name"));
        if (name == null || name.isBlank()) {
            return Result.error(new ImportError(rowNumber, "name", str(row.get("name")), "name is required"));
        }
        String sku = trimToNull(row.get("sku"));
        if (sku == null) {
            return Result.error(new ImportError(rowNumber, "sku", str(row.get("sku")), "sku is required"));
        }

        Optional<BigDecimal> price = parsePrice(row.get("price"));
        if (price.isEmpty()) {
            return Result.error(new ImportError(rowNumber, "price", str(row.get("price")),
                    "price is not a valid number"));
        }
        if (price.get().signum() < 0) {
            return Result.error(new ImportError(rowNumber, "price", str(row.get("price")),
                    "price must be >= 0"));
        }

        Optional<Integer> stock = parseInt(row.get("stock"));
        if (stock.isEmpty()) {
            return Result.error(new ImportError(rowNumber, "stock", str(row.get("stock")),
                    "stock is not a valid integer"));
        }
        if (stock.get() < 0) {
            return Result.error(new ImportError(rowNumber, "stock", str(row.get("stock")),
                    "stock must be >= 0"));
        }

        BigDecimal weight = parseWeight(row.get("weight_kg")).orElse(null);
        String description = sanitise(row.get("description"));
        String category = trimToNull(row.get("category")); // blank -> null (allowed)

        return Result.valid(new ValidRow(name, sku, description, category,
                price.get(), stock.get(), weight));
    }

    // Strip HTML tags (XSS) then trim. SQLi is handled by parameterised writes;
    // sanitisation here is defence in depth for stored/rendered strings.
    private static String sanitise(String value) {
        if (value == null) {
            return null;
        }
        return TAGS.matcher(value).replaceAll("").trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String str(String value) {
        return value == null ? "" : value;
    }

    private static Optional<BigDecimal> parsePrice(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String cleaned = PRICE_NOISE.matcher(raw.trim()).replaceAll("");
        if (cleaned.isBlank() || cleaned.equals("-") || cleaned.equals(".")) {
            return Optional.empty(); // e.g. "free"
        }
        try {
            return Optional.of(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.valueOf(raw.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<BigDecimal> parseWeight(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(raw.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public record ValidRow(
            String name, String sku, String description, String category,
            BigDecimal price, Integer stock, BigDecimal weightKg) {
    }

    public record Result(ValidRow row, ImportError error) {
        static Result valid(ValidRow row) {
            return new Result(row, null);
        }

        static Result error(ImportError error) {
            return new Result(null, error);
        }

        public boolean isValid() {
            return error == null;
        }
    }
}
