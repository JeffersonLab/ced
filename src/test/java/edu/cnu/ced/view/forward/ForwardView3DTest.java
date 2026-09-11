package edu.cnu.ced.view.forward;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventStore;
import edu.cnu.ced.geometry.DCGeometry;
import edu.cnu.ced.geometry.ECGeometry;
import edu.cnu.ced.geometry.FTOFGeometry;
import edu.cnu.ced.geometry.PCALGeometry;

/**
 * Construction smoke test for {@link ForwardView3D}, mirroring {@code
 * edu.cnu.ced.view.ftcal.FTCalView3DTest} -- see that test's own javadoc
 * for why this matters beyond a compile.
 */
class ForwardView3DTest {

	@Test
	void constructsAndDisposesWithoutError() throws Exception {
		DCGeometry dc = new DCGeometry();
		dc.initializeFromSource();
		FTOFGeometry ftof = new FTOFGeometry();
		ftof.initializeFromSource();
		PCALGeometry pcal = new PCALGeometry();
		pcal.initializeFromSource();
		ECGeometry ecal = new ECGeometry();
		ecal.initializeFromSource();
		EventNavigator navigator = new EventNavigator(new EventStore());

		SwingUtilities.invokeAndWait(() -> {
			ForwardView3D view = assertDoesNotThrow(() -> new ForwardView3D(dc, ftof, pcal, ecal, navigator));
			assertEquals("Forward 3D", view.getTitle());
			assertDoesNotThrow(view::dispose);
		});
	}
}
