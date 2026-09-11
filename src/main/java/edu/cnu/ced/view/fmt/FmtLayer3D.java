package edu.cnu.ced.view.fmt;

import java.awt.Color;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.geometry.FMTGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * One FMT layer's 1024 strips, drawn as boxes in a single {@code
 * drawShape()} call -- matching {@link edu.cnu.ced.view.central.CndLayer3D}'s
 * reasoning (one item per layer rather than legacy CED's one item per
 * strip, which would mean 6144 items competing for {@code Panel3D}'s
 * per-frame opaque/transparent classification and sort for no visual
 * benefit: strips within a layer never need independent visibility).
 *
 * <p>
 * Overlay priority per strip, matching legacy CED's own {@code
 * FMTStrip3D.drawData()}: a cluster seed strip (magenta) beats a
 * reconstructed hit (red) beats a raw ADC hit (this event's TURBO
 * fraction, matching every other 3D detector's ADC coloring) beats the
 * plain default (alternating light-yellow/light-green by layer parity).
 * </p>
 */
final class FmtLayer3D extends DetectorItem3D {

	private static final int[][] FACES = {
			{ 0, 1, 2, 3 }, { 3, 7, 6, 2 }, { 0, 4, 7, 3 },
			{ 0, 4, 5, 1 }, { 1, 5, 6, 2 }, { 4, 5, 6, 7 } };

	private static final Color EVEN_LAYER_COLOR = new Color(255, 255, 224); // light yellow
	private static final Color ODD_LAYER_COLOR = new Color(144, 238, 144); // light green
	private static final Color CLUSTER_SEED_COLOR = Color.magenta;
	private static final Color RECON_HIT_COLOR = Color.red;

	private final FMTPanel3D panel;
	private final int layer; // 1-based
	private final CedDisplayOption layerOption;
	private final Color defaultColor;

	FmtLayer3D(FMTPanel3D panel, int layer, CedDisplayOption layerOption) {
		super(panel);
		this.panel = panel;
		this.layer = layer;
		this.layerOption = layerOption;
		this.defaultColor = ((layer - 1) % 2 == 0) ? EVEN_LAYER_COLOR : ODD_LAYER_COLOR;
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		FMTGeometry geometry = panel.geometry();
		if (geometry == null) {
			return;
		}
		int alpha = getFillAlpha();
		Color base = new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), alpha);
		int layerIndex = layer - 1;
		float[] coords = new float[24];
		for (int strip = 1; strip <= FMTGeometry.STRIP_COUNT; strip++) {
			if (!panel.isDisplayed(regionOption(FMTGeometry.region(strip)))) {
				continue;
			}
			fillVertices(geometry, layerIndex, strip - 1, coords);
			Color color = overlayColor(strip, alpha, base);
			for (int[] face : FACES) {
				Support3D.drawQuad(drawable, coords, face[0], face[1], face[2], face[3], color, 1f, true);
			}
		}
	}

	private static void fillVertices(FMTGeometry geometry, int layerIndex, int stripIndex, float[] coords) {
		List<Point3> vertices = geometry.stripVertices(layerIndex, stripIndex);
		for (int i = 0; i < 8; i++) {
			Point3 point = vertices.get(i);
			coords[3 * i] = (float) point.x();
			coords[3 * i + 1] = (float) point.y();
			coords[3 * i + 2] = (float) point.z();
		}
	}

	private Color overlayColor(int strip, int alpha, Color fallback) {
		if (panel.isDisplayed(CedDisplayOption.CLUSTERS) && panel.isClusterSeed(layer - 1, strip)) {
			return withAlpha(CLUSTER_SEED_COLOR, alpha);
		}
		if (panel.isDisplayed(CedDisplayOption.RECON_HITS) && panel.hasReconHit(layer - 1, strip)) {
			return withAlpha(RECON_HIT_COLOR, alpha);
		}
		Integer adc = panel.adc(layer - 1, strip);
		if (adc != null) {
			int maximum = panel.maximumAdc();
			double fraction = maximum == 0 ? 0.0 : (double) adc / maximum;
			return withAlpha(ScientificColorMap.TURBO.colorAt(fraction), alpha);
		}
		return fallback;
	}

	private static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private static CedDisplayOption regionOption(int region) {
		return switch (region) {
		case 1 -> CedDisplayOption.FMT_REGION_1;
		case 2 -> CedDisplayOption.FMT_REGION_2;
		case 3 -> CedDisplayOption.FMT_REGION_3;
		default -> CedDisplayOption.FMT_REGION_4;
		};
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(CedDisplayOption.FMT) && panel.isDisplayed(layerOption);
	}
}
