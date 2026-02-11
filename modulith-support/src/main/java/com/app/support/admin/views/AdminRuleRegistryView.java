package com.app.support.admin.views;

import com.app.governance.rules.RuleEngineService;
import com.app.governance.rules.SystemRule;
import com.app.governance.rules.SystemRuleRepo;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "admin/rules", layout = AdminMainLayout.class)
@PageTitle("Rule Registry | Vasu E-Commerce")
@RolesAllowed("ADMIN")
public class AdminRuleRegistryView extends VerticalLayout {

    private final SystemRuleRepo ruleRepo;
    private final RuleEngineService ruleEngine;
    private final Grid<SystemRule> grid = new Grid<>(SystemRule.class);

    public AdminRuleRegistryView(SystemRuleRepo ruleRepo, RuleEngineService ruleEngine) {
        this.ruleRepo = ruleRepo;
        this.ruleEngine = ruleEngine;

        setSizeFull();
        setPadding(true);

        // Header with search and cache clear
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);

        TextField search = new TextField();
        search.setPlaceholder("Filter by key or category...");
        search.setWidth("300px");
        search.addValueChangeListener(e -> refreshGrid(e.getValue()));

        Button clearCacheBtn = new Button("Flush Global Cache", VaadinIcon.REFRESH.create(), e -> {
            ruleEngine.clearCache();
            Notification.show("Rule Engine cache cleared! New rules will be fetched from DB.");
        });
        clearCacheBtn.addClassName("bg-red-50");

        Button addBtn = new Button("Add Rule", VaadinIcon.PLUS.create(), e -> {
            // Mock add for now
            Notification.show("Add functionality is in development.");
        });

        header.add(search, clearCacheBtn, addBtn);

        configureGrid();

        add(header, grid);
        refreshGrid("");
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setColumns("ruleKey", "category", "expression", "active");

        grid.getColumns().forEach(c -> {
            c.setAutoWidth(true);
            c.setResizable(true);
        });

        grid.addComponentColumn(rule -> {
            Button testBtn = new Button("Test", e -> {
                Notification.show("Evaluating Rule...");
            });
            return testBtn;
        });
    }

    private void refreshGrid(String filter) {
        List<SystemRule> rules = ruleRepo.findAll();
        if (filter != null && !filter.isEmpty()) {
            rules = rules.stream()
                    .filter(r -> r.getRuleKey().toLowerCase().contains(filter.toLowerCase()) ||
                            r.getCategory().toLowerCase().contains(filter.toLowerCase()))
                    .toList();
        }
        grid.setItems(rules);
    }
}
