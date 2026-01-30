package com.app.support.admin.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;

/**
 * Latency Distribution Heatmap (Native Visualizer).
 * Visualizes request latencies using Micrometer Histogram buckets.
 */
@Route("admin/latency-heatmap")
@RolesAllowed("ADMIN")
public class AdminLatencyHeatmapView extends VerticalLayout {

    private final MeterRegistry meterRegistry;

    public AdminLatencyHeatmapView(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        setSpacing(true);
        setPadding(true);

        add(new H1("Latency Distribution Heatmap"));
        add(new com.vaadin.flow.component.html.Span(
                "Real-time visual analysis of response time 'Buckets'. Useful for finding outliers."));

        renderMetricHeatmap("http.server.requests");
        renderMetricHeatmap("checkout.process");
        renderMetricHeatmap("ai.tagging.generate");
    }

    private void renderMetricHeatmap(String metricName) {
        add(new H2("Metric: " + metricName));

        try {
            Timer timer = meterRegistry.find(metricName).timer();
            if (timer == null) {
                add(new com.vaadin.flow.component.html.Span("No data recorded for this metric yet."));
                return;
            }

            HistogramSnapshot snapshot = timer.takeSnapshot();
            CountAtBucket[] buckets = snapshot.histogramCounts();

            if (buckets.length == 0) {
                add(new com.vaadin.flow.component.html.Span("Histogram not enabled or no buckets found."));
                return;
            }

            VerticalLayout chartArea = new VerticalLayout();
            chartArea.getStyle().set("background", "#f1f5f9").set("padding", "20px").set("border-radius", "8px");

            double maxCount = Arrays.stream(buckets).mapToDouble(CountAtBucket::count).max().orElse(1.0);

            for (CountAtBucket bucket : buckets) {
                double bucketLimitMs = bucket.bucket() / 1_000_000.0; // ns to ms
                double count = bucket.count();
                double percentage = (count / maxCount) * 100;

                HorizontalLayout barRow = new HorizontalLayout();
                barRow.setWidthFull();
                barRow.setAlignItems(Alignment.CENTER);

                com.vaadin.flow.component.html.Span bucketLabel = new com.vaadin.flow.component.html.Span(
                        String.format("< %.1f ms", bucketLimitMs));
                bucketLabel.setWidth("100px");
                bucketLabel.getStyle().set("font-size", "12px").set("color", "#475569");

                Div bar = new Div();
                bar.setHeight("20px");
                bar.setWidth(percentage + "%");
                bar.getStyle().set("background", getColorForPercentage(percentage));
                bar.getStyle().set("border-radius", "4px");
                bar.getStyle().set("min-width", "2px");

                com.vaadin.flow.component.html.Span countLabel = new com.vaadin.flow.component.html.Span(
                        String.format("%.0f", count));
                countLabel.getStyle().set("font-size", "12px").set("color", "#64748b");

                barRow.add(bucketLabel, bar, countLabel);
                chartArea.add(barRow);
            }
            add(chartArea);

        } catch (Exception e) {
            add(new com.vaadin.flow.component.html.Span("Error rendering heatmap: " + e.getMessage()));
        }
    }

    private String getColorForPercentage(double percentage) {
        if (percentage > 80)
            return "#ef4444"; // Red for high concentration in a bucket
        if (percentage > 50)
            return "#f59e0b"; // Orange
        if (percentage > 20)
            return "#3b82f6"; // Blue
        return "#10b981"; // Green
    }
}
