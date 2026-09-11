package edu.cnu.ced.view.forward;

import java.awt.Color;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.geometry.DCGeometry;
import edu.cnu.ced.geometry.Segment3;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * One DC superlayer, drawn as a single translucent hexahedron approximating
 * its overall envelope, plus a thin line for every wire with a raw hit this
 * event. Matches legacy CED's own {@code cnuphys.ced.ced3d.DCSuperLayer3D}
 * in spirit (one item per (sector, superlayer), a static "volume" shape
 * plus hit-wire DOCA lines), but not in exact geometry: legacy derives its
 * six-vertex wedge from a {@code DCGeometry.superLayerVertices(...)} that
 * has no equivalent in {@code mdi_ced}'s own (wire-level, not
 * superlayer-level) {@code DCGeometry}.
 *
 * <p>
 * The envelope here is instead the hexahedron spanning the superlayer's
 * four boundary wires -- (layer 1, wire 1), (layer 1, wire 112), (layer 6,
 * wire 1), (layer 6, wire 112) -- using each one's own two real endpoints
 * as the hexahedron's eight corners. This is a real (not fabricated)
 * envelope built entirely from actual wire geometry; it approximates
 * legacy's exact wedge only to the extent a drift chamber superlayer's
 * true cross-section is already close to that hexahedron's.
 * </p>
 */
final class DcSuperLayer3D extends DetectorItem3D {

	private static final int[][] FACES = {
			{ 0, 1, 2, 3 }, { 3, 7, 6, 2 }, { 0, 4, 7, 3 },
			{ 0, 4, 5, 1 }, { 1, 5, 6, 2 }, { 4, 5, 6, 7 } };

	private static final Color DEFAULT_COLOR = new Color(245, 222, 179); // wheat
	private static final Color HIT_COLOR = new Color(255, 0, 0, 160);
	private static final float HIT_LINE_WIDTH = 3f;

	private final ForwardPanel3D panel;
	private final int sector;
	private final int superlayer;
	private final float[] coords;

	DcSuperLayer3D(ForwardPanel3D panel, int sector, int superlayer) {
		super(panel);
		this.panel = panel;
		this.sector = sector;
		this.superlayer = superlayer;
		this.coords = envelope(panel.dcGeometry(), sector, superlayer);
	}

	private static float[] envelope(DCGeometry geometry, int sector, int superlayer) {
		Segment3 nearLow = geometry.wireLine(sector, superlayer, 1, 1);
		Segment3 nearHigh = geometry.wireLine(sector, superlayer, 1, DCGeometry.WIRE_COUNT);
		Segment3 farLow = geometry.wireLine(sector, superlayer, DCGeometry.LAYER_COUNT, 1);
		Segment3 farHigh = geometry.wireLine(sector, superlayer, DCGeometry.LAYER_COUNT, DCGeometry.WIRE_COUNT);
		float[] coords = new float[24];
		set(coords, 0, nearLow.start());
		set(coords, 1, nearHigh.start());
		set(coords, 2, nearHigh.end());
		set(coords, 3, nearLow.end());
		set(coords, 4, farLow.start());
		set(coords, 5, farHigh.start());
		set(coords, 6, farHigh.end());
		set(coords, 7, farLow.end());
		return coords;
	}

	private static void set(float[] coords, int corner, edu.cnu.ced.geometry.Point3 point) {
		int i = corner * 3;
		coords[i] = (float) point.x();
		coords[i + 1] = (float) point.y();
		coords[i + 2] = (float) point.z();
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		int alpha = getFillAlpha();
		Color color = new Color(DEFAULT_COLOR.getRed(), DEFAULT_COLOR.getGreen(), DEFAULT_COLOR.getBlue(), alpha);
		for (int[] face : FACES) {
			Support3D.drawQuad(drawable, coords, face[0], face[1], face[2], face[3], color, 1f, true);
		}
	}

	@Override
	protected void drawData(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.RAW_DATA)) {
			return;
		}
		DCGeometry geometry = panel.dcGeometry();
		List<ForwardPanel3D.LayerWire> hits = panel.dcRawHits(sector, superlayer);
		for (ForwardPanel3D.LayerWire hit : hits) {
			Segment3 line = geometry.wireLine(sector, superlayer, hit.layer(), hit.wire());
			Support3D.drawLine(drawable, line.start().x(), line.start().y(), line.start().z(),
					line.end().x(), line.end().y(), line.end().z(), HIT_COLOR, HIT_LINE_WIDTH);
		}
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(CedDisplayOption.DC) && panel.isDisplayed(ForwardPanel3D.sectorOption(sector));
	}
}
