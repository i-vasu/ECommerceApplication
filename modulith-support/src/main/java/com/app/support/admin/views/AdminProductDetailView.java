package com.app.support.admin.views;

import com.app.catalog.ProductService;
import com.app.catalog.entities.Product;
import com.app.catalog.payloads.ProductDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.Optional;

@Route(value = "admin/products/detail", layout = AdminMainLayout.class)
@PageTitle("Product Details | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminProductDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProductService productService;
    private final com.app.finance.services.TaxSyncService taxSyncService;
    private final com.app.catalog.MediaService mediaService;
    private Long productId;

    private final TextField name = new TextField("Product Name");
    private final TextField itemCode = new TextField("Item Code");
    private final TextArea description = new TextArea("Description");
    private final NumberField price = new NumberField("Price (₹)");
    private final NumberField discount = new NumberField("Discount (%)");
    private final NumberField quantity = new NumberField("Initial Stock");
    private final ComboBox<com.app.catalog.payloads.CategoryDTO> category = new ComboBox<>("Category");
    private final ComboBox<com.app.finance.entities.TaxRate> taxRate = new ComboBox<>("Tax Rate");
    
    // Image Upload
    private final com.vaadin.flow.component.html.Image imagePreview = new com.vaadin.flow.component.html.Image();
    private final com.vaadin.flow.component.upload.Upload upload;
    private String currentImageUrl;

    public AdminProductDetailView(ProductService productService, 
                                  com.app.finance.services.TaxSyncService taxSyncService,
                                  com.app.catalog.MediaService mediaService) {
        this.productService = productService;
        this.taxSyncService = taxSyncService;
        this.mediaService = mediaService;
        
        setSpacing(true);
        setPadding(true);

        // Image Upload Config
        com.vaadin.flow.component.upload.receivers.MemoryBuffer buffer = new com.vaadin.flow.component.upload.receivers.MemoryBuffer();
        upload = new com.vaadin.flow.component.upload.Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        upload.setMaxFileSize(5 * 1024 * 1024); // 5MB
        upload.setDropLabel(new com.vaadin.flow.component.html.Span("Upload Product Image"));
        
        upload.addSucceededListener(event -> {
            try {
                String url = mediaService.storeFile(buffer.getInputStream(), event.getFileName());
                currentImageUrl = url;
                imagePreview.setSrc(url);
                imagePreview.setVisible(true);
                Notification.show("Image uploaded successfully");
            } catch (Exception e) {
                Notification.show("Upload failed: " + e.getMessage());
            }
        });

        imagePreview.setMaxWidth("200px");
        imagePreview.setVisible(false);
        imagePreview.getStyle().set("border-radius", "8px").set("border", "1px solid #eee");

        FormLayout form = new FormLayout();
        form.add(name, itemCode, description, price, discount, quantity, category, taxRate);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
        
        category.setItemLabelGenerator(com.app.catalog.payloads.CategoryDTO::getCategoryName);
        
        taxRate.setItemLabelGenerator(rate -> rate.getTaxName() + " (" + rate.getPercentage() + "%)");

        try {
            java.util.List<com.app.catalog.payloads.CategoryDTO> categories = productService.getAllCategoryDetails();
            category.setItems(categories);
            
            java.util.List<com.app.finance.entities.TaxRate> rates = taxSyncService.getAllTaxRates();
            taxRate.setItems(rates);
        } catch (Exception e) {
            Notification.show("Failed to load data: " + e.getMessage());
        }

        form.setColspan(description, 2);

        Button save = new Button("Save Product", e -> saveProduct());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button delete = new Button("Delete", e -> deleteProduct());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        add(new H1("Edit Product"), 
            new HorizontalLayout(imagePreview, upload),
            form, 
            new HorizontalLayout(save, delete));
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.productId = parameter;
        if (productId != null && productId > 0) {
            ProductDTO dto = productService.getProductById(productId);
            name.setValue(dto.productName());
            itemCode.setValue(dto.itemCode());
            description.setValue(dto.description());
            price.setValue(dto.price().doubleValue());
            discount.setValue(dto.discount().doubleValue());
            quantity.setValue(dto.quantity().doubleValue());
            
            if (dto.taxRateId() != null) {
                taxSyncService.getAllTaxRates().stream()
                    .filter(r -> r.getId().equals(dto.taxRateId()))
                    .findFirst()
                    .ifPresent(taxRate::setValue);
            }
            
            if (dto.image() != null && !dto.image().isEmpty()) {
                currentImageUrl = dto.image();
                imagePreview.setSrc(dto.image());
                imagePreview.setVisible(true);
            }
        }
    }

    private void saveProduct() {
        try {
            java.math.BigDecimal priceVal = java.math.BigDecimal.valueOf(price.getValue());
            java.math.BigDecimal discountVal = java.math.BigDecimal.valueOf(discount.getValue());
            java.math.BigDecimal specialPriceVal = priceVal.subtract(discountVal);
            Long taxId = taxRate.getValue() != null ? taxRate.getValue().getId() : null;

            ProductDTO productDTO = new ProductDTO(
                productId,
                name.getValue(),
                itemCode.getValue(),
                currentImageUrl,
                description.getValue(),
                quantity.getValue().intValue(),
                priceVal,
                discountVal,
                specialPriceVal,
                null, null, null, 0.0, // variants, media, reviews, rating
                null, null, null, null, null, null, // pulse, scarcity, guide, story, notes, measurements
                taxId
            );
            
            if (productId != null && productId > 0) {
                productService.updateProduct(productId, productDTO);
                Notification.show("Product updated successfully");
            } else {
                productService.addProduct(category.getValue().getCategoryId(), productDTO);
                Notification.show("Product created successfully");
            }
            getUI().ifPresent(ui -> ui.navigate(AdminProductListView.class));
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage());
        }
    }

    private void deleteProduct() {
        try {
            productService.deleteProduct(productId);
            Notification.show("Product deleted");
            getUI().ifPresent(ui -> ui.navigate(AdminProductListView.class));
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage());
        }
    }
}
