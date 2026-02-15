package com.app.catalog.sku;

import com.app.catalog.entities.GtsProperty;
import com.app.catalog.entities.GtsPropertyValue;
import com.app.catalog.entities.Product;
import com.app.catalog.entities.ProductVariant;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Deterministic SKU Generator for Global Textile Scheme (GTS) Matrix.
 * Format: [ARTICLE]-[COLOR]-[SIZE]-[PROP_SUFFIX]
 */
@Service
public class SKUGeneratorService {

    /**
     * Generates a unique, readable SKU for a variant based on GTS dimensions.
     */
    public String generateSKU(Product product, ProductVariant variant) {
        StringBuilder sku = new StringBuilder();

        // 1. Article Master Reference (Using simplified Category + Product ID)
        String genre = "GEN";
        if (product.getCategory() != null && product.getCategory().getCategoryName() != null) {
             genre = product.getCategory().getCategoryName().substring(0, Math.min(3, product.getCategory().getCategoryName().length())).toUpperCase();
        }
        sku.append(genre).append(product.getProductId());

        // 2. GTS Properties (Sorted by Property ID for deterministic output)
        if (variant.getGtsProperties() != null) {
            List<GtsPropertyValue> props = variant.getGtsProperties().stream()
                    .filter(v -> v.getProperty() != null)
                    .sorted(Comparator.comparing(v -> v.getProperty().getPropertyId()))
                    .toList();

            if (!props.isEmpty()) {
                sku.append("-");
                sku.append(props.stream()
                        .map(GtsPropertyValue::getCode)
                        .collect(Collectors.joining("-")));
            }
        }

        return sku.toString();
    }
}
