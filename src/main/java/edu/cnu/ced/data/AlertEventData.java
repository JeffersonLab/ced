package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/**
 * Immutable ALERT (AHDC drift chamber + ATOF time-of-flight) data extracted
 * directly from one atomic event snapshot.
 * <p>
 * {@code AHDC::adc} packs its wire's superlayer/layer into a single decimal
 * digit pair ({@code layer = 10*superlayer + layer}, both 1-based) rather
 * than separate columns -- confirmed against legacy CED's own {@code
 * AlertDCGeometryNumbering.fromDataNumbering}, not guessed, since a wrong
 * decode would silently misplace every wire hit. {@code AHDC::hits} carries
 * superlayer and layer as separate columns instead. Both resolve a wire's
 * position via {@link edu.cnu.ced.geometry.AlertGeometry#dcWires}, since
 * neither bank gives one directly; AHDC's own address has no real sector
 * division (the geometry has exactly one: sector 0), matching the bank's own
 * always-1 sector column.
 * </p>
 * <p>
 * {@code AHDC::clusters} and both ATOF banks carry an explicit world
 * position already -- but in mm (per their own bank documentation), unlike
 * {@code AlertGeometry}'s own points, which are already cm (confirmed
 * empirically, same as FMTGeometry/URWTGeometry) -- so those, and only
 * those, need a /10 here.
 * </p>
 */
