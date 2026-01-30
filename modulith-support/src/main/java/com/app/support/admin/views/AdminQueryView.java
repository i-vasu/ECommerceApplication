package com.app.support.admin.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@Log4j2
@Route("admin/query-scratchpad")
@RolesAllowed("ADMIN")
public class AdminQueryView extends VerticalLayout {

    private final JdbcTemplate jdbcTemplate;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;
    private final VerticalLayout resultsLayout = new VerticalLayout();

    public AdminQueryView(JdbcTemplate jdbcTemplate,
            org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
        setSpacing(true);
        setPadding(true);

        add(new H1("SQL Analysis Scratchpad (Metabase Alternative)"));
        add(new com.vaadin.flow.component.html.Span(
                "Run ad-hoc SELECT queries against the analytics views or production tables."));

        TextArea sqlArea = new TextArea("SQL Query");
        sqlArea.setPlaceholder("SELECT * FROM view_daily_revenue LIMIT 10;");
        sqlArea.setWidthFull();
        sqlArea.setHeight("200px");
        sqlArea.getStyle().set("font-family", "monospace");

        Button runBtn = new Button("Execute Query", e -> executeQuery(sqlArea.getValue()));
        runBtn.getStyle().set("background", "#1e293b");
        runBtn.getStyle().set("color", "white");

        add(sqlArea, runBtn, resultsLayout);
    }

    private void executeQuery(String sql) {
        resultsLayout.removeAll();
        if (sql == null || sql.trim().isEmpty())
            return;

        String sanitizedSql = sql.trim().toUpperCase();
        if (!sanitizedSql.startsWith("SELECT")) {
            Notification.show("Security Error: Only SELECT queries are permitted in the scratchpad.", 5000,
                    Notification.Position.MIDDLE);
            return;
        }

        try {
            org.springframework.transaction.support.TransactionTemplate tt = new org.springframework.transaction.support.TransactionTemplate(
                    transactionManager);
            tt.setReadOnly(true);

            List<Map<String, Object>> results = tt.execute(status -> jdbcTemplate.queryForList(sql));

            if (results == null || results.isEmpty()) {
                resultsLayout.add(new com.vaadin.flow.component.html.Span(
                        "Query executed successfully but returned no results."));
                return;
            }

            resultsLayout.add(new H2("Query Results (" + results.size() + " rows)"));
            Grid<Map<String, Object>> grid = new Grid<>();

            // Dynamically add columns based on first row keys
            Map<String, Object> firstRow = results.get(0);
            for (String column : firstRow.keySet()) {
                grid.addColumn(row -> row.get(column)).setHeader(column).setAutoWidth(true).setSortable(true);
            }

            grid.setItems(results);
            grid.setHeight("500px");
            resultsLayout.add(grid);

        } catch (Exception e) {
            log.error("Ad-hoc query failed: {}", e.getMessage());
            Notification.show("SQL Error: " + e.getMessage(), 5000, Notification.Position.BOTTOM_END);
        }
    }
}
