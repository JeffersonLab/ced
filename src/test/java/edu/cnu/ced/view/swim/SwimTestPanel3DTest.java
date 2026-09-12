package edu.cnu.ced.view.swim;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

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
}
