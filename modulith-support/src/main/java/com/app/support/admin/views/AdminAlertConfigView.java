package com.app.support.admin.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import com.app.support.repositories.jdbc.MonitoringJdbcRepo;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Route("admin/alert-config")
@RolesAllowed("ADMIN")
public class AdminAlertConfigView extends VerticalLayout {

    private final MonitoringJdbcRepo monitoringRepo;

    public AdminAlertConfigView(MonitoringJdbcRepo monitoringRepo) {
        this.monitoringRepo = monitoringRepo;
        setSpacing(true);
        setPadding(true);

        add(new H1("Alert Configuration Center (Dynatrace Replacement)"));
        add(new com.vaadin.flow.component.html.Span(
                "Define thresholds for system health metrics to receive instant notifications."));

        showRuleList();
        showCreateForm();
        showHistory();
    }

    private void showRuleList() {
        add(new H2("Active Alert Rules"));
        Grid<Map<String, Object>> grid = new Grid<>();
        grid.setItems(monitoringRepo.getAlertRules());

        grid.addColumn(row -> row.get("name")).setHeader("Rule Name");
        grid.addColumn(row -> row.get("metric_name")).setHeader("Metric");
        grid.addColumn(row -> row.get("comparison_operator")).setHeader("Op");
        grid.addColumn(row -> row.get("threshold")).setHeader("Threshold");
        grid.addColumn(row -> row.get("last_triggered_at")).setHeader("Last Triggered");

        add(grid);
    }

    private void showCreateForm() {
        add(new H2("Create New Alert Rule"));
        VerticalLayout form = new VerticalLayout();

        TextField nameField = new TextField("Rule Name (e.g., High CPU)");
        TextField metricField = new TextField("Micrometer Metric ID");
        metricField.setPlaceholder("system.cpu.usage");

        ComboBox<String> opBox = new ComboBox<>("Comparison");
        opBox.setItems(">", "<", ">=", "<=");

        NumberField thresholdField = new NumberField("Threshold Value");

        Button saveBtn = new Button("Activate Rule", e -> {
            monitoringRepo.insertAlertRule(
                    nameField.getValue(), metricField.getValue(), thresholdField.getValue(), opBox.getValue());
            Notification.show("New monitoring alert active!");
            getUI().ifPresent(ui -> ui.refreshCurrentRoute(true));
        });

        form.add(nameField, metricField, opBox, thresholdField, saveBtn);
        add(form);
    }

    private void showHistory() {
        add(new H2("Recent Alert History"));
        Grid<Map<String, Object>> grid = new Grid<>();
        grid.setItems(monitoringRepo.getAlertHistory());

        grid.addColumn(row -> row.get("triggered_at")).setHeader("Time");
        grid.addColumn(row -> row.get("message")).setHeader("Alert Message");
        grid.addColumn(row -> row.get("actual_value")).setHeader("Violating Value");

        add(grid);
    }
}
