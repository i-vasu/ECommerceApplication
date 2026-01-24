package com.app.admin.controllers;

import com.app.admin.services.ERPNextProductSyncService;
import com.app.admin.services.ERPNextSetupService;
import com.app.core.payloads.ApiResponse;
import com.app.core.multitenancy.TenantManagementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "E-Commerce Application")
public class ProductAdminController {

    @Autowired
    private ERPNextProductSyncService syncService;

    @Autowired
    private ERPNextSetupService setupService;

    @Autowired
    private TenantManagementService tenantService;

    @PostMapping("/setup/erpnext")
    public ResponseEntity<ApiResponse<String>> setupERPNext() {
        String result = setupService.setup();
        return ResponseEntity.ok(ApiResponse.success(result, "ERPNext Setup completed"));
    }

    @PostMapping("/products/sync")
    public ResponseEntity<ApiResponse<String>> syncProducts() {
        var tenants = tenantService.getActiveTenants();
        for (var tenant : tenants) {
            syncService.syncItems(tenant);
        }
        return ResponseEntity.ok(ApiResponse.success("Sync initiated for all tenants", "Sync started"));
    }
}
