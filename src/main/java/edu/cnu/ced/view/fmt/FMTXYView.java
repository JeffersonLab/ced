package edu.cnu.ced.view.fmt;

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
import edu.cnu.ced.data.FMTAccumulation;
import edu.cnu.ced.data.FMTEventData;
import edu.cnu.ced.data.FMTEventData.AdcHit;
import edu.cnu.ced.data.FMTEventData.Cluster;
import edu.cnu.ced.data.FMTEventData.Cross;
import edu.cnu.ced.data.FMTEventData.ReconHit;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.FMTGeometry;
import edu.cnu.ced.geometry.Segment3;
import edu.cnu.ced.view.CedXYView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Forward Micromegas Tracker (FMT) laboratory XY display backed directly by
 * CLAS banks -- six strip layers viewed transverse to the beam, each a
 * distinct color for orientation, hits/clusters/crosses overlaid on top.
 * <p>
 * FMTGeometry's own points are already in cm (unlike BSTGeometry/BMTGeometry,
 * which store mm and need a /10 at every call site) -- confirmed empirically
 * against real geometry rather than assumed, since getting this wrong would
 * make the whole view off by a factor of 10.
 * </p>
 */
@SuppressWarnings("serial")
public final class FMTXYView extends CedXYView {

	private static final Color[] LAYER_COLORS = {
			new Color(220, 20, 60), new Color(255, 140, 0), new Color(184, 134, 11),
			new Color(34, 139, 34), new Color(30, 144, 255), new Color(148, 0, 211)
	};
	private static final Color RECON_COLOR = new Color(225, 35, 25);
	private static final Color CLUSTER_COLOR = new Color(205, 0, 205);
	private static final Color CROSS_COLOR = new Color(20, 145, 35);
	private static final int MARKER = 4;

	private final FMTGeometry geometry;
	private final FMTAccumulation accumulation;
	private final Map<Object, Point> markers = new HashMap<>();
	// Screen-space endpoints for every drawn strip, cached from the draw pass so
	// getFeedbackStrings (called on every mouse move) doesn't redo 6144 geometry
	// lookups and world-to-local transforms per hover -- matching the lesson from
	// the Filter dialog's own earlier sluggishness (heavy per-event work run on
	// something invoked far more often than a repaint).
	private final Map<StripAddress, Point[]> stripScreenPoints = new HashMap<>();
	private volatile FMTEventData eventData = FMTEventData.from(null);

