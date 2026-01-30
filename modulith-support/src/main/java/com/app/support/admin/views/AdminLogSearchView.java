package com.app.support.admin.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@Route("admin/log-search")
@RolesAllowed("ADMIN")
public class AdminLogSearchView extends VerticalLayout {

    private final JdbcTemplate jdbcTemplate;
    private final Grid<Map<String, Object>> logGrid = new Grid<>();
    private final TextField queryField = new TextField("Search logs (BM25 Full-Text)");

    public AdminLogSearchView(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        add(new H1("ParadeDB Log Intelligence"));
        add(new Span(
                "Use full-text search to find specific errors, stack traces, or occurrences across your diagnostic logs."));

        HorizontalLayout searchBar = new HorizontalLayout();
        queryField.setPlaceholder("e.g. 'NullPointerException' OR 'Payment failed'...");
        queryField.setWidth("500px");

        Button searchBtn = new Button("Search with ParadeDB", e -> searchLogs(queryField.getValue()));
        searchBtn.getStyle().set("background", "#1e293b").set("color", "white");

        searchBar.add(queryField, searchBtn);
        searchBar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        add(searchBar);

        configureGrid();
        add(new H2("Diagnostic Results"), logGrid);
    }

    private void configureGrid() {
        logGrid.addColumn(row -> row.get("timestamp")).setHeader("Time").setAutoWidth(true);
        logGrid.addColumn(row -> row.get("level")).setHeader("Level").setAutoWidth(true);
        logGrid.addColumn(row -> row.get("logger")).setHeader("Module").setAutoWidth(true);
        logGrid.addColumn(row -> row.get("message")).setHeader("Message").setResizable(true);
        logGrid.addColumn(row -> row.get("trace_id")).setHeader("Trace ID").setAutoWidth(true);

        logGrid.setHeightFull();
    }

    private void searchLogs(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadRecentLogs();
            return;
        }

        try {
            // Leverage ParadeDB's pg_search syntax for high-performance BM25 search
            String sql = "SELECT timestamp, level, logger, message, trace_id " +
                    "FROM indexed_logs " +
                    "WHERE message @@@ ? " +
                    "ORDER BY timestamp DESC LIMIT 100";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, query);
            logGrid.setItems(results);
            Notification.show("Found " + results.size() + " matches using ParadeDB.");
        } catch (Exception e) {
            Notification.show("Search failed: Check ParadeDB index status.");
        }
    }

    private void loadRecentLogs() {
        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT timestamp, level, logger, message, trace_id FROM indexed_logs ORDER BY timestamp DESC LIMIT 50");
        logGrid.setItems(logs);
    }
}
