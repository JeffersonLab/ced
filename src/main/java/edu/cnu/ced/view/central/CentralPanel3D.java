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
import edu.cnu.ced.geometry.CNDGeometry;
import edu.cnu.ced.geometry.CTOFGeometry;
import edu.cnu.ced.view3d.CedPanel3D;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;

/**
 * The 3D scene for {@link CentralView3D}: an axis set, one item per CND
 * layer, and one item for all of CTOF.
 *
 * <p>
 * Scoped to CND and CTOF only for now -- legacy CED's own Central 3D view
 * also draws BST and BMT (the silicon and micromegas barrel trackers), but
 * those use a materially different vertex format in mdi_ced's own
 * geometry classes (strip endpoints and panel limits, not box corners)
 * that legacy derives through its own {@code BSTGeometry.getLayerQuads}/
 * {@code BMTGeometry.getCRZEndPoints} -- {@code mdi_ced}'s {@code
 * BSTGeometry}/{@code BMTGeometry} have no equivalent yet. Left as a
 * follow-up rather than blocking CND/CTOF, which -- like FTCal -- already
 * expose exactly the box-corner vertex format this package's rendering
 * expects.
 * </p>
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

	private volatile Map<CndKey, AdcHit> cndByKey = Map.of();
	private volatile Map<Integer, AdcHit> ctofByPaddle = Map.of();
	private volatile int cndMaxAdc;
	private volatile int ctofMaxAdc;

	CentralPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(EnumSet.of(CedDisplayOption.VOLUMES, CedDisplayOption.TRUTH,
				CedDisplayOption.CND, CedDisplayOption.CND_LAYER_1, CedDisplayOption.CND_LAYER_2,
				CedDisplayOption.CND_LAYER_3, CedDisplayOption.CTOF),
				angleX, angleY, angleZ, xDist, yDist, zDist);
	}

	void setGeometry(CNDGeometry cnd, CTOFGeometry ctof) {
		this.cndGeometry = cnd;
		this.ctofGeometry = ctof;
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
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	/** Refreshes the current event's ADC hits; called by {@link CentralView3D}. */
	void setEventData(CentralEventData data) {
		Map<CndKey, AdcHit> cnd = new HashMap<>();
		Map<Integer, AdcHit> ctof = new HashMap<>();
		int cndMax = 0;
		int ctofMax = 0;
		for (AdcHit hit : data.adcHits()) {
			if (hit.detector() == Detector.CND) {
				cnd.put(new CndKey(hit.sector(), hit.layer(), hit.order()), hit);
				cndMax = Math.max(cndMax, hit.adc());
			} else if (hit.detector() == Detector.CTOF) {
				ctof.put(hit.component(), hit);
				ctofMax = Math.max(ctofMax, hit.adc());
			}
		}
		this.cndByKey = Map.copyOf(cnd);
		this.ctofByPaddle = Map.copyOf(ctof);
		this.cndMaxAdc = cndMax;
		this.ctofMaxAdc = ctofMax;
	}

	CNDGeometry cndGeometry() {
		return cndGeometry;
	}

	CTOFGeometry ctofGeometry() {
		return ctofGeometry;
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

	private record CndKey(int sector, int layer, int order) {
	}
}
