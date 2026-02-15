package com.app.support.admin.views;

import com.app.security.UserService;
import com.app.security.payloads.UserDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;
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
@RolesAllowed({"ADMIN", "SUPPORT"})
public class AdminUserDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final UserService userService;
    private final com.app.core.contracts.EmailServiceContract emailService;
    private final com.app.support.repositories.CommunicationLogRepo communicationLogRepo;
    private Long userId;

    private final TextField firstName = new TextField("First Name");
    private final TextField lastName = new TextField("Last Name");
    private final TextField email = new TextField("Email");
    private final TextField mobileNumber = new TextField("Mobile Number");
    private final Span rewardPoints = new Span();

    public AdminUserDetailView(UserService userService, 
                               com.app.core.contracts.EmailServiceContract emailService,
                               com.app.support.repositories.CommunicationLogRepo communicationLogRepo) {
        this.userService = userService;
        this.emailService = emailService;
        this.communicationLogRepo = communicationLogRepo;
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

        Button sendEmailBtn = new Button("Send Custom Email", e -> openEmailDialog());
        sendEmailBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        com.vaadin.flow.component.tabs.Tabs tabs = new com.vaadin.flow.component.tabs.Tabs();
        com.vaadin.flow.component.tabs.Tab profileTab = new com.vaadin.flow.component.tabs.Tab("Customer Profile");
        com.vaadin.flow.component.tabs.Tab historyTab = new com.vaadin.flow.component.tabs.Tab("Communication History");
        tabs.add(profileTab, historyTab);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        showProfile(content, form, save, deactivate, sendEmailBtn);

        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            if (event.getSelectedTab().equals(profileTab)) {
                showProfile(content, form, save, deactivate, sendEmailBtn);
            } else {
                showHistory(content);
            }
        });

        add(new H1("Customer Management"), tabs, content);
    }

    private void showProfile(VerticalLayout content, FormLayout form, Button save, Button deactivate, Button sendEmailBtn) {
        content.add(new Span("Manage customer details and reward points"), form, 
            new HorizontalLayout(new Span("Current Rewards: "), rewardPoints),
            new HorizontalLayout(save, deactivate, sendEmailBtn));
    }

    private void showHistory(VerticalLayout content) {
        content.add(new com.vaadin.flow.component.html.H2("Past Communications"));
        Grid<com.app.support.entities.CommunicationLog> grid = new Grid<>(com.app.support.entities.CommunicationLog.class);
        grid.setItems(communicationLogRepo.findByRecipientEmailOrderByCreatedAtDesc(email.getValue()));
        grid.removeAllColumns();
        
        grid.addColumn(com.app.support.entities.CommunicationLog::getCreatedAt).setHeader("Date").setAutoWidth(true);
        grid.addColumn(com.app.support.entities.CommunicationLog::getSubject).setHeader("Subject").setAutoWidth(true);
        grid.addColumn(com.app.support.entities.CommunicationLog::getType).setHeader("Type");
        grid.addColumn(com.app.support.entities.CommunicationLog::getStatus).setHeader("Status");
        
        grid.setItemDetailsRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(log -> {
            VerticalLayout detail = new VerticalLayout();
            detail.add(new Span("Message Body:"));
            detail.add(new com.vaadin.flow.component.html.Pre(log.getBody()));
            return detail;
        }));

        content.add(grid);
    }

    private void openEmailDialog() {
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.setHeaderTitle("Compose Custom Email to " + email.getValue());

        TextField subject = new TextField("Subject");
        subject.setWidthFull();
        com.vaadin.flow.component.textfield.TextArea body = new com.vaadin.flow.component.textfield.TextArea("Message Content");
        body.setWidthFull();
        body.setHeight("250px");

        Button send = new Button("Send Email", ev -> {
            try {
                emailService.sendEmail(email.getValue(), subject.getValue(), body.getValue());
                
                // Log the communication
                com.app.support.entities.CommunicationLog log = new com.app.support.entities.CommunicationLog();
                log.setRecipientEmail(email.getValue());
                log.setSubject(subject.getValue());
                log.setBody(body.getValue());
                log.setType("CUSTOM_EMAIL");
                log.setStatus("SENT");
                communicationLogRepo.save(log);

                Notification.show("Email sent and logged successfully!");
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Error sending email: " + ex.getMessage());
            }
        });
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button cancel = new Button("Cancel", ev -> dialog.close());

        dialog.add(new VerticalLayout(subject, body));
        dialog.getFooter().add(cancel, send);
        dialog.open();
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
