package com.app.erp_sync.admin.controllers;

import com.app.core.multitenancy.TenantManagementService;
import com.app.core.payloads.ApiResponse;
import com.app.erp_sync.services.ERPNextProductSyncService;
import com.app.erp_sync.services.ERPNextSetupService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ERPNextProductSyncService syncService;
    private final ERPNextSetupService setupService;
    private final TenantManagementService tenantService;

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
