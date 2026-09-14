package edu.cnu.ced.view.forward;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.DCEventData.Cross;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws every DC cross for the current event: a short line along the
 * cross's own direction vector, plus a point marker at its position,
 * colored by reconstruction kind (HB/TB/AI HB/AI TB). Matches legacy
 * CED's own {@code cnuphys.ced.ced3d.CrossDrawer3D} in spirit, but not
 * its "tilted sector" to "sector" coordinate rotation: {@code
 * DCEventData.Cross}'s x/y/z are already plain lab-frame coordinates --
 * confirmed by the 2D {@code DCXYView} already plotting them directly,
 * with no such rotation of its own.
 *
 * <p>
 * Extends {@link Item3D} directly rather than {@link
 * edu.cnu.ced.view3d.DetectorItem3D}, matching legacy's own choice for
 * this class: a cross is live event data, not a detector "volume", so it
 * should draw regardless of the Volumes toggle or alpha, which {@code
 * DetectorItem3D}'s template method would otherwise gate it on.
 * </p>
 */
final class ForwardCrossDrawer3D extends Item3D {

	private static final float CROSS_LENGTH_CM = 30f;
	private static final float POINT_SIZE = 11f;
	private static final float POINT_OUTLINE_SIZE = 13f;

	private final ForwardPanel3D panel;

	ForwardCrossDrawer3D(ForwardPanel3D panel) {
		super(panel);
		this.panel = panel;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.CROSSES)) {
			return;
		}
		for (Cross cross : panel.crosses()) {
			Color color = CedDrawingStyle.reconstructionColor(cross.kind());
			float x = cross.x();
			float y = cross.y();
			float z = cross.z();
			float tx = x + CROSS_LENGTH_CM * cross.directionX();
			float ty = y + CROSS_LENGTH_CM * cross.directionY();
			float tz = z + CROSS_LENGTH_CM * cross.directionZ();

			Support3D.drawLine(drawable, x, y, z, tx, ty, tz, Color.black, 3f);
			Support3D.drawLine(drawable, x, y, z, tx, ty, tz, Color.gray, 1f);

			Support3D.drawPoint(drawable, x, y, z, Color.black, POINT_OUTLINE_SIZE, true);
			Support3D.drawPoint(drawable, x, y, z, color, POINT_SIZE, true);
		}
	}
}
