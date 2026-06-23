package com.gsswec.ecommerce.products.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.gsswec.ecommerce.products.application.usecase.CsvValidator;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvValidatorTest {

    private final CsvValidator validator = new CsvValidator();

    private Map<String, String> row(String name, String sku, String description,
            String category, String price, String stock, String weight) {
        Map<String, String> m = new HashMap<>();
        m.put("name", name);
        m.put("sku", sku);
        m.put("description", description);
        m.put("category", category);
        m.put("price", price);
        m.put("stock", stock);
        m.put("weight_kg", weight);
        return m;
    }

    @Test
    void acceptsAWellFormedRow() {
        var r = validator.validate(1, row("Running Shoes", "RS-001", "desc", "Footwear", "89.99", "150", "0.35"));
        assertThat(r.isValid()).isTrue();
        assertThat(r.row().price()).isEqualByComparingTo("89.99");
        assertThat(r.row().stock()).isEqualTo(150);
    }

    @Test
    void parsesCurrencyPrefixedPrice() {
        var r = validator.validate(1, row("Wireless Mouse", "WM-042", "d", "Electronics", "$29.99", "75", "0.12"));
        assertThat(r.isValid()).isTrue();
        assertThat(r.row().price()).isEqualByComparingTo("29.99");
    }

    @Test
    void rejectsNonNumericPrice() {
        var r = validator.validate(1, row("Yoga Mat", "YM-015", "d", "Sports", "free", "200", "1.2"));
        assertThat(r.isValid()).isFalse();
        assertThat(r.error().field()).isEqualTo("price");
    }

    @Test
    void acceptsZeroPrice() {
        var r = validator.validate(1, row("Mystery Box", "MB-001", "d", "Gifts", "0.00", "50", "1.0"));
        assertThat(r.isValid()).isTrue();
        assertThat(r.row().price()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsNegativeStock() {
        var r = validator.validate(1, row("Desk Lamp", "DL-007", "d", "Home", "45.50", "-5", "2.1"));
        assertThat(r.isValid()).isFalse();
        assertThat(r.error().field()).isEqualTo("stock");
    }

    @Test
    void acceptsLargeStock() {
        var r = validator.validate(1, row("Gift Card", "GC-025", "d", "", "25.00", "99999", "0"));
        assertThat(r.isValid()).isTrue();
        assertThat(r.row().stock()).isEqualTo(99999);
        assertThat(r.row().category()).isNull(); // blank category allowed
    }

    @Test
    void stripsXssTagsFromName() {
        var r = validator.validate(1, row("<script>alert('xss')</script>", "XS-001", "d", "Electronics", "19.99", "100", "0.1"));
        assertThat(r.isValid()).isTrue();
        assertThat(r.row().name()).doesNotContain("<script>").doesNotContain("</script>");
    }

    @Test
    void acceptsSqlInjectionStringAsPlainData() {
        // Parameterised writes make this safe; the validator just treats it as text.
        var r = validator.validate(1, row("Robert'); DROP TABLE products;--", "SQL-001", "d", "Games", "9.99", "50", "0.5"));
        assertThat(r.isValid()).isTrue();
        assertThat(r.row().sku()).isEqualTo("SQL-001");
    }

    @Test
    void rejectsMissingName() {
        var r = validator.validate(1, row(null, "HD-099", "d", "Electronics", "149.99", "30", "0.25"));
        assertThat(r.isValid()).isFalse();
        assertThat(r.error().field()).isEqualTo("name");
    }

    @Test
    void rejectsWhitespaceOnlyName() {
        var r = validator.validate(1, row("     ", "WS-001", "d", "Misc", "5.00", "10", "0.1"));
        assertThat(r.isValid()).isFalse();
        assertThat(r.error().field()).isEqualTo("name");
    }

    @Test
    void rejectsMissingSku() {
        var r = validator.validate(1, row("No SKU", "  ", "d", "Misc", "5.00", "10", "0.1"));
        assertThat(r.isValid()).isFalse();
        assertThat(r.error().field()).isEqualTo("sku");
    }

    @Test
    void rejectsMissingStock() {
        var r = validator.validate(1, row("Gaming Keyboard", "GK-088", "d", "Electronics", "129.99", "", null));
        assertThat(r.isValid()).isFalse();
        assertThat(r.error().field()).isEqualTo("stock");
    }

    @Test
    void capturesOptionalImageUrlWhenPresent() {
        Map<String, String> m = row("Camera", "CAM-1", "d", "Electronics", "499.00", "12", "0.6");
        m.put("image_url", "https://example.com/cam.jpg");
        var r = validator.validate(1, m);
        assertThat(r.isValid()).isTrue();
        assertThat(r.row().imageUrl()).isEqualTo("https://example.com/cam.jpg");
    }

    @Test
    void imageUrlIsNullWhenColumnAbsent() {
        var r = validator.validate(1, row("Camera", "CAM-2", "d", "Electronics", "499.00", "12", "0.6"));
        assertThat(r.isValid()).isTrue();
        assertThat(r.row().imageUrl()).isNull();
    }
}
