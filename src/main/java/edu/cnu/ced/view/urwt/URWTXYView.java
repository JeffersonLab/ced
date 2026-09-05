package edu.cnu.ced.view.urwt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
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
import edu.cnu.ced.data.URWTAccumulation;
import edu.cnu.ced.data.URWTEventData;
import edu.cnu.ced.data.URWTEventData.Cluster;
import edu.cnu.ced.data.URWTEventData.Cross;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.geometry.Segment3;
import edu.cnu.ced.geometry.URWTGeometry;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.swim.SwimmableParticle;
import edu.cnu.ced.view.CedXYView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * μrWT (Micro Ring Wire Tracker) laboratory XY display backed directly by
 * CLAS banks -- six sectors, each shown as its own physical panel outline
 * (one per layer), plus reconstructed/MC/HB/TB track overlays swum through
 * the field, matching legacy CED's own μrWT view (which draws Truth/REC
 * ::Particle/HB/TB tracks on top of the six panels) and CentralXYView's
 * established swim-and-draw pattern.
 * <p>
 * Earlier versions of this view drew every individual strip as its own
 * thin line, which for ~1400 strips per (sector, layer) rendered as a dense
 * scribble rather than legacy's clean six-panel hexagon. The fix: take the
 * convex hull of every strip's own two endpoints for a given (sector,
 * layer), which for a genuinely convex panel is exactly its true outline --
 * computed once per panel at construction time (geometry never changes),
 * not per repaint. An earlier attempt hulled only each strip's
 * <em>midpoint</em>, on the assumption that (as for BST/FMT/etc.) a strip's
 * start/end differ only by its own narrow width -- wrong here: many of
 * these strips are genuinely long (one endpoint near an inner convergence
 * point, the other at the panel's true outer edge), confirmed empirically
 * against the real geometry, not assumed. Collapsing each one to its
 * midpoint threw that real extent away and left a hull of only a handful
 * of nearly-collinear points -- a thin, degenerate dart rather than the
 * true wedge, and six of those darts crossing each other is exactly what
 * produces a six-pointed star.
 * </p>
 */
@SuppressWarnings("serial")
public final class URWTXYView extends CedXYView implements MagneticFieldChangeListener {

	private static final Color[] LAYER_COLORS = {
			new Color(220, 20, 60), new Color(255, 140, 0), new Color(30, 144, 255), new Color(148, 0, 211)
	};
	private static final Color CLUSTER_COLOR = new Color(205, 0, 205);
	private static final Color CROSS_COLOR = new Color(20, 145, 35);

	private final URWTGeometry geometry;
	private final URWTAccumulation accumulation;
	private final SwimTrajectoryCache swimCache;
	// Panel outlines (world-space convex hull of that (sector, layer)'s strip
	// midpoints), computed once at construction -- see class doc.
	private final Map<PanelAddress, List<Point3>> panelOutlines = new HashMap<>();
	private final Map<Object, Point> markers = new HashMap<>();
	private final List<ScreenTrack> screenReconTracks = new ArrayList<>();
	private final List<ScreenTrack> screenMcTracks = new ArrayList<>();
	private final List<ScreenTrack> screenHbTracks = new ArrayList<>();
	private final List<ScreenTrack> screenTbTracks = new ArrayList<>();
	private final List<ScreenTrack> screenAiHbTracks = new ArrayList<>();
	private final List<ScreenTrack> screenAiTbTracks = new ArrayList<>();
	private volatile URWTEventData eventData = URWTEventData.from(null);
	private volatile RecEventData recData = RecEventData.from(null);
	private volatile List<TrackRow> mcTracks = List.of();
	private volatile List<TrackRow> hbTracks = List.of();
	private volatile List<TrackRow> tbTracks = List.of();
	private volatile List<TrackRow> aiHbTracks = List.of();
	private volatile List<TrackRow> aiTbTracks = List.of();
	private volatile FieldProbe fieldProbe = FieldProbe.factory();

	public URWTXYView(URWTGeometry geometry, EventNavigator navigator, URWTAccumulation accumulation,
			SwimTrajectoryCache swimCache) {
		super(navigator, PropertyUtils.TITLE, "μrWT XY",
				PropertyUtils.WIDTH, 700, PropertyUtils.HEIGHT, 700,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(-250, 250, 500, -500),
				PropertyUtils.BACKGROUND, Color.WHITE,
				PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.accumulation = accumulation;
		this.swimCache = swimCache;
		buildPanelOutlines();
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT, CedDisplayOption.ACCUMULATION,
				CedDisplayOption.RECON_TRACKS, CedDisplayOption.MC_TRACKS,
				CedDisplayOption.HB_TRACKS, CedDisplayOption.TB_TRACKS,
				CedDisplayOption.AI_HB_TRACKS, CedDisplayOption.AI_TB_TRACKS,
				CedDisplayOption.CLUSTERS, CedDisplayOption.CROSSES),
				List.of("URWT::"), ScientificColorMap.TURBO, "Relative occupancy / accumulation");
		MagneticFields.getInstance().addMagneticFieldChangeListener(this);
	}

	private void buildPanelOutlines() {
		for (int sector = 1; sector <= URWTGeometry.SECTOR_COUNT; sector++) {
			for (int layer = 1; layer <= URWTGeometry.LAYER_COUNT; layer++) {
				List<Segment3> strips = geometry.detector(sector, layer).strips();
				// Both endpoints of every strip, not just their midpoint: many
				// strips here are genuinely long (one endpoint near an inner
				// convergence point, the other out at the panel's true outer
				// edge), not the near-zero-width segments a strip's start/end
				// are for other detectors (BST/FMT/etc.). Hulling only
				// midpoints throws that real extent away and collapses the
				// hull into a thin, degenerate dart -- confirmed empirically:
				// six of those darts, each stretching across the panel from
				// one side to the other, is exactly what produces a
				// six-pointed star when drawn.
				List<Point3> corners = new ArrayList<>(2 * strips.size());
				for (Segment3 strip : strips) {
					corners.add(new Point3(strip.start().x(), strip.start().y(), 0));
					corners.add(new Point3(strip.end().x(), strip.end().y(), 0));
				}
				panelOutlines.put(new PanelAddress(sector, layer), convexHull(corners));
			}
		}
	}

	/** Andrew's monotone chain -- exact for a genuinely convex panel, which a wedge/trapezoid strip layer is. */
	static List<Point3> convexHull(List<Point3> points) {
		List<Point3> sorted = new ArrayList<>(points);
		sorted.sort(Comparator.<Point3>comparingDouble(Point3::x).thenComparingDouble(Point3::y));
		int n = sorted.size();
		if (n < 3) return sorted;
		Point3[] hull = new Point3[2 * n];
		int k = 0;
		for (int i = 0; i < n; i++) {
			while (k >= 2 && cross(hull[k - 2], hull[k - 1], sorted.get(i)) <= 0) k--;
			hull[k++] = sorted.get(i);
		}
		int lower = k + 1;
		for (int i = n - 2; i >= 0; i--) {
			while (k >= lower && cross(hull[k - 2], hull[k - 1], sorted.get(i)) <= 0) k--;
			hull[k++] = sorted.get(i);
		}
		List<Point3> result = new ArrayList<>(k - 1);
		for (int i = 0; i < k - 1; i++) result.add(hull[i]);
		return result;
	}

	private static double cross(Point3 o, Point3 a, Point3 b) {
		return (a.x() - o.x()) * (b.y() - o.y()) - (a.y() - o.y()) * (b.x() - o.x());
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		eventData = URWTEventData.from(state.snapshot());
		recData = RecEventData.from(state.snapshot());
		mcTracks = MonteCarloTracks.from(state.snapshot()).tracks();
		hbTracks = ReconstructedTracks.hbTracks(state.snapshot());
		tbTracks = ReconstructedTracks.tbTracks(state.snapshot());
		aiHbTracks = ReconstructedTracks.aiHbTracks(state.snapshot());
		aiTbTracks = ReconstructedTracks.aiTbTracks(state.snapshot());
		swimCache.forEvent(state.snapshot());
	}

	private void draw(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			markers.clear();
			screenReconTracks.clear();
			screenMcTracks.clear();
			screenHbTracks.clear();
			screenTbTracks.clear();
			screenAiHbTracks.clear();
			screenAiTbTracks.clear();
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
				drawAccumulatedPanels(g, container);
			} else {
				drawPanels(g, container);
				if (isDisplayed(CedDisplayOption.CLUSTERS)) drawClusters(g, container);
				if (isDisplayed(CedDisplayOption.CROSSES)) drawCrosses(g, container);
				if (isDisplayed(CedDisplayOption.RECON_TRACKS)) drawReconTracks(g, container);
				drawTrackRows(g, container, CedDisplayOption.MC_TRACKS, mcTracks, screenMcTracks, null);
				drawTrackRows(g, container, CedDisplayOption.HB_TRACKS, hbTracks, screenHbTracks, null);
				drawTrackRows(g, container, CedDisplayOption.TB_TRACKS, tbTracks, screenTbTracks, null);
				drawTrackRows(g, container, CedDisplayOption.AI_HB_TRACKS, aiHbTracks, screenAiHbTracks,
						CedDrawingStyle.AI_HIT_BASED);
				drawTrackRows(g, container, CedDisplayOption.AI_TB_TRACKS, aiTbTracks, screenAiTbTracks,
						CedDrawingStyle.AI_TIME_BASED);
			}
			drawXYAxes(g, container);
		} finally {
			g.dispose();
		}
	}

	private void drawPanels(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(1.4f));
		for (int sector = 1; sector <= URWTGeometry.SECTOR_COUNT; sector++) {
			for (int layer = 1; layer <= URWTGeometry.LAYER_COUNT; layer++) {
				g.setColor(LAYER_COLORS[layer - 1]);
				g.drawPolygon(panelPolygon(container, sector, layer));
			}
		}
	}

	private void drawAccumulatedPanels(Graphics2D g, IContainer container) {
		int max = accumulation.maximumCount();
		g.setStroke(new BasicStroke(1.4f));
		for (int sector = 1; sector <= URWTGeometry.SECTOR_COUNT; sector++) {
			for (int layer = 1; layer <= URWTGeometry.LAYER_COUNT; layer++) {
				// Occupancy is per-strip, not per-panel; show the panel's peak
				// strip occupancy as its outline color, a coarser but honest
				// summary given the panel itself is drawn once, not per-strip.
				int strips = geometry.detector(sector, layer).strips().size();
				int count = 0;
				for (int strip = 1; strip <= strips; strip++) {
					count = Math.max(count, accumulation.count(sector, layer, strip));
				}
				g.setColor(count == 0 || max == 0
						? new Color(230, 230, 230)
						: ScientificColorMap.TURBO.colorAt((double) count / max));
				g.drawPolygon(panelPolygon(container, sector, layer));
			}
		}
	}

	private Polygon panelPolygon(IContainer container, int sector, int layer) {
		Polygon polygon = new Polygon();
		for (Point3 vertex : panelOutlines.get(new PanelAddress(sector, layer))) {
			Point p = screen(container, vertex.x(), vertex.y());
			polygon.addPoint(p.x, p.y);
		}
		return polygon;
	}

	private void drawClusters(Graphics2D g, IContainer container) {
		g.setColor(CLUSTER_COLOR);
		g.setStroke(new BasicStroke(2.2f));
		for (Cluster cluster : eventData.clusters()) {
			Point a = screen(container, cluster.xo(), cluster.yo());
			Point b = screen(container, cluster.xe(), cluster.ye());
			g.drawLine(a.x, a.y, b.x, b.y);
			Point mid = new Point((a.x + b.x) / 2, (a.y + b.y) / 2);
			markers.put(cluster, mid);
		}
	}

	private void drawCrosses(Graphics2D g, IContainer container) {
		g.setColor(CROSS_COLOR);
		g.setStroke(new BasicStroke(2f));
		for (Cross cross : eventData.crosses()) {
			if (Float.isNaN(cross.x()) || Float.isNaN(cross.y())) continue;
			Point p = screen(container, cross.x(), cross.y());
			markers.put(cross, p);
			g.drawOval(p.x - 6, p.y - 6, 12, 12);
			g.drawLine(p.x - 8, p.y, p.x + 8, p.y);
			g.drawLine(p.x, p.y - 8, p.x, p.y + 8);
		}
	}

	/** REC::Particle, swum and drawn species-colored -- same pattern as CentralXYView's own drawParticles. */
	private void drawReconTracks(Graphics2D g, IContainer container) {
		for (RecEventData.Particle particle : recData.particles()) {
			List<Point3> swum = swimCache.trajectory(SwimmableParticle.of(particle), fieldProbe);
			if (swum.size() < 2) continue;
			Color color = CedDrawingStyle.particleColor(particle.pid(), particle.charge());
			Stroke stroke = CedDrawingStyle.particleStroke(particle.pid(), particle.charge());
			List<Point> points = new ArrayList<>(swum.size());
			for (Point3 p : swum) points.add(screen(container, p.x(), p.y()));
			drawTrajectory(g, points, color, stroke);
			screenReconTracks.add(new ScreenTrack(particle, points));
		}
	}

	/**
	 * MC/HB/TB/AI-HB/AI-TB tracks, swum and drawn in direct (unrotated) XY --
	 * this view has no sector-rotated projection the way SectorView does, so
	 * it's simpler than that view's own version of this method.
	 *
	 * @param colorOverride drawn color for every track in this group, or {@code null} to color each by its own species
	 */
	private void drawTrackRows(Graphics2D g, IContainer container, CedDisplayOption option,
			List<TrackRow> tracks, List<ScreenTrack> sink, Color colorOverride) {
		if (!isDisplayed(option)) return;
		for (TrackRow track : tracks) {
			List<Point3> swum = swimCache.trajectory(SwimmableParticle.of(track), fieldProbe);
			if (swum.size() < 2) continue;
			Color color = colorOverride != null ? colorOverride
					: CedDrawingStyle.particleColor(track.pid(), track.charge());
			Stroke stroke = CedDrawingStyle.particleStroke(track.pid(), track.charge());
			List<Point> points = new ArrayList<>(swum.size());
			for (Point3 p : swum) points.add(screen(container, p.x(), p.y()));
			drawTrajectory(g, points, color, stroke);
			sink.add(new ScreenTrack(track, points));
		}
	}

	private static void drawTrajectory(Graphics2D g, List<Point> points, Color color, Stroke stroke) {
		g.setColor(color);
		g.setStroke(stroke);
		for (int i = 1; i < points.size(); i++) {
			Point a = points.get(i - 1), b = points.get(i);
			g.drawLine(a.x, a.y, b.x, b.y);
		}
		Point vertex = points.get(0);
		g.fillOval(vertex.x - 3, vertex.y - 3, 6, 6);
		g.setColor(CedDrawingStyle.outline(color));
		g.setStroke(new BasicStroke(1f));
		g.drawOval(vertex.x - 3, vertex.y - 3, 6, 6);
	}

	private static Point screen(IContainer container, double x, double y) {
		Point point = new Point();
		container.worldToLocal(point, x, y);
		return point;
	}

	private record PanelAddress(int sector, int layer) { }
	private record ScreenTrack(Object track, List<Point> points) { }

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint, Point2D.Double worldPoint,
			List<String> feedback) {
		super.getFeedbackStrings(container, screenPoint, worldPoint, feedback);
		addXYFeedback(worldPoint, "cm", feedback);
		for (int sector = 1; sector <= URWTGeometry.SECTOR_COUNT; sector++) {
			for (int layer = 1; layer <= URWTGeometry.LAYER_COUNT; layer++) {
				if (panelPolygon(container, sector, layer).contains(screenPoint)) {
					feedback.add(String.format("$wheat$μrWT sector %d layer %d", sector, layer));
					break;
				}
			}
		}
		for (Map.Entry<Object, Point> entry : markers.entrySet()) {
			if (entry.getValue().distance(screenPoint) <= 9) {
				addMarkerFeedback(entry.getKey(), feedback);
				break;
			}
		}
		addTrackFeedback(screenReconTracks, screenPoint, "REC", "deep sky blue", feedback);
		addTrackFeedback(screenMcTracks, screenPoint, "MC", "orange red", feedback);
		addTrackFeedback(screenHbTracks, screenPoint, "HB", "yellow", feedback);
		addTrackFeedback(screenTbTracks, screenPoint, "TB", "dark orange", feedback);
		addTrackFeedback(screenAiHbTracks, screenPoint, "AI HB", "spring green", feedback);
		addTrackFeedback(screenAiTbTracks, screenPoint, "AI TB", "magenta", feedback);
	}

	private static void addTrackFeedback(List<ScreenTrack> tracks, Point screenPoint, String label, String color,
			List<String> feedback) {
		for (ScreenTrack drawn : tracks) {
			if (!nearAnySegment(drawn.points(), screenPoint, 5.0)) continue;
			if (drawn.track() instanceof RecEventData.Particle particle) {
				feedback.add(String.format("$%s$%s %s (pid %d, q=%+d)", color, label, particle.displayName(),
						particle.pid(), particle.charge()));
			} else if (drawn.track() instanceof TrackRow track) {
				feedback.add(String.format("$%s$%s %s (pid %d, q=%+d)", color, label, track.name(),
						track.pid(), track.charge()));
			}
			break;
		}
	}

	private static boolean nearAnySegment(List<Point> points, Point screenPoint, double tolerance) {
		for (int i = 1; i < points.size(); i++) {
			Point a = points.get(i - 1), b = points.get(i);
			if (java.awt.geom.Line2D.ptSegDist(a.x, a.y, b.x, b.y, screenPoint.x, screenPoint.y) <= tolerance) {
				return true;
			}
		}
		return false;
	}

	private static void addMarkerFeedback(Object marker, List<String> feedback) {
		if (marker instanceof Cluster cluster) {
			feedback.add(String.format("$magenta$μrWT cluster sector %d layer %d size %d",
					cluster.sector(), cluster.layer(), cluster.size()));
			feedback.add(String.format("$magenta$energy %.4f eV  time %.3f ns", cluster.energy(), cluster.time()));
		} else if (marker instanceof Cross cross) {
			feedback.add(String.format("$green$μrWT cross sector %d region %d xyz (%.3f, %.3f, %.3f) cm",
					cross.sector(), cross.region(), cross.x(), cross.y(), cross.z()));
		}
	}

	@Override
	public void magneticFieldChanged() {
		fieldProbe = FieldProbe.factory();
		refresh();
	}

	@Override
	public void dispose() {
		MagneticFields.getInstance().removeMagneticFieldChangeListener(this);
		super.dispose();
	}
}
