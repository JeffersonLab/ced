package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/**
 * Immutable μrWT (Micro Ring Wire Tracker) data extracted directly from one
 * atomic event snapshot.
 * <p>
 * {@code URWT::hits} addresses a strip by (sector, layer, strip), but its own
 * bank documentation describes layer as spanning 1-9 (three groups of three,
 * associated with PCAL/ECIN/ECOUT), while {@link edu.cnu.ced.geometry.URWTGeometry}
 * models only 4 layers per sector -- the two numbering schemes don't
 * obviously line up, and guessing wrong would silently mis-plot every raw
 * hit. Rather than risk that, {@code Hit} carries its own address for
 * occupancy accounting (see {@link URWTAccumulation}, which only needs the
 * address, not a resolved position) but this class does not attempt to
 * resolve one; a consuming view draws hits only where geometry resolution
 * is unambiguous. {@code URWT::clusters} and {@code URWT::crosses} carry
 * their own explicit world positions directly (already in cm) and don't
 * have this problem.
 * </p>
 */
public record URWTEventData(List<Hit> hits, List<Cluster> clusters, List<Cross> crosses) {

	public static final String HITS_BANK = "URWT::hits";
	public static final String CLUSTERS_BANK = "URWT::clusters";
	public static final String CROSSES_BANK = "URWT::crosses";
	private static final URWTEventData EMPTY = new URWTEventData(List.of(), List.of(), List.of());

	public URWTEventData {
		hits = List.copyOf(hits);
		clusters = List.copyOf(clusters);
		crosses = List.copyOf(crosses);
	}

	public static URWTEventData from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) {
			return EMPTY;
		}
		List<Hit> hits = new ArrayList<>();
		List<Cluster> clusters = new ArrayList<>();
		List<Cross> crosses = new ArrayList<>();
		readHits(snapshot.bank(HITS_BANK).orElse(null), hits);
		readClusters(snapshot.bank(CLUSTERS_BANK).orElse(null), clusters);
		readCrosses(snapshot.bank(CROSSES_BANK).orElse(null), crosses);
		return hits.isEmpty() && clusters.isEmpty() && crosses.isEmpty()
				? EMPTY : new URWTEventData(hits, clusters, crosses);
	}

	private static void readHits(DataBank bank, List<Hit> destination) {
		if (!hasColumns(bank, "sector", "layer", "strip", "energy", "time")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new Hit(row, byteValue(bank, "sector", row), byteValue(bank, "layer", row),
					bank.getShort("strip", row), bank.getFloat("energy", row), bank.getFloat("time", row),
					shortValue(bank, "clusterId", row), shortValue(bank, "status", row)));
		}
	}

	private static void readClusters(DataBank bank, List<Cluster> destination) {
		if (!hasColumns(bank, "sector", "layer", "xo", "yo", "zo", "xe", "ye", "ze")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new Cluster(row, byteValue(bank, "sector", row), byteValue(bank, "layer", row),
					bank.getFloat("xo", row), bank.getFloat("yo", row), bank.getFloat("zo", row),
					bank.getFloat("xe", row), bank.getFloat("ye", row), bank.getFloat("ze", row),
					floatValue(bank, "energy", row), floatValue(bank, "time", row),
					shortValue(bank, "size", row)));
		}
	}

	private static void readCrosses(DataBank bank, List<Cross> destination) {
		if (!hasColumns(bank, "x", "y", "z")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new Cross(row, byteValue(bank, "sector", row), byteValue(bank, "region", row),
					bank.getFloat("x", row), bank.getFloat("y", row), bank.getFloat("z", row),
					floatValue(bank, "energy", row), floatValue(bank, "time", row)));
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

	private static short shortValue(DataBank bank, String name, int row) {
		return BankAccess.hasColumn(bank, name) ? bank.getShort(name, row) : 0;
	}

	private static byte byteValue(DataBank bank, String name, int row) {
		return BankAccess.hasColumn(bank, name) ? bank.getByte(name, row) : 0;
	}

	private static float floatValue(DataBank bank, String name, int row) {
		return BankAccess.hasColumn(bank, name) ? bank.getFloat(name, row) : Float.NaN;
	}

	/** @param layer the bank's own 1..9 layer numbering -- see class doc; not necessarily an URWTGeometry layer */
	public record Hit(int row, int sector, int layer, int strip, float energy, float time,
			int clusterId, int status) { }

	/** World-frame strip endpoints, already in cm -- no geometry lookup needed. */
	public record Cluster(int row, int sector, int layer, float xo, float yo, float zo,
			float xe, float ye, float ze, float energy, float time, int size) { }

	/** World-frame position, already in cm. */
	public record Cross(int row, int sector, int region, float x, float y, float z,
			float energy, float time) { }
}
