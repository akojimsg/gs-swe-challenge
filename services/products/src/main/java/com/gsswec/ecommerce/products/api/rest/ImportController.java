package com.gsswec.ecommerce.products.api.rest;

import com.gsswec.ecommerce.products.api.rest.dto.ImportJobResponse;
import com.gsswec.ecommerce.products.application.usecase.ImportCatalog;
import com.gsswec.ecommerce.products.domain.exception.ProductNotFoundException;
import com.gsswec.ecommerce.products.application.port.out.ImportJobRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products/import")
@Tag(name = "Product Import", description = "Async CSV catalogue import [ADMIN]")
public class ImportController {

    private final ImportCatalog importCatalog;
    private final ImportJobRepository jobs;

    public ImportController(ImportCatalog importCatalog, ImportJobRepository jobs) {
        this.importCatalog = importCatalog;
        this.jobs = jobs;
    }

    @PostMapping
    @Operation(summary = "Upload a CSV for async import; returns 202 + importId [ADMIN]")
    public ResponseEntity<ImportJobResponse> upload(
            @RequestParam("file") MultipartFile file, Principal principal) throws IOException {
        UUID requestedBy = principal != null ? UUID.fromString(principal.getName()) : new UUID(0, 0);
        var job = importCatalog.start(requestedBy);
        importCatalog.process(job.id(), file.getBytes());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ImportJobResponse.from(job));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get import job status and result summary [ADMIN]")
    public ImportJobResponse status(@PathVariable UUID id) {
        return jobs.findById(id).map(ImportJobResponse::from)
                .orElseThrow(ProductNotFoundException::new);
    }
}
