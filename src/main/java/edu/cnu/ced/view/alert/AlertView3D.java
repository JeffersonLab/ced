package edu.cnu.ced.view.alert;

import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.AlertGeometry;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.view3d.CedView3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * ALERT detector 3D view: the AHDC drift chamber (wire lines, hit wires
 * highlighted) and the ATOF time-of-flight system (paddle boxes; see
 * {@link AlertTofGroup3D}'s javadoc for why those have no hit
 * highlighting yet), plus AHDC/ATOF reconstructed clusters and every
 * category of reconstructed/Monte Carlo track, each swum on demand
 * through the same shared {@link SwimTrajectoryCache} every 2D view
 * already uses. Sixth of CED's seven 3D views, built on the same shared
 * {@code edu.cnu.ced.view3d} infrastructure as the others.
 */
public final class AlertView3D extends CedView3D {

	private static final String TITLE = "ALERT 3D";

	// Same initial camera orientation as legacy's AlertView3D.
	private static final float ANGLE_X = 0f;
	private static final float ANGLE_Y = 90f;
	private static final float ANGLE_Z = 90f;
	private static final float DIST_X = 0f;
	private static final float DIST_Y = 0f;
	private static final float DIST_Z = -400f;

	public AlertView3D(AlertGeometry geometry, SwimTrajectoryCache swimCache, EventNavigator navigator) {
		super(navigator,
				PropertyUtils.TITLE, TITLE,
				PropertyUtils.ANGLE_X, ANGLE_X, PropertyUtils.ANGLE_Y, ANGLE_Y, PropertyUtils.ANGLE_Z, ANGLE_Z,
				PropertyUtils.DIST_X, DIST_X, PropertyUtils.DIST_Y, DIST_Y, PropertyUtils.DIST_Z, DIST_Z,
				PropertyUtils.VISIBLE, true);
		AlertPanel3D panel = (AlertPanel3D) cedPanel3D();
		panel.setGeometry(geometry);
		panel.setSwimCache(swimCache);
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		AlertPanel3D panel = new AlertPanel3D(angleX, angleY, angleZ, xDist, yDist, zDist);
		// Matches legacy CED's own AlertView3D.make3DPanel(): re-homes the
		// identity orientation with an extra 180-degree spin about y, same
		// as CentralView3D's/FMTView3D's.
		panel.loadIdentityMatrix();
		panel.rotateY(180f);
		return panel;
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		((AlertPanel3D) cedPanel3D()).setEventData(state.snapshot());
	}
}
