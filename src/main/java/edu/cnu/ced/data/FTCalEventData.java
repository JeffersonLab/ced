package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/** Immutable FTCAL data extracted from one atomic event snapshot. */
public record FTCalEventData(List<AdcHit> adcHits, List<ReconHit> reconHits,
		int maximumAdc) {

	public static final String ADC_BANK = "FTCAL::adc";
	public static final String HIT_BANK = "FTCAL::hits";
	private static final FTCalEventData EMPTY = new FTCalEventData(List.of(), List.of(), 0);

	public FTCalEventData {
		adcHits = List.copyOf(adcHits);
		reconHits = List.copyOf(reconHits);
		maximumAdc = Math.max(0, maximumAdc);
	}

	/** Extract all available FTCAL display data defensively. */
	public static FTCalEventData from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) {
			return EMPTY;
		}
		ArrayList<AdcHit> adc = new ArrayList<>();
		ArrayList<ReconHit> hits = new ArrayList<>();
		int maximum = readAdc(snapshot.bank(ADC_BANK).orElse(null), adc);
		readHits(snapshot.bank(HIT_BANK).orElse(null), hits);
		return adc.isEmpty() && hits.isEmpty() ? EMPTY
				: new FTCalEventData(adc, hits, maximum);
	}

	private static int readAdc(DataBank bank, List<AdcHit> destination) {
		if (!hasColumns(bank, "component", "ADC", "time", "order")) return 0;
		int maximum = 0;
		for (int row = 0; row < bank.rows(); row++) {
			int value = bank.getInt("ADC", row);
			if (value <= 0) continue;
			destination.add(new AdcHit(bank.getShort("component", row), value,
					bank.getFloat("time", row), bank.getByte("order", row)));
			maximum = Math.max(maximum, value);
		}
		return maximum;
	}

	private static void readHits(DataBank bank, List<ReconHit> destination) {
		if (!hasColumns(bank, "hitID", "x", "y", "z")) return;
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new ReconHit(bank.getShort("hitID", row),
					bank.getFloat("x", row), bank.getFloat("y", row),
					bank.getFloat("z", row)));
		}
	}

	private static boolean hasColumns(DataBank bank, String... names) {
		if (bank == null) return false;
		for (String name : names) if (!BankAccess.hasColumn(bank, name)) return false;
		return true;
	}

	public record AdcHit(int component, int adc, float time, int order) { }
	public record ReconHit(int id, float x, float y, float z) { }
}
