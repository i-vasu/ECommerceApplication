package com.app.finance.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores the mapping between local entity IDs and their corresponding Zoho Books entity IDs.
 * This enables bidirectional sync: linking invoices to payments, marking POs as received, etc.
 */
@Entity
@Table(name = "zoho_entity_mappings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"entityType", "localId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZohoEntityMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ZohoEntityType entityType;

    /** The local application entity ID (e.g. orderId, quoteId, poId) */
    @Column(nullable = false)
    private String localId;

    /** The Zoho Books entity ID (e.g. salesorder_id, invoice_id, estimate_id) */
    @Column(nullable = false)
    private String zohoId;

    /** Optional: a second Zoho ID if the entity has been converted (e.g. estimate → sales order) */
    private String zohoSecondaryId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    public enum ZohoEntityType {
        SALES_ORDER,
        INVOICE,
        PURCHASE_ORDER,
        ESTIMATE,
        CREDIT_NOTE,
        CUSTOMER,
        VENDOR,
        BILL,
        EXPENSE,
        JOURNAL
    }

    public static ZohoEntityMapping create(ZohoEntityType type, String localId, String zohoId) {
        ZohoEntityMapping mapping = new ZohoEntityMapping();
        mapping.setEntityType(type);
        mapping.setLocalId(localId);
        mapping.setZohoId(zohoId);
        mapping.setCreatedAt(LocalDateTime.now());
        return mapping;
    }
}
