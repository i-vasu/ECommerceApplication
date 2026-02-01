package com.app.legal.admin.views;

import com.app.legal.entities.LegalAgreement;
import com.app.legal.services.LegalService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route("admin/legal")
@RolesAllowed("ADMIN")
public class AdminLegalView extends VerticalLayout {

    private final LegalService legalService;
    private final Grid<LegalAgreement> grid = new Grid<>(LegalAgreement.class);
    
    private final TextField typeField = new TextField("Type (e.g. TERMS_OF_SERVICE)");
    private final TextField titleField = new TextField("Title");
    private final TextField versionField = new TextField("Version");
    private final Checkbox activeBox = new Checkbox("Active");
    private final TextArea contentArea = new TextArea("Content");

    public AdminLegalView(LegalService legalService) {
        this.legalService = legalService;
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        add(new H1("Legal & Compliance Management"));
        add(new com.vaadin.flow.component.html.Span("Manage legal agreements, terms of service and privacy policies."));

        configureGrid();
        add(new H2("Existing Agreements"), grid);
        
        showForm();
    }

    private void configureGrid() {
        grid.setColumns("type", "title", "version", "active", "updatedAt");
        grid.setItems(legalService.getAllAgreements());
        grid.asSingleSelect().addValueChangeListener(event -> populateForm(event.getValue()));
    }

    private void showForm() {
        add(new H2("Create/Edit Agreement"));
        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        
        contentArea.setWidthFull();
        contentArea.setHeight("300px");
        
        Button saveBtn = new Button("Save Agreement", e -> {
            LegalAgreement agreement = grid.asSingleSelect().getValue();
            if (agreement == null) agreement = new LegalAgreement();
            
            agreement.setType(typeField.getValue());
            agreement.setTitle(titleField.getValue());
            agreement.setVersion(versionField.getValue());
            agreement.setActive(activeBox.getValue());
            agreement.setContent(contentArea.getValue());
            
            legalService.saveAgreement(agreement);
            Notification.show("Agreement saved successfully!");
            grid.setItems(legalService.getAllAgreements());
            clearForm();
        });
        
        form.add(typeField, titleField, versionField, activeBox, contentArea, saveBtn);
        add(form);
    }

    private void populateForm(LegalAgreement agreement) {
        if (agreement == null) {
            clearForm();
            return;
        }
        typeField.setValue(agreement.getType());
        titleField.setValue(agreement.getTitle());
        versionField.setValue(agreement.getVersion());
        activeBox.setValue(agreement.isActive());
        contentArea.setValue(agreement.getContent());
    }

    private void clearForm() {
        typeField.clear();
        titleField.clear();
        versionField.clear();
        activeBox.clear();
        contentArea.clear();
    }
}
