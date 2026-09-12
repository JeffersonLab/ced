package edu.cnu.ced.view.swim;

import edu.cnu.ced.view3d.Plain3DViewSupport;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Standalone 3D swimmer test: lets a developer swim a hypothetical
 * particle (arbitrary charge, vertex, and momentum) through the current
 * magnetic field and see the resulting trajectory, independent of any
 * physics event. Seventh and last of CED's planned 3D views, matching
 * legacy CED's own {@code cnuphys.ced.ced3d.view.SwimmingTestView3D}.
 *
 * <p>
 * Deliberately extends {@link PlainView3D} directly rather than {@link
 * edu.cnu.ced.view3d.CedView3D}: every other 3D view is a live display of
 * the current physics event, bridging an {@code EventNavigator}: this one
 * has no event to bridge (legacy's own {@code SwimmingTestView3D} is
 * similarly a plain {@code PlainView3D}, not its event-aware {@code
 * CedView3D}). Still goes through {@link Plain3DViewSupport#
 * prepareKeyVals} directly for the same GL-deadlock/container/sizing
 * fixes every other 3D view gets from {@code CedView3D}.
 * </p>
 */
public final class SwimTestView3D extends PlainView3D {

	private static final String TITLE = "Swimming Testing 3D";

	// Same initial camera orientation as legacy's SwimmingTestView3D
	// (shared with ForwardView3D's, since this view looks at the same
	// forward region).
	private static final float ANGLE_X = -90f;
	private static final float ANGLE_Y = 0f;
	private static final float ANGLE_Z = -90f;
	private static final float DIST_X = -200f;
	private static final float DIST_Y = 0f;
	private static final float DIST_Z = -1600f;

	public SwimTestView3D() {
		super(Plain3DViewSupport.prepareKeyVals(new Object[] {
				PropertyUtils.TITLE, TITLE,
				PropertyUtils.ANGLE_X, ANGLE_X, PropertyUtils.ANGLE_Y, ANGLE_Y, PropertyUtils.ANGLE_Z, ANGLE_Z,
				PropertyUtils.DIST_X, DIST_X, PropertyUtils.DIST_Y, DIST_Y, PropertyUtils.DIST_Z, DIST_Z,
				PropertyUtils.VISIBLE, true }));
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		return new SwimTestPanel3D(angleX, angleY, angleZ, xDist, yDist, zDist);
	}
}