	public FMTXYView(FMTGeometry geometry, EventNavigator navigator, FMTAccumulation accumulation) {
		super(navigator, PropertyUtils.TITLE, "FMT XY",
				PropertyUtils.WIDTH, 700, PropertyUtils.HEIGHT, 700,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(-24, 24, 48, -48),
				PropertyUtils.BACKGROUND, Color.WHITE,
				PropertyUtils.TOOLBARBITS, TOOLBAR_BITS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.accumulation = accumulation;
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT, CedDisplayOption.ACCUMULATION,
				CedDisplayOption.RAW_DATA, CedDisplayOption.RECON_HITS, CedDisplayOption.CLUSTERS,
				CedDisplayOption.CROSSES),
				List.of("FMT::"), ScientificColorMap.TURBO, "Relative ADC / accumulation");
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		eventData = FMTEventData.from(state.snapshot());
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
				if (isDisplayed(CedDisplayOption.RAW_DATA)) drawAdcHits(g, container);
				if (isDisplayed(CedDisplayOption.RECON_HITS)) drawReconHits(g, container);
				if (isDisplayed(CedDisplayOption.CLUSTERS)) drawClusters(g, container);
				if (isDisplayed(CedDisplayOption.CROSSES)) drawCrosses(g, container);
			}
			drawXYAxes(g, container);
		} finally {
			g.dispose();
		}
	}

	/** Every strip of every layer, as a thin reference line -- one color per layer. */
	private void drawStrips(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(1f));
		for (int layer = 0; layer < FMTGeometry.LAYER_COUNT; layer++) {
			g.setColor(LAYER_COLORS[layer]);
			for (int strip = 0; strip < FMTGeometry.STRIP_COUNT; strip++) {
				drawStripLine(g, container, layer, strip);
			}
		}
	}

	/** Same strip fan, but colored by accumulated occupancy instead of a flat per-layer color. */
	private void drawAccumulatedStrips(Graphics2D g, IContainer container) {
		int max = accumulation.maximumCount();
		g.setStroke(new BasicStroke(1f));
		for (int layer = 0; layer < FMTGeometry.LAYER_COUNT; layer++) {
			for (int strip = 1; strip <= FMTGeometry.STRIP_COUNT; strip++) {
				int count = accumulation.count(layer, strip);
				g.setColor(count == 0 || max == 0
						? new Color(230, 230, 230)
						: ScientificColorMap.TURBO.colorAt((double) count / max));
				drawStripLine(g, container, layer, strip - 1);
			}
		}
	}

	private void drawStripLine(Graphics2D g, IContainer container, int layer, int strip) {
		Segment3 line = geometry.stripLine(layer, strip);
		Point a = screen(container, line.start().x(), line.start().y());
		Point b = screen(container, line.end().x(), line.end().y());
		stripScreenPoints.put(new StripAddress(layer, strip), new Point[] { a, b });
		g.drawLine(a.x, a.y, b.x, b.y);
	}

	private record StripAddress(int layer, int strip) { }

	private void drawAdcHits(Graphics2D g, IContainer container) {
		int max = eventData.maximumAdc();
		if (max == 0) return;
		g.setStroke(new BasicStroke(3f));
		for (AdcHit hit : eventData.adcHits()) {
			if (hit.layer() < 0 || hit.layer() >= FMTGeometry.LAYER_COUNT
					|| hit.strip() < 1 || hit.strip() > FMTGeometry.STRIP_COUNT) {
				continue;
			}
			Segment3 line = geometry.stripLine(hit.layer(), hit.strip() - 1);
			Point a = screen(container, line.start().x(), line.start().y());
			Point b = screen(container, line.end().x(), line.end().y());
			g.setColor(ScientificColorMap.TURBO.colorAt((double) hit.adc() / max));
			g.drawLine(a.x, a.y, b.x, b.y);
		}
	}

	private void drawReconHits(Graphics2D g, IContainer container) {
		g.setColor(RECON_COLOR);
		for (ReconHit hit : eventData.reconHits()) {
			Point p = stripMidpoint(container, hit.layer(), hit.strip());
			if (p == null) continue;
			markers.put(hit, p);
			g.fillOval(p.x - MARKER, p.y - MARKER, 2 * MARKER, 2 * MARKER);
		}
	}

	private void drawClusters(Graphics2D g, IContainer container) {
		g.setColor(CLUSTER_COLOR);
		g.setStroke(new BasicStroke(1.6f));
		for (Cluster cluster : eventData.clusters()) {
			Point p = stripMidpoint(container, cluster.layer(), cluster.seedStrip());
			if (p == null) continue;
			markers.put(cluster, p);
			g.drawLine(p.x - 7, p.y, p.x + 7, p.y);
			g.drawLine(p.x, p.y - 7, p.x, p.y + 7);
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

	private Point stripMidpoint(IContainer container, int layer, int strip) {
		if (layer < 0 || layer >= FMTGeometry.LAYER_COUNT
				|| strip < 1 || strip > FMTGeometry.STRIP_COUNT) {
			return null;
		}
		Segment3 line = geometry.stripLine(layer, strip - 1);
		return screen(container, (line.start().x() + line.end().x()) / 2,
				(line.start().y() + line.end().y()) / 2);
	}

	private static Point screen(IContainer container, double x, double y) {
		Point point = new Point();
		container.worldToLocal(point, x, y);
		return point;
	}

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
			int strip = closest.strip() + 1;
			feedback.add(String.format("$wheat$FMT layer %d strip %d region %d",
					closest.layer() + 1, strip, FMTGeometry.region(strip)));
		}
		for (Map.Entry<Object, Point> entry : markers.entrySet()) {
			if (entry.getValue().distance(screenPoint) <= 9) {
				addMarkerFeedback(entry.getKey(), feedback);
				break;
			}
		}
	}

	private static void addMarkerFeedback(Object marker, List<String> feedback) {
		if (marker instanceof ReconHit hit) {
			feedback.add(String.format("$deep sky blue$FMT hit layer %d strip %d", hit.layer() + 1, hit.strip()));
			feedback.add(String.format("$deep sky blue$energy %.4f  time %.3f", hit.energy(), hit.time()));
		} else if (marker instanceof Cluster cluster) {
			feedback.add(String.format("$magenta$FMT cluster layer %d seed strip %d size %d",
					cluster.layer() + 1, cluster.seedStrip(), cluster.size()));
			feedback.add(String.format("$magenta$energy %.4f  time %.3f", cluster.energy(), cluster.time()));
		} else if (marker instanceof Cross cross) {
			feedback.add(String.format("$green$FMT cross sector %d region %d xyz (%.3f, %.3f, %.3f) cm",
					cross.sector(), cross.region(), cross.x(), cross.y(), cross.z()));
		}
	}
}
