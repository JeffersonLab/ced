package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/** Immutable ECAL inner/outer-stack data read directly from one event snapshot. */
public record ECalEventData(List<AdcHit> adcHits, List<ReconHit> reconHits,
		int maximumAdc) {

	public static final String ADC_BANK = "ECAL::adc";
	public static final String RECON_BANK = "REC::Calorimeter";
	private static final ECalEventData EMPTY = new ECalEventData(List.of(), List.of(), 0);

	public ECalEventData {
		adcHits = List.copyOf(adcHits);
		reconHits = List.copyOf(reconHits);
		maximumAdc = Math.max(0, maximumAdc);
	}

	public static ECalEventData from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) return EMPTY;
		ArrayList<AdcHit> adc = new ArrayList<>();
		ArrayList<ReconHit> recon = new ArrayList<>();
		int maximum = readAdc(snapshot.bank(ADC_BANK).orElse(null), adc);
		readRecon(snapshot.bank(RECON_BANK).orElse(null), recon);
		return adc.isEmpty() && recon.isEmpty() ? EMPTY : new ECalEventData(adc, recon, maximum);
	}

	private static int readAdc(DataBank bank, List<AdcHit> destination) {
		if (!hasColumns(bank, "sector", "layer", "component", "ADC", "time")) return 0;
		int maximum = 0;
		for (int row = 0; row < bank.rows(); row++) {
			int layer = bank.getByte("layer", row);
			int adc = bank.getInt("ADC", row);
			if (layer < 4 || layer > 9 || adc <= 0) continue;
			int relativeLayer = layer - 4;
			destination.add(new AdcHit(bank.getByte("sector", row), relativeLayer / 3,
					relativeLayer % 3, bank.getShort("component", row), adc,
					bank.getFloat("time", row)));
			maximum = Math.max(maximum, adc);
		}
		return maximum;
	}

	private static void readRecon(DataBank bank, List<ReconHit> destination) {
		if (!hasColumns(bank, "sector", "layer", "energy", "time", "x", "y", "z")) return;
		boolean hasRadius = BankAccess.hasColumn(bank, "radius");
		for (int row = 0; row < bank.rows(); row++) {
			int layer = bank.getByte("layer", row);
			if (layer < 4 || layer > 9) continue;
			int relativeLayer = layer - 4;
			destination.add(new ReconHit(row, bank.getByte("sector", row), relativeLayer / 3,
					relativeLayer % 3, bank.getFloat("energy", row), bank.getFloat("time", row),
					bank.getFloat("x", row), bank.getFloat("y", row), bank.getFloat("z", row),
					hasRadius ? bank.getFloat("radius", row) : 0f));
		}
	}

	private static boolean hasColumns(DataBank bank, String... names) {
		if (bank == null) return false;
		for (String name : names) if (!BankAccess.hasColumn(bank, name)) return false;
		return true;
	}

	public record AdcHit(int sector, int plane, int view, int strip, int adc, float time) { }
	public record ReconHit(int row, int sector, int plane, int view, float energy,
			float time, float x, float y, float z, float radius) { }
}
