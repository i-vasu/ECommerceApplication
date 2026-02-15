package com.app.support.admin.views;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;

/**
 * Vaadin AppShell configuration class.
 * This class centralizes PWA and other shell-level configurations required by Vaadin 23+.
 */
@PWA(
    name = "Vaabhi Heritage Admin",
    shortName = "Vaabhi Admin",
    offlinePath = "offline.html",
    backgroundColor = "#2b0a0a",
    themeColor = "#8b0000"
)
public class AppShell implements AppShellConfigurator {
}
