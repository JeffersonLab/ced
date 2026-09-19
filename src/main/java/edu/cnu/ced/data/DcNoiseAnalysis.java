package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cnuphys.snr.NoiseReductionParameters;
import cnuphys.snr.SNRAnalysisLevel;
import cnuphys.snr.clas12.Clas12NoiseAnalysis;
import cnuphys.snr.clas12.Clas12NoiseResult;

import edu.cnu.ced.data.DCEventData.RawHit;

/**
 * Wraps {@code cnuphys.snr}'s CLAS12 drift-chamber noise-rejection
 * algorithm: flags which raw DC hits look like electronic/pickup noise
 * rather than real particle hits, from wire-adjacency patterns alone (no
 * timing or reconstruction information needed), and exposes the
 * algorithm's own visual explanation for those flags (which wires it
 * associated with a left- or right-leaning track-like segment) --
 * matching legacy CED's own {@code cnuphys.ced.noise.NoiseManager} in
 * full, minus its per-(sector, superlayer) parameter-tuning dialog
 * ({@code cnuphys.ced.dcnoise.edit.NoiseParameterDialog}, a separate,
 * not yet ported follow-up); this uses the algorithm's own default
 * parameters for every superlayer.
 *
 * <p>
 * Stateful and not thread-safe, mirroring {@link Clas12NoiseAnalysis}
 * itself: construct one per application (like {@code SwimTrajectoryCache})
 * and call {@link #noiseFlags}/{@link #allMaskCells} only from the thread
 * that owns it (the EDT, same as every view that would call it).
 * </p>
 */
public final class DcNoiseAnalysis {

	private static final int SECTOR_COUNT = 6;
	private static final int SUPERLAYER_COUNT = 6;
	private static final int LAYER_COUNT = 6;

	private final Clas12NoiseAnalysis analysis = new Clas12NoiseAnalysis(SNRAnalysisLevel.TWOSTAGE);

	/**
	 * Runs the noise-rejection algorithm over one event's raw DC hits. As
	 * a side effect (mirroring {@code Clas12NoiseAnalysis} itself), this
	 * also refreshes the per-(sector, superlayer) segment masks {@link
	 * #allMaskCells} reads -- call this first, once per event, before
	 * reading masks for the same event.
	 *
	 * @param rawHits this event's raw hits, in the same order as {@link
	 *                DCEventData#rawHits()}
	 * @return which of {@code rawHits} (by list index, same order) the
	 *         algorithm flags as likely noise
	 */
	public boolean[] noiseFlags(List<RawHit> rawHits) {
		int count = rawHits.size();
		int[] sector = new int[count];
		int[] superlayer = new int[count];
		int[] layer = new int[count];
		int[] wire = new int[count];
		for (int i = 0; i < count; i++) {
			RawHit hit = rawHits.get(i);
			sector[i] = hit.sector();
			superlayer[i] = hit.superlayer();
			layer[i] = hit.layer();
			wire[i] = hit.wire();
		}
		analysis.clear();
		Clas12NoiseResult result = new Clas12NoiseResult();
		analysis.findNoise(sector, superlayer, layer, wire, result);
		return result.noise != null ? result.noise : new boolean[count];
	}

	/**
	 * Every (sector, superlayer)'s current segment mask -- the noise
	 * algorithm's own visual explanation of which raw hits it associated
	 * with a real, left- or right-leaning track-like segment the last
	 * time {@link #noiseFlags} ran. A raw hit not covered by any mask
	 * cell here is the hit the algorithm flagged as noise. Matches
	 * legacy CED's own {@code DCHexSuperLayer#drawMasks} exactly
	 * (confirmed empirically: a straight six-layer run produces mask
	 * cells at and around its own wire in every layer with growing
	 * per-layer shifts; an isolated hit produces none).
	 *
	 * @return only the (sector, superlayer) pairs with at least one mask cell
	 */
	public Map<Address, List<MaskCell>> allMaskCells() {
		Map<Address, List<MaskCell>> result = new HashMap<>();
		for (int sector = 1; sector <= SECTOR_COUNT; sector++) {
			for (int superlayer = 1; superlayer <= SUPERLAYER_COUNT; superlayer++) {
				List<MaskCell> cells = maskCells(sector, superlayer);
				if (!cells.isEmpty()) {
					result.put(new Address(sector, superlayer), cells);
				}
			}
		}
		return Map.copyOf(result);
	}

	private List<MaskCell> maskCells(int sector, int superlayer) {
		NoiseReductionParameters parameters = analysis.getParameters(sector - 1, superlayer - 1);
		int numWire = parameters.getNumWire();
		List<MaskCell> cells = new ArrayList<>();
		for (int wire0 = 0; wire0 < numWire; wire0++) {
			if (parameters.getLeftSegments().checkBit(wire0)) {
				addMaskCells(cells, wire0 + 1, parameters.getLeftLayerShifts(), 1, numWire, true);
			}
			if (parameters.getRightSegments().checkBit(wire0)) {
				addMaskCells(cells, wire0 + 1, parameters.getRightLayerShifts(), -1, numWire, false);
			}
		}
		return cells;
	}

	private static void addMaskCells(List<MaskCell> cells, int wire, int[] shifts, int sign, int numWire, boolean left) {
		for (int layer = 1; layer <= LAYER_COUNT; layer++) {
			cells.add(new MaskCell(layer, wire, left));
			for (int shift = 1; shift <= shifts[layer - 1]; shift++) {
				int shiftedWire = wire + sign * shift;
				if (shiftedWire > 0 && shiftedWire <= numWire) {
					cells.add(new MaskCell(layer, shiftedWire, left));
				}
			}
		}
	}

	/** 1-based (sector, superlayer) address. */
	public record Address(int sector, int superlayer) { }

	/**
	 * One (1-based layer, 1-based wire) cell covered by a segment mask,
	 * and whether it came from a left-leaning ({@code true}) or
	 * right-leaning ({@code false}) segment -- a view typically colors
	 * the two differently, matching legacy's own {@code maskFillLeft}/
	 * {@code maskFillRight}.
	 */
	public record MaskCell(int layer, int wire, boolean left) { }
}
