package edu.cnu.ced.view.alert;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.AlertEventData;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws every AHDC and ATOF reconstructed cluster position for the
 * current event as a point marker. The closest ALERT-specific analog to
 * Forward 3D's {@code ForwardCrossDrawer3D}: unlike the forward DC's own
 * crosses, {@code AlertEventData}'s {@code DcCluster}/{@code TofCluster}
 * carry only a position, no direction vector -- confirmed by their record
 * shapes, not guessed -- and legacy CED's own {@code cnuphys.ced.ced3d.
 * alert} package has no dedicated cross/cluster drawer at all to mirror
 * (unlike {@code CrossDrawer3D} for the forward DC), so this is new
 * rather than ported.
 *
 * <p>
 * Extends {@link Item3D} directly rather than {@link
 * edu.cnu.ced.view3d.DetectorItem3D}, matching {@link
 * edu.cnu.ced.view.forward.ForwardCrossDrawer3D}'s own reasoning: a
 * cluster is live event data, not a detector "volume".
 * </p>
 */
final class AlertClusterDrawer3D extends Item3D {

	private static final float POINT_SIZE = 11f;
	private static final float POINT_OUTLINE_SIZE = 13f;
	private static final Color DC_CLUSTER_COLOR = CedDrawingStyle.RECON_CLUSTER;
	private static final Color TOF_CLUSTER_COLOR = CedDrawingStyle.RECON_HIT;

	private final AlertPanel3D panel;

	AlertClusterDrawer3D(AlertPanel3D panel) {
		super(panel);
		this.panel = panel;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.CLUSTERS)) {
			return;
		}
		for (AlertEventData.DcCluster cluster : panel.dcClusters()) {
			drawMarker(drawable, cluster.x(), cluster.y(), cluster.z(), DC_CLUSTER_COLOR);
		}
		for (AlertEventData.TofCluster cluster : panel.tofClusters()) {
			drawMarker(drawable, cluster.x(), cluster.y(), cluster.z(), TOF_CLUSTER_COLOR);
		}
	}

	private static void drawMarker(GLAutoDrawable drawable, float x, float y, float z, Color color) {
		Support3D.drawPoint(drawable, x, y, z, Color.black, POINT_OUTLINE_SIZE, true);
		Support3D.drawPoint(drawable, x, y, z, color, POINT_SIZE, true);
	}
}
