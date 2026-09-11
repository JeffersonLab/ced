package edu.cnu.ced.view.urwt;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.URWTEventData;
import edu.cnu.ced.geometry.URWTGeometry;
import edu.cnu.ced.view3d.CedPanel3D;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;

/** The 3D scene for {@link UrwtView3D}: an axis set and one item per (sector, layer) μrWT detector. */
final class UrwtPanel3D extends CedPanel3D {

	private static final float X_MAX = 400f;
	private static final float Y_MAX = 400f;
	private static final float Z_MIN = -100f;
	private static final float Z_MAX = 250f;

	// Set via setGeometry() by UrwtView3D immediately after this panel is
	// constructed (see make3DPanel()); see FTCalPanel3D's own comment on
	// the identical constructor-ordering reason.
	private URWTGeometry geometry;

	private volatile Map<Address, List<URWTEventData.Hit>> hits = Map.of();

	UrwtPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(EnumSet.of(CedDisplayOption.VOLUMES, CedDisplayOption.TRUTH, CedDisplayOption.RAW_DATA,
				CedDisplayOption.URWT_LAYER_1, CedDisplayOption.URWT_LAYER_2,
				CedDisplayOption.URWT_LAYER_3, CedDisplayOption.URWT_LAYER_4),
				angleX, angleY, angleZ, xDist, yDist, zDist);
	}

	void setGeometry(URWTGeometry geometry) {
		this.geometry = geometry;
	}

	@Override
	public void createInitialItems() {
		addItem(new Axes3D(this, -X_MAX, X_MAX, -Y_MAX, Y_MAX, Z_MIN, Z_MAX,
				new String[] { "x", "y", "z" }, Color.darkGray, 1f, 6, 6, 6,
				Color.black, new Color(0, 100, 0), new Font("SansSerif", Font.PLAIN, 10), 0));
		for (int sector = 1; sector <= URWTGeometry.SECTOR_COUNT; sector++) {
			for (int layer = 1; layer <= URWTGeometry.LAYER_COUNT; layer++) {
				addItem(new UrwtDetector3D(this, sector, layer));
			}
		}
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	/** Refreshes the current event's hits; called by {@link UrwtView3D}. */
	void setEventData(URWTEventData data) {
		Map<Address, List<URWTEventData.Hit>> map = new HashMap<>();
		for (URWTEventData.Hit hit : data.hits()) {
			map.computeIfAbsent(new Address(hit.sector(), hit.layer()), k -> new ArrayList<>()).add(hit);
		}
		this.hits = Map.copyOf(map);
	}

	URWTGeometry geometry() {
		return geometry;
	}

	List<URWTEventData.Hit> hits(int sector, int layer) {
		return hits.getOrDefault(new Address(sector, layer), List.of());
	}

	/** 1-based (sector, layer) address for a hit lookup. */
	private record Address(int sector, int layer) {
	}
}
