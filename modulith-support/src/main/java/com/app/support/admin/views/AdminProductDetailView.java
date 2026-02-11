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
    private Long productId;

    private final TextField name = new TextField("Product Name");
    private final TextField itemCode = new TextField("Item Code");
    private final TextArea description = new TextArea("Description");
    private final NumberField price = new NumberField("Price (₹)");
    private final NumberField discount = new NumberField("Discount (%)");
    private final NumberField quantity = new NumberField("Initial Stock");
    private final ComboBox<com.app.catalog.payloads.CategoryDTO> category = new ComboBox<>("Category");

    public AdminProductDetailView(ProductService productService) {
        this.productService = productService;
        setSpacing(true);
        setPadding(true);

        FormLayout form = new FormLayout();
        form.add(name, itemCode, description, price, discount, quantity, category);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
        
        category.setItemLabelGenerator(com.app.catalog.payloads.CategoryDTO::getCategoryName);
        try {
            category.setItems(productService.getAllCategoryDetails());
        } catch (Exception e) {
            Notification.show("Failed to load categories");
        }

        form.setColspan(description, 2);

        Button save = new Button("Save Product", e -> saveProduct());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button delete = new Button("Delete", e -> deleteProduct());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        add(new H1("Edit Product"), form, new HorizontalLayout(save, delete));
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
        }
    }

    private void saveProduct() {
        try {
            Product p = new Product();
            p.setProductName(name.getValue());
            p.setItemCode(itemCode.getValue());
            p.setDescription(description.getValue());
            p.setPrice(BigDecimal.valueOf(price.getValue()));
            p.setDiscount(BigDecimal.valueOf(discount.getValue()));
            p.setQuantity(quantity.getValue().intValue());
            
            if (productId != null && productId > 0) {
                productService.updateProduct(productId, p);
                Notification.show("Product updated successfully");
            } else {
                productService.addProduct(category.getValue().getCategoryId(), p);
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
