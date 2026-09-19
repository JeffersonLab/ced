package edu.cnu.ced.data;

import java.util.List;

import cnuphys.snr.SNRAnalysisLevel;
import cnuphys.snr.clas12.Clas12NoiseAnalysis;
import cnuphys.snr.clas12.Clas12NoiseResult;

import edu.cnu.ced.data.DCEventData.RawHit;

/**
 * Wraps {@code cnuphys.snr}'s CLAS12 drift-chamber noise-rejection
 * algorithm: flags which raw DC hits look like electronic/pickup noise
 * rather than real particle hits, from wire-adjacency patterns alone (no
 * timing or reconstruction information needed) -- matching legacy CED's
 * own {@code cnuphys.ced.noise.NoiseManager}, minus its per-(sector,
 * superlayer) parameter-tuning dialog ({@code
 * cnuphys.ced.dcnoise.edit.NoiseParameterDialog}, a separate, not yet
 * ported follow-up); this uses the algorithm's own default parameters
 * for every superlayer.
 *
 * <p>
 * Stateful and not thread-safe, mirroring {@link Clas12NoiseAnalysis}
 * itself: construct one per application (like {@code SwimTrajectoryCache})
 * and call {@link #noiseFlags} only from the thread that owns it (the
 * EDT, same as every view that would call it).
 * </p>
 */
public final class DcNoiseAnalysis {

	private final Clas12NoiseAnalysis analysis = new Clas12NoiseAnalysis(SNRAnalysisLevel.TWOSTAGE);

	/**
	 * Runs the noise-rejection algorithm over one event's raw DC hits.
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
}
