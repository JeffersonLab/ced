package edu.cnu.ced.view.fmt;

import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.FMTGeometry;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.view3d.CedView3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Forward Micromegas Tracker 3D view: six layers of 1024 strips each,
 * colored by cluster seed / reconstructed hit / raw ADC hit (in that
 * priority) where present, plus FMT reconstructed crosses and every
 * category of reconstructed/Monte Carlo track, each swum on demand
 * through the same shared {@link SwimTrajectoryCache} every 2D view
 * already uses. Fourth of CED's seven 3D views, built on the same shared
 * {@code edu.cnu.ced.view3d} infrastructure as FTCal/Central 3D.
 */
public final class FMTView3D extends CedView3D {

	private static final String TITLE = "FMT 3D";

	// Same initial camera orientation as legacy's FMTView3D.
	private static final float ANGLE_X = 0f;
	private static final float ANGLE_Y = 90f;
	private static final float ANGLE_Z = 90f;
	private static final float DIST_X = 0f;
	private static final float DIST_Y = 0f;
	private static final float DIST_Z = -60f;

	public FMTView3D(FMTGeometry geometry, SwimTrajectoryCache swimCache, EventNavigator navigator) {
		super(navigator,
				PropertyUtils.TITLE, TITLE,
				PropertyUtils.ANGLE_X, ANGLE_X, PropertyUtils.ANGLE_Y, ANGLE_Y, PropertyUtils.ANGLE_Z, ANGLE_Z,
				PropertyUtils.DIST_X, DIST_X, PropertyUtils.DIST_Y, DIST_Y, PropertyUtils.DIST_Z, DIST_Z,
				PropertyUtils.VISIBLE, true);
		FMTPanel3D panel = (FMTPanel3D) cedPanel3D();
		panel.setGeometry(geometry);
		panel.setSwimCache(swimCache);
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		FMTPanel3D panel = new FMTPanel3D(angleX, angleY, angleZ, xDist, yDist, zDist);
		// Matches legacy CED's own FMTView3D.make3DPanel(): re-homes the
		// identity orientation with an extra 180-degree spin about y, same
		// as CentralView3D's.
		panel.loadIdentityMatrix();
		panel.rotateY(180f);
		return panel;
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		((FMTPanel3D) cedPanel3D()).setEventData(state.snapshot());
	}
}
