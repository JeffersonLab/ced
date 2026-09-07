package edu.cnu.ced.view.dc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Line2D;
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
import edu.cnu.ced.data.DCAccumulation;
import edu.cnu.ced.data.DCEventData;
import edu.cnu.ced.data.DCEventData.Cross;
import edu.cnu.ced.data.DCEventData.RawHit;
import edu.cnu.ced.data.DCEventData.ReconHit;
import edu.cnu.ced.data.DCEventData.ReconKind;
import edu.cnu.ced.data.MonteCarloTracks;
import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.data.ReconstructedTracks;
import edu.cnu.ced.data.TrackRow;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.DCGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.geometry.Segment3;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.swim.SwimmableParticle;
import edu.cnu.ced.view.CedXYView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Forward drift-chamber laboratory XY (transverse, looking downstream)
 * display backed directly by CLAS banks -- the true-geometry counterpart to
 * the schematic {@link DCHexView}/{@link AllDCView}, matching legacy CED's
 * own {@code DCXYView}.
 * <p>
 * Each (sector, superlayer)'s six layers are collapsed into one outline
 * (convex hull of every layer's wire-1 and wire-112 endpoints -- both ends
 * of each, 24 points per superlayer), the same technique legacy's own
 * {@code DCXYSectorItem} uses (it traces the same 24 points by hand; a hull
 * of them is the same shape, matching {@link edu.cnu.ced.view.urwt.URWTXYView}'s
 * identical convex-hull-panel-outline pattern here). Individual hits and
 * recon hits are drawn as the real wire's own full line (not a cell
 * polygon, since this is a true-geometry view, not a schematic one like
 * {@code DCHexView}) -- legacy's own choice too. Segments are not drawn:
 * {@link DCEventData.Segment}'s (x, z) pair is expressed in each sector's
 * own tilted local frame (see {@link edu.cnu.ced.view.sector.SectorView}'s
 * own R-Z projection, which already handles that transform); crosses,
 * unlike segments, already carry real lab-frame (x, y, z) and need no such
 * transform, so they -- not segments -- are this view's cross-check overlay,
 * matching legacy's own DCXYView (which draws crosses, never segments).
 */
@SuppressWarnings("serial")
public final class DCXYView extends CedXYView implements MagneticFieldChangeListener {

	private static final Color[] SUPERLAYER_COLORS = {
			new Color(220, 20, 60), new Color(255, 140, 0), new Color(184, 134, 11),
			new Color(34, 139, 34), new Color(30, 144, 255), new Color(148, 0, 211)
	};
	private static final Color FRAMEWORK_COLOR = new Color(190, 195, 200);
	private static final Color SECTOR_LABEL = new Color(60, 60, 60, 90);

	private final DCGeometry geometry;
	private final DCAccumulation accumulation;
	private final SwimTrajectoryCache swimCache;
	// Convex-hull outline of each (sector, superlayer) -- world-space, computed
	// once at construction (geometry never changes); see class doc.
	private final Map<PanelAddress, List<Point3>> panelOutlines = new HashMap<>();
	private final Map<PanelAddress, Polygon> panelPolygons = new HashMap<>();
	// Screen-space endpoints for every drawn wire (hit or accumulated), cached
	// from the draw pass so getFeedbackStrings doesn't redo geometry lookups
	// and world-to-local transforms on every mouse move -- same reasoning as
	// AlertXYView's identical wireScreenPoints cache.
	private final Map<WireAddress, Point[]> wireScreenPoints = new HashMap<>();
	private final Map<Object, Point> markers = new HashMap<>();
	private final List<ScreenTrack> screenReconTracks = new ArrayList<>();
	private final List<ScreenTrack> screenMcTracks = new ArrayList<>();
	private final List<ScreenTrack> screenHbTracks = new ArrayList<>();
	private final List<ScreenTrack> screenTbTracks = new ArrayList<>();
	private final List<ScreenTrack> screenAiHbTracks = new ArrayList<>();
	private final List<ScreenTrack> screenAiTbTracks = new ArrayList<>();
	private volatile DCEventData eventData = DCEventData.from(null);
	private volatile RecEventData recData = RecEventData.from(null);
	private volatile List<TrackRow> mcTracks = List.of();
	private volatile List<TrackRow> hbTracks = List.of();
	private volatile List<TrackRow> tbTracks = List.of();
	private volatile List<TrackRow> aiHbTracks = List.of();
	private volatile List<TrackRow> aiTbTracks = List.of();
	private volatile FieldProbe fieldProbe = FieldProbe.factory();

