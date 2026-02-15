package com.app.support.admin.views;

import com.app.finance.entities.LedgerEntry;
import com.app.finance.services.FinancialLedgerService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

@Route(value = "admin/finance", layout = AdminMainLayout.class)
@PageTitle("Financial Health Dashboard | Fashion ERP")
@RolesAllowed("ADMIN")
public class AdminFinanceDashboardView extends VerticalLayout {

    private final FinancialLedgerService ledgerService;
    private final Grid<LedgerEntry> ledgerGrid = new Grid<>(LedgerEntry.class);

    public AdminFinanceDashboardView(FinancialLedgerService ledgerService) {
        this.ledgerService = ledgerService;
        setSizeFull();
        setPadding(true);

        add(new H2("Financial Health Dashboard"));

        // KPI Cards
        HorizontalLayout kpiContainer = new HorizontalLayout();
        kpiContainer.setWidthFull();
        
        kpiContainer.add(createKPICard("Revenue (Sales)", "REV-SALES", "green"));
        kpiContainer.add(createKPICard("Inventory Asset", "AST-INV", "blue"));
        kpiContainer.add(createKPICard("Accounts Payable", "LIA-AP", "red"));
        
        add(kpiContainer);

        // Ledger Feed
        ledgerGrid.setColumns("entryDate", "accountNumber", "accountType", "type", "amount", "description");
        ledgerGrid.setItems(ledgerService.getAllEntries()); // Assuming getAllEntries exists
        add(new H2("Recent Ledger Transactions"), ledgerGrid);
    }

    private VerticalLayout createKPICard(String title, String accountCode, String color) {
        BigDecimal balance = ledgerService.getBalance(accountCode);
        
        VerticalLayout card = new VerticalLayout();
        card.getStyle().set("border", "1px solid #ddd");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("padding", "15px");
        card.setWidth("250px");

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("font-weight", "bold");
        
        Span balanceSpan = new Span("₹ " + balance.toString());
        balanceSpan.getStyle().set("font-size", "24px");
        balanceSpan.getStyle().set("color", color);

        card.add(titleSpan, balanceSpan);
        return card;
    }
}
