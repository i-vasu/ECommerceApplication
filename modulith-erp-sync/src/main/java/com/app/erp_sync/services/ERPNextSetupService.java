package com.app.erp_sync.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import org.springframework.core.ParameterizedTypeReference;
import com.app.core.multitenancy.Tenant;
import com.app.core.multitenancy.TenantManagementService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ERPNextSetupService {

    @Autowired
    private RestClient restClient;

    @Value("${erpnext.api.base-url:http://localhost:8000}")
    private String erpNextUrl;

    // We need Admin credentials to create DocTypes
    @Value("${erpnext.admin.username:Administrator}")
    private String adminUsername;

    @Value("${erpnext.admin.password:admin}")
    private String adminPassword;

    @Autowired
    private TenantManagementService tenantService;

    public String setup() {
        StringBuilder report = new StringBuilder();

        List<Tenant> tenants = tenantService.getActiveTenants();
        if (tenants.isEmpty()) {
            report.append("No active tenants found. Running setup on default localhost:8000.\n");
            // Fallback to default logic if no tenants
        }

        for (Tenant tenant : tenants) {
            report.append("--- Setting up Tenant: ").append(tenant.getTenantId()).append(" ---\n");
            try {
                String siteUrl = tenant.getErpNextUrl();
                String cookie = login(siteUrl);

                if (cookie == null || cookie.contains("sid=guest") || cookie.isEmpty()) {
                    report.append("Failed to login as Administrator for ").append(siteUrl).append("\n");
                    continue;
                }

                // 2. Check & Create DocTypes
                report.append(ensureDocType(siteUrl, cookie, "Brand", true));
                report.append(ensureDocType(siteUrl, cookie, "Season", false));
                report.append(ensureDocType(siteUrl, cookie, "Size Guide", false));

                // 3. Check Standard DocTypes
                String[] standards = { "Item Attribute", "Price List", "Loyalty Program", "Campaign", "Sales Return",
                        "Shipping Rule", "Tax Rule" };
                for (String dt : standards) {
                    report.append(ensureDocType(siteUrl, cookie, dt, true));
                }

            } catch (Exception e) {
                log.error("Setup failed for tenant " + tenant.getTenantId(), e);
                report.append("Error: ").append(e.getMessage()).append("\n");
            }
            report.append("\n");
        }

        return report.toString();
    }

    private String login(String baseUrl) {
        try {
            Map<String, String> creds = new HashMap<>();
            creds.put("usr", adminUsername);
            creds.put("pwd", adminPassword);

            ResponseEntity<Map<String, Object>> response = restClient.post()
                    .uri(baseUrl + "/api/method/login")
                    .body(creds)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode().is2xxSuccessful()) {
                List<String> cookies = response.getHeaders().get("Set-Cookie");
                if (cookies != null && !cookies.isEmpty()) {
                    return String.join(";", cookies);
                }
                return "sid=guest";
            }
        } catch (Exception e) {
            log.error("Login failed for " + baseUrl, e);
        }
        return null;
    }

    private String ensureDocType(String baseUrl, String cookie, String docTypeName, boolean isStandard) {
        try {
            // Check existence
            try {
                restClient.get()
                        .uri(baseUrl + "/api/resource/DocType/" + docTypeName)
                        .header("Cookie", cookie)
                        .retrieve()
                        .toBodilessEntity();
                return "DocType '" + docTypeName + "' exists.\n";
            } catch (Exception e) {
                // Not found, create it
                if (isStandard) {
                    return "Standard DocType '" + docTypeName
                            + "' is missing! (Cannot create standard types via API easily without app context).\n";
                }

                log.info("Creating Custom DocType: {}", docTypeName);
                Map<String, Object> docType = new HashMap<>();
                docType.put("doctype", "DocType");
                docType.put("name", docTypeName);
                docType.put("module", "Custom");
                docType.put("custom", 1);
                docType.put("fields", List.of(
                        Map.of("fieldname", "description", "label", "Description", "fieldtype", "Text")));

                restClient.post()
                        .uri(baseUrl + "/api/resource/DocType")
                        .header("Cookie", cookie)
                        .body(docType)
                        .retrieve()
                        .toBodilessEntity();

                return "Created DocType '" + docTypeName + "'.\n";
            }
        } catch (Exception e) {
            return "Error checking/creating '" + docTypeName + "': " + e.getMessage() + "\n";
        }
    }
}
