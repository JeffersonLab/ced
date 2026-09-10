package edu.cnu.ced.view.central;

import java.awt.Color;
import java.awt.Font;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.CentralEventData;
import edu.cnu.ced.data.CentralEventData.AdcHit;
import edu.cnu.ced.data.CentralEventData.Detector;
import edu.cnu.ced.geometry.BMTGeometry;
import edu.cnu.ced.geometry.BSTGeometry;
import edu.cnu.ced.geometry.CNDGeometry;
import edu.cnu.ced.geometry.CTOFGeometry;
import edu.cnu.ced.view3d.CedPanel3D;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;

/**
 * The 3D scene for {@link CentralView3D}: an axis set, one item per CND
 * layer, one item for all of CTOF, and one item per BST/BMT layer.
 */
final class CentralPanel3D extends CedPanel3D {

	private static final float XY_MAX = 50f;
	private static final float Z_MIN = -50f;
	private static final float Z_MAX = 50f;

	// Set via setGeometry() by CentralView3D immediately after this panel
	// is constructed (see make3DPanel()); see FTCalPanel3D's own comment
	// on the identical constructor-ordering reason.
	private CNDGeometry cndGeometry;
	private CTOFGeometry ctofGeometry;
	private BSTGeometry bstGeometry;
	private BMTGeometry bmtGeometry;

	private volatile Map<CndKey, AdcHit> cndByKey = Map.of();
	private volatile Map<Integer, AdcHit> ctofByPaddle = Map.of();
	private volatile Map<PanelKey, Integer> bstByPanel = Map.of();
	private volatile Map<PanelKey, Integer> bmtByPanel = Map.of();
	private volatile int cndMaxAdc;
	private volatile int ctofMaxAdc;
	private volatile int bstMaxAdc;
	private volatile int bmtMaxAdc;

	CentralPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(EnumSet.of(CedDisplayOption.VOLUMES, CedDisplayOption.TRUTH,
				CedDisplayOption.CND, CedDisplayOption.CND_LAYER_1, CedDisplayOption.CND_LAYER_2,
				CedDisplayOption.CND_LAYER_3, CedDisplayOption.CTOF,
				CedDisplayOption.BST, CedDisplayOption.BST_LAYER_1, CedDisplayOption.BST_LAYER_2,
				CedDisplayOption.BST_LAYER_3, CedDisplayOption.BST_LAYER_4, CedDisplayOption.BST_LAYER_5,
				CedDisplayOption.BST_LAYER_6,
				CedDisplayOption.BMT, CedDisplayOption.BMT_LAYER_1, CedDisplayOption.BMT_LAYER_2,
				CedDisplayOption.BMT_LAYER_3, CedDisplayOption.BMT_LAYER_4, CedDisplayOption.BMT_LAYER_5,
				CedDisplayOption.BMT_LAYER_6),
				angleX, angleY, angleZ, xDist, yDist, zDist);
	}

	void setGeometry(CNDGeometry cnd, CTOFGeometry ctof, BSTGeometry bst, BMTGeometry bmt) {
		this.cndGeometry = cnd;
		this.ctofGeometry = ctof;
		this.bstGeometry = bst;
		this.bmtGeometry = bmt;
	}

	@Override
	public void createInitialItems() {
		addItem(new Axes3D(this, -XY_MAX, XY_MAX, -XY_MAX, XY_MAX, Z_MIN, Z_MAX,
				new String[] { "x", "y", "z" }, Color.darkGray, 1f, 6, 6, 6,
				Color.black, new Color(0, 100, 0), new Font("SansSerif", Font.PLAIN, 10), 0));
		addItem(new CndLayer3D(this, 1, CedDisplayOption.CND_LAYER_1));
		addItem(new CndLayer3D(this, 2, CedDisplayOption.CND_LAYER_2));
		addItem(new CndLayer3D(this, 3, CedDisplayOption.CND_LAYER_3));
		addItem(new Ctof3D(this));
		addItem(new BstLayer3D(this, 1, CedDisplayOption.BST_LAYER_1));
		addItem(new BstLayer3D(this, 2, CedDisplayOption.BST_LAYER_2));
		addItem(new BstLayer3D(this, 3, CedDisplayOption.BST_LAYER_3));
		addItem(new BstLayer3D(this, 4, CedDisplayOption.BST_LAYER_4));
		addItem(new BstLayer3D(this, 5, CedDisplayOption.BST_LAYER_5));
		addItem(new BstLayer3D(this, 6, CedDisplayOption.BST_LAYER_6));
		addItem(new BmtLayer3D(this, 1, CedDisplayOption.BMT_LAYER_1));
		addItem(new BmtLayer3D(this, 2, CedDisplayOption.BMT_LAYER_2));
		addItem(new BmtLayer3D(this, 3, CedDisplayOption.BMT_LAYER_3));
		addItem(new BmtLayer3D(this, 4, CedDisplayOption.BMT_LAYER_4));
		addItem(new BmtLayer3D(this, 5, CedDisplayOption.BMT_LAYER_5));
		addItem(new BmtLayer3D(this, 6, CedDisplayOption.BMT_LAYER_6));
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	/** Refreshes the current event's ADC hits; called by {@link CentralView3D}. */
	void setEventData(CentralEventData data) {
		Map<CndKey, AdcHit> cnd = new HashMap<>();
		Map<Integer, AdcHit> ctof = new HashMap<>();
		Map<PanelKey, Integer> bst = new HashMap<>();
		Map<PanelKey, Integer> bmt = new HashMap<>();
		int cndMax = 0;
		int ctofMax = 0;
		int bstMax = 0;
		int bmtMax = 0;
		for (AdcHit hit : data.adcHits()) {
			switch (hit.detector()) {
			case CND -> {
				cnd.put(new CndKey(hit.sector(), hit.layer(), hit.order()), hit);
				cndMax = Math.max(cndMax, hit.adc());
			}
			case CTOF -> {
				ctof.put(hit.component(), hit);
				ctofMax = Math.max(ctofMax, hit.adc());
			}
			case BST -> {
				// Panel-level (not strip-level) coloring: the strongest hit
				// among all 256 strips on this (sector, layer) panel.
				bst.merge(new PanelKey(hit.sector(), hit.layer()), hit.adc(), Math::max);
				bstMax = Math.max(bstMax, hit.adc());
			}
			case BMT -> {
				bmt.merge(new PanelKey(hit.sector(), hit.layer()), hit.adc(), Math::max);
				bmtMax = Math.max(bmtMax, hit.adc());
			}
			}
		}
		this.cndByKey = Map.copyOf(cnd);
		this.ctofByPaddle = Map.copyOf(ctof);
		this.bstByPanel = Map.copyOf(bst);
		this.bmtByPanel = Map.copyOf(bmt);
		this.cndMaxAdc = cndMax;
		this.ctofMaxAdc = ctofMax;
		this.bstMaxAdc = bstMax;
		this.bmtMaxAdc = bmtMax;
	}

	CNDGeometry cndGeometry() {
		return cndGeometry;
	}

	CTOFGeometry ctofGeometry() {
		return ctofGeometry;
	}

	BSTGeometry bstGeometry() {
		return bstGeometry;
	}

	BMTGeometry bmtGeometry() {
		return bmtGeometry;
	}

	/** ADC for the CND channel at (sector, layer, order=databaseToDetector(layer,paddle)[2]-1), or {@code null}. */
	Integer cndAdc(int sector, int layer, int order) {
		AdcHit hit = cndByKey.get(new CndKey(sector, layer, order));
		return hit == null ? null : hit.adc();
	}

	int cndMaximumAdc() {
		return cndMaxAdc;
	}

	/** ADC for the CTOF paddle (1-48), or {@code null}. */
	Integer ctofAdc(int paddle) {
		AdcHit hit = ctofByPaddle.get(paddle);
		return hit == null ? null : hit.adc();
	}

	int ctofMaximumAdc() {
		return ctofMaxAdc;
	}

	/** Strongest ADC seen anywhere on the (1-based sector, 1-based layer) BST panel this event, or {@code null}. */
	Integer bstAdc(int sector, int layer) {
		return bstByPanel.get(new PanelKey(sector, layer));
	}

	int bstMaximumAdc() {
		return bstMaxAdc;
	}

	/** Strongest ADC seen anywhere on the (1-based sector, 1-based layer) BMT panel this event, or {@code null}. */
	Integer bmtAdc(int sector, int layer) {
		return bmtByPanel.get(new PanelKey(sector, layer));
	}

	int bmtMaximumAdc() {
		return bmtMaxAdc;
	}

	private record CndKey(int sector, int layer, int order) {
	}

	/** 1-based (sector, layer) panel address, shared by the BST and BMT hit maps. */
	private record PanelKey(int sector, int layer) {
	}
}
