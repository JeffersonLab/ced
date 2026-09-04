package edu.cnu.ced.event;

import java.util.Optional;

import org.jlab.io.base.DataBank;

/**
 * The current event's trigger word, from row 0 of the optional
 * {@code RUN::trigger} bank -- distinct from {@code RUN::config}'s own
 * {@code trigger} column ({@link RunConfig#trigger()}), which is a different
 * value in the CLAS12 data model. Legacy CED's main-window trigger-bit
 * status bar reads this bank specifically (its {@code cnuphys.ced.alldata.
 * RunTriggers}), so this matches that rather than reusing RunConfig's field.
 */
public record RunTrigger(int id, long trigger) {

	public static final String BANK_NAME = "RUN::trigger";

	public static Optional<RunTrigger> from(EventSnapshot snapshot) {
		if (snapshot == null) {
			return Optional.empty();
		}
		return snapshot.bank(BANK_NAME).flatMap(RunTrigger::fromBank);
	}

	static Optional<RunTrigger> fromBank(DataBank bank) {
		if (bank == null || bank.rows() < 1 || !BankAccess.hasColumn(bank, "trigger")) {
			return Optional.empty();
		}
		int id = BankAccess.hasColumn(bank, "id") ? bank.getInt("id", 0) : 0;
		// the 32-bit trigger word, widened to an unsigned value so all 32 bits
		// (including bit 31) test correctly -- matches legacy's own
		// MathUtilities.getUnsignedInt(int) via the standard library equivalent.
		long trigger = Integer.toUnsignedLong(bank.getInt("trigger", 0));
		return Optional.of(new RunTrigger(id, trigger));
	}

	/** @return whether bit {@code index} (0-31) is set in the trigger word */
	public boolean bit(int index) {
		return (trigger & (1L << index)) != 0;
	}
}
