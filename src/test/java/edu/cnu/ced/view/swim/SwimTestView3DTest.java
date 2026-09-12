package edu.cnu.ced.view.swim;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/**
 * Construction smoke test for {@link SwimTestView3D}, mirroring {@code
 * edu.cnu.ced.view.ftcal.FTCalView3DTest} -- see that test's own javadoc
 * for why this matters beyond a compile. No geometry or EventNavigator
 * fixture is needed here: unlike every other 3D view, this one has no
 * such dependency.
 */
class SwimTestView3DTest {

	@Test
	void constructsAndDisposesWithoutError() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SwimTestView3D view = assertDoesNotThrow(SwimTestView3D::new);
			assertEquals("Swimming Testing 3D", view.getTitle());
			assertDoesNotThrow(view::dispose);
		});
	}
}
