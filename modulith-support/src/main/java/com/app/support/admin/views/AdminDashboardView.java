package com.app.support.admin.views;

import com.app.support.repositories.jdbc.AnalyticsJdbcRepo;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Route(value = "admin/dashboard-native", layout = AdminMainLayout.class)
@RolesAllowed("ADMIN")
public class AdminDashboardView extends VerticalLayout {

        private final AnalyticsJdbcRepo analyticsRepo;

        public AdminDashboardView(AnalyticsJdbcRepo analyticsRepo) {
                this.analyticsRepo = analyticsRepo;
                setSpacing(true);
                setPadding(true);

                H1 title = new H1("Fashion Store Native Business Intelligence");
                title.getStyle().set("margin-bottom", "10px");
                add(title);

                // Summary Section
                addSummaryCards();

                // Tabs for different sections
                Tabs tabs = new Tabs();
                Tab salesTab = new Tab("Sales & Revenue");
                Tab catalogTab = new Tab("Catalog & Search");
                Tab customerTab = new Tab("Customer Insights");
                tabs.add(salesTab, catalogTab, customerTab);
                add(tabs);

                Div contentArea = new Div();
                contentArea.setWidthFull();
                add(contentArea);

                // Default content
                showSalesAnalytics(contentArea);

                tabs.addSelectedChangeListener(event -> {
                        contentArea.removeAll();
                        if (event.getSelectedTab().equals(salesTab)) {
                                showSalesAnalytics(contentArea);
                        } else if (event.getSelectedTab().equals(catalogTab)) {
                                showCatalogAnalytics(contentArea);
                        } else if (event.getSelectedTab().equals(customerTab)) {
                                showCustomerAnalytics(contentArea);
                        }
                });
        }

