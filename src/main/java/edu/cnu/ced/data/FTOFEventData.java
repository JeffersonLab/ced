package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/** Immutable FTOF data extracted directly from one atomic event snapshot. */
public record FTOFEventData(List<AdcHit> adcHits, List<ReconHit> reconHits,
		List<Cluster> clusters, int maximumAdc) {

	public static final String ADC_BANK = "FTOF::adc";
	public static final String HIT_BANK = "FTOF::hits";
	public static final String CLUSTER_BANK = "FTOF::clusters";
	private static final FTOFEventData EMPTY = new FTOFEventData(
			List.of(), List.of(), List.of(), 0);

	public FTOFEventData {
		adcHits = List.copyOf(adcHits);
		reconHits = List.copyOf(reconHits);
		clusters = List.copyOf(clusters);
		maximumAdc = Math.max(0, maximumAdc);
	}

	public static FTOFEventData from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) return EMPTY;
		ArrayList<AdcHit> adc = new ArrayList<>();
		ArrayList<ReconHit> hits = new ArrayList<>();
		ArrayList<Cluster> clusters = new ArrayList<>();
		int maximum = readAdc(snapshot.bank(ADC_BANK).orElse(null), adc);
		readHits(snapshot.bank(HIT_BANK).orElse(null), hits);
		readClusters(snapshot.bank(CLUSTER_BANK).orElse(null), clusters);
		return adc.isEmpty() && hits.isEmpty() && clusters.isEmpty() ? EMPTY
				: new FTOFEventData(adc, hits, clusters, maximum);
	}

	private static int readAdc(DataBank bank, List<AdcHit> destination) {
		if (!hasColumns(bank, "sector", "layer", "component", "order", "ADC", "time")) return 0;
		int maximum = 0;
		for (int row = 0; row < bank.rows(); row++) {
			int sector = bank.getByte("sector", row);
			int panel = bank.getByte("layer", row) - 1;
			int paddle = bank.getShort("component", row);
			int adc = bank.getInt("ADC", row);
			if (!valid(sector, panel, paddle) || adc <= 0) continue;
			destination.add(new AdcHit(sector, panel, paddle,
					bank.getByte("order", row), adc, bank.getFloat("time", row)));
			maximum = Math.max(maximum, adc);
		}
		return maximum;
	}

	private static void readHits(DataBank bank, List<ReconHit> destination) {
		if (!hasColumns(bank, "sector", "layer", "component", "id", "energy", "time", "x", "y", "z")) return;
		for (int row = 0; row < bank.rows(); row++) {
			int sector = bank.getByte("sector", row);
			int panel = bank.getByte("layer", row) - 1;
			int paddle = bank.getShort("component", row);
			if (!valid(sector, panel, paddle)) continue;
			destination.add(new ReconHit(row, sector, panel, paddle,
					bank.getShort("id", row), bank.getFloat("energy", row),
					bank.getFloat("time", row), bank.getFloat("x", row),
					bank.getFloat("y", row), bank.getFloat("z", row)));
		}
	}

	private static void readClusters(DataBank bank, List<Cluster> destination) {
		if (!hasColumns(bank, "sector", "layer", "component", "id", "status",
				"energy", "time", "x", "y", "z")) return;
		for (int row = 0; row < bank.rows(); row++) {
			int sector = bank.getByte("sector", row);
			int panel = bank.getByte("layer", row) - 1;
			int paddle = bank.getShort("component", row);
			if (!valid(sector, panel, paddle)) continue;
			destination.add(new Cluster(row, sector, panel, paddle,
					bank.getShort("id", row), bank.getShort("status", row),
					bank.getFloat("energy", row), bank.getFloat("time", row),
					bank.getFloat("x", row), bank.getFloat("y", row),
					bank.getFloat("z", row)));
		}
	}

	private static boolean valid(int sector, int panel, int paddle) {
		return sector >= 1 && sector <= 6 && panel >= 0 && panel < 3 && paddle > 0;
	}

	private static boolean hasColumns(DataBank bank, String... names) {
		if (bank == null) return false;
		for (String name : names) if (!BankAccess.hasColumn(bank, name)) return false;
		return true;
	}

	public record AdcHit(int sector, int panel, int paddle, int order, int adc, float time) { }
	public record ReconHit(int row, int sector, int panel, int paddle, int id,
			float energy, float time, float x, float y, float z) { }
	public record Cluster(int row, int sector, int panel, int paddle, int id,
			int status, float energy, float time, float x, float y, float z) { }
}
