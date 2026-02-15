package com.app.support.admin.views;

import com.app.catalog.ProductService;
import com.app.catalog.entities.Product;
import com.app.catalog.payloads.CategoryDTO;
import com.app.core.utils.CsvUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Route(value = "admin/import-products", layout = AdminMainLayout.class)
@PageTitle("Bulk Import | Vasu Admin")
@RolesAllowed({"ADMIN", "OPERATOR"})
public class AdminProductImportView extends VerticalLayout {

    private final ProductService productService;
    private final List<Map<String, String>> importedData = new ArrayList<>();
    private final Grid<Map<String, String>> previewGrid = new Grid<>();

    public AdminProductImportView(ProductService productService) {
        this.productService = productService;
        setSizeFull();
        setPadding(true);

        add(new H1("Bulk Product Import (CSV)"));
        add(new Span("Download Template: productName, itemCode, description, quantity, price, discount, categoryName"));

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv");

        upload.addSucceededListener(event -> {
            try (InputStream inputStream = buffer.getInputStream()) {
                List<Map<String, String>> data = CsvUtils.parseCsv(inputStream);
                importedData.clear();
                importedData.addAll(data);
                updatePreview();
            } catch (Exception e) {
                Notification.show("Error parsing CSV: " + e.getMessage());
            }
        });

        Button processBtn = new Button("Process Import", e -> processImport());
        processBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        processBtn.setEnabled(false);

        add(upload, previewGrid, processBtn);
        
        previewGrid.setVisible(false);
        
        upload.addSucceededListener(e -> processBtn.setEnabled(!importedData.isEmpty()));
    }

    private void updatePreview() {
        previewGrid.removeAllColumns();
        if (importedData.isEmpty()) return;

        Map<String, String> firstRow = importedData.get(0);
        firstRow.keySet().forEach(key -> {
            previewGrid.addColumn(row -> row.get(key)).setHeader(key);
        });

        previewGrid.setItems(importedData);
        previewGrid.setVisible(true);
        previewGrid.setHeight("300px");
    }

    private void processImport() {
        int successCount = 0;
        int errorCount = 0;
        
        List<CategoryDTO> categories = productService.getAllCategoryDetails();

        for (Map<String, String> row : importedData) {
            try {
                String categoryName = row.get("categoryName");
                Optional<CategoryDTO> category = categories.stream()
                        .filter(c -> c.getCategoryName().equalsIgnoreCase(categoryName))
                        .findFirst();

                if (category.isEmpty()) {
                    throw new RuntimeException("Category not found: " + categoryName);
                }

                com.app.catalog.payloads.ProductDTO productDTO = new com.app.catalog.payloads.ProductDTO(
                        null,
                        row.get("productName"),
                        row.get("itemCode"),
                        null,
                        row.get("description"),
                        Integer.parseInt(row.get("quantity")),
                        new BigDecimal(row.get("price")),
                        new BigDecimal(row.getOrDefault("discount", "0")),
                        new BigDecimal(row.get("price")).subtract(new BigDecimal(row.getOrDefault("discount", "0"))),
                        null,
                        null,
                        null,
                        0.0);

                productService.addProduct(category.get().getCategoryId(), productDTO);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                System.err.println("Error importing row: " + row + " - " + e.getMessage());
            }
        }

        Notification.show("Import Complete! Success: " + successCount + ", Errors: " + errorCount);
        importedData.clear();
        previewGrid.setVisible(false);
    }
}
