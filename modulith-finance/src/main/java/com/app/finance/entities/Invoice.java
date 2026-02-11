package com.app.finance.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber; // Format: INV-YYYY-SEQ

    @Column(name = "order_id", unique = true, nullable = false)
    private String orderId;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "tax_amount")
    private Double taxAmount;

    // GST Breakdown stored as JSONB
    // { "CGST": 100.0, "SGST": 100.0, "IGST": 0.0 }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> taxBreakdown;

    // Snapshot of items at time of invoice (Price, Quantity, HSN)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> items; 

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @CreationTimestamp
    private LocalDateTime generatedAt;

    public enum InvoiceStatus {
        GENERATED, SENT, CANCELLED
    }
}
