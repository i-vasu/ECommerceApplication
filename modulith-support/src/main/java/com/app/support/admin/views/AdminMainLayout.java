package com.app.support.admin.views;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

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
        H1 logo = new H1("VAABHI Admin");
        logo.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.MEDIUM,
            LumoUtility.TextColor.PRIMARY
        );

        Button logout = new Button("Log Out", VaadinIcon.SIGN_OUT.create(), e -> {
            // In a real app, this would use SecurityContextLogoutHandler
            getUI().ifPresent(ui -> ui.getPage().setLocation("/admin/logout"));
        });
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        logout.getStyle().set("margin-left", "auto");

        // Environment Switcher
        com.vaadin.flow.component.select.Select<String> envSelect = new com.vaadin.flow.component.select.Select<>();
        envSelect.setItems("PROD", "TEST");
        envSelect.setLabel("Environment");
        
        // Load initial state from cookie
        String currentTenant = "PROD"; // Default
        jakarta.servlet.http.Cookie[] cookies = com.vaadin.flow.server.VaadinServletRequest.getCurrent().getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie c : cookies) {
                if ("TENANT_ID".equals(c.getName())) {
                   if (c.getValue().contains("test")) currentTenant = "TEST";
                }
            }
        }
        envSelect.setValue(currentTenant);

        envSelect.addValueChangeListener(event -> {
            String selected = event.getValue();
            String tenantId = "PROD".equals(selected) ? "tenant1_prod" : "tenant1_test";
            
            // Set Cookie via JavaScript because Vaadin's Cookie API is sometimes tricky in async
            getUI().ifPresent(ui -> {
                ui.getPage().executeJs("document.cookie = 'TENANT_ID=" + tenantId + "; path=/; max-age=31536000;'");
                ui.getPage().reload();
            });
        });
        envSelect.getStyle().set("margin-right", "1em");

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, envSelect, logout);
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
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOperator = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR"));
        boolean isSupport = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPPORT"));

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", AdminDashboardView.class, VaadinIcon.DASHBOARD.create()));
        
        if (isAdmin || isOperator) {
            nav.addItem(new SideNavItem("Products", AdminProductListView.class, VaadinIcon.PACKAGE.create()));
            nav.addItem(new SideNavItem("Bulk Import", AdminProductImportView.class, VaadinIcon.UPLOAD_ALT.create()));
        }
        
        if (isAdmin || isOperator || isSupport) {
            nav.addItem(new SideNavItem("Orders", AdminOrderListView.class, VaadinIcon.CART.create()));
        }
        
        if (isAdmin || isOperator) {
            nav.addItem(new SideNavItem("Returns", AdminReturnView.class, VaadinIcon.BACKSPACE.create()));
        }
        
        if (isAdmin || isOperator || isSupport) {
            SideNavItem logisticsGroup = new SideNavItem("Logistics");
            logisticsGroup.setPrefixComponent(VaadinIcon.TRUCK.create());
            
            if (isAdmin || isOperator) {
                 logisticsGroup.addItem(new SideNavItem("Stock Levels", AdminInventoryView.class, VaadinIcon.STORAGE.create()));
            }
            logisticsGroup.addItem(new SideNavItem("Inventory Logs", AdminInventoryTransactionView.class, VaadinIcon.FILE_TEXT.create()));
            nav.addItem(logisticsGroup);
        }

        if (isAdmin || isSupport) {
            nav.addItem(new SideNavItem("Users", AdminUserListView.class, VaadinIcon.USERS.create()));
            nav.addItem(new SideNavItem("Support", AdminSupportView.class, VaadinIcon.CHAT.create()));
            nav.addItem(new SideNavItem("CRM & Loyalty", AdminCRMView.class, VaadinIcon.HEART.create()));
        }
        
        if (isAdmin || isOperator) {
            SideNavItem procurementGroup = new SideNavItem("Procurement");
            procurementGroup.setPrefixComponent(VaadinIcon.CART.create());
            procurementGroup.addItem(new SideNavItem("Vendors", AdminVendorView.class, VaadinIcon.TRUCK.create()));
            procurementGroup.addItem(new SideNavItem("Purchase Orders", AdminProcurementView.class, VaadinIcon.CART.create()));
            nav.addItem(procurementGroup);
        }
        
        if (isAdmin || isOperator) {
            SideNavItem cmsGroup = new SideNavItem("Content Manager");
            cmsGroup.setPrefixComponent(VaadinIcon.PENCIL.create());
            cmsGroup.addItem(new SideNavItem("Hero Banners", AdminBannerView.class, VaadinIcon.PICTURE.create()));
            cmsGroup.addItem(new SideNavItem("Static Pages", AdminPageView.class, VaadinIcon.FILE_TEXT.create()));
            nav.addItem(cmsGroup);
        }

        if (isAdmin) {
             SideNavItem integrationsGroup = new SideNavItem("Integrations");
             integrationsGroup.setPrefixComponent(VaadinIcon.CONNECT.create());
             
             // External Links
             com.vaadin.flow.component.sidenav.SideNavItem zoho = new SideNavItem("Zoho Books");
             zoho.setPath("https://books.zoho.com/app");
             zoho.setPrefixComponent(VaadinIcon.FILE_TEXT_O.create());
             integrationsGroup.addItem(zoho);
             
             com.vaadin.flow.component.sidenav.SideNavItem razorpay = new SideNavItem("Razorpay");
             razorpay.setPath("https://dashboard.razorpay.com");
             razorpay.setPrefixComponent(VaadinIcon.CREDIT_CARD.create());
             integrationsGroup.addItem(razorpay);
             
             com.vaadin.flow.component.sidenav.SideNavItem shiprocket = new SideNavItem("Shiprocket");
             shiprocket.setPath("https://app.shiprocket.in");
             shiprocket.setPrefixComponent(VaadinIcon.TRUCK.create());
             integrationsGroup.addItem(shiprocket);
             
             nav.addItem(integrationsGroup);
        }
        
        if (isAdmin) {
            nav.addItem(new SideNavItem("Finance Dashboard", AdminFinanceDashboardView.class, VaadinIcon.CHART.create()));
            nav.addItem(new SideNavItem("Stock Reconciliation", AdminStockReconciliationView.class, VaadinIcon.CLIPBOARD_CHECK.create()));
            nav.addItem(new SideNavItem("Marketing", AdminMarketingView.class, VaadinIcon.BELL.create()));
            nav.addItem(new SideNavItem("Broadcast", AdminBroadcastView.class, VaadinIcon.BELL.create()));
        }

        if (isAdmin) {
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
            techNav.addItem(new SideNavItem("Global Analytics", GlobalAnalyticsView.class, VaadinIcon.CHART_3D.create()));
            
            navContainer.add(nav, techNav);
        } else {
            navContainer.add(nav);
        }

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