	public DCXYView(DCGeometry geometry, EventNavigator navigator, DCAccumulation accumulation,
			SwimTrajectoryCache swimCache) {
		super(navigator, PropertyUtils.TITLE, "DC XY",
				PropertyUtils.WIDTH, 820, PropertyUtils.HEIGHT, 820,
				// Same mirroring convention as every other XY view here
				// (CentralXYView, URWTXYView, ...): positive x0 with negative
				// width mirrors +x to the left of the screen, matching
				// CLAS12's "looking downstream" display. DC's outer
				// superlayer reaches ~413 cm (confirmed empirically against
				// DCGeometry.absoluteMaxWireX()); 490 cm leaves comfortable
				// room for the sector labels drawn just beyond it (see
				// outerLabelPosition), matching legacy's own generous margin.
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(490, -490, -980, 980),
				PropertyUtils.BACKGROUND, Color.WHITE,
				PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.accumulation = accumulation;
		this.swimCache = swimCache;
		buildPanelOutlines();
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT, CedDisplayOption.ACCUMULATION,
				CedDisplayOption.RAW_DATA, CedDisplayOption.HB_HITS, CedDisplayOption.TB_HITS,
				CedDisplayOption.AI_HB_HITS, CedDisplayOption.AI_TB_HITS, CedDisplayOption.CROSSES,
				CedDisplayOption.RECON_TRACKS, CedDisplayOption.MC_TRACKS,
				CedDisplayOption.HB_TRACKS, CedDisplayOption.TB_TRACKS,
				CedDisplayOption.AI_HB_TRACKS, CedDisplayOption.AI_TB_TRACKS),
				List.of("DC::"), ScientificColorMap.TURBO, "Relative occupancy / accumulation");
		MagneticFields.getInstance().addMagneticFieldChangeListener(this);
	}

	private void buildPanelOutlines() {
		for (int sector = 1; sector <= DCGeometry.SECTOR_COUNT; sector++) {
			for (int superlayer = 1; superlayer <= DCGeometry.SUPERLAYER_COUNT; superlayer++) {
				List<Point3> corners = new ArrayList<>(2 * DCGeometry.LAYER_COUNT);
				for (int layer = 1; layer <= DCGeometry.LAYER_COUNT; layer++) {
					addEndpoints(corners, sector, superlayer, layer, 1);
					addEndpoints(corners, sector, superlayer, layer, DCGeometry.WIRE_COUNT);
				}
				panelOutlines.put(new PanelAddress(sector, superlayer), convexHull(corners));
			}
		}
	}

	private void addEndpoints(List<Point3> corners, int sector, int superlayer, int layer, int wire) {
		Segment3 line = geometry.wireLine(sector, superlayer, layer, wire);
		corners.add(new Point3(line.start().x(), line.start().y(), 0));
		corners.add(new Point3(line.end().x(), line.end().y(), 0));
	}

	/** Andrew's monotone chain -- exact for a genuinely convex panel, which a superlayer's own outline is. */
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
		eventData = DCEventData.from(state.snapshot());
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
			panelPolygons.clear();
			wireScreenPoints.clear();
			markers.clear();
			screenReconTracks.clear();
			screenMcTracks.clear();
			screenHbTracks.clear();
			screenTbTracks.clear();
			screenAiHbTracks.clear();
			screenAiTbTracks.clear();
			drawFramework(g, container);
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
				drawAccumulatedWires(g, container);
			} else {
				if (isDisplayed(CedDisplayOption.RAW_DATA)) drawRawWires(g, container);
				drawReconWires(g, container);
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

	/** The six superlayer outlines per sector -- always visible, purely for orientation (no data meaning), like every other detector's "panel" outline here. */
	private void drawFramework(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(1f));
		g.setColor(FRAMEWORK_COLOR);
		for (int sector = 1; sector <= DCGeometry.SECTOR_COUNT; sector++) {
			for (int superlayer = 1; superlayer <= DCGeometry.SUPERLAYER_COUNT; superlayer++) {
				Polygon polygon = panelPolygon(container, sector, superlayer);
				panelPolygons.put(new PanelAddress(sector, superlayer), polygon);
				g.drawPolygon(polygon);
			}
		}
		drawSectorLabels(g, container);
	}

	private Polygon panelPolygon(IContainer container, int sector, int superlayer) {
		Polygon polygon = new Polygon();
		for (Point3 vertex : panelOutlines.get(new PanelAddress(sector, superlayer))) {
			Point p = screen(container, vertex.x(), vertex.y());
			polygon.addPoint(p.x, p.y);
		}
		return polygon;
	}

	/**
	 * One big, semi-transparent sector number just beyond the outermost
	 * superlayer -- matching legacy's own placement (outside the chamber,
	 * not centered within it, unlike URWTXYView's own sector labels).
	 * Reuses the all-superlayer centroid purely for its angle (a reliable
	 * stand-in for "this sector's own bisecting angle" without having to
	 * derive that analytically), then pushes out to the sector's own
	 * outermost (superlayer 6) vertex radius plus a fixed margin.
	 */
	private void drawSectorLabels(Graphics2D g, IContainer container) {
		Font old = g.getFont();
		g.setFont(old.deriveFont(Font.BOLD, Math.max(26f, old.getSize2D() * 2.4f)));
		g.setColor(SECTOR_LABEL);
		for (int sector = 1; sector <= DCGeometry.SECTOR_COUNT; sector++) {
			Point3 label = outerLabelPosition(sector);
			if (label == null) continue;
			Point p = screen(container, label.x(), label.y());
			String text = Integer.toString(sector);
			g.drawString(text, p.x - g.getFontMetrics().stringWidth(text) / 2, p.y + 9);
		}
		g.setFont(old);
	}

	private Point3 outerLabelPosition(int sector) {
		Point3 centroid = sectorCentroid(sector);
		if (centroid == null) return null;
		double angle = Math.atan2(centroid.y(), centroid.x());
		double radius = outermostRadius(sector) + 25;
		return new Point3(radius * Math.cos(angle), radius * Math.sin(angle), 0);
	}

	/** The farthest-from-origin vertex of a sector's own outermost (superlayer 6) outline. */
	private double outermostRadius(int sector) {
		double max = 0;
		for (Point3 vertex : panelOutlines.get(new PanelAddress(sector, DCGeometry.SUPERLAYER_COUNT))) {
			max = Math.max(max, Math.hypot(vertex.x(), vertex.y()));
		}
		return max;
	}

	/** The centroid of every hull vertex across all six superlayers of one sector -- a reliable proxy for that sector's own bisecting angle. */
	private Point3 sectorCentroid(int sector) {
		double sumX = 0, sumY = 0;
		int count = 0;
		for (int superlayer = 1; superlayer <= DCGeometry.SUPERLAYER_COUNT; superlayer++) {
			for (Point3 vertex : panelOutlines.get(new PanelAddress(sector, superlayer))) {
				sumX += vertex.x();
				sumY += vertex.y();
				count++;
			}
		}
		return count == 0 ? null : new Point3(sumX / count, sumY / count, 0);
	}

	/** Every raw hit's own wire, drawn as its real full line -- legacy's own choice, not a schematic cell. */
	private void drawRawWires(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(2f));
		for (RawHit hit : eventData.rawHits()) {
			drawWireHit(g, container, hit.sector(), hit.superlayer(), hit.layer(), hit.wire(),
					CedDrawingStyle.RAW_HIT);
		}
	}

	private void drawReconWires(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(2f));
		for (ReconHit hit : eventData.reconHits()) {
			if (!isDisplayed(reconOption(hit.kind()))) continue;
			drawWireHit(g, container, hit.sector(), hit.superlayer(), hit.layer(), hit.wire(),
					CedDrawingStyle.reconstructionColor(hit.kind()));
		}
	}

	private void drawAccumulatedWires(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(2f));
		for (int superlayer = 1; superlayer <= DCGeometry.SUPERLAYER_COUNT; superlayer++) {
			int ceiling = accumulation.percentileCount(superlayer, 0.95);
			if (ceiling == 0) continue;
			for (int sector = 1; sector <= DCGeometry.SECTOR_COUNT; sector++) {
				for (int layer = 1; layer <= DCGeometry.LAYER_COUNT; layer++) {
					for (int wire = 1; wire <= DCGeometry.WIRE_COUNT; wire++) {
						int count = accumulation.count(sector, superlayer, layer, wire);
						if (count == 0) continue;
						Color color = ScientificColorMap.TURBO.colorAt(Math.min(1.0, (double) count / ceiling));
						drawWireHit(g, container, sector, superlayer, layer, wire, color);
					}
				}
			}
		}
	}

	private void drawWireHit(Graphics2D g, IContainer container, int sector, int superlayer, int layer, int wire,
			Color color) {
		Segment3 line = geometry.wireLine(sector, superlayer, layer, wire);
		Point a = screen(container, line.start().x(), line.start().y());
		Point b = screen(container, line.end().x(), line.end().y());
		wireScreenPoints.put(new WireAddress(sector, superlayer, layer, wire), new Point[] { a, b });
		g.setColor(color);
		g.drawLine(a.x, a.y, b.x, b.y);
	}

	private void drawCrosses(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(2f));
		for (Cross cross : eventData.crosses()) {
			Color color = CedDrawingStyle.reconstructionColor(cross.kind());
			g.setColor(color);
			Point p = screen(container, cross.x(), cross.y());
			markers.put(cross, p);
			g.drawOval(p.x - 6, p.y - 6, 12, 12);
			g.drawLine(p.x - 8, p.y, p.x + 8, p.y);
			g.drawLine(p.x, p.y - 8, p.x, p.y + 8);
		}
	}

	private static CedDisplayOption reconOption(ReconKind kind) {
		return switch (kind) {
			case HB -> CedDisplayOption.HB_HITS;
			case TB -> CedDisplayOption.TB_HITS;
			case AI_HB -> CedDisplayOption.AI_HB_HITS;
			case AI_TB -> CedDisplayOption.AI_TB_HITS;
		};
	}

	/** REC::Particle, swum and drawn species-colored -- same pattern as CentralXYView's/URWTXYView's own drawReconTracks. */
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

	private record PanelAddress(int sector, int superlayer) { }
	private record WireAddress(int sector, int superlayer, int layer, int wire) { }
	private record ScreenTrack(Object track, List<Point> points) { }

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint, Point2D.Double worldPoint,
			List<String> feedback) {
		super.getFeedbackStrings(container, screenPoint, worldPoint, feedback);
		addXYFeedback(worldPoint, "cm", feedback);
		WireAddress closest = null;
		double best = 6.0;
		for (Map.Entry<WireAddress, Point[]> entry : wireScreenPoints.entrySet()) {
			Point[] points = entry.getValue();
			double distance = Line2D.ptSegDist(points[0].x, points[0].y, points[1].x, points[1].y,
					screenPoint.x, screenPoint.y);
			if (distance < best) {
				best = distance;
				closest = entry.getKey();
			}
		}
		boolean found = false;
		if (closest != null) {
			addWireFeedback(closest, feedback);
			found = true;
		}
		if (!found) {
			for (Map.Entry<Object, Point> entry : markers.entrySet()) {
				if (entry.getValue().distance(screenPoint) <= 9) {
					addMarkerFeedback(entry.getKey(), feedback);
					found = true;
					break;
				}
			}
		}
		if (!found) {
			for (Map.Entry<PanelAddress, Polygon> entry : panelPolygons.entrySet()) {
				if (entry.getValue().contains(screenPoint)) {
					PanelAddress address = entry.getKey();
					feedback.add(String.format("$wheat$DC sector %d superlayer %d",
							address.sector(), address.superlayer()));
					break;
				}
			}
		}
		addTrackFeedback(screenReconTracks, screenPoint, "REC", "deep sky blue", feedback);
		addTrackFeedback(screenMcTracks, screenPoint, "MC", "orange red", feedback);
		addTrackFeedback(screenHbTracks, screenPoint, "HB", "yellow", feedback);
		addTrackFeedback(screenTbTracks, screenPoint, "TB", "dark orange", feedback);
		addTrackFeedback(screenAiHbTracks, screenPoint, "AI HB", "spring green", feedback);
		addTrackFeedback(screenAiTbTracks, screenPoint, "AI TB", "magenta", feedback);
	}

	private void addWireFeedback(WireAddress address, List<String> feedback) {
		feedback.add(String.format("$wheat$DC sector %d superlayer %d layer %d wire %d",
				address.sector(), address.superlayer(), address.layer(), address.wire()));
		if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
			feedback.add(String.format("$wheat$occupancy %d / %d events",
					accumulation.count(address.sector(), address.superlayer(), address.layer(), address.wire()),
					accumulation.eventCount()));
			return;
		}
		for (RawHit hit : eventData.rawHits()) {
			if (matches(address, hit)) {
				feedback.add(String.format("$red$raw hit tdc %d order %d", hit.tdc(), hit.order()));
			}
		}
		for (ReconHit hit : eventData.reconHits()) {
			if (isDisplayed(reconOption(hit.kind())) && matches(address, hit)) {
				feedback.add(String.format("$orange$%s hit cluster %d status %d trkDOCA %.3f cm",
						hit.kind(), hit.clusterId(), hit.status(), hit.trackDoca()));
			}
		}
	}

	private static boolean matches(WireAddress address, RawHit hit) {
		return address.sector() == hit.sector() && address.superlayer() == hit.superlayer()
				&& address.layer() == hit.layer() && address.wire() == hit.wire();
	}

	private static boolean matches(WireAddress address, ReconHit hit) {
		return address.sector() == hit.sector() && address.superlayer() == hit.superlayer()
				&& address.layer() == hit.layer() && address.wire() == hit.wire();
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
			if (Line2D.ptSegDist(a.x, a.y, b.x, b.y, screenPoint.x, screenPoint.y) <= tolerance) {
				return true;
			}
		}
		return false;
	}

	private static void addMarkerFeedback(Object marker, List<String> feedback) {
		if (marker instanceof Cross cross) {
			String color = switch (cross.kind()) {
				case HB -> "yellow";
				case TB -> "dark orange";
				case AI_HB -> "spring green";
				case AI_TB -> "magenta";
			};
			feedback.add(String.format("$%s$%s cross sector %d region %d xyz (%.3f, %.3f, %.3f) cm",
					color, cross.kind(), cross.sector(), cross.region(), cross.x(), cross.y(), cross.z()));
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
