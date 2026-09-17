package edu.cnu.ced.view.alert;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.MagneticFieldChangeListener;
import cnuphys.magfield.MagneticFields;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.AlertEventData;
import edu.cnu.ced.data.MonteCarloTracks;
import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.data.ReconstructedTracks;
import edu.cnu.ced.data.TrackRow;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.ced.geometry.AlertGeometry;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.view3d.CedPanel3D;
import edu.cnu.ced.view3d.TrackTrajectoryDrawer3D;
import edu.cnu.ced.view3d.TrackTrajectorySource;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;

/**
 * The 3D scene for {@link AlertView3D}: an axis set, one item per
 * populated AHDC (drift chamber) layer, one item per populated ATOF
 * (time-of-flight) (sector, superlayer, layer) group, one item for AHDC/
 * ATOF reconstructed clusters, and one item for every category of
 * reconstructed/Monte Carlo track.
 */
final class AlertPanel3D extends CedPanel3D implements MagneticFieldChangeListener, TrackTrajectorySource {

	private static final float XY_MAX = 150f;
	private static final float Z_MIN = -20f;
	private static final float Z_MAX = 200f;

	private static final int DC_SECTOR_COUNT = 1;
	private static final int DC_SUPERLAYER_COUNT = 5;
	private static final int DC_LAYER_COUNT = 2;
	private static final int TOF_SECTOR_COUNT = 15;
	private static final int TOF_SUPERLAYER_COUNT = 2;
	private static final int TOF_LAYER_COUNT = 4;

	// Set via setGeometry()/setSwimCache() by AlertView3D immediately
	// after this panel is constructed (see make3DPanel()); see
	// FTCalPanel3D's own comment on the identical constructor-ordering
	// reason.
	private AlertGeometry geometry;
	private SwimTrajectoryCache swimCache;

	// Refreshed on every magnetic-field change, matching every 2D view's
	// own identical fieldProbe field (e.g. SectorView, DCXYView).
	private volatile FieldProbe fieldProbe = FieldProbe.factory();

	private volatile Map<Address, List<AlertEventData.DcAdcHit>> dcAdcHits = Map.of();
	private volatile Map<Address, List<AlertEventData.TofHit>> tofHits = Map.of();
	private volatile List<AlertEventData.DcCluster> dcClusters = List.of();
	private volatile List<AlertEventData.TofCluster> tofClusters = List.of();

	private volatile List<TrackRow> mcTracks = List.of();
	private volatile List<TrackRow> hbTracks = List.of();
	private volatile List<TrackRow> tbTracks = List.of();
	private volatile List<TrackRow> aiHbTracks = List.of();
	private volatile List<TrackRow> aiTbTracks = List.of();
	private volatile List<TrackRow> cvtTracks = List.of();
	private volatile List<RecEventData.Particle> recParticles = List.of();

	AlertPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(EnumSet.of(CedDisplayOption.VOLUMES, CedDisplayOption.TRUTH, CedDisplayOption.RAW_DATA,
				CedDisplayOption.ALERT_DC, CedDisplayOption.ALERT_TOF,
				CedDisplayOption.ALERT_SECTOR_1, CedDisplayOption.ALERT_SECTOR_2, CedDisplayOption.ALERT_SECTOR_3,
				CedDisplayOption.ALERT_SECTOR_4, CedDisplayOption.ALERT_SECTOR_5, CedDisplayOption.ALERT_SECTOR_6,
				CedDisplayOption.ALERT_SECTOR_7, CedDisplayOption.ALERT_SECTOR_8, CedDisplayOption.ALERT_SECTOR_9,
				CedDisplayOption.ALERT_SECTOR_10, CedDisplayOption.ALERT_SECTOR_11, CedDisplayOption.ALERT_SECTOR_12,
				CedDisplayOption.ALERT_SECTOR_13, CedDisplayOption.ALERT_SECTOR_14, CedDisplayOption.ALERT_SECTOR_15,
				CedDisplayOption.CLUSTERS, CedDisplayOption.MC_TRACKS, CedDisplayOption.HB_TRACKS,
				CedDisplayOption.TB_TRACKS, CedDisplayOption.AI_HB_TRACKS, CedDisplayOption.AI_TB_TRACKS,
				CedDisplayOption.RECON_TRACKS, CedDisplayOption.CVT_TRACKS),
				angleX, angleY, angleZ, xDist, yDist, zDist);
		MagneticFields.getInstance().addMagneticFieldChangeListener(this);
	}

	void setGeometry(AlertGeometry geometry) {
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
		for (int sector = 0; sector < DC_SECTOR_COUNT; sector++) {
			for (int superlayer = 0; superlayer < DC_SUPERLAYER_COUNT; superlayer++) {
				for (int layer = 0; layer < DC_LAYER_COUNT; layer++) {
					if (!geometry.dcWires(sector, superlayer, layer).isEmpty()) {
						addItem(new AlertDcLayer3D(this, sector, superlayer, layer));
					}
				}
			}
		}
		for (int sector = 0; sector < TOF_SECTOR_COUNT; sector++) {
			for (int superlayer = 0; superlayer < TOF_SUPERLAYER_COUNT; superlayer++) {
				for (int layer = 0; layer < TOF_LAYER_COUNT; layer++) {
					if (!geometry.tofPaddles(sector, superlayer, layer).isEmpty()) {
						addItem(new AlertTofGroup3D(this, sector, superlayer, layer));
					}
				}
			}
		}
		addItem(new AlertClusterDrawer3D(this));
		addItem(new TrackTrajectoryDrawer3D<>(this));
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	/** Refreshes every piece of this event's display data; called by {@link AlertView3D}. */
	void setEventData(EventSnapshot snapshot) {
		AlertEventData data = AlertEventData.from(snapshot);

		Map<Address, List<AlertEventData.DcAdcHit>> map = new HashMap<>();
		for (AlertEventData.DcAdcHit hit : data.dcAdcHits()) {
			map.computeIfAbsent(new Address(hit.sector(), hit.superlayer(), hit.layer()), k -> new ArrayList<>())
					.add(hit);
		}
		this.dcAdcHits = Map.copyOf(map);

		Map<Address, List<AlertEventData.TofHit>> tofMap = new HashMap<>();
		for (AlertEventData.TofHit hit : data.tofHits()) {
			int superlayer = hit.component() == 10 ? 0 : 1;
			tofMap.computeIfAbsent(new Address(hit.sector(), superlayer, hit.layer()), k -> new ArrayList<>())
					.add(hit);
		}
		this.tofHits = Map.copyOf(tofMap);

		this.dcClusters = data.dcClusters();
		this.tofClusters = data.tofClusters();

		this.mcTracks = MonteCarloTracks.from(snapshot).tracks();
		this.hbTracks = ReconstructedTracks.hbTracks(snapshot);
		this.tbTracks = ReconstructedTracks.tbTracks(snapshot);
		this.aiHbTracks = ReconstructedTracks.aiHbTracks(snapshot);
		this.aiTbTracks = ReconstructedTracks.aiTbTracks(snapshot);
		this.cvtTracks = ReconstructedTracks.cvtTracks(snapshot);
		this.recParticles = RecEventData.from(snapshot).particles();
	}

	AlertGeometry geometry() {
		return geometry;
	}

	List<AlertEventData.DcAdcHit> dcAdcHits(int sector, int superlayer, int layer) {
		return dcAdcHits.getOrDefault(new Address(sector, superlayer, layer), List.of());
	}

	List<AlertEventData.TofHit> tofHits(int sector, int superlayer, int layer) {
		return tofHits.getOrDefault(new Address(sector, superlayer, layer), List.of());
	}

	List<AlertEventData.DcCluster> dcClusters() {
		return dcClusters;
	}

	List<AlertEventData.TofCluster> tofClusters() {
		return tofClusters;
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

	/** The shared per-sector master display toggle for ATOF's 15 (0-based) sectors. */
	static CedDisplayOption sectorOption(int sector) {
		return switch (sector) {
		case 0 -> CedDisplayOption.ALERT_SECTOR_1;
		case 1 -> CedDisplayOption.ALERT_SECTOR_2;
		case 2 -> CedDisplayOption.ALERT_SECTOR_3;
		case 3 -> CedDisplayOption.ALERT_SECTOR_4;
		case 4 -> CedDisplayOption.ALERT_SECTOR_5;
		case 5 -> CedDisplayOption.ALERT_SECTOR_6;
		case 6 -> CedDisplayOption.ALERT_SECTOR_7;
		case 7 -> CedDisplayOption.ALERT_SECTOR_8;
		case 8 -> CedDisplayOption.ALERT_SECTOR_9;
		case 9 -> CedDisplayOption.ALERT_SECTOR_10;
		case 10 -> CedDisplayOption.ALERT_SECTOR_11;
		case 11 -> CedDisplayOption.ALERT_SECTOR_12;
		case 12 -> CedDisplayOption.ALERT_SECTOR_13;
		case 13 -> CedDisplayOption.ALERT_SECTOR_14;
		default -> CedDisplayOption.ALERT_SECTOR_15;
		};
	}

	/** 0-based (sector, superlayer, layer) address, shared by AHDC and ATOF lookups. */
	private record Address(int sector, int superlayer, int layer) {
	}
}
