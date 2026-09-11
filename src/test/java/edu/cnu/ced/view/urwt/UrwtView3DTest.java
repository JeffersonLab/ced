package edu.cnu.ced.view.urwt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventStore;
import edu.cnu.ced.geometry.URWTGeometry;

/**
 * Construction smoke test for {@link UrwtView3D}, mirroring {@code
 * edu.cnu.ced.view.ftcal.FTCalView3DTest} -- see that test's own javadoc
 * for why this matters beyond a compile.
 */
class UrwtView3DTest {

	@Test
	void constructsAndDisposesWithoutError() throws Exception {
		URWTGeometry geometry = new URWTGeometry();
		geometry.initializeFromSource();
		EventNavigator navigator = new EventNavigator(new EventStore());

		SwingUtilities.invokeAndWait(() -> {
			UrwtView3D view = assertDoesNotThrow(() -> new UrwtView3D(geometry, navigator));
			assertEquals("μRWT 3D", view.getTitle());
			assertDoesNotThrow(view::dispose);
		});
	}
}
