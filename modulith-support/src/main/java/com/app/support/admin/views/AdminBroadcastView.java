package com.app.support.admin.views;

import com.app.marketing.entities.MarketingBroadcast;
import com.app.marketing.repositories.MarketingBroadcastRepo;
import com.app.marketing.services.BroadcastService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/broadcasts", layout = AdminMainLayout.class)
@RolesAllowed("ADMIN")
public class AdminBroadcastView extends VerticalLayout {

    private final MarketingBroadcastRepo broadcastRepo;
    private final BroadcastService broadcastService;

    public AdminBroadcastView(MarketingBroadcastRepo broadcastRepo, BroadcastService broadcastService) {
        this.broadcastRepo = broadcastRepo;
        this.broadcastService = broadcastService;

        setSpacing(true);
        setPadding(true);

        add(new H1("Broadcast Command Center"));
        add(new com.vaadin.flow.component.html.Span(
                "Send one-off messages and newsletters to targeted user segments."));

        showBroadcastList();
        showCreateForm();
    }

    private void showBroadcastList() {
        add(new H2("Past Broadcasts"));
        Grid<MarketingBroadcast> grid = new Grid<>(MarketingBroadcast.class);
        grid.setItems(broadcastRepo.findAll());
        grid.removeAllColumns();

        grid.addColumn(MarketingBroadcast::getName).setHeader("Campaign Name");
        grid.addColumn(MarketingBroadcast::getSegmentName).setHeader("Segment");
        grid.addColumn(MarketingBroadcast::getStatus).setHeader("Status");
        grid.addColumn(MarketingBroadcast::getSentCount).setHeader("Sent To");
        grid.addColumn(MarketingBroadcast::getSentAt).setHeader("Sent At");

        grid.addComponentColumn(b -> {
            Button sendBtn = new Button("Send Now", e -> {
                broadcastService.executeBroadcast(b.getBroadcastId());
                Notification.show("Broadcast started in background...");
            });
            sendBtn.setEnabled("DRAFT".equals(b.getStatus()));
            return sendBtn;
        });

        add(grid);
    }

    private void showCreateForm() {
        add(new H2("Create New Broadcast"));
        VerticalLayout form = new VerticalLayout();

        TextField nameField = new TextField("Broadcast Name");
        ComboBox<String> segmentBox = new ComboBox<>("Target Segment");
        segmentBox.setItems("ALL", "VIP", "CHURN_RISK");

        TextField templateField = new TextField("Thymeleaf Template");
        templateField.setPlaceholder("e.g. weekly-newsletter");

        Button saveBtn = new Button("Create Draft", e -> {
            MarketingBroadcast b = new MarketingBroadcast();
            b.setName(nameField.getValue());
            b.setSegmentName(segmentBox.getValue());
            b.setTemplateName(templateField.getValue());
            b.setStatus("DRAFT");
            broadcastRepo.save(b);
            Notification.show("Draft created successfully!");
            getUI().ifPresent(ui -> ui.refreshCurrentRoute(true));
        });

        form.add(nameField, segmentBox, templateField, saveBtn);
        add(form);
    }
}
