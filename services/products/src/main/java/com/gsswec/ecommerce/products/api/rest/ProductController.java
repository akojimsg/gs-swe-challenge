package com.gsswec.ecommerce.products.api.rest;

import com.gsswec.ecommerce.products.api.rest.dto.CreateProductRequest;
import com.gsswec.ecommerce.products.api.rest.dto.PagedResponse;
import com.gsswec.ecommerce.products.api.rest.dto.ProductResponse;
import com.gsswec.ecommerce.products.api.rest.dto.UpdateProductRequest;
import com.gsswec.ecommerce.products.application.port.out.ProductSearchCriteria;
import com.gsswec.ecommerce.products.application.usecase.CreateProduct;
import com.gsswec.ecommerce.products.application.usecase.DeleteProduct;
import com.gsswec.ecommerce.products.application.usecase.GetProduct;
import com.gsswec.ecommerce.products.application.usecase.SearchProducts;
import com.gsswec.ecommerce.products.application.usecase.UpdateProduct;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Public catalogue reads and admin product management")
public class ProductController {

    private final SearchProducts searchProducts;
    private final GetProduct getProduct;
    private final CreateProduct createProduct;
    private final UpdateProduct updateProduct;
    private final DeleteProduct deleteProduct;

    public ProductController(
            SearchProducts searchProducts, GetProduct getProduct, CreateProduct createProduct,
            UpdateProduct updateProduct, DeleteProduct deleteProduct) {
        this.searchProducts = searchProducts;
        this.getProduct = getProduct;
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.deleteProduct = deleteProduct;
    }

    @GetMapping
    @Operation(summary = "Browse/search the catalogue (public)")
    public PagedResponse<ProductResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        var criteria = new ProductSearchCriteria(q, category, minPrice, maxPrice, inStock, page, size, sort);
        return PagedResponse.from(searchProducts.search(criteria), ProductResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id (public)")
    public ProductResponse getById(@PathVariable UUID id) {
        return ProductResponse.from(getProduct.byId(id));
    }

    @PostMapping
    @Operation(summary = "Create a product [ADMIN]")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest req) {
        var product = createProduct.create(new CreateProduct.Command(
                req.name(), req.sku(), req.description(), req.category(),
                req.price(), req.stock(), req.weightKg(), req.imageUrl()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a product [ADMIN]")
    public ProductResponse replace(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest req) {
        return ProductResponse.from(updateProduct.update(id, toCommand(req), false));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a product [ADMIN]")
    public ProductResponse patch(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest req) {
        return ProductResponse.from(updateProduct.update(id, toCommand(req), true));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product [ADMIN]")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteProduct.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static UpdateProduct.Command toCommand(UpdateProductRequest req) {
        return new UpdateProduct.Command(req.name(), req.description(), req.category(),
                req.price(), req.stock(), req.weightKg(), req.imageUrl(), req.active());
    }
}
