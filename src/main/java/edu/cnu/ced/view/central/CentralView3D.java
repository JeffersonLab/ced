package edu.cnu.ced.view.central;

import edu.cnu.ced.data.CentralEventData;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.BMTGeometry;
import edu.cnu.ced.geometry.BSTGeometry;
import edu.cnu.ced.geometry.CNDGeometry;
import edu.cnu.ced.geometry.CTOFGeometry;
import edu.cnu.ced.view3d.CedView3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Central detector 3D view: CND (three layers, 48 paddles each), CTOF
 * (48 paddles), and BST/BMT (six barrel-tracker layers each), every panel
 * colored by this event's ADC hit where present. Second of CED's seven 3D
 * views (after FTCal 3D), built on the same shared {@code
 * edu.cnu.ced.view3d} infrastructure.
 */
public final class CentralView3D extends CedView3D {

	private static final String TITLE = "Central 3D";

	// Same initial camera orientation as legacy's CentralView3D.
	private static final float ANGLE_X = 0f;
	private static final float ANGLE_Y = 90f;
	private static final float ANGLE_Z = 90f;
	private static final float DIST_X = 0f;
	private static final float DIST_Y = 0f;
	private static final float DIST_Z = -140f;

	public CentralView3D(CNDGeometry cnd, CTOFGeometry ctof, BSTGeometry bst, BMTGeometry bmt,
			EventNavigator navigator) {
		super(navigator,
				PropertyUtils.TITLE, TITLE,
				PropertyUtils.ANGLE_X, ANGLE_X, PropertyUtils.ANGLE_Y, ANGLE_Y, PropertyUtils.ANGLE_Z, ANGLE_Z,
				PropertyUtils.DIST_X, DIST_X, PropertyUtils.DIST_Y, DIST_Y, PropertyUtils.DIST_Z, DIST_Z,
				PropertyUtils.VISIBLE, true);
		((CentralPanel3D) cedPanel3D()).setGeometry(cnd, ctof, bst, bmt);
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		CentralPanel3D panel = new CentralPanel3D(angleX, angleY, angleZ, xDist, yDist, zDist);
		// Matches legacy CED's own CentralView3D.make3DPanel(): the default
		// orientation above leaves Central mirrored front-to-back relative
		// to FTCal/the rest of the detector, so it re-homes the identity
		// orientation with an extra 180-degree spin about y.
		panel.loadIdentityMatrix();
		panel.rotateY(180f);
		return panel;
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		((CentralPanel3D) cedPanel3D()).setEventData(CentralEventData.from(state.snapshot()));
	}
}
