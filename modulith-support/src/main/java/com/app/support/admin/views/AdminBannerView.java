package com.app.support.admin.views;

import com.app.marketing.entities.HeroBanner;
import com.app.marketing.services.BannerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/cms/banners", layout = AdminMainLayout.class)
@PageTitle("Banner Manager | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminBannerView extends VerticalLayout {

    private final BannerService bannerService;
    private final Grid<HeroBanner> grid = new Grid<>(HeroBanner.class, false);

    public AdminBannerView(BannerService bannerService) {
        this.bannerService = bannerService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        HorizontalLayout header = new HorizontalLayout();
        H1 title = new H1("Hero Banners");
        Button addBtn = new Button("Add Banner", VaadinIcon.PLUS.create(), e -> openEditor(new HeroBanner()));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        header.add(title, addBtn);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();

        configureGrid();
        add(header, grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(HeroBanner::getTitle).setHeader("Title").setSortable(true).setFlexGrow(1);
        grid.addColumn(HeroBanner::getDisplayOrder).setHeader("Order").setWidth("80px").setFlexGrow(0);
        grid.addColumn(b -> b.isActive() ? "Active" : "Hidden").setHeader("Status");
        
        grid.addComponentColumn(banner -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openEditor(banner));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                bannerService.deleteBanner(banner.getId());
                updateList();
                Notification.show("Banner deleted");
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Actions");
    }

    private void updateList() {
        grid.setItems(bannerService.getAllBanners());
    }

    private void openEditor(HeroBanner banner) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(banner.getId() == null ? "New Banner" : "Edit Banner");

        TextField titleField = new TextField("Title");
        titleField.setValue(banner.getTitle() != null ? banner.getTitle() : "");
        titleField.setWidthFull();

        TextField imageUrlField = new TextField("Image URL");
        imageUrlField.setValue(banner.getImageUrl() != null ? banner.getImageUrl() : "");
        imageUrlField.setPlaceholder("https://example.com/image.jpg");
        imageUrlField.setWidthFull();

        TextField targetUrlField = new TextField("Target URL (Link)");
        targetUrlField.setValue(banner.getTargetUrl() != null ? banner.getTargetUrl() : "");
        targetUrlField.setWidthFull();

        IntegerField orderField = new IntegerField("Display Order");
        orderField.setValue(banner.getDisplayOrder() != null ? banner.getDisplayOrder() : 1);
        
        Checkbox activeField = new Checkbox("Active");
        activeField.setValue(banner.isActive());

        VerticalLayout form = new VerticalLayout(titleField, imageUrlField, targetUrlField, orderField, activeField);
        form.setPadding(false);

        Button saveBtn = new Button("Save", e -> {
            banner.setTitle(titleField.getValue());
            banner.setImageUrl(imageUrlField.getValue());
            banner.setTargetUrl(targetUrlField.getValue());
            banner.setDisplayOrder(orderField.getValue());
            banner.setActive(activeField.getValue());

            if (banner.getId() == null) {
                bannerService.createBanner(banner);
            } else {
                bannerService.updateBanner(banner.getId(), banner);
            }
            updateList();
            dialog.close();
            Notification.show("Banner saved!");
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }
}
