package edu.cnu.ced.view.alert;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.AlertAccumulation;
import edu.cnu.ced.data.AlertEventData;
import edu.cnu.ced.data.AlertEventData.DcAdcHit;
import edu.cnu.ced.data.AlertEventData.DcCluster;
import edu.cnu.ced.data.AlertEventData.DcHit;
import edu.cnu.ced.data.AlertEventData.TofCluster;
import edu.cnu.ced.data.AlertEventData.TofHit;
import edu.cnu.ced.data.CentralAccumulation;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.AlertGeometry;
import edu.cnu.ced.geometry.AlertGeometry.Paddle;
import edu.cnu.ced.geometry.CNDGeometry;
import edu.cnu.ced.geometry.CTOFGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.geometry.Segment3;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.view.central.CndCtofXYView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * ALERT (AHDC drift chamber + ATOF time-of-flight) laboratory XY display
 * backed directly by CLAS banks -- {@link CndCtofXYView} (CND + CTOF, shared
 * with {@link edu.cnu.ced.view.central.CentralXYView}) plus AHDC + ATOF, the
 * same default world system as Central XY, and the same
 * RECON_TRACKS/MC_TRACKS swim-and-draw pattern (but not CVT_TRACKS, which
 * stays Central-Detector-specific -- ALERT has no CVT detector; legacy's own
 * ALERT view keeping literal "CVTRec Trajectory" labels is an artifact of
 * copy-pasting CentralXYView's control panel wholesale, not something to
 * reproduce).
 * <p>
 * AHDC has one real sector (its wires are drawn colored by superlayer, 0-4).
 * ATOF has 15 real sectors; each (sector, superlayer, layer) triple's paddle
 * tiles together with its neighbors to fill out one sector's wedge (matching
 * CND/PCAL's own many-small-adjacent-polygon convention), confirmed
 * empirically: a "bar" (superlayer 0) layer's 4 components sit at the same
 * radius, 6 degrees apart in phi, while a "wedge" (superlayer 1) layer's own
 * 10 sub-components differ only in z (irrelevant for this transverse view,
 * so only the first is drawn). See {@link AlertEventData}'s class doc for
 * why ATOF hits/clusters are plotted at their own bank-given position rather
 * than resolved through this same geometry, and why accumulation only
 * covers AHDC wires.
 * </p>
 */
@SuppressWarnings("serial")
public final class AlertXYView extends CndCtofXYView {

	private static final Color[] SUPERLAYER_COLORS = {
			new Color(220, 20, 60), new Color(255, 140, 0), new Color(184, 134, 11),
			new Color(34, 139, 34), new Color(30, 144, 255)
	};
	private static final Color TOF_FILL = new Color(225, 225, 235);
	private static final Color TOF_OUTLINE = new Color(120, 120, 140);
	private static final Color TOF_LABEL = new Color(40, 75, 145, 170);
	private static final Color RECON_COLOR = new Color(225, 35, 25);
	private static final Color CLUSTER_COLOR = new Color(205, 0, 205);
	private static final int MARKER = 4;

	private final AlertGeometry geometry;
	private final AlertAccumulation accumulation;
	// Screen-space endpoints for every drawn AHDC wire, cached from the draw
	// pass so getFeedbackStrings doesn't redo geometry lookups and
	// world-to-local transforms on every mouse move -- same reasoning as
	// FMTXYView/URWTXYView's identical caches.
	private final Map<WireAddress, Point[]> wireScreenPoints = new HashMap<>();
	private final Map<TofAddress, Polygon> tofPolygons = new HashMap<>();
	private volatile AlertEventData eventData = AlertEventData.from(null);

