package edu.cnu.ced.view.fmt;

import java.awt.Color;
import java.awt.Font;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.MagneticFieldChangeListener;
import cnuphys.magfield.MagneticFields;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.FMTEventData;
import edu.cnu.ced.data.FMTEventData.AdcHit;
import edu.cnu.ced.data.FMTEventData.Cluster;
import edu.cnu.ced.data.FMTEventData.Cross;
import edu.cnu.ced.data.FMTEventData.ReconHit;
import edu.cnu.ced.data.FMTEventData.TrackStatus;
import edu.cnu.ced.data.FMTEventData.Trajectory;
import edu.cnu.ced.data.MonteCarloTracks;
import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.data.ReconstructedTracks;
import edu.cnu.ced.data.TrackRow;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.ced.geometry.FMTGeometry;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.view3d.CedPanel3D;
import edu.cnu.ced.view3d.TrackTrajectoryDrawer3D;
import edu.cnu.ced.view3d.TrackTrajectorySource;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;

/**
 * The 3D scene for {@link FMTView3D}: an axis set, one item per FMT
 * layer, one item for FMT reconstructed crosses, one item for every
 * category of reconstructed/Monte Carlo track, and one item for FMT's
 * own per-layer track-trajectory points ({@code FMT::Trajectory}).
 */
final class FMTPanel3D extends CedPanel3D implements MagneticFieldChangeListener, TrackTrajectorySource {

	private static final float XY_MAX = 25f;
	private static final float Z_MIN = 0f;
	private static final float Z_MAX = 50f;

	// Set via setGeometry()/setSwimCache() by FMTView3D immediately after
	// this panel is constructed (see make3DPanel()); see FTCalPanel3D's
	// own comment on the identical constructor-ordering reason.
	private FMTGeometry geometry;
	private SwimTrajectoryCache swimCache;

	// Refreshed on every magnetic-field change, matching every 2D view's
	// own identical fieldProbe field (e.g. SectorView, DCXYView).
	private volatile FieldProbe fieldProbe = FieldProbe.factory();

	private volatile Map<LayerStrip, Integer> adcByStrip = Map.of();
	private volatile Set<LayerStrip> clusterSeeds = Set.of();
	private volatile Set<LayerStrip> reconHitStrips = Set.of();
	private volatile int maxAdc;
	private volatile List<Cross> crosses = List.of();
	private volatile List<Trajectory> trajectories = List.of();
	private volatile Map<Integer, Boolean> originalDcTrackByIndex = Map.of();

	private volatile List<TrackRow> mcTracks = List.of();
	private volatile List<TrackRow> hbTracks = List.of();
	private volatile List<TrackRow> tbTracks = List.of();
	private volatile List<TrackRow> aiHbTracks = List.of();
	private volatile List<TrackRow> aiTbTracks = List.of();
	private volatile List<TrackRow> cvtTracks = List.of();
	private volatile List<RecEventData.Particle> recParticles = List.of();

	FMTPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(EnumSet.of(CedDisplayOption.VOLUMES, CedDisplayOption.TRUTH, CedDisplayOption.RECON_HITS,
				CedDisplayOption.CLUSTERS,
				CedDisplayOption.FMT, CedDisplayOption.FMT_LAYER_1, CedDisplayOption.FMT_LAYER_2,
				CedDisplayOption.FMT_LAYER_3, CedDisplayOption.FMT_LAYER_4, CedDisplayOption.FMT_LAYER_5,
				CedDisplayOption.FMT_LAYER_6,
				CedDisplayOption.FMT_REGION_1, CedDisplayOption.FMT_REGION_2, CedDisplayOption.FMT_REGION_3,
				CedDisplayOption.FMT_REGION_4,
				CedDisplayOption.CROSSES, CedDisplayOption.MC_TRACKS, CedDisplayOption.HB_TRACKS,
				CedDisplayOption.TB_TRACKS, CedDisplayOption.AI_HB_TRACKS, CedDisplayOption.AI_TB_TRACKS,
				CedDisplayOption.RECON_TRACKS, CedDisplayOption.CVT_TRACKS, CedDisplayOption.FMT_TRAJECTORIES),
				angleX, angleY, angleZ, xDist, yDist, zDist);
		MagneticFields.getInstance().addMagneticFieldChangeListener(this);
	}

	void setGeometry(FMTGeometry geometry) {
		this.geometry = geometry;
	}

	void setSwimCache(SwimTrajectoryCache swimCache) {
		this.swimCache = swimCache;
	}

	@Override
	public void magneticFieldChanged() {
		fieldProbe = FieldProbe.factory();
		refresh();
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
		addItem(new FmtCrossDrawer3D(this));
		addItem(new FmtTrajectoryDrawer3D(this));
		addItem(new TrackTrajectoryDrawer3D<>(this));
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	/** Refreshes every piece of this event's display data; called by {@link FMTView3D}. */
	void setEventData(EventSnapshot snapshot) {
		FMTEventData data = FMTEventData.from(snapshot);

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
		this.crosses = data.crosses();
		this.trajectories = data.trajectories();

		Map<Integer, Boolean> statusByIndex = new HashMap<>();
		for (TrackStatus status : data.trackStatuses()) {
			statusByIndex.put(status.trackIndex(), status.isOriginalDcTrack());
		}
		this.originalDcTrackByIndex = Map.copyOf(statusByIndex);

		this.mcTracks = MonteCarloTracks.from(snapshot).tracks();
		this.hbTracks = ReconstructedTracks.hbTracks(snapshot);
		this.tbTracks = ReconstructedTracks.tbTracks(snapshot);
		this.aiHbTracks = ReconstructedTracks.aiHbTracks(snapshot);
		this.aiTbTracks = ReconstructedTracks.aiTbTracks(snapshot);
		this.cvtTracks = ReconstructedTracks.cvtTracks(snapshot);
		this.recParticles = RecEventData.from(snapshot).particles();
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

	List<Cross> crosses() {
		return crosses;
	}

	List<Trajectory> trajectories() {
		return trajectories;
	}

	/** Whether {@code trackIndex} (a DC track index from an {@link Trajectory}) is still just its original DC-only fit. */
	boolean isOriginalDcTrack(int trackIndex) {
		return originalDcTrackByIndex.getOrDefault(trackIndex, Boolean.FALSE);
	}

	@Override
	public SwimTrajectoryCache swimCache() {
		return swimCache;
	}

	@Override
	public FieldProbe fieldProbe() {
		return fieldProbe;
	}

	@Override
	public List<TrackRow> mcTracks() {
		return mcTracks;
	}

	@Override
	public List<TrackRow> hbTracks() {
		return hbTracks;
	}

	@Override
	public List<TrackRow> tbTracks() {
		return tbTracks;
	}

	@Override
	public List<TrackRow> aiHbTracks() {
		return aiHbTracks;
	}

	@Override
	public List<TrackRow> aiTbTracks() {
		return aiTbTracks;
	}

	@Override
	public List<TrackRow> cvtTracks() {
		return cvtTracks;
	}

	@Override
	public List<RecEventData.Particle> recParticles() {
		return recParticles;
	}

	/** 0-based layer, 1-based strip -- matching FMTEventData's own addressing. */
	private record LayerStrip(int layer, int strip) {
	}
}
