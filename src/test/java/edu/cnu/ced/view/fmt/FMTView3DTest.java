package edu.cnu.ced.view.fmt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventStore;
import edu.cnu.ced.geometry.FMTGeometry;

/**
 * Construction smoke test for {@link FMTView3D}, mirroring {@code
 * edu.cnu.ced.view.ftcal.FTCalView3DTest} -- see that test's own javadoc
 * for why this matters beyond a compile.
 */
class FMTView3DTest {

	@Test
	void constructsAndDisposesWithoutError() throws Exception {
		FMTGeometry geometry = new FMTGeometry();
		geometry.initializeFromSource();
		EventNavigator navigator = new EventNavigator(new EventStore());

		SwingUtilities.invokeAndWait(() -> {
			FMTView3D view = assertDoesNotThrow(() -> new FMTView3D(geometry, navigator));
			assertEquals("FMT 3D", view.getTitle());
			assertDoesNotThrow(view::dispose);
		});
	}
}