	public AlertXYView(AlertGeometry geometry, CNDGeometry cnd, CTOFGeometry ctof, EventNavigator navigator,
			AlertAccumulation accumulation, CentralAccumulation centralAccumulation, SwimTrajectoryCache swimCache) {
		super(cnd, ctof, navigator, centralAccumulation, swimCache,
				PropertyUtils.TITLE, "ALERT XY",
				PropertyUtils.WIDTH, 860, PropertyUtils.HEIGHT, 760,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(40, -40, -80, 80),
				PropertyUtils.BACKGROUND, Color.WHITE,
				PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.accumulation = accumulation;
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT, CedDisplayOption.ACCUMULATION,
				CedDisplayOption.RAW_DATA, CedDisplayOption.RECON_HITS, CedDisplayOption.CLUSTERS,
				CedDisplayOption.CROSSES, CedDisplayOption.CONNECT_CLUSTER_ENDPOINTS,
				CedDisplayOption.RECON_TRACKS, CedDisplayOption.MC_TRACKS),
				List.of("AHDC::", "ATOF::", "CND", "CTOF"), ScientificColorMap.TURBO,
				"Relative ADC / accumulation");
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		super.eventChanged(state);
		eventData = AlertEventData.from(state.snapshot());
	}

	private void draw(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			clearSharedDrawState();
			wireScreenPoints.clear();
			tofPolygons.clear();
			drawCTOF(g, container);
			drawCND(g, container);
			drawTofPanels(g, container);
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
				drawAccumulatedWires(g, container);
			} else {
				drawWires(g, container);
				if (isDisplayed(CedDisplayOption.RAW_DATA)) drawDcAdcHits(g, container);
				if (isDisplayed(CedDisplayOption.RECON_HITS)) {
					drawDcHits(g, container);
					drawTofHits(g, container);
				}
				if (isDisplayed(CedDisplayOption.CLUSTERS)) {
					drawClusters(g, container);
					drawDcClusters(g, container);
					drawTofClusters(g, container);
				}
				if (isDisplayed(CedDisplayOption.CROSSES)) drawCrosses(g, container);
				if (isDisplayed(CedDisplayOption.RECON_TRACKS)) drawParticles(g, container);
				if (isDisplayed(CedDisplayOption.MC_TRACKS)) drawMcTracks(g, container);
			}
			drawXYAxes(g, container);
		} finally {
			g.dispose();
		}
	}

	/**
	 * ATOF's 15 sectors x 2 superlayers x 4 layers, each an adjacent tiled
	 * polygon (matching CND/PCAL's own many-small-polygon convention) rather
	 * than a centroid dot -- one number label per sector, at that sector's
	 * middle bar layer.
	 */
	private void drawTofPanels(Graphics2D g, IContainer container) {
		for (int sector = 0; sector < 15; sector++) {
			for (int superlayer = 0; superlayer < 2; superlayer++) {
				for (int layer = 0; layer < 4; layer++) {
					List<Paddle> paddles = geometry.tofPaddles(sector, superlayer, layer);
					if (paddles.isEmpty()) continue;
					// A "wedge" (superlayer 1) layer's own sub-components differ
					// only in z, irrelevant for this transverse view -- only the
					// first is needed.
					Polygon polygon = tofPolygon(container, paddles.get(0));
					tofPolygons.put(new TofAddress(sector, superlayer, layer), polygon);
					g.setColor(TOF_FILL);
					g.fillPolygon(polygon);
					g.setColor(TOF_OUTLINE);
					g.drawPolygon(polygon);
				}
			}
			drawTofSectorLabel(g, container, sector);
		}
	}

	private void drawTofSectorLabel(Graphics2D g, IContainer container, int sector) {
		List<Paddle> paddles = geometry.tofPaddles(sector, 0, 1);
		if (paddles.isEmpty()) return;
		Point3 centroid = centroid(paddles.get(0).vertices());
		Point p = screen(container, centroid.x(), centroid.y());
		Font old = g.getFont();
		g.setFont(old.deriveFont(Font.BOLD, Math.max(10f, old.getSize2D())));
		g.setColor(TOF_LABEL);
		String label = Integer.toString(sector + 1);
		g.drawString(label, p.x - g.getFontMetrics().stringWidth(label) / 2, p.y + 4);
		g.setFont(old);
	}

	/** The paddle's front face (vertices 0-3): confirmed identical in (x, y) to the back face (4-7), so this is exact for an XY projection. */
	private Polygon tofPolygon(IContainer container, Paddle paddle) {
		Polygon polygon = new Polygon();
		List<Point3> vertices = paddle.vertices();
		for (int i = 0; i < 4; i++) {
			Point p = screen(container, vertices.get(i).x(), vertices.get(i).y());
			polygon.addPoint(p.x, p.y);
		}
		return polygon;
	}

	private static Point3 centroid(List<Point3> vertices) {
		double x = 0, y = 0, z = 0;
		for (Point3 vertex : vertices) {
			x += vertex.x(); y += vertex.y(); z += vertex.z();
		}
		int n = vertices.size();
		return new Point3(x / n, y / n, z / n);
	}

	private void drawWires(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(1f));
		for (int superlayer = 0; superlayer < 5; superlayer++) {
			g.setColor(SUPERLAYER_COLORS[superlayer]);
			for (int layer = 0; layer < 2; layer++) {
				List<Segment3> wires = geometry.dcWires(0, superlayer, layer);
				for (int wire = 0; wire < wires.size(); wire++) {
					drawWireLine(g, container, superlayer, layer, wire, wires.get(wire));
				}
			}
		}
	}

	private void drawAccumulatedWires(Graphics2D g, IContainer container) {
		int max = accumulation.maximumCount();
		g.setStroke(new BasicStroke(1f));
		for (int superlayer = 0; superlayer < 5; superlayer++) {
			for (int layer = 0; layer < 2; layer++) {
				List<Segment3> wires = geometry.dcWires(0, superlayer, layer);
				for (int wire = 0; wire < wires.size(); wire++) {
					int count = accumulation.count(superlayer, layer, wire);
					g.setColor(count == 0 || max == 0
							? new Color(230, 230, 230)
							: ScientificColorMap.TURBO.colorAt((double) count / max));
					drawWireLine(g, container, superlayer, layer, wire, wires.get(wire));
				}
			}
		}
	}

	private void drawWireLine(Graphics2D g, IContainer container, int superlayer, int layer, int wire,
			Segment3 line) {
		Point a = screen(container, line.start().x(), line.start().y());
		Point b = screen(container, line.end().x(), line.end().y());
		wireScreenPoints.put(new WireAddress(superlayer, layer, wire), new Point[] { a, b });
		g.drawLine(a.x, a.y, b.x, b.y);
	}

	private void drawDcAdcHits(Graphics2D g, IContainer container) {
		int max = eventData.maximumDcAdc();
		if (max == 0) return;
		g.setStroke(new BasicStroke(3f));
		for (DcAdcHit hit : eventData.dcAdcHits()) {
			List<Segment3> wires = geometry.dcWires(hit.sector(), hit.superlayer(), hit.layer());
			if (hit.wire() < 0 || hit.wire() >= wires.size()) continue;
			Segment3 line = wires.get(hit.wire());
			Point a = screen(container, line.start().x(), line.start().y());
			Point b = screen(container, line.end().x(), line.end().y());
			g.setColor(ScientificColorMap.TURBO.colorAt((double) hit.adc() / max));
			g.drawLine(a.x, a.y, b.x, b.y);
		}
	}

	private void drawDcHits(Graphics2D g, IContainer container) {
		g.setColor(RECON_COLOR);
		for (DcHit hit : eventData.dcHits()) {
			List<Segment3> wires = geometry.dcWires(0, hit.superlayer(), hit.layer());
			if (hit.wire() < 0 || hit.wire() >= wires.size()) continue;
			Segment3 line = wires.get(hit.wire());
			Point p = screen(container, (line.start().x() + line.end().x()) / 2,
					(line.start().y() + line.end().y()) / 2);
			markers.put(hit, p);
			g.fillOval(p.x - MARKER, p.y - MARKER, 2 * MARKER, 2 * MARKER);
		}
	}

	private void drawTofHits(Graphics2D g, IContainer container) {
		g.setColor(RECON_COLOR.darker());
		for (TofHit hit : eventData.tofHits()) {
			Point p = screen(container, hit.x(), hit.y());
			markers.put(hit, p);
			g.fillRect(p.x - MARKER, p.y - MARKER, 2 * MARKER, 2 * MARKER);
		}
	}

	private void drawDcClusters(Graphics2D g, IContainer container) {
		g.setColor(CLUSTER_COLOR);
		g.setStroke(new BasicStroke(1.6f));
		for (DcCluster cluster : eventData.dcClusters()) {
			Point p = screen(container, cluster.x(), cluster.y());
			markers.put(cluster, p);
			g.drawLine(p.x - 7, p.y, p.x + 7, p.y);
			g.drawLine(p.x, p.y - 7, p.x, p.y + 7);
		}
	}

	private void drawTofClusters(Graphics2D g, IContainer container) {
		g.setColor(CLUSTER_COLOR.darker());
		g.setStroke(new BasicStroke(1.6f));
		for (TofCluster cluster : eventData.tofClusters()) {
			Point p = screen(container, cluster.x(), cluster.y());
			markers.put(cluster, p);
			g.drawOval(p.x - 6, p.y - 6, 12, 12);
		}
	}

	private record WireAddress(int superlayer, int layer, int wire) { }
	private record TofAddress(int sector, int superlayer, int layer) { }

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint, Point2D.Double worldPoint,
			List<String> feedback) {
		super.getFeedbackStrings(container, screenPoint, worldPoint, feedback);
		addXYFeedback(worldPoint, "cm", feedback);
		boolean found = addBarrelPolygonFeedback(screenPoint, feedback);
		if (!found) {
			for (Map.Entry<TofAddress, Polygon> entry : tofPolygons.entrySet()) {
				if (entry.getValue().contains(screenPoint)) {
					TofAddress address = entry.getKey();
					feedback.add(String.format("$wheat$ATOF sector %d superlayer %d layer %d",
							address.sector() + 1, address.superlayer(), address.layer()));
					found = true;
					break;
				}
			}
		}
		if (!found) {
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
			if (closest != null) {
				feedback.add(String.format("$wheat$AHDC superlayer %d layer %d wire %d",
						closest.superlayer(), closest.layer(), closest.wire()));
			}
		}
		addMarkersAndTrackFeedback(screenPoint, feedback);
		for (Map.Entry<Object, Point> entry : markers.entrySet()) {
			if (entry.getValue().distance(screenPoint) <= 9 && addMarkerFeedback(entry.getKey(), feedback)) break;
		}
	}

	private static boolean addMarkerFeedback(Object marker, List<String> feedback) {
		if (marker instanceof DcHit hit) {
			feedback.add(String.format("$deep sky blue$AHDC hit superlayer %d layer %d wire %d",
					hit.superlayer(), hit.layer(), hit.wire()));
			feedback.add(String.format("$deep sky blue$time %.3f ns  doca %.3f cm", hit.time(), hit.doca()));
			return true;
		} else if (marker instanceof TofHit hit) {
			feedback.add(String.format("$deep sky blue$ATOF hit sector %d layer %d component %d",
					hit.sector(), hit.layer(), hit.component()));
			feedback.add(String.format("$deep sky blue$energy %.3f MeV  time %.3f ns", hit.energy(), hit.time()));
			return true;
		} else if (marker instanceof DcCluster cluster) {
			feedback.add(String.format("$magenta$AHDC cluster xyz (%.3f, %.3f, %.3f) cm",
					cluster.x(), cluster.y(), cluster.z()));
			return true;
		} else if (marker instanceof TofCluster cluster) {
			feedback.add(String.format("$magenta$ATOF cluster xyz (%.3f, %.3f, %.3f) cm  energy %.3f MeV",
					cluster.x(), cluster.y(), cluster.z(), cluster.energy()));
			return true;
		}
		return false;
	}
}
