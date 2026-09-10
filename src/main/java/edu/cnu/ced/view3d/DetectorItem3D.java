package edu.cnu.ced.view3d;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.mdi.mdi3D.item3D.Item3D;

/**
 * Common base for a single detector component drawn in a {@link
 * CedPanel3D} scene (a crystal, a wire, a paddle, a strip, ...).
 *
 * <p>
 * Splits {@link #draw(GLAutoDrawable)} into the same two phases legacy
 * CED's {@code cnuphys.ced.ced3d.DetectorItem3D} used: {@link
 * #drawShape(GLAutoDrawable)} for the item's static geometry (gated on the
 * "Volumes" toggle and a minimum alpha, so a fully transparent scene skips
 * the draw call entirely) and {@link #drawData(GLAutoDrawable)} for
 * whatever live event data the item overlays on that geometry (ADC hits,
 * truth points, crosses, ...). Both default to no-ops; a subclass overrides
 * only the phase(s) it actually draws.
 * </p>
 */
public abstract class DetectorItem3D extends Item3D {

	protected final CedPanel3D cedPanel3D;

	protected DetectorItem3D(CedPanel3D panel3D) {
		super(panel3D);
		this.cedPanel3D = panel3D;
	}

	@Override
	public final void draw(GLAutoDrawable drawable) {
		if (!show()) {
			return;
		}
		if (cedPanel3D.isDisplayed(CedDisplayOption.VOLUMES) && cedPanel3D.getVolumeAlpha() > 2) {
			drawShape(drawable);
		}
		drawData(drawable);
	}

	/** Draw this item's static detector geometry. Default: no-op. */
	protected void drawShape(GLAutoDrawable drawable) {
	}

	/** Draw this item's live event data. Default: no-op. */
	protected void drawData(GLAutoDrawable drawable) {
	}

	/**
	 * Whether this item should be considered for drawing at all this frame.
	 * Default: always. Overridable for items that only apply to part of an
	 * event (e.g. a specific sector or sub-detector variant).
	 */
	protected boolean show() {
		return true;
	}

	/** The current alpha (0-255) to use for a translucent "volume" fill. */
	protected final int volumeAlpha() {
		return cedPanel3D.getVolumeAlpha();
	}

	/**
	 * Truth/species color for a reconstructed or Monte Carlo particle, from
	 * the same PID palette used by every 2D CED view (see {@link
	 * CedDrawingStyle#particleColor(int, int)}), so a track or hit colored
	 * by particle species here matches how it would be colored in a 2D CED
	 * view of the same event.
	 */
	protected static Color truthColor(int pid, int charge) {
		return CedDrawingStyle.particleColor(pid, charge);
	}
}
