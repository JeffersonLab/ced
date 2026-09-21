package edu.cnu.ced.swim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jlab.geom.DetectorHit;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.geometry.Point3;

/**
 * Confirms {@link FastMcHitFinder} against the same empirical signature
 * used to validate it by hand before writing any drawing code: a straight
 * track through a DC sector's own center crosses every one of that
 * sector's 36 (superlayer, layer) planes exactly once, while a track aimed
 * straight down a sector boundary crosses none.
 */
class FastMcHitFinderTest {

	private static final int EXPECTED_LAYERS_PER_SECTOR = 36; // 6 superlayers x 6 layers

	@Test
	void aTrackThroughASectorCenterCrossesEveryDcLayer() {
		FastMcHitFinder finder = new FastMcHitFinder();
		List<DetectorHit> hits = finder.findDcHits(straightLine(25.0, 0.0, 1000.0));

		assertEquals(EXPECTED_LAYERS_PER_SECTOR, hits.size());
		for (DetectorHit hit : hits) {
			assertEquals(0, hit.getSectorId(), "a phi=0 track belongs entirely to 0-based sector 0");
		}
	}

	@Test
	void aTrackDownASectorBoundaryCrossesNoDcLayers() {
		FastMcHitFinder finder = new FastMcHitFinder();
		// Sector centers sit at phi = 0, 60, 120, ...; sector boundaries at
		// phi = 30, 90, 150, ... -- confirmed empirically this misses every layer.
		List<DetectorHit> hits = finder.findDcHits(straightLine(25.0, 30.0, 1000.0));

		assertTrue(hits.isEmpty());
	}

	@Test
	void tooShortATrajectoryFindsNoHits() {
		FastMcHitFinder finder = new FastMcHitFinder();
		assertEquals(0, finder.findDcHits(List.of(new Point3(0, 0, 0))).size());
		assertEquals(0, finder.findDcHits(List.of()).size());
	}

	/** A straight lab-frame line from the origin, sampled every 1 cm -- matching the density used to validate this class by hand. */
	private static List<Point3> straightLine(double thetaDeg, double phiDeg, double lengthCm) {
		double thetaRad = Math.toRadians(thetaDeg);
		double phiRad = Math.toRadians(phiDeg);
		double ux = Math.sin(thetaRad) * Math.cos(phiRad);
		double uy = Math.sin(thetaRad) * Math.sin(phiRad);
		double uz = Math.cos(thetaRad);
		List<Point3> points = new ArrayList<>();
		for (double s = 0; s <= lengthCm; s += 1.0) {
			points.add(new Point3(ux * s, uy * s, uz * s));
		}
		return points;
	}
}
