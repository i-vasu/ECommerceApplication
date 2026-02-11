package com.app.support.admin.views;

import com.app.security.UserService;
import com.app.security.payloads.UserDTO;
import com.app.security.payloads.UserResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * Vaadin View for User Management
 */
@Route(value = "admin/users-native", layout = AdminMainLayout.class)
@PageTitle("User Management | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminUserListView extends VerticalLayout {

    private final UserService userService;
    private final Grid<UserDTO> grid = new Grid<>(UserDTO.class, false);

    public AdminUserListView(UserService userService) {
        this.userService = userService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        add(new H1("User Management"), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        
        grid.addColumn(UserDTO::userId).setHeader("User ID").setSortable(true).setWidth("100px");
        grid.addColumn(UserDTO::email).setHeader("Email").setSortable(true).setFlexGrow(2);
        
        grid.addComponentColumn(u -> {
            String rolesStr = "USER";
            if (u.roles() != null && !u.roles().isEmpty()) {
                java.util.List<String> roleNames = new java.util.ArrayList<>();
                for (com.app.security.entities.Role r : u.roles()) {
                    roleNames.add(r.getRoleName().replace("ROLE_", ""));
                }
                rolesStr = String.join(", ", roleNames);
            }
            Span roleBadge = new Span(rolesStr);
            roleBadge.getElement().getThemeList().add("badge");
            if (rolesStr.contains("ADMIN")) {
                roleBadge.getElement().getThemeList().add("success");
            } else {
                roleBadge.getElement().getThemeList().add("contrast");
            }
            return roleBadge;
        }).setHeader("Role").setWidth("150px");

        grid.addComponentColumn(u -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> {
                getUI().ifPresent(ui -> ui.navigate(AdminUserDetailView.class, u.userId()));
            });
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button viewBtn = new Button("View History", VaadinIcon.USER_CLOCK.create(), e -> {
                com.vaadin.flow.component.notification.Notification.show("User profile and historical data for " + u.email() + " is ready for implementation.");
            });
            viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            
            return new HorizontalLayout(editBtn, viewBtn);
        }).setHeader("Actions").setWidth("150px");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private void updateList() {
        try {
            UserResponse response = userService.getAllUsers(0, 100, "userId", "asc");
            grid.setItems(response.getContent());
        } catch (Exception e) {
            com.vaadin.flow.component.notification.Notification.show("Error: " + e.getMessage());
        }
    }
}
