package edu.cnu.ced.view.forward;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.MagneticField;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * Draws a coarse 3D grid of colored points showing one magnetic field's own
 * magnitude within its cylindrical extent (a bounding box in x/y clipped to
 * the field's own max radius, spanning its own z range, both read directly
 * from the field) -- matching legacy CED's own {@code
 * cnuphys.ced.ced3d.FieldBoundary}, minus its dead "boundary cylinder"
 * shape: that class extends {@code bCNU3D}'s own {@code Cylinder} and
 * passes it a translucent tint color, but its own {@code draw()} override
 * never calls {@code super.draw()} -- only its own point-grid method -- so
 * that shape (and tint) was never actually visible in legacy either; this
 * drawer skips both rather than port dead code.
 *
 * <p>
 * Legacy colors each point via a {@code ColorScaleModel} built from a
 * nonlinear (exponential) value scale over the field's own max magnitude,
 * so faraway low-field points still spread across the color range instead
 * of clustering at one end -- a {@code cnuphys.bCNU}-only class not
 * available here. This drawer reproduces that same nonlinear spread with a
 * direct formula ({@link #colorFraction}, the inverse of legacy's own
 * bin-edge formula) instead, feeding {@link ScientificColorMap#VIRIDIS}
 * (legacy's own choice for both the torus and solenoid scale) rather than
 * porting the bin-table machinery. Unlike legacy (which normalizes every
 * field against the composite {@code MagneticFields.maxFieldMagnitude()}),
 * this normalizes each field against its own {@code
 * MagneticField#getMaxFieldMagnitude()} -- more correct for two fields
 * with different peak magnitudes.
 * </p>
 *
 * <p>
 * Gated on {@link CedDisplayOption#FIELD_MAP}, unchecked by default --
 * matching legacy's own "Map Extents" checkbox, which starts unchecked
 * given how expensive this grid is to redraw every frame (recomputed
 * live from {@link FieldProbe}, with no caching, same as legacy).
 * </p>
 */
final class ForwardFieldMapDrawer3D extends Item3D {

	private static final float STEP_CM = 10f;
	private static final float EPSILON_CM = 0.001f;
	private static final float MINIMUM_MAGNITUDE_KG = 0.1f;
	private static final float ALPHA_SCALE = 150f;
	private static final float KILOGAUSS_PER_UNIT = 10f;
	private static final double NONLINEAR_SPEEDUP = 6.0;

	private final ForwardPanel3D panel;
	private final MagneticField field;

	ForwardFieldMapDrawer3D(ForwardPanel3D panel, MagneticField field) {
		super(panel);
		this.panel = panel;
		this.field = field;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.FIELD_MAP)) {
			return;
		}
		FieldProbe probe = FieldProbe.factory(field);
		float zMin = (float) field.getZCoordinate().getMin();
		float zMax = (float) field.getZCoordinate().getMax();
		float radius = (float) field.getRCoordinate().getMax();
		float radiusSquared = radius * radius;
		float maxMagnitude = field.getMaxFieldMagnitude() / KILOGAUSS_PER_UNIT;

		float shiftX = (float) field.getShiftX();
		float shiftY = (float) field.getShiftY();
		float shiftZ = (float) field.getShiftZ();

		for (float x = -radius; x < radius + EPSILON_CM; x += STEP_CM) {
			for (float y = -radius; y < radius + EPSILON_CM; y += STEP_CM) {
				if (x * x + y * y >= radiusSquared) {
					continue;
				}
				for (float z = zMin; z < zMax + EPSILON_CM; z += STEP_CM) {
					float magnitude = probe.fieldMagnitude(x + shiftX, y + shiftY, z + shiftZ) / KILOGAUSS_PER_UNIT;
					if (magnitude <= MINIMUM_MAGNITUDE_KG) {
						continue;
					}
					Color color = ScientificColorMap.VIRIDIS.colorAt(colorFraction(magnitude, maxMagnitude));
					int alpha = (int) Math.min(255f, ALPHA_SCALE * magnitude);
					Color translucent = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
					Support3D.drawPoint(drawable, x + shiftX, y + shiftY, z + shiftZ, translucent, STEP_CM, false);
				}
			}
		}
	}

	/** Inverts legacy's own nonlinear (exponential) bin-edge formula into a continuous [0, 1] fraction. */
	private static float colorFraction(double magnitude, double maxMagnitude) {
		if (maxMagnitude <= 0) {
			return 0f;
		}
		double ratio = Math.max(0.0, Math.min(1.0, magnitude / maxMagnitude));
		return (float) (Math.log1p(ratio * Math.expm1(NONLINEAR_SPEEDUP)) / NONLINEAR_SPEEDUP);
	}
}
