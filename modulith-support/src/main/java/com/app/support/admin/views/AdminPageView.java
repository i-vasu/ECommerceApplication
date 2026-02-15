package com.app.support.admin.views;

import com.app.support.domain.ContentPageService;
import com.app.support.entities.ContentPage;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/cms/pages", layout = AdminMainLayout.class)
@PageTitle("Page Manager | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminPageView extends VerticalLayout {

    private final ContentPageService pageService;
    private final Grid<ContentPage> grid = new Grid<>(ContentPage.class, false);

    public AdminPageView(ContentPageService pageService) {
        this.pageService = pageService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        HorizontalLayout header = new HorizontalLayout();
        H1 title = new H1("Static Pages");
        Button addBtn = new Button("Create Page", VaadinIcon.PLUS.create(), e -> openEditor(new ContentPage()));
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
        grid.addColumn(ContentPage::getTitle).setHeader("Title").setSortable(true).setFlexGrow(1);
        grid.addColumn(ContentPage::getSlug).setHeader("Slug").setWidth("150px");
        grid.addColumn(p -> p.isPublished() ? "Published" : "Draft").setHeader("Status");

        grid.addComponentColumn(page -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openEditor(page));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            
            Button viewBtn = new Button(VaadinIcon.EYE.create(), e -> {
                getUI().ifPresent(ui -> ui.getPage().open("/pages/" + page.getSlug(), "_blank"));
            });
            viewBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                pageService.deletePage(page.getId());
                updateList();
                Notification.show("Page deleted");
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

            return new HorizontalLayout(editBtn, viewBtn, deleteBtn);
        }).setHeader("Actions");
    }

    private void updateList() {
        grid.setItems(pageService.getAllPages());
    }

    private void openEditor(ContentPage page) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(page.getId() == null ? "New Page" : "Edit Page");
        dialog.setWidth("800px");
        dialog.setHeight("600px");

        TextField titleField = new TextField("Title");
        titleField.setValue(page.getTitle() != null ? page.getTitle() : "");
        titleField.setWidthFull();

        TextField slugField = new TextField("Slug (URL Path)");
        slugField.setValue(page.getSlug() != null ? page.getSlug() : "");
        slugField.setPlaceholder("e.g., about-us");
        slugField.setWidthFull();

        TextArea contentField = new TextArea("HTML Content");
        contentField.setValue(page.getContent() != null ? page.getContent() : "");
        contentField.setWidthFull();
        contentField.setHeight("300px"); // Taller editor
        contentField.setPlaceholder("Enter HTML content here...");

        Checkbox publishedField = new Checkbox("Published");
        publishedField.setValue(page.isPublished());

        VerticalLayout form = new VerticalLayout(titleField, slugField, contentField, publishedField);
        form.setPadding(false);
        form.setSizeFull();

        Button saveBtn = new Button("Save Page", e -> {
            page.setTitle(titleField.getValue());
            page.setSlug(slugField.getValue());
            page.setContent(contentField.getValue());
            page.setPublished(publishedField.getValue());

            if (page.getId() == null) {
                pageService.createPage(page);
            } else {
                pageService.updatePage(page.getId(), page);
            }
            updateList();
            dialog.close();
            Notification.show("Page saved successfully!");
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }
}
