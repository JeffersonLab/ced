package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/** Immutable raw and reconstructed data for one HTCC or LTCC event. */
public record CherenkovEventData(String detector, List<AdcHit> adcHits,
		List<TdcHit> tdcHits, List<ReconHit> reconHits, int maximumAdc) {

	public CherenkovEventData {
		adcHits = List.copyOf(adcHits);
		tdcHits = List.copyOf(tdcHits);
		reconHits = List.copyOf(reconHits);
		maximumAdc = Math.max(0, maximumAdc);
	}

	public static CherenkovEventData from(EventSnapshot snapshot, String detector) {
		String name = "LTCC".equals(detector) ? "LTCC" : "HTCC";
		if (snapshot == null || !snapshot.hasEvent()) return empty(name);
		ArrayList<AdcHit> adc = new ArrayList<>();
		ArrayList<TdcHit> tdc = new ArrayList<>();
		ArrayList<ReconHit> recon = new ArrayList<>();
		int maximum = readAdc(snapshot.bank(name + "::adc").orElse(null), adc);
		readTdc(snapshot.bank(name + "::tdc").orElse(null), tdc);
		readRecon(snapshot.bank(name + "::rec").orElse(null), recon);
		return new CherenkovEventData(name, adc, tdc, recon, maximum);
	}

	private static CherenkovEventData empty(String name) {
		return new CherenkovEventData(name, List.of(), List.of(), List.of(), 0);
	}

	private static int readAdc(DataBank bank, List<AdcHit> out) {
		if (!has(bank, "sector", "layer", "component", "order", "ADC", "time")) return 0;
		int maximum = 0;
		for (int row = 0; row < bank.rows(); row++) {
			int sector = bank.getByte("sector", row);
			int half = bank.getByte("layer", row);
			int ring = bank.getShort("component", row);
			int adc = bank.getInt("ADC", row);
			if (!valid(sector, half, ring) || adc <= 0) continue;
			out.add(new AdcHit(sector, half, ring, bank.getByte("order", row), adc,
					bank.getFloat("time", row)));
			maximum = Math.max(maximum, adc);
		}
		return maximum;
	}

	private static void readTdc(DataBank bank, List<TdcHit> out) {
		if (!has(bank, "sector", "layer", "component", "order", "TDC")) return;
		for (int row = 0; row < bank.rows(); row++) {
			int sector = bank.getByte("sector", row);
			int half = bank.getByte("layer", row);
			int ring = bank.getShort("component", row);
			if (valid(sector, half, ring)) out.add(new TdcHit(sector, half, ring,
					bank.getByte("order", row), bank.getInt("TDC", row)));
		}
	}

	private static void readRecon(DataBank bank, List<ReconHit> out) {
		if (!has(bank, "id", "x", "y", "z")) return;
		for (int row = 0; row < bank.rows(); row++) {
			double x = bank.getFloat("x", row);
			double y = bank.getFloat("y", row);
			double phi = Math.toDegrees(Math.atan2(y, x));
			if (phi < -30.0) phi += 360.0;
			int sector = Math.max(1, Math.min(6, (int) Math.floor((phi + 30.0) / 60.0) + 1));
			out.add(new ReconHit(row, bank.getShort("id", row), sector, x, y,
					bank.getFloat("z", row)));
		}
	}

	private static boolean valid(int sector, int half, int ring) {
		return sector >= 1 && sector <= 6 && half >= 1 && half <= 2 && ring > 0;
	}

	private static boolean has(DataBank bank, String... columns) {
		if (bank == null) return false;
		for (String column : columns) if (!BankAccess.hasColumn(bank, column)) return false;
		return true;
	}

	public record AdcHit(int sector, int half, int ring, int order, int adc, float time) { }
	public record TdcHit(int sector, int half, int ring, int order, int tdc) { }
	public record ReconHit(int row, int id, int sector, double x, double y, double z) { }
}
