package edu.cnu.ced.view.ftcal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventStore;
import edu.cnu.ced.geometry.FTCALGeometry;

/**
 * Construction smoke test for {@link FTCalView3D} and the shared {@code
 * edu.cnu.ced.view3d} infrastructure it is built on.
 *
 * <p>
 * {@link edu.cnu.ced.view3d.CedPanel3D}'s {@code addNorth()}/{@code
 * addEast()} hooks -- and {@link edu.cnu.ced.view3d.CedView3D}'s own
 * "Next" button installation -- run partway through their respective
 * superclass constructors, before this class's own constructor bodies
 * (where their real arguments live) have executed; see the ordering
 * comments in those classes. This test exists to catch a regression in
 * that ordering (an NPE from a field read before it is actually assigned)
 * that a compile alone would not catch.
 * </p>
 */
class FTCalView3DTest {

	@Test
	void constructsAndDisposesWithoutError() throws Exception {
		FTCALGeometry geometry = new FTCALGeometry();
		geometry.initializeFromSource();
		EventNavigator navigator = new EventNavigator(new EventStore());

		SwingUtilities.invokeAndWait(() -> {
			FTCalView3D view = assertDoesNotThrow(() -> new FTCalView3D(geometry, navigator));
			assertEquals("FTCal 3D", view.getTitle());
			assertDoesNotThrow(view::dispose);
		});
	}
}
