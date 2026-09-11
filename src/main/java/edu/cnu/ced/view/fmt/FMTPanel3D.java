package edu.cnu.ced.view.fmt;

import java.awt.Color;
import java.awt.Font;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.FMTEventData;
import edu.cnu.ced.data.FMTEventData.AdcHit;
import edu.cnu.ced.data.FMTEventData.Cluster;
import edu.cnu.ced.data.FMTEventData.ReconHit;
import edu.cnu.ced.geometry.FMTGeometry;
import edu.cnu.ced.view3d.CedPanel3D;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;

/** The 3D scene for {@link FMTView3D}: an axis set and one item per FMT layer. */
final class FMTPanel3D extends CedPanel3D {

	private static final float XY_MAX = 25f;
	private static final float Z_MIN = 0f;
	private static final float Z_MAX = 50f;

	// Set via setGeometry() by FMTView3D immediately after this panel is
	// constructed (see make3DPanel()); see FTCalPanel3D's own comment on
	// the identical constructor-ordering reason.
	private FMTGeometry geometry;

	private volatile Map<LayerStrip, Integer> adcByStrip = Map.of();
	private volatile Set<LayerStrip> clusterSeeds = Set.of();
	private volatile Set<LayerStrip> reconHitStrips = Set.of();
	private volatile int maxAdc;

	FMTPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(EnumSet.of(CedDisplayOption.VOLUMES, CedDisplayOption.TRUTH, CedDisplayOption.RECON_HITS,
				CedDisplayOption.CLUSTERS,
				CedDisplayOption.FMT, CedDisplayOption.FMT_LAYER_1, CedDisplayOption.FMT_LAYER_2,
				CedDisplayOption.FMT_LAYER_3, CedDisplayOption.FMT_LAYER_4, CedDisplayOption.FMT_LAYER_5,
				CedDisplayOption.FMT_LAYER_6,
				CedDisplayOption.FMT_REGION_1, CedDisplayOption.FMT_REGION_2, CedDisplayOption.FMT_REGION_3,
				CedDisplayOption.FMT_REGION_4),
				angleX, angleY, angleZ, xDist, yDist, zDist);
	}

	void setGeometry(FMTGeometry geometry) {
		this.geometry = geometry;
	}

	@Override
	public void createInitialItems() {
		addItem(new Axes3D(this, -XY_MAX, XY_MAX, -XY_MAX, XY_MAX, Z_MIN, Z_MAX,
				new String[] { "x", "y", "z" }, Color.darkGray, 1f, 6, 6, 6,
				Color.black, new Color(0, 100, 0), new Font("SansSerif", Font.PLAIN, 10), 0));
		addItem(new FmtLayer3D(this, 1, CedDisplayOption.FMT_LAYER_1));
		addItem(new FmtLayer3D(this, 2, CedDisplayOption.FMT_LAYER_2));
		addItem(new FmtLayer3D(this, 3, CedDisplayOption.FMT_LAYER_3));
		addItem(new FmtLayer3D(this, 4, CedDisplayOption.FMT_LAYER_4));
		addItem(new FmtLayer3D(this, 5, CedDisplayOption.FMT_LAYER_5));
		addItem(new FmtLayer3D(this, 6, CedDisplayOption.FMT_LAYER_6));
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	/** Refreshes the current event's ADC hits/recon hits/cluster seeds; called by {@link FMTView3D}. */
	void setEventData(FMTEventData data) {
		Map<LayerStrip, Integer> adc = new HashMap<>();
		int max = 0;
		for (AdcHit hit : data.adcHits()) {
			adc.merge(new LayerStrip(hit.layer(), hit.strip()), hit.adc(), Math::max);
			max = Math.max(max, hit.adc());
		}
		Set<LayerStrip> recon = new HashSet<>();
		for (ReconHit hit : data.reconHits()) {
			recon.add(new LayerStrip(hit.layer(), hit.strip()));
		}
		Set<LayerStrip> clusters = new HashSet<>();
		for (Cluster cluster : data.clusters()) {
			clusters.add(new LayerStrip(cluster.layer(), cluster.seedStrip()));
		}
		this.adcByStrip = Map.copyOf(adc);
		this.reconHitStrips = Set.copyOf(recon);
		this.clusterSeeds = Set.copyOf(clusters);
		this.maxAdc = max;
	}

	FMTGeometry geometry() {
		return geometry;
	}

	/** ADC for the (0-based layer, 1-based strip) channel, strongest of its two read-out ends, or {@code null}. */
	Integer adc(int layer, int strip) {
		return adcByStrip.get(new LayerStrip(layer, strip));
	}

	int maximumAdc() {
		return maxAdc;
	}

	boolean hasReconHit(int layer, int strip) {
		return reconHitStrips.contains(new LayerStrip(layer, strip));
	}

	boolean isClusterSeed(int layer, int strip) {
		return clusterSeeds.contains(new LayerStrip(layer, strip));
	}

	/** 0-based layer, 1-based strip -- matching FMTEventData's own addressing. */
	private record LayerStrip(int layer, int strip) {
	}
}
