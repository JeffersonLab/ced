package edu.cnu.ced.event;

import java.util.Optional;

import org.jlab.io.base.DataBank;

/** Immutable first-row values from the optional {@code RUN::config} bank. */
public record RunConfig(int run, int event, long trigger, long timestamp,
		byte type, byte mode, float solenoid, float torus) {

	public static final String BANK_NAME = "RUN::config";
	private static final String[] REQUIRED = { "run", "event", "solenoid", "torus" };

	public static Optional<RunConfig> from(EventSnapshot snapshot) {
		if (snapshot == null) {
			return Optional.empty();
		}
		return snapshot.bank(BANK_NAME).flatMap(RunConfig::fromBank);
	}

	static Optional<RunConfig> fromBank(DataBank bank) {
		if (bank == null || bank.rows() < 1) {
			return Optional.empty();
		}
		for (String column : REQUIRED) {
			if (!BankAccess.hasColumn(bank, column)) {
				return Optional.empty();
			}
		}

		int run = bank.getInt("run", 0);
		int event = bank.getInt("event", 0);
		float solenoid = bank.getFloat("solenoid", 0);
		float torus = bank.getFloat("torus", 0);
		if (run < 0 || event < 0 || !Float.isFinite(solenoid) || !Float.isFinite(torus)) {
			return Optional.empty();
		}

		return Optional.of(new RunConfig(run, event,
				longValue(bank, "trigger"), longValue(bank, "timestamp"),
				byteValue(bank, "type"), byteValue(bank, "mode"), solenoid, torus));
	}

	private static long longValue(DataBank bank, String column) {
		return BankAccess.hasColumn(bank, column) ? bank.getLong(column, 0) : -1L;
	}

	private static byte byteValue(DataBank bank, String column) {
		return BankAccess.hasColumn(bank, column) ? bank.getByte(column, 0) : (byte) -1;
	}
}
