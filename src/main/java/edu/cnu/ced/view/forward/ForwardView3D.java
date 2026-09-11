package edu.cnu.ced.view.forward;

import edu.cnu.ced.data.DCEventData;
import edu.cnu.ced.data.ECalEventData;
import edu.cnu.ced.data.FTOFEventData;
import edu.cnu.ced.data.PCalEventData;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.DCGeometry;
import edu.cnu.ced.geometry.ECGeometry;
import edu.cnu.ced.geometry.FTOFGeometry;
import edu.cnu.ced.geometry.PCALGeometry;
import edu.cnu.ced.view3d.CedView3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Forward detector 3D view: DC (six superlayers per sector), FTOF (three
 * panels per sector), and PCAL/ECAL (U/V/W strip-view triangles, inner and
 * outer stack for ECAL), every element colored by this event's ADC/raw hit
 * where present. Fifth of CED's seven 3D views, built on the same shared
 * {@code edu.cnu.ced.view3d} infrastructure as the others.
 */
public final class ForwardView3D extends CedView3D {

	private static final String TITLE = "Forward 3D";

	// Same initial camera orientation as legacy's ForwardView3D.
	private static final float ANGLE_X = -90f;
	private static final float ANGLE_Y = 0f;
	private static final float ANGLE_Z = -90f;
	private static final float DIST_X = -200f;
	private static final float DIST_Y = 0f;
	private static final float DIST_Z = -1600f;

	public ForwardView3D(DCGeometry dc, FTOFGeometry ftof, PCALGeometry pcal, ECGeometry ecal,
			EventNavigator navigator) {
		super(navigator,
				PropertyUtils.TITLE, TITLE,
				PropertyUtils.ANGLE_X, ANGLE_X, PropertyUtils.ANGLE_Y, ANGLE_Y, PropertyUtils.ANGLE_Z, ANGLE_Z,
				PropertyUtils.DIST_X, DIST_X, PropertyUtils.DIST_Y, DIST_Y, PropertyUtils.DIST_Z, DIST_Z,
				PropertyUtils.VISIBLE, true);
		((ForwardPanel3D) cedPanel3D()).setGeometry(dc, ftof, pcal, ecal);
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		return new ForwardPanel3D(angleX, angleY, angleZ, xDist, yDist, zDist);
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		((ForwardPanel3D) cedPanel3D()).setEventData(
				DCEventData.from(state.snapshot()),
				FTOFEventData.from(state.snapshot()),
				PCalEventData.from(state.snapshot()),
				ECalEventData.from(state.snapshot()));
	}
}
