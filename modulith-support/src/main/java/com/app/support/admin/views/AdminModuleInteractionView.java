package com.app.support.admin.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.JavaPackage;

import java.util.stream.Collectors;

/**
 * Module Interaction Topology (Spring Modulith Visualizer).
 * Analyzes the internal architecture and displays the "Monolith Topology".
 */
@Route("admin/module-topology")
@RolesAllowed("ADMIN")
public class AdminModuleInteractionView extends VerticalLayout {

    public AdminModuleInteractionView() {
        setSpacing(true);
        setPadding(true);

        add(new H1("Module Interaction Topology"));
        add(new Span("Autonomous analysis of the modular monolith structure using Spring Modulith."));

        try {
            // Analyze the application structure
            ApplicationModules modules = ApplicationModules.of("com.app");

            add(new H2("Detected Modules (" + modules.stream().count() + ")"));

            modules.forEach(module -> {
                VerticalLayout moduleCard = new VerticalLayout();
                moduleCard.getStyle().set("background", "#ffffff").set("padding", "15px").set("border-radius", "10px")
                        .set("box-shadow", "0 2px 5px rgba(0,0,0,0.1)");

                Span name = new Span(module.getDisplayName());
                name.getStyle().set("font-weight", "bold").set("font-size", "18px").set("color", "#1e3a8a");

                Span pkg = new Span("Base Package: " + module.getBasePackage().getName());
                pkg.getStyle().set("font-size", "12px").set("color", "#64748b");

                moduleCard.add(name, pkg);

                // Analyze dependencies
                String dependencies = module.getDirectDependencies(modules).stream()
                        .map(m -> m.getTargetModule().getDisplayName())
                        .collect(Collectors.joining(", "));

                if (!dependencies.isEmpty()) {
                    Span depLabel = new Span("➡ Relies on: " + dependencies);
                    depLabel.getStyle().set("font-size", "14px").set("color", "#059669");
                    moduleCard.add(depLabel);
                } else {
                    Span independent = new Span("💎 Fully Independent Module");
                    independent.getStyle().set("font-size", "14px").set("color", "#7c3aed");
                    moduleCard.add(independent);
                }

                add(moduleCard);
            });

        } catch (Exception e) {
            add(new Span("Error analyzing modules: " + e.getMessage()));
        }
    }
}
