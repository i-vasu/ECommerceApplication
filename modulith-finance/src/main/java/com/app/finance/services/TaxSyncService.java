package com.app.finance.services;

import com.app.finance.entities.TaxRate;
import com.app.finance.integration.zoho.ZohoClient;
import com.app.finance.repositories.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxSyncService {

    private final ZohoClient zohoClient;
    private final TaxRateRepository taxRateRepository;

    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void syncTaxRates() {
        log.info("Starting Tax Rate Sync from Zoho...");
        List<Map<String, Object>> zohoTaxes = zohoClient.getTaxes();

        for (Map<String, Object> zTax : zohoTaxes) {
            try {
                String zohoId = (String) zTax.get("tax_id");
                String name = (String) zTax.get("tax_name");
                Double percentage = (Double) zTax.get("tax_percentage");

                TaxRate taxRate = taxRateRepository.findByZohoTaxId(zohoId)
                        .orElse(new TaxRate());

                taxRate.setZohoTaxId(zohoId);
                taxRate.setTaxName(name);
                taxRate.setPercentage(percentage);

                taxRateRepository.save(taxRate);
            } catch (Exception e) {
                log.error("Failed to sync individual tax rate: {}", zTax, e);
            }
        }
        log.info("Tax Rate Sync Completed. Total rates: {}", zohoTaxes.size());
    }
    
    public List<TaxRate> getAllTaxRates() {
        return taxRateRepository.findAll();
    }
}
