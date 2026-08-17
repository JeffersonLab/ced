package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/** Immutable drift-chamber data extracted from one atomic event snapshot. */
public record DCEventData(List<RawHit> rawHits, List<ReconHit> reconHits) {

	public static final String RAW_BANK = "DC::tdc";
	public static final String TOT_BANK = "DC::tot";
	private static final DCEventData EMPTY = new DCEventData(List.of(), List.of());

	public enum ReconKind { HB, TB, AI_HB, AI_TB }

	public DCEventData {
		rawHits = List.copyOf(rawHits);
		reconHits = List.copyOf(reconHits);
	}

	public static DCEventData from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) return EMPTY;
		ArrayList<RawHit> raw = new ArrayList<>();
		ArrayList<ReconHit> recon = new ArrayList<>();
		DataBank timingBank = snapshot.bank(RAW_BANK).filter(bank -> bank.rows() > 0)
				.orElseGet(() -> snapshot.bank(TOT_BANK).filter(bank -> bank.rows() > 0)
						.orElse(null));
		readRaw(timingBank, raw);
		readRecon(snapshot.bank("HitBasedTrkg::Hits").orElse(null), ReconKind.HB, recon);
		readRecon(snapshot.bank("TimeBasedTrkg::TBHits").orElse(null), ReconKind.TB, recon);
		readRecon(snapshot.bank("HitBasedTrkg::AIHits").orElse(null), ReconKind.AI_HB, recon);
		readRecon(snapshot.bank("TimeBasedTrkg::AIHits").orElse(null), ReconKind.AI_TB, recon);
		return raw.isEmpty() && recon.isEmpty() ? EMPTY : new DCEventData(raw, recon);
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
}
