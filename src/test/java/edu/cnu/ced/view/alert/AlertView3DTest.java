package edu.cnu.ced.view.alert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventStore;
import edu.cnu.ced.geometry.AlertGeometry;

/**
 * Construction smoke test for {@link AlertView3D}, mirroring {@code
 * edu.cnu.ced.view.ftcal.FTCalView3DTest} -- see that test's own javadoc
 * for why this matters beyond a compile.
 */
class AlertView3DTest {

	@Test
	void constructsAndDisposesWithoutError() throws Exception {
		AlertGeometry geometry = new AlertGeometry();
		geometry.initializeFromSource();
		EventNavigator navigator = new EventNavigator(new EventStore());

		SwingUtilities.invokeAndWait(() -> {
			AlertView3D view = assertDoesNotThrow(() -> new AlertView3D(geometry, navigator));
			assertEquals("ALERT 3D", view.getTitle());
			assertDoesNotThrow(view::dispose);
		});
	}
}
