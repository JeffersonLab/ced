package edu.cnu.ced.view.alert;

import java.awt.Color;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.AlertEventData;
import edu.cnu.ced.geometry.AlertGeometry;
import edu.cnu.ced.geometry.Segment3;
import edu.cnu.ced.view3d.DetectorItem3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * One ALERT drift-chamber (AHDC) layer, all its wires drawn as thin lines
 * in a single {@code drawShape()} call -- matching legacy CED's own {@code
 * cnuphys.ced.ced3d.alert.AlertDCLayer3D} (one item per (sector,
 * superlayer, layer), all wires drawn together; AHDC's wire count per
 * layer is small enough that legacy itself never split this further).
 * AHDC has no real sector division ({@link AlertGeometry}'s sole DC
 * sector is always 0), so there is no per-sector gate here.
 */
final class AlertDcLayer3D extends DetectorItem3D {

	private static final Color WIRE_COLOR = Color.lightGray;
	private static final Color HIT_COLOR = Color.red;
	private static final float WIRE_LINE_WIDTH = 1.5f;
	private static final float HIT_LINE_WIDTH = 3f;

	private final AlertPanel3D panel;
	private final int sector;
	private final int superlayer;
	private final int layer;

	AlertDcLayer3D(AlertPanel3D panel, int sector, int superlayer, int layer) {
		super(panel);
		this.panel = panel;
		this.sector = sector;
		this.superlayer = superlayer;
		this.layer = layer;
	}

	@Override
	protected void drawShape(GLAutoDrawable drawable) {
		for (Segment3 wire : panel.geometry().dcWires(sector, superlayer, layer)) {
			Support3D.drawLine(drawable, wire.start().x(), wire.start().y(), wire.start().z(),
					wire.end().x(), wire.end().y(), wire.end().z(), WIRE_COLOR, WIRE_LINE_WIDTH);
		}
	}

	@Override
	protected void drawData(GLAutoDrawable drawable) {
		if (!panel.isDisplayed(CedDisplayOption.RAW_DATA)) {
			return;
		}
		List<Segment3> wires = panel.geometry().dcWires(sector, superlayer, layer);
		for (AlertEventData.DcAdcHit hit : panel.dcAdcHits(sector, superlayer, layer)) {
			if (hit.wire() < 0 || hit.wire() >= wires.size()) {
				continue;
			}
			Segment3 wire = wires.get(hit.wire());
			Support3D.drawLine(drawable, wire.start().x(), wire.start().y(), wire.start().z(),
					wire.end().x(), wire.end().y(), wire.end().z(), HIT_COLOR, HIT_LINE_WIDTH);
		}
	}

	@Override
	protected boolean show() {
		return panel.isDisplayed(CedDisplayOption.ALERT_DC);
	}
}
