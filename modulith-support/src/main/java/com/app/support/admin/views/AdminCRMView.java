package com.app.support.admin.views;

import com.app.support.domain.CRMService;
import com.app.support.entities.CustomerProfile;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.HashMap;
import java.util.Map;

@Route(value = "admin/crm-native", layout = AdminMainLayout.class)
@PageTitle("CRM & Loyalty | Vasu Admin")
@RolesAllowed({"ADMIN", "SUPPORT"})
public class AdminCRMView extends VerticalLayout {

    private final CRMService crmService;
    private final TextField emailSearch = new TextField("Customer Email");
    private final VerticalLayout profileLayout = new VerticalLayout();
    
    // Profile Fields
    private final Span tierBadge = new Span();
    private final IntegerField pointsField = new IntegerField("Reward Points");
    private final TextField heightField = new TextField("Height (cm)");
    private final TextField chestField = new TextField("Chest (cm)");
    private final TextField waistField = new TextField("Waist (cm)");

    public AdminCRMView(CRMService crmService) {
        this.crmService = crmService;
        setSpacing(true);
        setPadding(true);

        add(new H1("Customer Relationship Management"));
        
        HorizontalLayout searchBar = new HorizontalLayout(emailSearch, new Button("Load Profile", e -> loadProfile()));
        searchBar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        add(searchBar, profileLayout);
        
        profileLayout.setVisible(false);
    }

    private void loadProfile() {
        String email = emailSearch.getValue();
        if (email == null || email.isEmpty()) return;
        
        CustomerProfile profile = crmService.getProfile(email);
        renderProfile(profile);
    }

    private void renderProfile(CustomerProfile profile) {
        profileLayout.removeAll();
        profileLayout.setVisible(true);
        
        profileLayout.add(new H2("Profile for " + profile.getEmail()));
        
        tierBadge.setText(profile.getTier().name());
        tierBadge.getElement().getThemeList().add("badge");
        if (profile.getTier() == CustomerProfile.CustomerTier.PLATINUM) tierBadge.getElement().getThemeList().add("success");
        else if (profile.getTier() == CustomerProfile.CustomerTier.GOLD) tierBadge.getElement().getThemeList().add("contrast");
        
        pointsField.setValue(profile.getRewardPoints());
        
        Map<String, Object> measurements = profile.getMeasurements();
        if (measurements == null) measurements = new HashMap<>();
        
        heightField.setValue(String.valueOf(measurements.getOrDefault("height", "")));
        chestField.setValue(String.valueOf(measurements.getOrDefault("chest", "")));
        waistField.setValue(String.valueOf(measurements.getOrDefault("waist", "")));
        
        FormLayout form = new FormLayout();
        form.add(new HorizontalLayout(new Span("Current Tier: "), tierBadge), pointsField, heightField, chestField, waistField);
        
        Button save = new Button("Update CRM Data", e -> saveProfile(profile.getEmail()));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        profileLayout.add(form, save);
    }

    private void saveProfile(String email) {
        try {
            Map<String, Object> measurements = new HashMap<>();
            measurements.put("height", heightField.getValue());
            measurements.put("chest", chestField.getValue());
            measurements.put("waist", waistField.getValue());
            
            crmService.updateMeasurements(email, measurements);
            // Points update could be separate or here
            Notification.show("Profile updated");
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage());
        }
    }
}
