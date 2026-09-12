package edu.cnu.ced.view.swim;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
