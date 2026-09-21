package edu.cnu.ced.view.swim;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.view.swim.SwimTestPanel3D.SurfaceChoice;
import edu.cnu.ced.view.swim.SwimTestPanel3D.SurfaceType;

/**
 * Exercises the one genuinely novel piece of behavior in {@link
 * SwimTestPanel3D}: {@link SwimTestPanel3D#swim} actually swimming a
 * hypothetical particle and updating the displayed trajectory, rather
 * than just constructing the scene ({@link SwimTestView3DTest} already
 * covers construction). {@code SwimTestPanel3D} is package-private, so
 * this constructs one directly rather than going through the view.
 */
class SwimTestPanel3DTest {

	@Test
	void swimmingAReasonableTrackProducesATrajectoryAndClearRemovesIt() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SwimTestPanel3D panel = new SwimTestPanel3D(0f, 0f, 0f, 0f, 0f, 0f);
			// A 1 GeV/c positive track at 25 degrees, from the origin --
			// well within the swimmer's normal operating range.
			boolean first = panel.swim(1, 0, 0, 0, 1.0, 25.0, 0.0);
			assertTrue(first, "expected a usable trajectory for a 1 GeV/c track");

			// Swimming again (a different momentum) must not throw --
			// this exercises the "update the existing PolyLine3D in place"
			// branch of swim(), not just the "create it" one.
			boolean second = panel.swim(-1, 0, 0, 0, 2.0, 40.0, 90.0);
			assertTrue(second, "expected a usable trajectory for the second track too");

