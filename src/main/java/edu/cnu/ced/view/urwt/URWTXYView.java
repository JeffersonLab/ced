package edu.cnu.ced.view.urwt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.URWTAccumulation;
import edu.cnu.ced.data.URWTEventData;
import edu.cnu.ced.data.URWTEventData.Cluster;
import edu.cnu.ced.data.URWTEventData.Cross;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.Segment3;
import edu.cnu.ced.geometry.URWTGeometry;
import edu.cnu.ced.view.CedXYView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * μrWT (Micro Ring Wire Tracker) laboratory XY display backed directly by
 * CLAS banks -- four strip layers per sector viewed transverse to the beam.
 * <p>
 * No raw-hit markers: {@code URWT::hits} addresses a strip by (sector,
 * layer, strip), but its layer numbering doesn't cleanly line up with
 * {@link URWTGeometry}'s own 4-layer model (see {@link URWTEventData}'s
 * class doc), so this draws only what has an unambiguous position --
 * geometry itself, clusters (which carry explicit endpoints), and crosses
 * (which carry an explicit position) -- plus occupancy accumulation, which
 * only needs the address, not a resolved one.
 * </p>
 * <p>
 * URWTGeometry's own points are already in cm, confirmed empirically the
 * same way as FMTGeometry's were, not assumed.
 * </p>
 */
@SuppressWarnings("serial")
public final class URWTXYView extends CedXYView {

	private static final Color[] LAYER_COLORS = {
			new Color(220, 20, 60), new Color(255, 140, 0), new Color(30, 144, 255), new Color(148, 0, 211)
	};
	private static final Color CLUSTER_COLOR = new Color(205, 0, 205);
	private static final Color CROSS_COLOR = new Color(20, 145, 35);

	private final URWTGeometry geometry;
	private final URWTAccumulation accumulation;
	private final Map<Object, Point> markers = new HashMap<>();
	// Screen-space endpoints for every drawn strip, cached from the draw pass so
	// getFeedbackStrings (called on every mouse move) doesn't redo geometry
	// lookups and world-to-local transforms per hover -- same reasoning as
	// FMTXYView's identical cache.
	private final Map<StripAddress, Point[]> stripScreenPoints = new HashMap<>();
	private volatile URWTEventData eventData = URWTEventData.from(null);

	public URWTXYView(URWTGeometry geometry, EventNavigator navigator, URWTAccumulation accumulation) {
		super(navigator, PropertyUtils.TITLE, "μrWT XY",
				PropertyUtils.WIDTH, 700, PropertyUtils.HEIGHT, 700,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(-250, 250, 500, -500),
				PropertyUtils.BACKGROUND, Color.WHITE,
				PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.accumulation = accumulation;
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT, CedDisplayOption.ACCUMULATION,
				CedDisplayOption.CLUSTERS, CedDisplayOption.CROSSES),
				List.of("URWT::"), ScientificColorMap.TURBO, "Relative occupancy / accumulation");
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		eventData = URWTEventData.from(state.snapshot());
	}

	private void draw(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			markers.clear();
			stripScreenPoints.clear();
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
				drawAccumulatedStrips(g, container);
			} else {
				drawStrips(g, container);
				if (isDisplayed(CedDisplayOption.CLUSTERS)) drawClusters(g, container);
				if (isDisplayed(CedDisplayOption.CROSSES)) drawCrosses(g, container);
			}
			drawXYAxes(g, container);
		} finally {
			g.dispose();
		}
	}

	private void drawStrips(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(1f));
		for (int sector = 1; sector <= URWTGeometry.SECTOR_COUNT; sector++) {
			for (int layer = 1; layer <= URWTGeometry.LAYER_COUNT; layer++) {
				g.setColor(LAYER_COLORS[layer - 1]);
				List<Segment3> strips = geometry.detector(sector, layer).strips();
				for (int strip = 1; strip <= strips.size(); strip++) {
					drawStripLine(g, container, sector, layer, strip, strips.get(strip - 1));
				}
			}
		}
	}

	private void drawAccumulatedStrips(Graphics2D g, IContainer container) {
		int max = accumulation.maximumCount();
		g.setStroke(new BasicStroke(1f));
		for (int sector = 1; sector <= URWTGeometry.SECTOR_COUNT; sector++) {
			for (int layer = 1; layer <= URWTGeometry.LAYER_COUNT; layer++) {
				List<Segment3> strips = geometry.detector(sector, layer).strips();
				for (int strip = 1; strip <= strips.size(); strip++) {
					int count = accumulation.count(sector, layer, strip);
					g.setColor(count == 0 || max == 0
							? new Color(230, 230, 230)
							: ScientificColorMap.TURBO.colorAt((double) count / max));
					drawStripLine(g, container, sector, layer, strip, strips.get(strip - 1));
				}
			}
		}
	}

	private void drawStripLine(Graphics2D g, IContainer container, int sector, int layer, int strip,
			Segment3 line) {
		Point a = screen(container, line.start().x(), line.start().y());
		Point b = screen(container, line.end().x(), line.end().y());
		stripScreenPoints.put(new StripAddress(sector, layer, strip), new Point[] { a, b });
		g.drawLine(a.x, a.y, b.x, b.y);
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

	private static Point screen(IContainer container, double x, double y) {
		Point point = new Point();
		container.worldToLocal(point, x, y);
		return point;
	}

	private record StripAddress(int sector, int layer, int strip) { }

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint, Point2D.Double worldPoint,
			List<String> feedback) {
		super.getFeedbackStrings(container, screenPoint, worldPoint, feedback);
		addXYFeedback(worldPoint, "cm", feedback);
		StripAddress closest = null;
		double best = 6.0;
		for (Map.Entry<StripAddress, Point[]> entry : stripScreenPoints.entrySet()) {
			Point[] points = entry.getValue();
			double distance = Line2D.ptSegDist(points[0].x, points[0].y, points[1].x, points[1].y,
					screenPoint.x, screenPoint.y);
			if (distance < best) {
				best = distance;
				closest = entry.getKey();
			}
		}
		if (closest != null) {
			feedback.add(String.format("$wheat$μrWT sector %d layer %d strip %d",
					closest.sector(), closest.layer(), closest.strip()));
		}
		for (Map.Entry<Object, Point> entry : markers.entrySet()) {
			if (entry.getValue().distance(screenPoint) <= 9) {
				addMarkerFeedback(entry.getKey(), feedback);
				break;
			}
		}
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
}
