package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.ced.geometry.FMTGeometry;

/**
 * Immutable Forward Micromegas Tracker (FMT) data extracted directly from one
 * atomic event snapshot.
 * <p>
 * {@code FMT::Hits} and {@code FMT::Clusters} give a sub-strip position
 * ({@code localY}/{@code centroid}) along the strip rather than a full (x, y,
 * z); this class doesn't resolve that into a world position (nothing else in
 * this codebase's other detector data classes stores world coordinates for a
 * strip-addressed hit either -- see {@code CentralEventData.ReconHit}), so a
 * consuming view draws these at their strip's own geometric position and
 * treats the sub-strip refinement as a later addition if ever needed.
 * </p>
 */
public record FMTEventData(List<AdcHit> adcHits, List<ReconHit> reconHits,
		List<Cluster> clusters, List<Cross> crosses, List<Trajectory> trajectories,
		List<TrackStatus> trackStatuses, int maximumAdc) {

	public static final String ADC_BANK = "FMT::adc";
	public static final String HITS_BANK = "FMT::Hits";
	public static final String CLUSTERS_BANK = "FMT::Clusters";
	public static final String CROSSES_BANK = "FMT::Crosses";
	public static final String TRAJECTORY_BANK = "FMT::Trajectory";
	public static final String TRACKS_BANK = "FMT::Tracks";
	private static final FMTEventData EMPTY =
			new FMTEventData(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 0);

	public FMTEventData {
		adcHits = List.copyOf(adcHits);
		reconHits = List.copyOf(reconHits);
		clusters = List.copyOf(clusters);
		crosses = List.copyOf(crosses);
		trajectories = List.copyOf(trajectories);
		trackStatuses = List.copyOf(trackStatuses);
		maximumAdc = Math.max(0, maximumAdc);
	}

	public static FMTEventData from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) {
			return EMPTY;
		}
		List<AdcHit> adc = new ArrayList<>();
		List<ReconHit> hits = new ArrayList<>();
		List<Cluster> clusters = new ArrayList<>();
		List<Cross> crosses = new ArrayList<>();
		List<Trajectory> trajectories = new ArrayList<>();
		List<TrackStatus> trackStatuses = new ArrayList<>();
		int maximum = readAdc(snapshot.bank(ADC_BANK).orElse(null), adc);
		readHits(snapshot.bank(HITS_BANK).orElse(null), hits);
		readClusters(snapshot.bank(CLUSTERS_BANK).orElse(null), clusters);
		readCrosses(snapshot.bank(CROSSES_BANK).orElse(null), crosses);
		readTrajectories(snapshot.bank(TRAJECTORY_BANK).orElse(null), trajectories);
		readTrackStatuses(snapshot.bank(TRACKS_BANK).orElse(null), trackStatuses);
		return adc.isEmpty() && hits.isEmpty() && clusters.isEmpty() && crosses.isEmpty()
				&& trajectories.isEmpty() && trackStatuses.isEmpty()
				? EMPTY : new FMTEventData(adc, hits, clusters, crosses, trajectories, trackStatuses, maximum);
	}

	private static int readAdc(DataBank bank, List<AdcHit> destination) {
		if (!hasColumns(bank, "layer", "component", "order", "ADC", "time")) {
			return 0;
		}
		int maximum = 0;
		for (int row = 0; row < bank.rows(); row++) {
			int layer = bank.getByte("layer", row);
			int adc = bank.getInt("ADC", row);
			if (layer < 1 || layer > FMTGeometry.LAYER_COUNT || adc <= 0) {
				continue;
			}
			destination.add(new AdcHit(layer - 1, bank.getShort("component", row),
					bank.getByte("order", row), adc, bank.getFloat("time", row)));
			maximum = Math.max(maximum, adc);
		}
		return maximum;
	}

	private static void readHits(DataBank bank, List<ReconHit> destination) {
		if (!hasColumns(bank, "layer", "strip", "energy", "time")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			int layer = bank.getByte("layer", row);
			if (layer < 1 || layer > FMTGeometry.LAYER_COUNT) {
				continue;
			}
			destination.add(new ReconHit(row, layer - 1, bank.getShort("strip", row),
					bank.getFloat("energy", row), bank.getFloat("time", row),
					shortValue(bank, "clusterIndex", row), shortValue(bank, "trackIndex", row)));
		}
	}

	private static void readClusters(DataBank bank, List<Cluster> destination) {
		if (!hasColumns(bank, "layer", "seedStrip", "size", "energy", "time")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			int layer = bank.getByte("layer", row);
			if (layer < 1 || layer > FMTGeometry.LAYER_COUNT) {
				continue;
			}
			destination.add(new Cluster(row, layer - 1, bank.getShort("seedStrip", row),
					bank.getShort("size", row), bank.getFloat("energy", row), bank.getFloat("time", row),
					shortValue(bank, "trackIndex", row)));
		}
	}

	private static void readCrosses(DataBank bank, List<Cross> destination) {
		if (!hasColumns(bank, "x", "y", "z")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new Cross(row, bank.getFloat("x", row), bank.getFloat("y", row),
					bank.getFloat("z", row), byteValue(bank, "sector", row), byteValue(bank, "region", row),
					shortValue(bank, "trkID", row)));
		}
	}

	private static void readTrajectories(DataBank bank, List<Trajectory> destination) {
		if (!hasColumns(bank, "index", "layer", "x", "y", "z", "dx", "dy", "dz")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			int layer = bank.getByte("layer", row);
			if (layer < 1 || layer > FMTGeometry.LAYER_COUNT) {
				continue;
			}
			destination.add(new Trajectory(bank.getShort("index", row), layer - 1,
					bank.getFloat("x", row), bank.getFloat("y", row), bank.getFloat("z", row),
					bank.getFloat("dx", row), bank.getFloat("dy", row), bank.getFloat("dz", row)));
		}
	}

	private static void readTrackStatuses(DataBank bank, List<TrackStatus> destination) {
		if (!hasColumns(bank, "index", "status")) {
			return;
		}
		for (int row = 0; row < bank.rows(); row++) {
			destination.add(new TrackStatus(bank.getShort("index", row), bank.getByte("status", row)));
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

	/**
	 * @param layer 0-based FMT layer (0..{@value FMTGeometry#LAYER_COUNT}-1)
	 * @param strip 1-based strip number
	 * @param order 0 = ADCL, 1 = ADCR
	 */
	public record AdcHit(int layer, int strip, int order, int adc, float time) { }

	/** @param layer 0-based FMT layer; @param strip 1-based strip number */
	public record ReconHit(int row, int layer, int strip, float energy, float time,
			int clusterIndex, int trackIndex) { }

	/** @param layer 0-based FMT layer; @param seedStrip 1-based strip number of the cluster's seed */
	public record Cluster(int row, int layer, int seedStrip, int size, float energy, float time,
			int trackIndex) { }

	/** World (lab-frame) position, already in cm -- unlike the strip-addressed rows above. */
	public record Cross(int row, float x, float y, float z, int sector, int region, int trackId) { }

	/**
	 * One track's position/direction at one FMT layer, from {@code
	 * FMT::Trajectory}. {@code x}/{@code y}/{@code z} are the world
	 * (lab-frame) position, already in cm -- but exactly {@code (0, 0, 0)}
	 * when the reconstruction didn't compute one for this row, in which
	 * case a consumer should resolve one from {@code dx}/{@code dy}/{@code
	 * dz} (the DC track's own position in this layer's local coordinates,
	 * also cm) via {@link edu.cnu.ced.geometry.FMTGeometry#localToGlobal},
	 * matching legacy CED's own {@code FMTTrajectoryDrawer3D} fallback.
	 *
	 * @param trackIndex row index into the DC track bank (matches {@link
	 *                    TrackStatus#trackIndex()}, not this class's own
	 *                    {@code row})
	 * @param layer       0-based FMT layer
	 */
	public record Trajectory(int trackIndex, int layer, float x, float y, float z,
			float dx, float dy, float dz) { }

	/**
	 * One DC track's FMT-refit status, from {@code FMT::Tracks} --
	 * whether the track shown at an {@link Trajectory} was actually
	 * refitted using FMT information ({@code status == 0}) or is still
	 * just its original DC-only fit ({@code status == 1}), per that
	 * bank's own documentation.
	 */
	public record TrackStatus(int trackIndex, int status) {
		public boolean isOriginalDcTrack() {
			return status == 1;
		}
	}
}
