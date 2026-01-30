package com.app.support.admin.views;

import com.app.core.logging.MemoryAppender;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.StreamResource;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Command;
import jakarta.annotation.security.RolesAllowed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Route("admin/tech-monitoring")
@RolesAllowed("ADMIN")
public class AdminTechDashboardView extends VerticalLayout {

    private final MeterRegistry meterRegistry;
    private final com.app.core.logging.JfrProfilingService jfrService;
    private final Pre logArea = new Pre();
    private final Div metricsContainer = new Div();

    public AdminTechDashboardView(MeterRegistry meterRegistry, com.app.core.logging.JfrProfilingService jfrService) {
        this.meterRegistry = meterRegistry;
        this.jfrService = jfrService;
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        add(new H1("Technical Monitoring (Grafana Replacement)"));

        // --- Performance Lab (JFR) ---
        add(new H2("Performance Lab (JFR Profiling)"));
        HorizontalLayout jfrControls = new HorizontalLayout();
        com.vaadin.flow.component.textfield.NumberField duration = new com.vaadin.flow.component.textfield.NumberField(
                "Duration (seconds)");
        duration.setValue(30.0);
        Button startJfr = new Button("Record Performance Flight", e -> {
            try {
                jfrService.startRecording(duration.getValue().intValue());
                com.vaadin.flow.component.notification.Notification
                        .show("JFR Recording Started. File will be in diagnostics/jfr/");
            } catch (Exception ex) {
                com.vaadin.flow.component.notification.Notification.show("Failed to start JFR: " + ex.getMessage());
            }
        });
        startJfr.getStyle().set("background", "#ec4899").set("color", "white");
        jfrControls.add(duration, startJfr);
        jfrControls.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        add(jfrControls);

        // --- Metrics Overview ---
        add(new H2("System Health"));
        metricsContainer.setWidthFull();
        metricsContainer.getStyle().set("display", "grid");
        metricsContainer.getStyle().set("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))");
        metricsContainer.getStyle().set("gap", "20px");
        add(metricsContainer);

        // --- Log Viewer ---
        add(new H2("Live Application Logs (Loki Replacement)"));
        logArea.getStyle().set("background", "#1e293b");
        logArea.getStyle().set("color", "#f8fafc");
        logArea.getStyle().set("padding", "15px");
        logArea.getStyle().set("border-radius", "8px");
        logArea.getStyle().set("font-family", "monospace");
        logArea.getStyle().set("font-size", "12px");
        logArea.getStyle().set("overflow", "auto");
        logArea.setHeight("400px");
        logArea.setWidthFull();
        add(logArea);

        // --- Disk Log Actions ---
        Anchor downloadLogs = new Anchor(new StreamResource("vaabhi.log", () -> {
            try {
                return new FileInputStream("logs/vaabhi.log");
            } catch (Exception e) {
                return new java.io.ByteArrayInputStream("Log file not found".getBytes());
            }
        }), "Download Full Logs (from Disk)");
        downloadLogs.getStyle().set("margin-top", "10px");
        add(downloadLogs);

        refreshData();
    }

    @Scheduled(fixedDelay = 2000)
    public void refreshData() {
        getUI().ifPresent(ui -> ui.access(() -> {
            updateMetrics();
            updateLogs();
        }));
    }

    private void updateMetrics() {
        metricsContainer.removeAll();

        double cpu = meterRegistry.get("system.cpu.usage").gauge().value() * 100;
        double memUsed = (meterRegistry.get("jvm.memory.used").gauge().value() / 1024 / 1024);
        double memMax = (meterRegistry.get("jvm.memory.max").gauge().value() / 1024 / 1024);
        double threads = meterRegistry.get("jvm.threads.live").gauge().value();

        metricsContainer.add(createMetricCard("CPU Usage", String.format("%.2f%%", cpu), "#7c3aed"));
        metricsContainer
                .add(createMetricCard("JVM Heap Used", String.format("%.0f MB / %.0f MB", memUsed, memMax), "#2563eb"));
        metricsContainer.add(createMetricCard("Active Threads", String.format("%.0f", threads), "#059669"));
        metricsContainer.add(createMetricCard("DB Connections", getDbConnectionStats(), "#db2777"));

        // --- Business Metrics ---
        metricsContainer.add(createMetricCard("Checkout Revenue", getCheckoutRevenue(), "#f59e0b"));
        metricsContainer.add(createMetricCard("Pending Journeys", getPendingJourneys(), "#8b5cf6"));
    }

    private String getCheckoutRevenue() {
        try {
            return String.format("$%.2f", meterRegistry.get("checkout.revenue").summary().totalAmount());
        } catch (Exception e) {
            return "$0.00";
        }
    }

    private String getPendingJourneys() {
        try {
            return String.format("%.0f steps", meterRegistry.get("marketing.workflow.pending").gauge().value());
        } catch (Exception e) {
            return "0";
        }
    }

    private void updateLogs() {
        String logs = String.join("", MemoryAppender.getRecentLogs());
        logArea.setText(logs);
        // Auto-scroll to bottom would be nice but simple setText is fine for now
    }

    private String getDbConnectionStats() {
        try {
            return String.format("%.0f active", meterRegistry.get("hikaricp.connections.active").gauge().value());
        } catch (Exception e) {
            return "N/A";
        }
    }

    private Div createMetricCard(String title, String value, String color) {
        Div card = new Div();
        card.getStyle().set("padding", "20px");
        card.getStyle().set("background", "white");
        card.getStyle().set("border-radius", "12px");
        card.getStyle().set("border-left", "5px solid " + color);
        card.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)");

        com.vaadin.flow.component.html.Span titleSpan = new com.vaadin.flow.component.html.Span(title);
        titleSpan.getStyle().set("font-size", "14px").set("color", "#64748b").set("display", "block");

        com.vaadin.flow.component.html.Span valueSpan = new com.vaadin.flow.component.html.Span(value);
        valueSpan.getStyle().set("font-size", "24px").set("font-weight", "bold").set("color", "#1e293b");

        card.add(titleSpan, valueSpan);
        return card;
    }
}
