package com.app.support.admin.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "admin/tracing", layout = AdminMainLayout.class)
@RolesAllowed("ADMIN")
public class AdminTraceView extends VerticalLayout implements HasUrlParameter<String> {

    private final JdbcTemplate jdbcTemplate;
    private final Grid<Map<String, Object>> logGrid = new Grid<>();
    private final TextField traceIdField = new TextField("Trace ID Search");

    public AdminTraceView(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        setSpacing(true);
        setPadding(true);

        add(new H1("Distributed Trace Sequence Viewer (Jaeger Replacement)"));
        add(new com.vaadin.flow.component.html.Span(
                "Visualize the execution path of a single request across all modules."));

        HorizontalLayout searchBar = new HorizontalLayout();
        traceIdField.setPlaceholder("Enter Trace ID...");
        traceIdField.setWidth("400px");

        Button searchBtn = new Button("Search Sequence", e -> loadTrace(traceIdField.getValue()));
        searchBtn.getStyle().set("background", "#1e293b").set("color", "white");

        searchBar.add(traceIdField, searchBtn);
        searchBar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        add(searchBar);

        configureGrid();
        add(new H2("Request Execution Flow"), logGrid);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        if (parameter != null && !parameter.isEmpty()) {
            traceIdField.setValue(parameter);
            loadTrace(parameter);
        }
    }

    private void configureGrid() {
        logGrid.addColumn(row -> row.get("timestamp")).setHeader("Time").setAutoWidth(true);
        logGrid.addColumn(row -> row.get("level")).setHeader("Level").setAutoWidth(true);
        logGrid.addColumn(row -> row.get("logger")).setHeader("Module/Source").setAutoWidth(true);
        logGrid.addColumn(row -> row.get("message")).setHeader("Message").setResizable(true);
        logGrid.setHeight("600px");
    }

    private void loadTrace(String traceId) {
        if (traceId == null || traceId.trim().isEmpty())
            return;

        try {
            // High-performance disk grep: Read the log file and filter by Trace ID
            // This allows viewing INFO logs for a specific request without indexing
            // everything in DB.
            File logFile = new File("logs/vaabhi.log");
            if (!logFile.exists()) {
                Notification.show("Log file logs/vaabhi.log not found.");
                return;
            }

            List<Map<String, Object>> logs = Files.lines(Paths.get("logs/vaabhi.log"))
                    .filter(line -> line.contains(traceId))
                    .map(this::parseLogLine)
                    .collect(Collectors.toList());

            if (logs.isEmpty()) {
                Notification.show("No traces found for ID: " + traceId, 3000, Notification.Position.MIDDLE);
            }
            logGrid.setItems(logs);

        } catch (Exception e) {
            Notification.show("Error searching trace: " + e.getMessage());
        }
    }

    private Map<String, Object> parseLogLine(String line) {
        // Pattern: %d{HH:mm:ss.SSS} [%t] %-5level %logger{36} [%X{traceId}, %X{spanId}]
        // - %msg%n
        // Crude but effective parsing for the UI:
        Map<String, Object> map = new java.util.HashMap<>();
        try {
            String[] parts = line.split(" ", 5);
            map.put("timestamp", parts[0]);
            map.put("level", parts[2]);
            map.put("logger", parts[3]);
            map.put("message", parts[4]);
        } catch (Exception e) {
            map.put("message", line); // Fallback to raw line
        }
        return map;
    }
}
