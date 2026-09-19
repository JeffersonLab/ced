package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.data.DCEventData.RawHit;
import edu.cnu.ced.data.DcNoiseAnalysis.Address;
import edu.cnu.ced.data.DcNoiseAnalysis.MaskCell;

class DcNoiseAnalysisTest {

	@Test
	void emptyHitsProduceAnEmptyFlagArray() {
		DcNoiseAnalysis analysis = new DcNoiseAnalysis();
		assertEquals(0, analysis.noiseFlags(List.of()).length);
	}

	@Test
	void anIsolatedHitWithNoNeighborsIsFlaggedAsNoise() {
		DcNoiseAnalysis analysis = new DcNoiseAnalysis();
		List<RawHit> hits = List.of(new RawHit(0, 1, 1, 1, 50, 0, 100));

		boolean[] flags = analysis.noiseFlags(hits);

		assertEquals(1, flags.length);
		assertTrue(flags[0], "a single hit with no adjacent hits in any layer should look like noise");
	}

	@Test
	void aStraightSixLayerRunAtTheSameWireIsNotFlaggedAsNoise() {
		DcNoiseAnalysis analysis = new DcNoiseAnalysis();
		List<RawHit> hits = List.of(
				new RawHit(0, 1, 1, 1, 50, 0, 100),
				new RawHit(1, 1, 1, 2, 50, 0, 100),
				new RawHit(2, 1, 1, 3, 50, 0, 100),
				new RawHit(3, 1, 1, 4, 50, 0, 100),
				new RawHit(4, 1, 1, 5, 50, 0, 100),
				new RawHit(5, 1, 1, 6, 50, 0, 100));

		boolean[] flags = analysis.noiseFlags(hits);

		assertEquals(6, flags.length);
		for (boolean flag : flags) {
			assertFalse(flag, "a contiguous straight-through run looks like a real track, not noise");
		}
	}

	@Test
	void reusingTheSameAnalysisAcrossEventsDoesNotLeakState() {
		DcNoiseAnalysis analysis = new DcNoiseAnalysis();
		List<RawHit> track = List.of(
				new RawHit(0, 1, 1, 1, 50, 0, 100),
				new RawHit(1, 1, 1, 2, 50, 0, 100),
				new RawHit(2, 1, 1, 3, 50, 0, 100),
				new RawHit(3, 1, 1, 4, 50, 0, 100),
				new RawHit(4, 1, 1, 5, 50, 0, 100),
				new RawHit(5, 1, 1, 6, 50, 0, 100));
		List<RawHit> isolated = List.of(new RawHit(0, 2, 3, 1, 20, 0, 100));

		assertFalse(analysis.noiseFlags(track)[0]);
		assertTrue(analysis.noiseFlags(isolated)[0]);
		// Re-running the same track after an unrelated event must still see it as a track.
		assertFalse(analysis.noiseFlags(track)[0]);
	}

	@Test
	void aStraightSixLayerRunProducesMaskCellsCoveringItsOwnWireInEveryLayer() {
		DcNoiseAnalysis analysis = new DcNoiseAnalysis();
		List<RawHit> hits = List.of(
				new RawHit(0, 1, 1, 1, 50, 0, 100),
				new RawHit(1, 1, 1, 2, 50, 0, 100),
				new RawHit(2, 1, 1, 3, 50, 0, 100),
				new RawHit(3, 1, 1, 4, 50, 0, 100),
				new RawHit(4, 1, 1, 5, 50, 0, 100),
				new RawHit(5, 1, 1, 6, 50, 0, 100));

		analysis.noiseFlags(hits);
		Map<Address, List<MaskCell>> allCells = analysis.allMaskCells();

		List<MaskCell> cells = allCells.get(new Address(1, 1));
		assertTrue(cells != null && !cells.isEmpty(), "a real track segment should produce mask cells");
		for (int layer = 1; layer <= 6; layer++) {
			int finalLayer = layer;
			assertTrue(cells.stream().anyMatch(cell -> cell.layer() == finalLayer && cell.wire() == 50),
					"expected a mask cell at layer " + layer + " wire 50, covering the track's own hit");
		}
	}

	@Test
	void anIsolatedHitProducesNoMaskCellsForItsSuperlayer() {
		DcNoiseAnalysis analysis = new DcNoiseAnalysis();
		List<RawHit> hits = List.of(new RawHit(0, 2, 3, 1, 20, 0, 100));

		analysis.noiseFlags(hits);
		Map<Address, List<MaskCell>> allCells = analysis.allMaskCells();

		assertFalse(allCells.containsKey(new Address(2, 3)),
				"an isolated hit has no explaining segment, so no mask cells");
	}
}
