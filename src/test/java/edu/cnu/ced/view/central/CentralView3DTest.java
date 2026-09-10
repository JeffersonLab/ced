package edu.cnu.ced.view.central;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventStore;
import edu.cnu.ced.geometry.BMTGeometry;
import edu.cnu.ced.geometry.BSTGeometry;
import edu.cnu.ced.geometry.CNDGeometry;
import edu.cnu.ced.geometry.CTOFGeometry;

/**
 * Construction smoke test for {@link CentralView3D}, mirroring {@code
 * edu.cnu.ced.view.ftcal.FTCalView3DTest} -- see that test's own javadoc
 * for why this matters beyond a compile (the {@code edu.cnu.ced.view3d}
 * base classes read fields that don't exist yet at certain points in the
 * {@code PlainView3D}/{@code Panel3D} constructor chain).
 */
class CentralView3DTest {

	@Test
	void constructsAndDisposesWithoutError() throws Exception {
		CNDGeometry cnd = new CNDGeometry();
		cnd.initializeFromSource();
		CTOFGeometry ctof = new CTOFGeometry();
		ctof.initializeFromSource();
		BSTGeometry bst = new BSTGeometry();
		bst.initializeFromSource();
		BMTGeometry bmt = new BMTGeometry();
		bmt.initializeFromSource();
		EventNavigator navigator = new EventNavigator(new EventStore());

		SwingUtilities.invokeAndWait(() -> {
			CentralView3D view = assertDoesNotThrow(() -> new CentralView3D(cnd, ctof, bst, bmt, navigator));
			assertEquals("Central 3D", view.getTitle());
			assertDoesNotThrow(view::dispose);
		});
	}
}
