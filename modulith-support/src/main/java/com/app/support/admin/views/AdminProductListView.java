package com.app.support.admin.views;

import com.app.catalog.ProductService;
import com.app.catalog.payloads.ProductDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * Vaadin View for Catalog Product Management
 */
@Route(value = "admin/products-native", layout = AdminMainLayout.class)
@PageTitle("Product Management | Vasu Admin")
@RolesAllowed({"ADMIN", "OPERATOR"})
public class AdminProductListView extends VerticalLayout {

    private final ProductService productService;
    private final Grid<ProductDTO> grid = new Grid<>(ProductDTO.class, false);
    private final TextField filterText = new TextField();

    public AdminProductListView(ProductService productService) {
        this.productService = productService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        add(new H1("Product Management"), getToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        
        grid.addComponentColumn(p -> {
            String imgUrl = p.image();
            if (imgUrl == null || imgUrl.isEmpty() || imgUrl.equals("default.png")) {
                imgUrl = "https://via.placeholder.com/50";
            }
            Image img = new Image(imgUrl, p.productName());
            img.setWidth("50px");
            img.setHeight("50px");
            img.getStyle().set("border-radius", "4px").set("object-fit", "cover");
            return img;
        }).setHeader("Preview").setFlexGrow(0).setWidth("80px");

        grid.addColumn(ProductDTO::productName).setHeader("Product Name").setSortable(true).setFlexGrow(2);
        grid.addColumn(ProductDTO::itemCode).setHeader("Item Code").setSortable(true).setWidth("120px");
        grid.addColumn(p -> "" + p.price()).setHeader("Price").setSortable(true).setWidth("100px");
        grid.addColumn(ProductDTO::quantity).setHeader("Units").setSortable(true).setWidth("80px");
        
        grid.addComponentColumn(p -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> {
                getUI().ifPresent(ui -> ui.navigate(AdminProductDetailView.class, p.productId()));
            });
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Delete Product?");
                dialog.setText("Are you sure you want to permanently delete " + p.productName() + "?");
                dialog.setCancelable(true);
                dialog.setConfirmText("Delete");
                dialog.setConfirmButtonTheme("error primary");
                dialog.addConfirmListener(ev -> {
                    productService.deleteProduct(p.productId());
                    updateList();
                });
                dialog.open();
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            
            actions.add(editBtn, deleteBtn);
            return actions;
        }).setHeader("Actions").setWidth("120px");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());

        Button addProductButton = new Button("Add Product", VaadinIcon.PLUS.create());
        addProductButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addProductButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(AdminProductDetailView.class)));

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addProductButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return toolbar;
    }

    private void updateList() {
        String filter = filterText.getValue();
        if (filter == null || filter.isEmpty()) {
            var response = productService.getAllProducts(0, 100, "productName", "asc");
            grid.setItems(response.content());
        } else {
            var response = productService.searchProductByKeyword(filter, 0, 100, "productName", "asc");
            grid.setItems(response.content());
        }
    }
}
