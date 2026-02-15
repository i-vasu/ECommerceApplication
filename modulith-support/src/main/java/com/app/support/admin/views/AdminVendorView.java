package com.app.support.admin.views;

import com.app.finance.payloads.VendorDTO;
import com.app.finance.services.VendorService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "admin/vendors", layout = AdminMainLayout.class)
@PageTitle("Vendor Master | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminVendorView extends VerticalLayout {

    private final VendorService vendorService;
    private final Grid<VendorDTO> grid = new Grid<>(VendorDTO.class, false);
    private final TextField filterText = new TextField();

    public AdminVendorView(VendorService vendorService) {
        this.vendorService = vendorService;
        
        addClassName("vendor-view");
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        add(new H1("Vendor Management Master"));
        
        add(getToolbar());
        configureGrid();
        
        add(grid);
        
        refreshGrid();
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());

        Button addVendorButton = new Button("Add Vendor", VaadinIcon.PLUS.create());
        addVendorButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addVendorButton.addClickListener(e -> Notification.show("Vendor Creation Dialog pending..."));

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addVendorButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void configureGrid() {
        grid.addColumn(VendorDTO::id).setHeader("ID").setWidth("70px").setFlexGrow(0);
        grid.addColumn(VendorDTO::name).setHeader("Business Name").setSortable(true).setFlexGrow(2);
        grid.addColumn(VendorDTO::category).setHeader("Category").setSortable(true).setFlexGrow(1);
        grid.addColumn(VendorDTO::gstin).setHeader("GSTIN").setFlexGrow(1);
        grid.addColumn(VendorDTO::email).setHeader("Email").setFlexGrow(1);
        
        grid.addComponentColumn(vendor -> {
            Span status = new Span(vendor.active() ? "Active" : "Inactive");
            status.getElement().getThemeList().add(vendor.active() ? "badge success" : "badge error");
            return status;
        }).setHeader("Status");

        grid.addComponentColumn(vendor -> {
            Button toggleBtn = new Button(vendor.active() ? "Deactivate" : "Activate", e -> {
                vendorService.toggleVendorStatus(vendor.id());
                refreshGrid();
                Notification.show("Vendor status updated");
            });
            toggleBtn.addThemeVariants(vendor.active() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS);
            toggleBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            return toggleBtn;
        }).setHeader("Actions");

        grid.setSizeFull();
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                Notification.show("Editing " + event.getValue().name());
            }
        });
    }

    private void refreshGrid() {
        grid.setItems(vendorService.getAllVendors());
    }

    private void updateList() {
        String filter = filterText.getValue();
        if (filter == null || filter.isEmpty()) {
            refreshGrid();
        } else {
            List<VendorDTO> vendors = vendorService.getAllVendors().stream()
                    .filter(v -> v.name().toLowerCase().contains(filter.toLowerCase()))
                    .toList();
            grid.setItems(vendors);
        }
    }
}


class Span extends com.vaadin.flow.component.html.Span {
    public Span(String text) {
        super(text);
    }
}
