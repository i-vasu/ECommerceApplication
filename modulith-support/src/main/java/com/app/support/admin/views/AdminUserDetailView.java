package com.app.support.admin.views;

import com.app.security.UserService;
import com.app.security.payloads.UserDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/users/detail", layout = AdminMainLayout.class)
@PageTitle("User Details | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminUserDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final UserService userService;
    private Long userId;

    private final TextField firstName = new TextField("First Name");
    private final TextField lastName = new TextField("Last Name");
    private final TextField email = new TextField("Email");
    private final TextField mobileNumber = new TextField("Mobile Number");
    private final Span rewardPoints = new Span();

    public AdminUserDetailView(UserService userService) {
        this.userService = userService;
        setSpacing(true);
        setPadding(true);

        FormLayout form = new FormLayout();
        form.add(firstName, lastName, email, mobileNumber);
        
        email.setReadOnly(true);

        Button save = new Button("Update User", e -> saveUser());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button deactivate = new Button("Deactivate Account", e -> {
            userService.deactivateAccount(userId);
            Notification.show("Account deactivated");
        });
        deactivate.addThemeVariants(ButtonVariant.LUMO_ERROR);

        add(new H1("User Profile"), new Span("Manage customer details and reward points"), form, 
            new HorizontalLayout(new Span("Current Rewards: "), rewardPoints),
            new HorizontalLayout(save, deactivate));
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.userId = parameter;
        if (userId != null) {
            UserDTO user = userService.getUserById(userId);
            firstName.setValue(user.firstName());
            lastName.setValue(user.lastName());
            email.setValue(user.email());
            mobileNumber.setValue(user.mobileNumber());
            // Assuming reward points might be in a separate field or DTO extension if needed
            // rewardPoints.setText("Points: " + user.getRewardPoints()); 
        }
    }

    private void saveUser() {
        try {
            UserDTO current = userService.getUserById(userId);
            UserDTO updated = current.toBuilder()
                .firstName(firstName.getValue())
                .lastName(lastName.getValue())
                .mobileNumber(mobileNumber.getValue())
                .build();
            userService.updateUser(userId, updated);
            Notification.show("User profile updated");
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage());
        }
    }
}
