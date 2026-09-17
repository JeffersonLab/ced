package edu.cnu.ced.view.fmt;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.FMTEventData.Cross;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws every FMT reconstructed cross for the current event as a point
 * marker, matching the 2D {@code FMTXYView.drawCrosses}'s own color
 * exactly ({@link CedDrawingStyle#RECON_CROSS}). Legacy CED's own {@code
 * cnuphys.ced.ced3d.fmt.FMTCrossDrawer3D} also draws a short line along
 * each cross's own direction vector; unlike the forward DC's {@code
 * Cross}, {@code FMTEventData.Cross} carries only a position, no
 * direction, confirmed by its record shape -- the same constraint {@code
 * edu.cnu.ced.view.central.CentralCrossDrawer3D}/{@code
 * edu.cnu.ced.view.alert.AlertClusterDrawer3D} already draw around -- so
 * this draws a point only.
 *
 * <p>
 * Extends {@link Item3D} directly rather than {@link
 * edu.cnu.ced.view3d.DetectorItem3D}: a cross is live event data, not a
 * detector "volume".
 * </p>
 */
final class FmtCrossDrawer3D extends Item3D {

	private static final float POINT_SIZE = 11f;
	private static final float POINT_OUTLINE_SIZE = 13f;

	private final FMTPanel3D panel;

	FmtCrossDrawer3D(FMTPanel3D panel) {
		super(panel);
		this.panel = panel;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.CROSSES)) {
			return;
		}
		for (Cross cross : panel.crosses()) {
			Support3D.drawPoint(drawable, cross.x(), cross.y(), cross.z(), Color.black, POINT_OUTLINE_SIZE, true);
			Support3D.drawPoint(drawable, cross.x(), cross.y(), cross.z(), CedDrawingStyle.RECON_CROSS, POINT_SIZE, true);
		}
	}
}
