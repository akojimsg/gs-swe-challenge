package com.gsswec.ecommerce.shared.constants;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreamNamesTest {

    private static List<String> declaredStreamValues() throws IllegalAccessException {
        List<String> values = new ArrayList<>();
        for (Field f : StreamNames.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                values.add((String) f.get(null));
            }
        }
        return values;
    }

    @Test
    void catalogueDeclaresAllFifteenStreams() throws IllegalAccessException {
        assertThat(declaredStreamValues()).hasSize(15);
    }

    @Test
    void streamNamesAreUniqueAndDotNamespaced() throws IllegalAccessException {
        List<String> values = declaredStreamValues();
        assertThat(values).doesNotHaveDuplicates();
        assertThat(values).allSatisfy(v -> assertThat(v).contains("."));
    }
}