        private void showSalesAnalytics(Div container) {
                VerticalLayout layout = new VerticalLayout();

                // --- ADDING CHARTS ---
                H2 chartTitle = new H2("Revenue Trend (Last 7 Days)");
                layout.add(chartTitle);

                Div chartContainer = new Div();
                chartContainer.setId("revenue-chart-container");
                chartContainer.setWidthFull();
                chartContainer.setHeight("400px");
                chartContainer.getStyle().set("background", "white");
                chartContainer.getStyle().set("padding", "20px");
                chartContainer.getStyle().set("border-radius", "12px");
                chartContainer.getStyle().set("border", "1px solid #e2e8f0");

                Canvas canvas = new Canvas();
                canvas.setId("revenueChart");
                chartContainer.add(canvas);
                layout.add(chartContainer);

                // Fetch data for chart
                List<Map<String, Object>> chartData = analyticsRepo.getDailyRevenue();

                String labels = chartData.stream().map(m -> "'" + m.get("order_date").toString() + "'")
                                .collect(java.util.stream.Collectors.joining(","));
                String values = chartData.stream().map(m -> m.get("gross_revenue").toString())
                                .collect(java.util.stream.Collectors.joining(","));

                // --- ADDING FUNNEL CHART ---
                H2 funnelTitle = new H2("Conversion Funnel (Total)");
                layout.add(funnelTitle);

                Div funnelContainer = new Div();
                funnelContainer.setId("funnel-chart-container");
                funnelContainer.setWidthFull();
                funnelContainer.setHeight("300px");
                funnelContainer.getStyle().set("background", "white");
                funnelContainer.getStyle().set("padding", "20px");
                funnelContainer.getStyle().set("border-radius", "12px");
                funnelContainer.getStyle().set("border", "1px solid #e2e8f0");

                Canvas funnelCanvas = new Canvas();
                funnelCanvas.setId("funnelChart");
                funnelContainer.add(funnelCanvas);
                layout.add(funnelContainer);

                Map<String, Object> funnelData = analyticsRepo.getFunnelStats().get(0);
                String funnelValues = String.format("%s, %s, %s",
                                funnelData.get("total_searches"), funnelData.get("total_add_to_carts"),
                                funnelData.get("total_orders"));

                // Inject Chart.js and render both
                UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/chart.js");
                UI.getCurrent().getPage().executeJs(
                                "setTimeout(() => {" +
                                                "  const ctx = document.getElementById('revenueChart').getContext('2d');"
                                                +
                                                "  if (window.myChart) window.myChart.destroy();" +
                                                "  window.myChart = new Chart(ctx, {" +
                                                "    type: 'line'," +
                                                "    data: {" +
                                                "      labels: [" + labels + "]," +
                                                "      datasets: [{" +
                                                "        label: 'Gross Revenue (₹)'," +
                                                "        data: [" + values + "]," +
                                                "        borderColor: '#7c3aed'," +
                                                "        backgroundColor: 'rgba(124, 58, 237, 0.1)'," +
                                                "        fill: true," +
                                                "        tension: 0.4," +
                                                "        borderWidth: 3" +
                                                "      }]" +
                                                "    }," +
                                                "    options: { responsive: true, maintainAspectRatio: false }" +
                                                "  });" +
                                                "  const ctxF = document.getElementById('funnelChart').getContext('2d');"
                                                +
                                                "  if (window.myFunnel) window.myFunnel.destroy();" +
                                                "  window.myFunnel = new Chart(ctxF, {" +
                                                "    type: 'bar'," +
                                                "    data: {" +
                                                "      labels: ['Product Search', 'Add to Cart', 'Purchase']," +
                                                "      datasets: [{" +
                                                "        label: 'Volume'," +
                                                "        data: [" + funnelValues + "]," +
                                                "        backgroundColor: ['#fecaca', '#fca5a5', '#ef4444']" +
                                                "      }]" +
                                                "    }," +
                                                "    options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false }"
                                                +
                                                "  });" +
                                                "}, 500);");

                layout.add(new H2("Daily Revenue (Last 30 Days)"));
                Grid<Map<String, Object>> revenueGrid = new Grid<>();
                List<Map<String, Object>> revenueData = analyticsRepo.getDailyRevenueRecent();

                revenueGrid.addColumn(m -> m.get("order_date")).setHeader("Date").setAutoWidth(true);
                revenueGrid.addColumn(m -> m.get("total_orders")).setHeader("Orders").setAutoWidth(true);
                revenueGrid.addColumn(m -> String.format("₹%.2f", ((Number) m.get("gross_revenue")).doubleValue()))
                                .setHeader("Gross Revenue").setAutoWidth(true);
                revenueGrid.addColumn(m -> String.format("₹%.2f", ((Number) m.get("avg_order_value")).doubleValue()))
                                .setHeader("AOV").setAutoWidth(true);
                revenueGrid.setItems(revenueData);

                Anchor download = new Anchor(
                                new StreamResource("revenue_report.csv", () -> createCsvStream(revenueData)),
                                "Download Revenue CSV");
                layout.add(download);

                layout.add(revenueGrid);

                layout.add(new H2("Top Products by Revenue"));
                Grid<Map<String, Object>> topProductsGrid = new Grid<>();
                List<Map<String, Object>> topProductsData = analyticsRepo.getTopProducts();

                topProductsGrid.addColumn(m -> m.get("product_name")).setHeader("Product");
                topProductsGrid.addColumn(m -> m.get("total_units_sold")).setHeader("Units Sold");
                topProductsGrid.addColumn(m -> String.format("₹%.2f", ((Number) m.get("total_revenue")).doubleValue()))
                                .setHeader("Total Revenue");
                topProductsGrid.setItems(topProductsData);
                layout.add(topProductsGrid);

                container.add(layout);
        }

        private void showCatalogAnalytics(Div container) {
                VerticalLayout layout = new VerticalLayout();

                layout.add(new H2("Search Intent (Top Keywords)"));
                Grid<Map<String, Object>> searchGrid = new Grid<>();
                List<Map<String, Object>> searchData = analyticsRepo.getSearchPerformance();

                searchGrid.addColumn(m -> m.get("keyword")).setHeader("Keyword");
                searchGrid.addColumn(m -> m.get("search_count")).setHeader("Search Count");
                searchGrid.addColumn(m -> m.get("last_result_count")).setHeader("Recent Result Count");
                searchGrid.setItems(searchData);
                layout.add(searchGrid);

                layout.add(new H2("Inventory Health Alerts"));
                Grid<Map<String, Object>> inventoryGrid = new Grid<>();
                List<Map<String, Object>> inventoryData = analyticsRepo.getInventoryHealth();

                inventoryGrid.addColumn(m -> m.get("product_name")).setHeader("Product");
                inventoryGrid.addColumn(m -> m.get("current_stock")).setHeader("Current Stock");
                inventoryGrid.addColumn(m -> m.get("status")).setHeader("Status");
                inventoryGrid.setItems(inventoryData);
                layout.add(inventoryGrid);

                container.add(layout);
        }

