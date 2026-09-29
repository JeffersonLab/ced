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
 * track through a sector's own center crosses every one of DC's 36
 * (superlayer, layer) planes, FTOF's 2 panels, PCAL's 3 views, and ECAL's
 * 6 (stack, view) planes it's aimed at -- all belonging to the same
 * 0-based sector -- while a track aimed straight down a sector boundary
 * crosses none of them.
 */
class FastMcHitFinderTest {

	private static final int EXPECTED_DC_LAYERS = 36; // 6 superlayers x 6 layers
	private static final int EXPECTED_FTOF_PANELS = 2;
	private static final int EXPECTED_PCAL_VIEWS = 3;
	private static final int EXPECTED_ECAL_PLANES = 6; // 2 stacks x 3 views

	@Test
	void aTrackThroughASectorCenterCrossesEveryDetectorItReaches() {
		FastMcHitFinder finder = new FastMcHitFinder();
		FastMcHitFinder.Hits hits = finder.findHits(straightLine(25.0, 0.0, 1000.0));

		assertEquals(EXPECTED_DC_LAYERS, hits.dc().size());
		assertEquals(EXPECTED_FTOF_PANELS, hits.ftof().size());
		assertEquals(EXPECTED_PCAL_VIEWS, hits.pcal().size());
		assertEquals(EXPECTED_ECAL_PLANES, hits.ecal().size());

		for (List<DetectorHit> group : List.of(hits.dc(), hits.ftof(), hits.pcal(), hits.ecal())) {
			for (DetectorHit hit : group) {
				assertEquals(0, hit.getSectorId(), "a phi=0 track belongs entirely to 0-based sector 0");
			}
		}
	}

	@Test
	void aTrackDownASectorBoundaryCrossesNothing() {
		FastMcHitFinder finder = new FastMcHitFinder();
		// Sector centers sit at phi = 0, 60, 120, ...; sector boundaries at
		// phi = 30, 90, 150, ... -- confirmed empirically this misses every layer.
		FastMcHitFinder.Hits hits = finder.findHits(straightLine(25.0, 30.0, 1000.0));

		assertTrue(hits.dc().isEmpty());
		assertTrue(hits.ftof().isEmpty());
		assertTrue(hits.pcal().isEmpty());
		assertTrue(hits.ecal().isEmpty());
	}

	@Test
	void tooShortATrajectoryFindsNoHits() {
		FastMcHitFinder finder = new FastMcHitFinder();
		assertEquals(FastMcHitFinder.Hits.EMPTY, finder.findHits(List.of(new Point3(0, 0, 0))));
		assertEquals(FastMcHitFinder.Hits.EMPTY, finder.findHits(List.of()));
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
