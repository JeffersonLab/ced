package edu.cnu.ced.view.ftcal;

import java.awt.Color;
import java.awt.Font;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.FTCalEventData;
import edu.cnu.ced.data.FTCalEventData.AdcHit;
import edu.cnu.ced.geometry.FTCALGeometry;
import edu.cnu.ced.view3d.CedPanel3D;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;

/** The 3D scene for {@link FTCalView3D}: an axis set plus one crystal per FTCAL component. */
final class FTCalPanel3D extends CedPanel3D {

	private static final float XY_MAX = 50f;
	private static final float Z_MIN = -50f;
	private static final float Z_MAX = 50f;

	// Set via setGeometry() by FTCalView3D immediately after this panel is
	// constructed (see make3DPanel()), rather than taken as a constructor
	// parameter here: this panel is itself built from inside Panel3D's own
	// constructor (via the make3DPanel() hook), before FTCalView3D's own
	// constructor body -- where its "geometry" constructor argument actually
	// lives -- has run. createInitialItems() below, which needs this field,
	// is only invoked later, once this panel's first OpenGL context is
	// initialized (see Panel3D#createInitialItems), which is safely after
	// setGeometry() has already been called.
	private FTCALGeometry geometry;
	private volatile FTCalEventData eventData = FTCalEventData.from(null);
	private volatile Map<Integer, AdcHit> adcByComponent = Map.of();

	FTCalPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(EnumSet.of(CedDisplayOption.VOLUMES, CedDisplayOption.TRUTH), angleX, angleY, angleZ,
				xDist, yDist, zDist);
	}

	void setGeometry(FTCALGeometry geometry) {
		this.geometry = geometry;
	}

	@Override
	public void createInitialItems() {
		addItem(new Axes3D(this, -XY_MAX, XY_MAX, -XY_MAX, XY_MAX, Z_MIN, Z_MAX,
				new String[] { "x", "y", "z" }, Color.black, 1.5f, 5, 5, 5,
				Color.gray, Color.black, new Font("SansSerif", Font.PLAIN, 10), 0));
		for (int id : geometry.componentIds()) {
			addItem(new FTCalCrystal3D(this, geometry, id));
		}
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	/** Refreshes the current event's ADC hits; called by {@link FTCalView3D}. */
	void setEventData(FTCalEventData data) {
		this.eventData = data;
		Map<Integer, AdcHit> byComponent = new HashMap<>();
		for (AdcHit hit : data.adcHits()) {
			byComponent.put(hit.component(), hit);
		}
		this.adcByComponent = Map.copyOf(byComponent);
	}

	AdcHit adcHit(int component) {
		return adcByComponent.get(component);
	}

	int maximumAdc() {
		return eventData.maximumAdc();
	}
}