        private void showCustomerAnalytics(Div container) {
                VerticalLayout layout = new VerticalLayout();

                layout.add(new H2("Daily Customer Growth"));
                Grid<Map<String, Object>> growthGrid = new Grid<>();
                List<Map<String, Object>> growthData = analyticsRepo.getUserGrowth();

                growthGrid.addColumn(m -> m.get("reg_date")).setHeader("Registration Date");
                growthGrid.addColumn(m -> m.get("new_users")).setHeader("New Users");
                growthGrid.setItems(growthData);
                layout.add(growthGrid);

                layout.add(new H2("Top Customers (LTV)"));
                Grid<Map<String, Object>> customerGrid = new Grid<>();
                List<Map<String, Object>> customerData = analyticsRepo.getUserActivity();

                customerGrid.addColumn(m -> m.get("email")).setHeader("Customer Email");
                customerGrid.addColumn(m -> m.get("order_count")).setHeader("Orders");
                customerGrid.addColumn(m -> String.format("₹%.2f", ((Number) m.get("lifetime_value")).doubleValue()))
                                .setHeader("LTV");
                customerGrid.setItems(customerData);
                layout.add(customerGrid);

                layout.add(new H2("Churn Risk (No orders > 60 days)"));
                Grid<Map<String, Object>> churnGrid = new Grid<>();
                List<Map<String, Object>> churnData = analyticsRepo.getChurnRisk();

                churnGrid.addColumn(m -> m.get("email")).setHeader("Email");
                churnGrid.addColumn(m -> m.get("last_order_date")).setHeader("Last Order");
                churnGrid.addColumn(m -> m.get("total_orders")).setHeader("Prev Orders");
                churnGrid.setItems(churnData);
                layout.add(churnGrid);

                container.add(layout);
        }

        private void addSummaryCards() {
                Div container = new Div();
                container.getStyle().set("display", "flex");
                container.getStyle().set("gap", "20px");
                container.getStyle().set("margin-bottom", "10px");

                Map<String, Object> totals = analyticsRepo.getSalesTotals();

                container.add(createCard("Total Net Sales",
                                String.format("₹%.2f", ((Number) totals.get("grand_total")).doubleValue()), "green"));
                container.add(createCard("Successful Orders", totals.get("total_count").toString(), "blue"));

                Map<String, Object> userCount = analyticsRepo.getUserCount();
                container.add(createCard("Total Customers", userCount.get("user_count").toString(), "purple"));

                Map<String, Object> productCount = analyticsRepo.getProductCount();
                container.add(createCard("Total Products", productCount.get("product_count").toString(), "orange"));

                add(container);
        }

        private java.io.InputStream createCsvStream(List<Map<String, Object>> data) {
                if (data.isEmpty())
                        return new ByteArrayInputStream("".getBytes());
                StringBuilder csv = new StringBuilder();
                // Headers
                csv.append(String.join(",", data.get(0).keySet())).append("\n");
                // Rows
                for (Map<String, Object> row : data) {
                        csv.append(row.values().stream().map(Object::toString)
                                        .collect(java.util.stream.Collectors.joining(",")))
                                        .append("\n");
                }
                return new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));
        }

        private Div createCard(String title, String value, String accentColor) {
                Div card = new Div();
                card.getStyle().set("padding", "20px");
                card.getStyle().set("border", "1px solid #e2e8f0");
                card.getStyle().set("border-left", "5px solid " + accentColor);
                card.getStyle().set("border-radius", "8px");
                card.getStyle().set("background", "white");
                card.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.05)");
                card.getStyle().set("min-width", "200px");

                Span titleSpan = new Span(title);
                titleSpan.getStyle().set("font-weight", "600");
                titleSpan.getStyle().set("color", "#64748b");
                titleSpan.getStyle().set("font-size", "0.9rem");
                titleSpan.getStyle().set("display", "block");

                Span valueSpan = new Span(value);
                valueSpan.getStyle().set("font-size", "1.75rem");
                valueSpan.getStyle().set("font-weight", "700");
                valueSpan.getStyle().set("color", "#1e293b");

                card.add(titleSpan, valueSpan);
                return card;
        }

        // Helper Canvas class
        @com.vaadin.flow.component.Tag("canvas")
        public static class Canvas extends com.vaadin.flow.component.HtmlComponent {
                public Canvas() {
                        super();
                }
        }
}
