package com.app.support.admin.views;

import com.app.marketing.entities.MarketingCampaignSetting;
import com.app.marketing.repositories.MarketingCampaignSettingRepo;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Route("admin/marketing-native")
@RolesAllowed("ADMIN")
public class AdminMarketingView extends VerticalLayout {

    private final JdbcTemplate jdbcTemplate;
    private final MarketingCampaignSettingRepo settingRepo;

    public AdminMarketingView(JdbcTemplate jdbcTemplate, MarketingCampaignSettingRepo settingRepo) {
        this.jdbcTemplate = jdbcTemplate;
        this.settingRepo = settingRepo;

        setSpacing(true);
        setPadding(true);

        add(new H1("Automation & Marketing Control"));

        Tabs tabs = new Tabs();
        Tab campaignsTab = new Tab("Campaign Settings");
        Tab roiTab = new Tab("Performance & ROI");
        tabs.add(campaignsTab, roiTab);
        add(tabs);

        Div contentArea = new Div();
        contentArea.setWidthFull();
        add(contentArea);

        showCampaignSettings(contentArea);

        tabs.addSelectedChangeListener(event -> {
            contentArea.removeAll();
            if (event.getSelectedTab().equals(campaignsTab)) {
                showCampaignSettings(contentArea);
            } else if (event.getSelectedTab().equals(roiTab)) {
                showROIAnalytics(contentArea);
            }
        });
    }

    private void showCampaignSettings(Div container) {
        VerticalLayout layout = new VerticalLayout();
        layout.add(new H2("Active Automation Flows"));

        Grid<MarketingCampaignSetting> grid = new Grid<>(MarketingCampaignSetting.class);
        grid.setItems(settingRepo.findAll());
        grid.removeAllColumns();

        grid.addColumn(MarketingCampaignSetting::getCampaignName).setHeader("Campaign").setAutoWidth(true);
        grid.addComponentColumn(setting -> {
            Checkbox active = new Checkbox(setting.isActive());
            active.addValueChangeListener(e -> {
                setting.setActive(e.getValue());
                settingRepo.save(setting);
                Notification.show("Status updated for " + setting.getCampaignName());
            });
            return active;
        }).setHeader("Active");

        grid.addColumn(MarketingCampaignSetting::getSubjectLine).setHeader("Subject Line");
        grid.addColumn(MarketingCampaignSetting::getFrequencyCapDays).setHeader("Freq Cap (Days)");

        layout.add(grid);

        layout.add(new H2("Update Settings"));
        TextField subjectField = new TextField("Subject Line A");
        TextField subjectBField = new TextField("Subject Line B (A/B Test)");
        IntegerField freqField = new IntegerField("Frequency Cap (Days)");
        com.vaadin.flow.component.textfield.BigDecimalField minValField = new com.vaadin.flow.component.textfield.BigDecimalField(
                "Min Cart Value (₹)");

        Button saveBtn = new Button("Save Settings", e -> {
            var setting = settingRepo.findByCampaignName("ABANDONED_CART_RECOVERY")
                    .orElse(new MarketingCampaignSetting());
            setting.setCampaignName("ABANDONED_CART_RECOVERY");
            setting.setSubjectLine(subjectField.getValue());
            setting.setSubjectLineB(subjectBField.getValue());
            setting.setFrequencyCapDays(freqField.getValue());
            setting.setMinCartValue(minValField.getValue());
            settingRepo.save(setting);
            grid.setItems(settingRepo.findAll());
            Notification.show("Campaign settings saved successfully!");
        });

        layout.add(subjectField, subjectBField, freqField, minValField, saveBtn);
        container.add(layout);
    }

    private void showROIAnalytics(Div container) {
        VerticalLayout layout = new VerticalLayout();
        layout.add(new H2("Campaign ROI & Attribution"));

        Grid<Map<String, Object>> roiGrid = new Grid<>();
        List<Map<String, Object>> roiData = jdbcTemplate.queryForList(
                "SELECT " +
                        "  cl.campaign_name, " +
                        "  COUNT(DISTINCT cl.link_id) as total_links_sent, " +
                        "  COUNT(DISTINCT ci.interaction_id) as total_clicks, " +
                        "  COUNT(DISTINCT o.order_id) as attributed_orders, " +
                        "  COALESCE(SUM(o.total_amount), 0) as attributed_revenue " +
                        "FROM campaign_links cl " +
                        "LEFT JOIN campaign_interactions ci ON cl.link_id = ci.link_id " +
                        "LEFT JOIN orders o ON cl.user_email = (SELECT email FROM users WHERE user_id = o.user_id) " +
                        "  AND o.created_at > cl.created_at AND o.created_at < cl.created_at + INTERVAL '2 days' " +
                        "GROUP BY cl.campaign_name");

        roiGrid.addColumn(m -> m.get("campaign_name")).setHeader("Campaign");
        roiGrid.addColumn(m -> m.get("total_clicks")).setHeader("Clicks");
        roiGrid.addColumn(m -> m.get("attributed_orders")).setHeader("Orders");
        roiGrid.addColumn(m -> String.format("₹%.2f", ((Number) m.get("attributed_revenue")).doubleValue()))
                .setHeader("ROI Revenue");

        roiGrid.setItems(roiData);
        layout.add(roiGrid);

        container.add(layout);
    }
}
