package edu.cnu.ced.view.central;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.CentralEventData.Cross;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws every BST/BMT reconstructed cross for the current event as a
 * point marker, one color regardless of detector -- matching the 2D
 * {@code CndCtofXYView.drawCrosses}'s own single-color convention exactly
 * (dark green, {@link CedDrawingStyle#RECON_CROSS}), rather than
 * distinguishing BST from BMT the way ADC-hit coloring does elsewhere in
 * this view.
 *
 * <p>
 * Unlike the forward DC's own {@code Cross} (see {@code
 * edu.cnu.ced.view.forward.ForwardCrossDrawer3D}), {@code
 * CentralEventData.Cross} carries only a position, no direction vector --
 * confirmed by its record shape, not guessed -- so this draws a point
 * only, matching {@code edu.cnu.ced.view.alert.AlertClusterDrawer3D}'s
 * own reasoning for the same constraint.
 * </p>
 *
 * <p>
 * Extends {@link Item3D} directly rather than {@link
 * edu.cnu.ced.view3d.DetectorItem3D}: a cross is live event data, not a
 * detector "volume".
 * </p>
 */
final class CentralCrossDrawer3D extends Item3D {

	private static final float POINT_SIZE = 11f;
	private static final float POINT_OUTLINE_SIZE = 13f;

	private final CentralPanel3D panel;

	CentralCrossDrawer3D(CentralPanel3D panel) {
		super(panel);
		this.panel = panel;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.CROSSES)) {
			return;
		}
		for (Cross cross : panel.crosses()) {
			if (Float.isNaN(cross.x()) || Float.isNaN(cross.y()) || Float.isNaN(cross.z())) {
				continue;
			}
			Support3D.drawPoint(drawable, cross.x(), cross.y(), cross.z(), Color.black, POINT_OUTLINE_SIZE, true);
			Support3D.drawPoint(drawable, cross.x(), cross.y(), cross.z(), CedDrawingStyle.RECON_CROSS, POINT_SIZE, true);
		}
	}
}
