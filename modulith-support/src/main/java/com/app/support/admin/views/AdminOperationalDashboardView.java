package com.app.support.admin.views;

import com.app.governance.audit.OperationalAudit;
import com.app.governance.audit.OperationalAuditRepo;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.format.DateTimeFormatter;

@Route(value = "admin/operational-dashboard")
@PageTitle("Operational Intelligence | Vasu Admin")
@RolesAllowed("ADMIN")
@Log4j2
public class AdminOperationalDashboardView extends VerticalLayout {

    private final OperationalAuditRepo auditRepo;
    private final Grid<OperationalAudit> auditGrid = new Grid<>(OperationalAudit.class, false);

    public AdminOperationalDashboardView(OperationalAuditRepo auditRepo) {
        this.auditRepo = auditRepo;

        addClassName("admin-operational-dashboard-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Operational Intelligence Dashboard");
        title.getStyle().set("margin-top", "0");

        HorizontalLayout statsRow = createStatsRow();

        configureGrid();

        add(title, statsRow, auditGrid);

        refreshData();
    }

    private HorizontalLayout createStatsRow() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();

        layout.add(createStatCard("Total Automations", "1,284", VaadinIcon.AUTOMATION, "text-blue-600"));
        layout.add(createStatCard("Fraud Intercepts", "12", VaadinIcon.SHIELD, "text-red-600"));
        layout.add(createStatCard("Revenue Recovered", "₹45,200", VaadinIcon.MONEY, "text-green-600"));
        layout.add(createStatCard("Inventory Syncs", "892", VaadinIcon.PACKAGE, "text-purple-600"));

        return layout;
    }

    private VerticalLayout createStatCard(String label, String value, VaadinIcon icon, String colorClass) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.getStyle().set("background", "white")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)")
                .set("border", "1px solid #eee");

        HorizontalLayout header = new HorizontalLayout();
        Icon iconC = icon.create();
        header.add(iconC, new Span(label));
        header.setAlignItems(Alignment.CENTER);

        Span val = new Span(value);
        val.getStyle().set("font-size", "24px").set("font-weight", "bold");

        card.add(header, val);
        return card;
    }

    private void configureGrid() {
        auditGrid.addColumn(audit -> audit.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .setHeader("Time").setFlexGrow(0).setWidth("100px");

        auditGrid.addColumn(OperationalAudit::getType).setHeader("Type").setFlexGrow(0).setWidth("150px");
        auditGrid.addColumn(OperationalAudit::getCategory).setHeader("Module").setFlexGrow(0).setWidth("120px");

        auditGrid.addComponentColumn(audit -> {
            Span span = new Span(audit.getDetail());
            if (audit.getDetail().contains("->")) {
                span.getStyle().set("font-family", "monospace").set("color", "#2563eb");
            }
            return span;
        }).setHeader("Operational Detail").setFlexGrow(1);

        auditGrid.addComponentColumn(audit -> {
            Span span = new Span(audit.isSuccess() ? "SUCCESS" : "FAILED");
            span.getStyle().set("padding", "2px 8px").set("border-radius", "4px")
                    .set("font-size", "12px").set("font-weight", "bold")
                    .set("color", "white")
                    .set("background", audit.isSuccess() ? "#059669" : "#dc2626");
            return span;
        }).setHeader("Status").setFlexGrow(0).setWidth("110px");

        auditGrid.setSizeFull();
        auditGrid.getStyle().set("border-radius", "12px").set("border", "1px solid #eee");
    }

    @Scheduled(fixedRate = 5000)
    public void refreshData() {
        getUI().ifPresent(ui -> ui.access(() -> {
            auditGrid.setItems(auditRepo.findTop50ByOrderByTimestampDesc());
        }));
    }
}
