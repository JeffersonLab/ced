package edu.cnu.ced.view.ftcal;

import edu.cnu.ced.data.FTCalEventData;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.FTCALGeometry;
import edu.cnu.ced.view3d.CedView3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Forward Tagger calorimeter 3D view: one translucent box per crystal,
 * colored by this event's ADC hit where present. The simplest of CED's
 * seven 3D views (see legacy CED's {@code cnuphys.ced.ced3d.view.
 * FTCalView3D}/{@code cnuphys.ced.ced3d.ftcal.FTCalPanel3D}), and the first
 * built on the shared {@code edu.cnu.ced.view3d} infrastructure.
 */
public final class FTCalView3D extends CedView3D {

	private static final String TITLE = "FTCal 3D";

	// Same initial camera orientation as legacy's FTCalView3D.
	private static final float ANGLE_X = 0f;
	private static final float ANGLE_Y = 90f;
	private static final float ANGLE_Z = 90f;
	private static final float DIST_X = 0f;
	private static final float DIST_Y = 0f;
	private static final float DIST_Z = -100f;

	public FTCalView3D(FTCALGeometry geometry, EventNavigator navigator) {
		super(navigator,
				PropertyUtils.TITLE, TITLE,
				PropertyUtils.ANGLE_X, ANGLE_X, PropertyUtils.ANGLE_Y, ANGLE_Y, PropertyUtils.ANGLE_Z, ANGLE_Z,
				PropertyUtils.DIST_X, DIST_X, PropertyUtils.DIST_Y, DIST_Y, PropertyUtils.DIST_Z, DIST_Z,
				PropertyUtils.VISIBLE, true);
		((FTCalPanel3D) cedPanel3D()).setGeometry(geometry);
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		return new FTCalPanel3D(angleX, angleY, angleZ, xDist, yDist, zDist);
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		((FTCalPanel3D) cedPanel3D()).setEventData(FTCalEventData.from(state.snapshot()));
	}
}