public record AlertEventData(List<DcAdcHit> dcAdcHits, List<DcHit> dcHits, List<DcCluster> dcClusters,
		List<TofHit> tofHits, List<TofCluster> tofClusters, int maximumDcAdc) {

	public static final String DC_ADC_BANK = "AHDC::adc";
	public static final String DC_HITS_BANK = "AHDC::hits";
	public static final String DC_CLUSTERS_BANK = "AHDC::clusters";
	public static final String TOF_HITS_BANK = "ATOF::hits";
	public static final String TOF_CLUSTERS_BANK = "ATOF::clusters";
	private static final AlertEventData EMPTY =
			new AlertEventData(List.of(), List.of(), List.of(), List.of(), List.of(), 0);
	private static final float MM_PER_CM = 10f;

	public AlertEventData {
		dcAdcHits = List.copyOf(dcAdcHits);
		dcHits = List.copyOf(dcHits);
		dcClusters = List.copyOf(dcClusters);
		tofHits = List.copyOf(tofHits);
		tofClusters = List.copyOf(tofClusters);
		maximumDcAdc = Math.max(0, maximumDcAdc);
	}

	public static AlertEventData from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) {
			return EMPTY;
		}
		List<DcAdcHit> dcAdc = new ArrayList<>();
		List<DcHit> dcHits = new ArrayList<>();
		List<DcCluster> dcClusters = new ArrayList<>();
		List<TofHit> tofHits = new ArrayList<>();
		List<TofCluster> tofClusters = new ArrayList<>();
		int maximum = readDcAdc(snapshot.bank(DC_ADC_BANK).orElse(null), dcAdc);
		readDcHits(snapshot.bank(DC_HITS_BANK).orElse(null), dcHits);
		readDcClusters(snapshot.bank(DC_CLUSTERS_BANK).orElse(null), dcClusters);
		readTofHits(snapshot.bank(TOF_HITS_BANK).orElse(null), tofHits);
		readTofClusters(snapshot.bank(TOF_CLUSTERS_BANK).orElse(null), tofClusters);
		return dcAdc.isEmpty() && dcHits.isEmpty() && dcClusters.isEmpty()
				&& tofHits.isEmpty() && tofClusters.isEmpty()
				? EMPTY : new AlertEventData(dcAdc, dcHits, dcClusters, tofHits, tofClusters, maximum);
	}

	private static int readDcAdc(DataBank bank, List<DcAdcHit> destination) {
		if (!hasColumns(bank, "sector", "layer", "component", "ADC", "time")) {
			return 0;
		}
		int maximum = 0;
		for (int row = 0; row < bank.rows(); row++) {
			int packedLayer = bank.getByte("layer", row);
			int superlayer = packedLayer / 10 - 1;
			int layer = packedLayer % 10 - 1;
			int adc = bank.getInt("ADC", row);
			if (superlayer < 0 || layer < 0 || adc <= 0) {
				continue;
			}
			destination.add(new DcAdcHit(bank.getByte("sector", row) - 1, superlayer, layer,
					bank.getShort("component", row) - 1, adc, bank.getFloat("time", row)));
			maximum = Math.max(maximum, adc);
		}
		return maximum;
	}

	private static void readDcHits(DataBank bank, List<DcHit> destination) {
		if (!hasColumns(bank, "layer", "superlayer", "wire", "time")) {
			return;
		}
		boolean hasDoca = BankAccess.hasColumn(bank, "doca");
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new DcHit(row, bank.getByte("superlayer", row) - 1, bank.getByte("layer", row) - 1,
					bank.getInt("wire", row) - 1, (float) bank.getDouble("time", row),
					hasDoca ? (float) bank.getDouble("doca", row) : Float.NaN, intValue(bank, "trackid", row)));
		}
	}

	private static void readDcClusters(DataBank bank, List<DcCluster> destination) {
		if (!hasColumns(bank, "x", "y", "z")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new DcCluster(row, bank.getFloat("x", row) / MM_PER_CM,
					bank.getFloat("y", row) / MM_PER_CM, bank.getFloat("z", row) / MM_PER_CM));
		}
	}

	private static void readTofHits(DataBank bank, List<TofHit> destination) {
		if (!hasColumns(bank, "sector", "layer", "component", "x", "y", "z", "time")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new TofHit(row, bank.getInt("sector", row), bank.getInt("layer", row),
					bank.getInt("component", row), bank.getFloat("x", row) / MM_PER_CM,
					bank.getFloat("y", row) / MM_PER_CM, bank.getFloat("z", row) / MM_PER_CM,
					floatValue(bank, "energy", row), bank.getFloat("time", row)));
		}
	}

	private static void readTofClusters(DataBank bank, List<TofCluster> destination) {
		if (!hasColumns(bank, "x", "y", "z", "time")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new TofCluster(row, bank.getFloat("x", row) / MM_PER_CM,
					bank.getFloat("y", row) / MM_PER_CM, bank.getFloat("z", row) / MM_PER_CM,
					floatValue(bank, "energy", row), bank.getFloat("time", row)));
		}
	}

	private static boolean hasColumns(DataBank bank, String... names) {
		if (bank == null) {
			return false;
		}
		for (String name : names) {
			if (!BankAccess.hasColumn(bank, name)) {
				return false;
			}
		}
		return true;
	}

	private static int intValue(DataBank bank, String name, int row) {
		return BankAccess.hasColumn(bank, name) ? bank.getInt(name, row) : 0;
	}

	private static float floatValue(DataBank bank, String name, int row) {
		return BankAccess.hasColumn(bank, name) ? bank.getFloat(name, row) : Float.NaN;
	}

	/** @param sector always 0 (AHDC has no real sector division); layer/superlayer/wire all 0-based */
	public record DcAdcHit(int sector, int superlayer, int layer, int wire, int adc, float time) { }

	public record DcHit(int row, int superlayer, int layer, int wire, float time, float doca, int trackId) { }

	/** World position, already converted to cm. */
	public record DcCluster(int row, float x, float y, float z) { }

	/** World position, already converted to cm; sector/layer/component are ATOF's own (paddle) addressing, not resolved via geometry. */
	public record TofHit(int row, int sector, int layer, int component, float x, float y, float z,
			float energy, float time) { }

	/** World position, already converted to cm. */
	public record TofCluster(int row, float x, float y, float z, float energy, float time) { }
}
