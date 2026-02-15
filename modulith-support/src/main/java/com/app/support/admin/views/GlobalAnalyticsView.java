package com.app.support.admin.views;

import com.app.governance.audit.OperationalAudit;
import com.app.governance.audit.OperationalAuditRepo;
import com.app.governance.rules.RuleEngineService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "admin/analytics-native", layout = AdminMainLayout.class)
@PageTitle("Global Intelligence & Analytics | Vaabhi Admin")
@RolesAllowed("ADMIN")
public class GlobalAnalyticsView extends VerticalLayout {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalAnalyticsView.class);

    private final OperationalAuditRepo auditRepo;
    private final RuleEngineService ruleEngine;
    private final com.app.governance.services.ProcessLatencyService latencyService;
    private final com.app.core.services.FunnelAnalyticsService funnelService;
    private final com.app.marketing.services.CustomerInsightService insightService;
    private final Grid<OperationalAudit> auditGrid = new Grid<>(OperationalAudit.class, false);
    private final TextField queryField = new TextField("Natural Intelligence Query (SpEL)");
    private final HorizontalLayout slaBar = new HorizontalLayout();

    public GlobalAnalyticsView(OperationalAuditRepo auditRepo, RuleEngineService ruleEngine,
            com.app.governance.services.ProcessLatencyService latencyService,
            com.app.core.services.FunnelAnalyticsService funnelService,
            com.app.marketing.services.CustomerInsightService insightService) {
        this.auditRepo = auditRepo;
        this.ruleEngine = ruleEngine;
        this.latencyService = latencyService;
        this.funnelService = funnelService;
        this.insightService = insightService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Global Operational Analytics");
        Span subtitle = new Span("Query live automation events using high-level business expressions.");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        configureQueryBar();
        configureSlaBar();
        configureGrid();

        add(title, subtitle, slaBar, queryField, auditGrid);

        refreshData("");
    }

    private void configureQueryBar() {
        queryField.setPlaceholder("e.g. category == 'Order' && success == false");
        queryField.setWidthFull();
        queryField.setClearButtonVisible(true);
        queryField.setPrefixComponent(VaadinIcon.SEARCH.create());

        queryField.addValueChangeListener(e -> refreshData(e.getValue()));

        Button runBtn = new Button("Run Analytics", VaadinIcon.PLAY.create(), e -> refreshData(queryField.getValue()));
        runBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout bar = new HorizontalLayout(queryField, runBtn);
        bar.setWidthFull();
        bar.setAlignItems(Alignment.BASELINE);

        // Example chips
        HorizontalLayout chips = new HorizontalLayout();
        chips.add(createChip("Failed Orders", "category == 'Order' && success == false"));
        chips.add(createChip("Fraud Blocks", "detail.contains('Blocked')"));
        chips.add(createChip("Returns", "category == 'Return'"));
        chips.add(createChip("Support Escalations", "category == 'Ticket' && detail.contains('ESCALATED')"));
        chips.add(createChip("High Value (>₹50k)", "category == 'Order' && detail.contains('PLACE')"));

        add(bar, chips);
    }

    private void configureSlaBar() {
        slaBar.setWidthFull();
        slaBar.setPadding(true);
        slaBar.getStyle().set("background", "#f8fafc").set("border-radius", "12px").set("border", "1px solid #e2e8f0");

        updateSlaMetrics();
    }

    private void updateSlaMetrics() {
        slaBar.removeAll();
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(7);

        slaBar.add(createSlaCard("Order Lead Time", latencyService.calculateAverageLeadTime("Order", since)));
        slaBar.add(createSlaCard("Shipment Velocity", latencyService.calculateAverageLeadTime("Shipment", since)));

        // Funnel Insight
        Map<String, Object> funnel = funnelService.getConversionFunnel();
        double convRate = (double) funnel.getOrDefault("cartToOrderRate", 0.0);
        slaBar.add(createValCard("Cart Conversion", String.format("%.1f%%", convRate * 100)));

        // Marketing Insight
        Map<String, Object> segments = insightService.getSegmentStats();
        Object vipCount = segments.getOrDefault("VIP", 0L);
        slaBar.add(createValCard("VIP Customers", vipCount.toString()));
    }

    private VerticalLayout createValCard(String title, String value) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);
        card.setAlignItems(Alignment.CENTER);
        Span t = new Span(title);
        t.getStyle().set("font-size", "0.8rem").set("color", "#64748b").set("font-weight", "600");
        Span v = new Span(value);
        v.getStyle().set("font-size", "1.5rem").set("font-weight", "bold").set("color", "#0f172a");
        card.add(t, v);
        return card;
    }

    private VerticalLayout createSlaCard(String title, Map<String, Double> data) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);
        card.setAlignItems(Alignment.CENTER);

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("font-size", "0.8rem").set("color", "#64748b").set("font-weight", "600");

        double avg = data.getOrDefault("averageMinutes", 0.0);
        String timeStr = avg > 60 ? String.format("%.1f hrs", avg / 60) : String.format("%.0f mins", avg);

        Span valSpan = new Span(timeStr);
        valSpan.getStyle().set("font-size", "1.5rem").set("font-weight", "bold").set("color", "#0f172a");

        card.add(titleSpan, valSpan);
        return card;
    }

    private Button createChip(String label, String formula) {
        Button chip = new Button(label, e -> queryField.setValue(formula));
        chip.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        chip.getStyle().set("background", "#f3f4f6").set("border-radius", "16px").set("color", "#374151");
        return chip;
    }

    private void configureGrid() {
        auditGrid.addColumn(OperationalAudit::getTimestamp).setHeader("Timestamp").setFlexGrow(0).setWidth("180px");
        auditGrid.addColumn(OperationalAudit::getType).setHeader("Type").setFlexGrow(0).setWidth("140px");
        auditGrid.addColumn(OperationalAudit::getCategory).setHeader("Module").setFlexGrow(0).setWidth("120px");
        auditGrid.addColumn(OperationalAudit::getDetail).setHeader("Contextual Detail").setFlexGrow(1);
        auditGrid.addColumn(OperationalAudit::getResult).setHeader("Result").setFlexGrow(0).setWidth("120px");

        auditGrid.setSizeFull();
        auditGrid.getStyle().set("border-radius", "12px");
    }

    private void refreshData(String query) {
        List<OperationalAudit> allAudits = auditRepo.findAll();

        if (query == null || query.isBlank()) {
            auditGrid.setItems(allAudits);
            return;
        }

        try {
            List<OperationalAudit> filtered = allAudits.stream().filter(audit -> {
                Map<String, Object> context = new HashMap<>();
                context.put("type", audit.getType());
                context.put("category", audit.getCategory());
                context.put("detail", audit.getDetail());
                context.put("success", audit.isSuccess());
                context.put("result", audit.getResult());

                return ruleEngine.evaluate(query, context);
            }).collect(Collectors.toList());

            auditGrid.setItems(filtered);
            Notification.show("Found " + filtered.size() + " matching records.", 3000,
                    Notification.Position.BOTTOM_END);
        } catch (Exception e) {
            log.error("Analytics query failed: {}", e.getMessage());
            Notification.show("Invalid query syntax: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }
}