			assertDoesNotThrow(panel::clearTrajectory);
		});
	}

	@Test
	void everyReferenceSurfaceSwimsAndDisplaysWithoutError() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SwimTestPanel3D panel = new SwimTestPanel3D(0f, 0f, 0f, 0f, 0f, 0f);

			// Fixed z: builds a Quad3D visual aid.
			assertTrue(panel.swim(1, 0, 0, 0, 1.0, 25.0, 0.0,
					new SurfaceChoice(SurfaceType.FIXED_Z, 300, 0, null, null, null, null, 0, 0.01)));

			// Fixed rho: builds a Cylinder visual aid, replacing the quad.
			assertTrue(panel.swim(1, 0, 0, 0, 1.0, 25.0, 0.0,
					new SurfaceChoice(SurfaceType.FIXED_RHO, 0, 100, null, null, null, null, 0, 0.01)));

			// Plane: builds another Quad3D, from a Plane's own vertex helper.
			assertTrue(panel.swim(1, 0, 0, 0, 1.0, 25.0, 0.0,
					new SurfaceChoice(SurfaceType.PLANE, 0, 0, new double[] { 0, 0, 1 },
							new double[] { 0, 0, 300 }, null, null, 0, 0.01)));

			// Cylinder: builds another Cylinder, from explicit endpoints.
			assertTrue(panel.swim(1, 0, 0, 0, 1.0, 25.0, 0.0,
					new SurfaceChoice(SurfaceType.CYLINDER, 0, 0, null, null,
							new double[] { 0, 0, -100 }, new double[] { 0, 0, 600 }, 100, 0.01)));

			// Back to the full path: removes the cylinder visual aid entirely.
			assertTrue(panel.swim(1, 0, 0, 0, 1.0, 25.0, 0.0, SurfaceChoice.fullPath()));

			assertDoesNotThrow(panel::clearTrajectory);
		});
	}

	@Test
	void runBatchAccumulatesResultsAcrossCallsAndClearEmptiesThem() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SwimTestPanel3D panel = new SwimTestPanel3D(0f, 0f, 0f, 0f, 0f, 0f);

			List<SwimSpec> firstBatch = List.of(
					new SwimSpec(1, 0, 0, 0, 1.0, 25.0, 0.0),
					new SwimSpec(-1, 0, 0, 0, 2.0, 40.0, 90.0));
			int firstSuccesses = panel.runBatch(firstBatch, SurfaceChoice.fullPath());
			assertEquals(2, firstSuccesses, "both reasonable tracks should reach the full path");
			assertEquals(2, panel.batchResults().size());

			// A second call must accumulate, not replace -- matching legacy's
			// own "results persist across Swim Trajectories clicks".
			List<SwimSpec> secondBatch = List.of(new SwimSpec(0, 0, 0, 0, 1.5, 30.0, 180.0));
			int secondSuccesses = panel.runBatch(secondBatch, SurfaceChoice.fullPath());
			assertEquals(1, secondSuccesses);
			assertEquals(3, panel.batchResults().size());

			assertDoesNotThrow(panel::clearBatch);
			assertEquals(0, panel.batchResults().size());
		});
	}

	@Test
	void runBatchKeepsAPartialTrajectoryForAnUnreachableTarget() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SwimTestPanel3D panel = new SwimTestPanel3D(0f, 0f, 0f, 0f, 0f, 0f);

			// theta=0 (straight along +z) but the target z (100000 cm) is
			// nowhere near reachable -- must fail, but still leave a
			// partial-path result behind for SwimBatchDrawer3D to draw in
			// black, matching legacy's own SwimResultDrawer.
			List<SwimSpec> batch = List.of(new SwimSpec(1, 0, 0, 0, 1.0, 0.0, 0.0));
			SurfaceChoice unreachable = new SurfaceChoice(SurfaceType.FIXED_Z, 100000, 0, null, null, null, null, 0, 0.01);

			int successes = panel.runBatch(batch, unreachable);
			assertEquals(0, successes);
			assertEquals(1, panel.batchResults().size(), "a failed swim should still be recorded");
			assertTrue(!panel.batchResults().get(0).success());
		});
	}

	@Test
	void showModeDefaultsToAllAndCanBeChanged() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SwimTestPanel3D panel = new SwimTestPanel3D(0f, 0f, 0f, 0f, 0f, 0f);

			assertEquals(SwimBatchShowMode.ALL, panel.batchShowMode());
			panel.setBatchShowMode(SwimBatchShowMode.FAILURES);
			assertEquals(SwimBatchShowMode.FAILURES, panel.batchShowMode());
		});
	}

	@Test
	void fastMcHitsAreOffByDefaultAndOnlyComputedWhenEnabled() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SwimTestPanel3D panel = new SwimTestPanel3D(0f, 0f, 0f, 0f, 0f, 0f);
			assertTrue(!panel.showFastMcHits());

			// Off by default: a real forward track through a sector center
			// swims fine, but no DC hits are computed for it.
			assertTrue(panel.swim(1, 0, 0, 0, 2.0, 25.0, 0.0));
			assertTrue(panel.manualDcHits().isEmpty());

			// Once enabled, the same swim computes real DC hits.
			panel.setShowFastMcHits(true);
			assertTrue(panel.swim(1, 0, 0, 0, 2.0, 25.0, 0.0));
			assertTrue(!panel.manualDcHits().isEmpty(), "a sector-center forward track should cross DC");

			assertDoesNotThrow(panel::clearTrajectory);
			assertTrue(panel.manualDcHits().isEmpty());
		});
	}

	@Test
	void fastMcHitsAccumulateAcrossBatchRunsAndClearEmptiesThem() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SwimTestPanel3D panel = new SwimTestPanel3D(0f, 0f, 0f, 0f, 0f, 0f);
			panel.setShowFastMcHits(true);

			List<SwimSpec> batch = List.of(new SwimSpec(1, 0, 0, 0, 2.0, 25.0, 0.0));
			panel.runBatch(batch, SurfaceChoice.fullPath());

			assertEquals(1, panel.batchDcHits().size());
			assertTrue(!panel.batchDcHits().get(0).isEmpty(), "a sector-center forward track should cross DC");

			panel.runBatch(batch, SurfaceChoice.fullPath());
			assertEquals(2, panel.batchDcHits().size(), "hits must accumulate across batch runs, like batchResults()");

			assertDoesNotThrow(panel::clearBatch);
			assertEquals(0, panel.batchDcHits().size());
		});
	}
}
