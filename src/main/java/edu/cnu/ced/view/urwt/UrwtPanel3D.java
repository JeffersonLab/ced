package edu.cnu.ced.view.urwt;

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
import edu.cnu.ced.data.MonteCarloTracks;
import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.data.ReconstructedTracks;
import edu.cnu.ced.data.TrackRow;
import edu.cnu.ced.data.URWTEventData;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.ced.geometry.URWTGeometry;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.view3d.CedPanel3D;
import edu.cnu.ced.view3d.TrackTrajectoryDrawer3D;
import edu.cnu.ced.view3d.TrackTrajectorySource;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;

/**
 * The 3D scene for {@link UrwtView3D}: an axis set, one item per (sector,
 * layer) μrWT detector, and one item for every category of
 * reconstructed/Monte Carlo track.
 *
 * <p>
 * Scoped to the swim-cache-based track drawing every other 3D view
 * shares, matching legacy CED's own {@code UrWTPanel3D} -- which draws
 * tracks via its own generic {@code TrajectoryDrawer3D} but has no
 * dedicated 3D cross drawer for {@code URWT::crosses} (that bank is only
 * ever drawn in the 2D {@code UrWTXYView}), so this view adds no cross
 * drawer either.
 * </p>
 */
final class UrwtPanel3D extends CedPanel3D implements MagneticFieldChangeListener, TrackTrajectorySource {

	private static final float X_MAX = 400f;
	private static final float Y_MAX = 400f;
	private static final float Z_MIN = -100f;
	private static final float Z_MAX = 250f;

	// Set via setGeometry()/setSwimCache() by UrwtView3D immediately after
	// this panel is constructed (see make3DPanel()); see FTCalPanel3D's own
	// comment on the identical constructor-ordering reason.
	private URWTGeometry geometry;
	private SwimTrajectoryCache swimCache;

	// Refreshed on every magnetic-field change, matching every 2D view's
	// own identical fieldProbe field (e.g. SectorView, DCXYView).
	private volatile FieldProbe fieldProbe = FieldProbe.factory();

	private volatile Map<Address, List<URWTEventData.Hit>> hits = Map.of();

	private volatile List<TrackRow> mcTracks = List.of();
	private volatile List<TrackRow> hbTracks = List.of();
	private volatile List<TrackRow> tbTracks = List.of();
	private volatile List<TrackRow> aiHbTracks = List.of();
	private volatile List<TrackRow> aiTbTracks = List.of();
	private volatile List<TrackRow> cvtTracks = List.of();
	private volatile List<RecEventData.Particle> recParticles = List.of();

	UrwtPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(EnumSet.of(CedDisplayOption.VOLUMES, CedDisplayOption.TRUTH, CedDisplayOption.RAW_DATA,
				CedDisplayOption.URWT_LAYER_1, CedDisplayOption.URWT_LAYER_2,
				CedDisplayOption.URWT_LAYER_3, CedDisplayOption.URWT_LAYER_4,
				CedDisplayOption.MC_TRACKS, CedDisplayOption.HB_TRACKS, CedDisplayOption.TB_TRACKS,
				CedDisplayOption.AI_HB_TRACKS, CedDisplayOption.AI_TB_TRACKS, CedDisplayOption.RECON_TRACKS,
				CedDisplayOption.CVT_TRACKS),
				angleX, angleY, angleZ, xDist, yDist, zDist);
		MagneticFields.getInstance().addMagneticFieldChangeListener(this);
	}

	void setGeometry(URWTGeometry geometry) {
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
		addItem(new Axes3D(this, -X_MAX, X_MAX, -Y_MAX, Y_MAX, Z_MIN, Z_MAX,
				new String[] { "x", "y", "z" }, Color.darkGray, 1f, 6, 6, 6,
				Color.black, new Color(0, 100, 0), new Font("SansSerif", Font.PLAIN, 10), 0));
		for (int sector = 1; sector <= URWTGeometry.SECTOR_COUNT; sector++) {
			for (int layer = 1; layer <= URWTGeometry.LAYER_COUNT; layer++) {
				addItem(new UrwtDetector3D(this, sector, layer));
			}
		}
		addItem(new TrackTrajectoryDrawer3D<>(this));
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	/** Refreshes every piece of this event's display data; called by {@link UrwtView3D}. */
	void setEventData(EventSnapshot snapshot) {
		URWTEventData data = URWTEventData.from(snapshot);

		Map<Address, List<URWTEventData.Hit>> map = new HashMap<>();
		for (URWTEventData.Hit hit : data.hits()) {
			map.computeIfAbsent(new Address(hit.sector(), hit.layer()), k -> new ArrayList<>()).add(hit);
		}
		this.hits = Map.copyOf(map);

		this.mcTracks = MonteCarloTracks.from(snapshot).tracks();
		this.hbTracks = ReconstructedTracks.hbTracks(snapshot);
		this.tbTracks = ReconstructedTracks.tbTracks(snapshot);
		this.aiHbTracks = ReconstructedTracks.aiHbTracks(snapshot);
		this.aiTbTracks = ReconstructedTracks.aiTbTracks(snapshot);
		this.cvtTracks = ReconstructedTracks.cvtTracks(snapshot);
		this.recParticles = RecEventData.from(snapshot).particles();
	}

	URWTGeometry geometry() {
		return geometry;
	}

	List<URWTEventData.Hit> hits(int sector, int layer) {
		return hits.getOrDefault(new Address(sector, layer), List.of());
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

	/** 1-based (sector, layer) address for a hit lookup. */
	private record Address(int sector, int layer) {
	}
}
