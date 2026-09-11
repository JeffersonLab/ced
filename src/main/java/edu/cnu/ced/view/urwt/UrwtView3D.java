package edu.cnu.ced.view.urwt;

import edu.cnu.ced.data.URWTEventData;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.URWTGeometry;
import edu.cnu.ced.view3d.CedView3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * μrWT (radial wall tagger) 3D view: six sectors of four layers each,
 * every detector drawn as a filled polygon tracing its strips' swept
 * area, with hit strips highlighted. Last of CED's seven 3D views, built
 * on the same shared {@code edu.cnu.ced.view3d} infrastructure as the
 * others.
 */
public final class UrwtView3D extends CedView3D {

	private static final String TITLE = "μRWT 3D";

	// Same initial camera orientation as legacy's UrwtView3D.
	private static final float ANGLE_X = 0f;
	private static final float ANGLE_Y = 90f;
	private static final float ANGLE_Z = 90f;
	private static final float DIST_X = 0f;
	private static final float DIST_Y = 0f;
	private static final float DIST_Z = -450f;

	public UrwtView3D(URWTGeometry geometry, EventNavigator navigator) {
		super(navigator,
				PropertyUtils.TITLE, TITLE,
				PropertyUtils.ANGLE_X, ANGLE_X, PropertyUtils.ANGLE_Y, ANGLE_Y, PropertyUtils.ANGLE_Z, ANGLE_Z,
				PropertyUtils.DIST_X, DIST_X, PropertyUtils.DIST_Y, DIST_Y, PropertyUtils.DIST_Z, DIST_Z,
				PropertyUtils.VISIBLE, true);
		((UrwtPanel3D) cedPanel3D()).setGeometry(geometry);
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		UrwtPanel3D panel = new UrwtPanel3D(angleX, angleY, angleZ, xDist, yDist, zDist);
		// Matches legacy CED's own UrwtView3D.make3DPanel(): re-homes the
		// identity orientation with an extra 180-degree spin about y, same
		// as CentralView3D's/FMTView3D's/AlertView3D's.
		panel.loadIdentityMatrix();
		panel.rotateY(180f);
		return panel;
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		((UrwtPanel3D) cedPanel3D()).setEventData(URWTEventData.from(state.snapshot()));
	}
}
