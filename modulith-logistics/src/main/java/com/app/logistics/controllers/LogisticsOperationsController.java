package com.app.logistics.controllers;

import com.app.logistics.shipping.services.NdrManagementService;
import com.app.logistics.shipping.services.WeightDiscrepancyService;
import com.app.logistics.shipping.services.CodRemittanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Logistics Operations Controller
 * Handles NDR, Weight Discrepancy, and COD management
 */
@RestController
@RequestMapping("/api/v1/logistics/operations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('WAREHOUSE_MANAGER')")
public class LogisticsOperationsController {

    private final NdrManagementService ndrService;
    private final WeightDiscrepancyService weightService;
    private final CodRemittanceService codService;

    // ==================== NDR Management ====================

    /**
     * Get all pending NDRs
     */
    @GetMapping("/ndr/pending")
    public ResponseEntity<Map<String, Object>> getPendingNdrs() {
        return ResponseEntity.ok(ndrService.getPendingNdrs());
    }

    /**
     * Get NDR details for specific AWB
     */
    @GetMapping("/ndr/awb/{awbNumber}")
    public ResponseEntity<Map<String, Object>> getNdrByAwb(@PathVariable String awbNumber) {
        return ResponseEntity.ok(ndrService.getNdrByAwb(awbNumber));
    }

    /**
     * Take action on NDR
     */
    @PostMapping("/ndr/action")
    public ResponseEntity<Map<String, Object>> takeNdrAction(
            @RequestParam String awbNumber,
            @RequestParam String action,
            @RequestParam(required = false) String remarks) {
        
        return ResponseEntity.ok(ndrService.reAttemptDelivery(awbNumber, action, remarks));
    }

    /**
     * Trigger auto-retry for all pending NDRs
     */
    @PostMapping("/ndr/auto-retry")
    public ResponseEntity<String> autoRetryNdrs() {
        ndrService.autoRetryPendingNdrs();
        return ResponseEntity.ok("Auto-retry initiated for pending NDRs");
    }

    // ==================== Weight Discrepancy ====================

    /**
     * Get all weight discrepancies
     */
    @GetMapping("/weight-discrepancy")
    public ResponseEntity<Map<String, Object>> getWeightDiscrepancies(
            @RequestParam(required = false) Map<String, Object> filters) {
        
        return ResponseEntity.ok(weightService.getWeightDiscrepancies(
            filters != null ? filters : Map.of()));
    }

    /**
     * Get weight discrepancy for specific AWB
     */
    @GetMapping("/weight-discrepancy/awb/{awbNumber}")
    public ResponseEntity<Map<String, Object>> getDiscrepancyByAwb(@PathVariable String awbNumber) {
        return ResponseEntity.ok(weightService.getDiscrepancyByAwb(awbNumber));
    }

    /**
     * Raise weight dispute
     */
    @PostMapping("/weight-discrepancy/dispute")
    public ResponseEntity<Map<String, Object>> raiseWeightDispute(
            @RequestParam String awbNumber,
            @RequestParam double actualWeight,
            @RequestParam double chargedWeight,
            @RequestParam String remarks) {
        
        return ResponseEntity.ok(weightService.raiseWeightDispute(
            awbNumber, actualWeight, chargedWeight, remarks));
    }

    /**
     * Auto-raise disputes for significant discrepancies
     */
    @PostMapping("/weight-discrepancy/auto-dispute")
    public ResponseEntity<String> autoRaiseDisputes(
            @RequestParam(defaultValue = "10.0") double thresholdPercentage) {
        
        weightService.autoRaiseDisputes(thresholdPercentage);
        return ResponseEntity.ok("Auto-dispute process initiated for discrepancies > " + 
                                thresholdPercentage + "%");
    }

    /**
     * Calculate potential savings from weight disputes
     */
    @GetMapping("/weight-discrepancy/savings")
    public ResponseEntity<Map<String, Object>> calculateSavings() {
        return ResponseEntity.ok(weightService.calculatePotentialSavings());
    }

    // ==================== COD Remittance ====================

    /**
     * Get COD remittance report
     */
    @GetMapping("/cod/remittance")
    public ResponseEntity<Map<String, Object>> getCodRemittance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        return ResponseEntity.ok(codService.getCodRemittance(fromDate, toDate));
    }

    /**
     * Get pending COD collections
     */
    @GetMapping("/cod/pending")
    public ResponseEntity<Map<String, Object>> getPendingCod() {
        return ResponseEntity.ok(codService.getPendingCodCollections());
    }

    /**
     * Calculate pending COD amount
     */
    @GetMapping("/cod/pending-amount")
    public ResponseEntity<Map<String, Object>> getPendingCodAmount() {
        return ResponseEntity.ok(codService.calculatePendingCodAmount());
    }

    /**
     * Get COD summary for date range
     */
    @GetMapping("/cod/summary")
    public ResponseEntity<Map<String, Object>> getCodSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        return ResponseEntity.ok(codService.getCodSummary(fromDate, toDate));
    }
}
