package com.app.core.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record JournalEntryPostedEvent(
		String journalId,
		LocalDate entryDate,
		String description,
		String referenceNumber,
		List<EntryLine> lines) {

	public record EntryLine(
			String accountName,
			BigDecimal debit,
			BigDecimal credit,
			String description) {
	}
}
