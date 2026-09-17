package edu.cnu.ced.view.forward;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.ECalEventData;
import edu.cnu.ced.data.PCalEventData;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws {@code REC::Calorimeter}'s reconstructed PCAL/ECAL hits for the
 * current event: a small black point at each hit's own position, plus (when
 * its energy is large enough to matter) a translucent sphere sized to that
 * energy -- matching legacy CED's own {@code cnuphys.ced.ced3d.RecDrawer3D}
 * and its {@code RecCalorimeter.radius(int)} exactly. {@code REC::Calorimeter}
 * carries no radius column of its own (confirmed against coatjava's own bank
 * definition), so legacy derives one from energy on a log scale, clamped to
 * [1, 40] cm; this drawer reproduces that same formula rather than reading
 * {@link PCalEventData.ReconHit#radius()}/{@link ECalEventData.ReconHit#radius()},
 * which are always {@code 0} for this bank (no source column to fill them
 * from -- see those records' own {@code from(EventSnapshot)}).
 *
 * <p>
 * Gated on {@link CedDisplayOption#RECON_CAL} (legacy's own {@code
 * showRecCal()} checkbox, already reused by the 2D {@code PCalView}/{@code
 * ECalView}) together with each detector's own {@link CedDisplayOption#PCAL}/
 * {@link CedDisplayOption#ECAL} master toggle -- matching legacy's {@code
 * showPCAL()}/{@code showECAL()} gating exactly. Unlike legacy, no per-sector
 * gate is applied here either, since legacy's own {@code RecDrawer3D} draws
 * every {@code REC::Calorimeter} row regardless of sector visibility.
 * </p>
 *
 * <p>
 * Extends {@link Item3D} directly rather than {@link
 * edu.cnu.ced.view3d.DetectorItem3D}: this is live event data, not a
 * detector "volume".
 * </p>
 */
final class ForwardRecDrawer3D extends Item3D {

	private static final float POINT_SIZE = 5f;
	private static final float MINIMUM_ENERGY = 0.05f;
	private static final float MINIMUM_RADIUS = 1f;
	private static final float MAXIMUM_RADIUS = 40f;
	private static final float LOG_OFFSET = 1.0e-8f;
	private static final int SPHERE_SLICES = 40;
	private static final int SPHERE_STACKS = 40;

	private final ForwardPanel3D panel;

	ForwardRecDrawer3D(ForwardPanel3D panel) {
		super(panel);
		this.panel = panel;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.RECON_CAL)) {
			return;
		}
		if (panel.isDisplayed(CedDisplayOption.PCAL)) {
			for (PCalEventData.ReconHit hit : panel.pcalReconHits()) {
				drawHit(drawable, hit.x(), hit.y(), hit.z(), hit.energy());
			}
		}
		if (panel.isDisplayed(CedDisplayOption.ECAL)) {
			for (ECalEventData.ReconHit hit : panel.ecalReconHits()) {
				drawHit(drawable, hit.x(), hit.y(), hit.z(), hit.energy());
			}
		}
	}

	private void drawHit(GLAutoDrawable drawable, float x, float y, float z, float energy) {
		Support3D.drawPoint(drawable, x, y, z, Color.black, POINT_SIZE, true);
		float radius = radiusForEnergy(energy);
		if (radius > 0) {
			Support3D.solidSphere(drawable, x, y, z, radius, SPHERE_SLICES, SPHERE_STACKS,
					CedDrawingStyle.RECON_CALORIMETER_FILL);
		}
	}

	/** Legacy's own {@code RecCalorimeter.radius(int)} formula, verbatim. */
	private static float radiusForEnergy(float energy) {
		if (energy < MINIMUM_ENERGY) {
			return 0f;
		}
		double value = Math.log((energy + LOG_OFFSET) / LOG_OFFSET);
		return (float) Math.max(MINIMUM_RADIUS, Math.min(MAXIMUM_RADIUS, value));
	}
}
