package com.app.support.admin.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Main Layout for all Vaadin-based Admin views
 */
public class AdminMainLayout extends AppLayout {

    public AdminMainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("VASU Admin");
        logo.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.MEDIUM,
            LumoUtility.TextColor.PRIMARY
        );

        Button logout = new Button("Log Out", VaadinIcon.SIGN_OUT.create(), e -> {
            // In a real app, this would use SecurityContextLogoutHandler
            getUI().ifPresent(ui -> ui.getPage().setLocation("/logout"));
        });
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        logout.getStyle().set("margin-left", "auto");

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(
            LumoUtility.Padding.Vertical.NONE,
            LumoUtility.Padding.Horizontal.MEDIUM,
            LumoUtility.Background.BASE
        );

        addToNavbar(header);
    }

    private void createDrawer() {
        VerticalLayout navContainer = new VerticalLayout();
        navContainer.setSpacing(false);
        navContainer.setPadding(true);

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", AdminDashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Products", AdminProductListView.class, VaadinIcon.PACKAGE.create()));
        nav.addItem(new SideNavItem("Orders", AdminOrderListView.class, VaadinIcon.CART.create()));
        nav.addItem(new SideNavItem("Returns", AdminReturnView.class, VaadinIcon.BACKSPACE.create()));
        
        SideNavItem logisticsGroup = new SideNavItem("Logistics");
        logisticsGroup.setPrefixComponent(VaadinIcon.TRUCK.create());
        logisticsGroup.addItem(new SideNavItem("Stock Levels", AdminInventoryView.class, VaadinIcon.STORAGE.create()));
        logisticsGroup.addItem(new SideNavItem("Inventory Logs", AdminInventoryTransactionView.class, VaadinIcon.FILE_TEXT.create()));
        nav.addItem(logisticsGroup);

        nav.addItem(new SideNavItem("Users", AdminUserListView.class, VaadinIcon.USERS.create()));
        nav.addItem(new SideNavItem("Support", AdminSupportView.class, VaadinIcon.CHAT.create()));
        nav.addItem(new SideNavItem("CRM & Loyalty", AdminCRMView.class, VaadinIcon.HEART.create()));
        nav.addItem(new SideNavItem("Procurement", AdminProcurementView.class, VaadinIcon.TRUCK.create()));
        nav.addItem(new SideNavItem("Marketing", AdminMarketingView.class, VaadinIcon.BELL.create()));
        nav.addItem(new SideNavItem("Broadcast", AdminBroadcastView.class, VaadinIcon.BELL.create()));

        SideNav techNav = new SideNav();
        techNav.setLabel("OPERATIONS & TECH");
        techNav.addItem(new SideNavItem("System Health", AdminOperationalDashboardView.class, VaadinIcon.DASHBOARD.create()));
        techNav.addItem(new SideNavItem("Tech Monitoring", AdminTechDashboardView.class, VaadinIcon.CHART.create()));
        techNav.addItem(new SideNavItem("Log Search", AdminLogSearchView.class, VaadinIcon.SEARCH.create()));
        techNav.addItem(new SideNavItem("Distributed Tracing", AdminTraceView.class, VaadinIcon.CONNECT.create()));
        techNav.addItem(new SideNavItem("Latency Heatmap", AdminLatencyHeatmapView.class, VaadinIcon.BAR_CHART.create()));
        techNav.addItem(new SideNavItem("Module Map", AdminModuleInteractionView.class, VaadinIcon.SITEMAP.create()));
        techNav.addItem(new SideNavItem("SQL Console", AdminQueryView.class, VaadinIcon.DATABASE.create()));
        techNav.addItem(new SideNavItem("Rule Registry", AdminRuleRegistryView.class, VaadinIcon.TASKS.create()));
        techNav.addItem(new SideNavItem("Alert Config", AdminAlertConfigView.class, VaadinIcon.BELL.create()));

        navContainer.add(nav, techNav);
        addToDrawer(navContainer);
    }

    private RouterLink createNavLink(String title, VaadinIcon icon, Class<? extends com.vaadin.flow.component.Component> viewClass) {
        RouterLink link = new RouterLink(viewClass);
        HorizontalLayout layout = new HorizontalLayout(icon.create(), new Span(title));
        layout.setSpacing(true);
        layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        link.add(layout);
        link.addClassNames(LumoUtility.Padding.SMALL, LumoUtility.TextColor.BODY);
        return link;
    }
}
