package com.app.core.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VendorReturnEvent(
		Long returnId,
		String poNumber,
		String vendorName,
		List<ReturnItem> items,
		String reason,
		LocalDateTime returnedAt) {

	public record ReturnItem(
			String itemCode,
			String itemName,
			int quantity,
			BigDecimal unitPrice,
			BigDecimal refundAmount) {
	}

	public VendorReturnEvent(Long returnId, String poNumber, String vendorName, List<ReturnItem> items, String reason) {
		this(returnId, poNumber, vendorName, items, reason, LocalDateTime.now());
	}
}
