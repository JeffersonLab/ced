package edu.cnu.ced.view.currentevent;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.data.BankColumns;
import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/**
 * Builds the sorted bank/column catalog for one event snapshot: every
 * present bank in alphabetical order, and within each bank every column in
 * alphabetical order -- the same two-level sort legacy CED's DataWarehouse
 * applies (bank names and column names are plain string sorts; what looks
 * like schema-definition order in a screenshot, e.g. "Hit10_ID" before
 * "Hit1_ID", is lexicographic ordering: '0' sorts before '_').
 */
public final class BankColumnCatalog {

	private BankColumnCatalog() { }

	/** @return the catalog for {@code snapshot}, empty if it carries no event */
	public static List<BankColumnEntry> build(EventSnapshot snapshot) {
		List<BankColumnEntry> entries = new ArrayList<>();
		if (snapshot == null || !snapshot.hasEvent()) {
			return List.copyOf(entries);
		}

		int bankIndex = 0;
		for (String bankName : snapshot.bankNames()) {
			DataBank bank = snapshot.bank(bankName).orElse(null);
			if (bank == null) {
				continue;
			}
			for (String column : BankAccess.columns(bank)) {
				entries.add(new BankColumnEntry(bankName, column, bankIndex,
						BankColumns.typeName(bank, column), bank.rows()));
			}
			bankIndex++;
		}
		return List.copyOf(entries);
	}
}
