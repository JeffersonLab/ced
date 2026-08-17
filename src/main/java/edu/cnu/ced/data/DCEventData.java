package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/** Immutable drift-chamber data extracted from one atomic event snapshot. */
public record DCEventData(List<RawHit> rawHits, List<ReconHit> reconHits,
		List<Cluster> clusters, List<Segment> segments, List<Cross> crosses) {

	public static final String RAW_BANK = "DC::tdc";
	public static final String TOT_BANK = "DC::tot";
	private static final DCEventData EMPTY = new DCEventData(List.of(), List.of(),
			List.of(), List.of(), List.of());

	public enum ReconKind { HB, TB, AI_HB, AI_TB }

	public DCEventData {
		rawHits = List.copyOf(rawHits);
		reconHits = List.copyOf(reconHits);
		clusters = List.copyOf(clusters);
		segments = List.copyOf(segments);
		crosses = List.copyOf(crosses);
	}

	/** Compatibility constructor for callers that only supply hit data. */
	public DCEventData(List<RawHit> rawHits, List<ReconHit> reconHits) {
		this(rawHits, reconHits, List.of(), List.of(), List.of());
	}

	/** Compatibility constructor for callers that do not yet supply crosses. */
	public DCEventData(List<RawHit> rawHits, List<ReconHit> reconHits,
			List<Cluster> clusters, List<Segment> segments) {
		this(rawHits, reconHits, clusters, segments, List.of());
	}

	public static DCEventData from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) return EMPTY;
		ArrayList<RawHit> raw = new ArrayList<>();
		ArrayList<ReconHit> recon = new ArrayList<>();
		ArrayList<Cluster> clusters = new ArrayList<>();
		ArrayList<Segment> segments = new ArrayList<>();
		ArrayList<Cross> crosses = new ArrayList<>();
		DataBank timingBank = snapshot.bank(RAW_BANK).filter(bank -> bank.rows() > 0)
				.orElseGet(() -> snapshot.bank(TOT_BANK).filter(bank -> bank.rows() > 0)
						.orElse(null));
		readRaw(timingBank, raw);
		readRecon(snapshot.bank("HitBasedTrkg::Hits").orElse(null), ReconKind.HB, recon);
		readRecon(snapshot.bank("TimeBasedTrkg::TBHits").orElse(null), ReconKind.TB, recon);
		readRecon(snapshot.bank("HitBasedTrkg::AIHits").orElse(null), ReconKind.AI_HB, recon);
		readRecon(snapshot.bank("TimeBasedTrkg::AIHits").orElse(null), ReconKind.AI_TB, recon);
		readClusters(snapshot.bank("HitBasedTrkg::HBClusters").orElse(null), ReconKind.HB, clusters);
		readClusters(snapshot.bank("TimeBasedTrkg::TBClusters").orElse(null), ReconKind.TB, clusters);
		readClusters(snapshot.bank("HitBasedTrkg::AIClusters").orElse(null), ReconKind.AI_HB, clusters);
		readClusters(snapshot.bank("TimeBasedTrkg::AIClusters").orElse(null), ReconKind.AI_TB, clusters);
		readSegments(snapshot.bank("HitBasedTrkg::HBSegments").orElse(null), ReconKind.HB, segments);
		readSegments(snapshot.bank("TimeBasedTrkg::TBSegments").orElse(null), ReconKind.TB, segments);
		readSegments(snapshot.bank("HitBasedTrkg::AISegments").orElse(null), ReconKind.AI_HB, segments);
		readSegments(snapshot.bank("TimeBasedTrkg::AISegments").orElse(null), ReconKind.AI_TB, segments);
		readCrosses(snapshot.bank("HitBasedTrkg::HBCrosses").orElse(null), ReconKind.HB, crosses);
		readCrosses(snapshot.bank("TimeBasedTrkg::TBCrosses").orElse(null), ReconKind.TB, crosses);
		readCrosses(snapshot.bank("HitBasedTrkg::AICrosses").orElse(null), ReconKind.AI_HB, crosses);
		readCrosses(snapshot.bank("TimeBasedTrkg::AICrosses").orElse(null), ReconKind.AI_TB, crosses);
		return raw.isEmpty() && recon.isEmpty() && clusters.isEmpty() && segments.isEmpty()
				&& crosses.isEmpty() ? EMPTY : new DCEventData(raw, recon, clusters, segments, crosses);
	}

	private static void readCrosses(DataBank bank, ReconKind kind,
			List<Cross> destination) {
		if (!hasColumns(bank, "sector", "region", "id", "x", "y", "z",
				"ux", "uy", "uz")) return;
		for (int row = 0; row < bank.rows(); row++) {
			int sector = bank.getByte("sector", row);
			int region = bank.getByte("region", row);
			float x = bank.getFloat("x", row);
			float y = bank.getFloat("y", row);
			float z = bank.getFloat("z", row);
			if (sector < 1 || sector > 6 || region < 1 || region > 3
					|| !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) continue;
			destination.add(new Cross(kind, row, sector, region,
					bank.getShort("id", row), x, y, z,
					optionalFloat(bank, "err_x", row), optionalFloat(bank, "err_y", row),
					optionalFloat(bank, "err_z", row), bank.getFloat("ux", row),
					bank.getFloat("uy", row), bank.getFloat("uz", row),
					optionalShort(bank, "Segment1_ID", row),
					optionalShort(bank, "Segment2_ID", row)));
		}
	}

	private static void readClusters(DataBank bank, ReconKind kind,
			List<Cluster> destination) {
		if (!hasColumns(bank, "sector", "superlayer", "id")) return;
		for (int row = 0; row < bank.rows(); row++) {
			int sector = bank.getByte("sector", row);
			int superlayer = bank.getByte("superlayer", row);
			if (sector < 1 || sector > 6 || superlayer < 1 || superlayer > 6) continue;
			ArrayList<Integer> hitIds = new ArrayList<>(12);
			for (int hit = 1; hit <= 12; hit++) {
				String column = "Hit" + hit + "_ID";
				if (!BankAccess.hasColumn(bank, column)) break;
				int id = bank.getShort(column, row);
				if (id <= 0) break;
				hitIds.add(id);
			}
			destination.add(new Cluster(kind, row, sector, superlayer,
					bank.getShort("id", row), List.copyOf(hitIds)));
		}
	}

	private static void readSegments(DataBank bank, ReconKind kind,
			List<Segment> destination) {
		if (!hasColumns(bank, "sector", "superlayer", "SegEndPoint1X",
				"SegEndPoint1Z", "SegEndPoint2X", "SegEndPoint2Z")) return;
		for (int row = 0; row < bank.rows(); row++) {
			int sector = bank.getByte("sector", row);
			int superlayer = bank.getByte("superlayer", row);
			if (sector < 1 || sector > 6 || superlayer < 1 || superlayer > 6) continue;
			destination.add(new Segment(kind, row, sector, superlayer,
					bank.getFloat("SegEndPoint1X", row), bank.getFloat("SegEndPoint1Z", row),
					bank.getFloat("SegEndPoint2X", row), bank.getFloat("SegEndPoint2Z", row)));
		}
	}

	private static void readRaw(DataBank bank, List<RawHit> destination) {
		if (!hasColumns(bank, "sector", "layer", "component", "order", "TDC")) return;
		for (int row = 0; row < bank.rows(); row++) {
			int sector = bank.getByte("sector", row);
			int globalLayer = bank.getByte("layer", row);
			int wire = bank.getShort("component", row);
			int superlayer = ((globalLayer - 1) / 6) + 1;
			int layer = ((globalLayer - 1) % 6) + 1;
			if (valid(sector, superlayer, layer, wire)) destination.add(new RawHit(row,
					sector, superlayer, layer, wire, bank.getByte("order", row),
					bank.getInt("TDC", row)));
		}
	}

	private static void readRecon(DataBank bank, ReconKind kind,
			List<ReconHit> destination) {
		if (!hasColumns(bank, "sector", "superlayer", "layer", "wire")) return;
		for (int row = 0; row < bank.rows(); row++) {
			int sector = bank.getByte("sector", row);
			int superlayer = bank.getByte("superlayer", row);
			int layer = bank.getByte("layer", row);
			int wire = bank.getShort("wire", row);
			if (valid(sector, superlayer, layer, wire)) destination.add(new ReconHit(kind,
					row, sector, superlayer, layer, wire, optionalShort(bank, "id", row),
					optionalShort(bank, "status", row), optionalShort(bank, "clusterID", row),
					optionalFloat(bank, "trkDoca", row), optionalFloat(bank, "docaError", row)));
		}
	}

	private static boolean valid(int sector, int superlayer, int layer, int wire) {
		return sector >= 1 && sector <= 6 && superlayer >= 1 && superlayer <= 6
				&& layer >= 1 && layer <= 6 && wire >= 1 && wire <= 112;
	}

	private static boolean hasColumns(DataBank bank, String... names) {
		if (bank == null) return false;
		for (String name : names) if (!BankAccess.hasColumn(bank, name)) return false;
		return true;
	}

	private static int optionalShort(DataBank bank, String name, int row) {
		return BankAccess.hasColumn(bank, name) ? bank.getShort(name, row) : -1;
	}

	private static float optionalFloat(DataBank bank, String name, int row) {
		return BankAccess.hasColumn(bank, name) ? bank.getFloat(name, row) : Float.NaN;
	}

	public record RawHit(int row, int sector, int superlayer, int layer, int wire,
			int order, int tdc) { }

	public record ReconHit(ReconKind kind, int row, int sector, int superlayer,
			int layer, int wire, int id, int status, int clusterId, float trackDoca,
			float docaError) { }

	public record Cluster(ReconKind kind, int row, int sector, int superlayer,
			int id, List<Integer> hitIds) { }

	public record Segment(ReconKind kind, int row, int sector, int superlayer,
			float x1, float z1, float x2, float z2) { }

	public record Cross(ReconKind kind, int row, int sector, int region, int id,
			float x, float y, float z, float errorX, float errorY, float errorZ,
			float directionX, float directionY, float directionZ,
			int segment1Id, int segment2Id) { }
}
