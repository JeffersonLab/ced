package edu.cnu.ced.view.ftof;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.FTOFAccumulation;
import edu.cnu.ced.data.FTOFEventData;
import edu.cnu.ced.data.FTOFEventData.AdcHit;
import edu.cnu.ced.data.FTOFEventData.Cluster;
import edu.cnu.ced.data.FTOFEventData.ReconHit;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.FTOFGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.view.CedHexView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;

/** Six-sector Forward Time-of-Flight display backed directly by CLAS banks. */
@SuppressWarnings("serial")
public final class FTOFView extends CedHexView {

	private static final Color EMPTY_FILL = new Color(248, 255, 252);
	private static final Color OUTLINE = new Color(175, 185, 185);
	private static final int HIT_RADIUS = 5;
	private static final int CLUSTER_RADIUS = 7;
	private static final String[] PANEL_NAMES = { "Panel 1A", "Panel 1B", "Panel 2" };

	private final FTOFGeometry geometry;
	private final FTOFAccumulation accumulation;
	private final Map<PaddleKey, Polygon> paddlePolygons = new HashMap<>();
	private final Map<ReconHit, Point> hitLocations = new HashMap<>();
	private final Map<Cluster, Point> clusterLocations = new HashMap<>();
	private volatile FTOFEventData eventData = FTOFEventData.from(null);

	public FTOFView(FTOFGeometry geometry, EventNavigator navigator,
			FTOFAccumulation accumulation) {
		super(navigator, PropertyUtils.TITLE, "FTOF",
				PropertyUtils.WIDTH, 820, PropertyUtils.HEIGHT, 760,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(440, -508, -880, 1016),
				PropertyUtils.BACKGROUND, Color.GRAY,
				PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.accumulation = accumulation;
		setAfterDraw(this::drawDetector);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT,
				CedDisplayOption.ACCUMULATION, CedDisplayOption.RAW_DATA,
				CedDisplayOption.RECON_HITS, CedDisplayOption.CLUSTERS,
				CedDisplayOption.PANEL_1A, CedDisplayOption.PANEL_1B,
				CedDisplayOption.PANEL_2), List.of("FTOF::"),
				ScientificColorMap.TURBO, "Relative ADC / accumulation");
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		eventData = FTOFEventData.from(state.snapshot());
	}

	private void drawDetector(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			paddlePolygons.clear();
			hitLocations.clear();
			clusterLocations.clear();
			int panel = selectedPanel();
			boolean accumulated = isDisplayed(CedDisplayOption.ACCUMULATION);
			for (int sector = 1; sector <= FTOFGeometry.SECTOR_COUNT; sector++) {
				for (int paddle = 1; paddle <= geometry.paddleCount(panel); paddle++) {
					PaddleKey key = new PaddleKey(sector, panel, paddle);
					Polygon polygon = paddlePolygon(container, key);
					paddlePolygons.put(key, polygon);
					double fraction = accumulated ? accumulatedFraction(key) : adcFraction(key);
					g.setColor(fraction > 0 ? ScientificColorMap.TURBO.colorAt(fraction) : EMPTY_FILL);
					g.fillPolygon(polygon);
					g.setColor(OUTLINE);
					g.drawPolygon(polygon);
				}
			}
			if (!accumulated && isDisplayed(CedDisplayOption.RECON_HITS)) drawHits(g, container, panel);
			if (!accumulated && isDisplayed(CedDisplayOption.CLUSTERS)) drawClusters(g, container, panel);
			drawXYAxes(g, container);
			g.setColor(new Color(255, 255, 255, 150));
			g.setFont((getFont() == null ? g.getFont() : getFont()).deriveFont(32f));
			g.drawString(PANEL_NAMES[panel], 10, 38);
		} finally {
			g.dispose();
		}
	}

	private double adcFraction(PaddleKey key) {
		if (!isDisplayed(CedDisplayOption.RAW_DATA) || eventData.maximumAdc() == 0) return 0;
		int maximum = 0;
		for (AdcHit hit : eventData.adcHits()) {
			if (hit.sector() == key.sector() && hit.panel() == key.panel()
					&& hit.paddle() == key.paddle()) maximum = Math.max(maximum, hit.adc());
		}
		return (double) maximum / eventData.maximumAdc();
	}

	private double accumulatedFraction(PaddleKey key) {
		int maximum = accumulation.maximumCount();
		return maximum == 0 ? 0 : (double) accumulation.count(
				key.sector(), key.panel(), key.paddle()) / maximum;
	}

	private void drawHits(Graphics2D g, IContainer container, int panel) {
		for (ReconHit hit : eventData.reconHits()) {
			if (hit.panel() != panel) continue;
			Point point = screenPoint(container, hit.x(), hit.y());
			hitLocations.put(hit, point);
			g.setColor(Color.CYAN);
			g.fillOval(point.x - HIT_RADIUS, point.y - HIT_RADIUS, 2 * HIT_RADIUS, 2 * HIT_RADIUS);
			g.setColor(Color.RED);
			g.drawOval(point.x - HIT_RADIUS, point.y - HIT_RADIUS, 2 * HIT_RADIUS, 2 * HIT_RADIUS);
		}
	}

	private void drawClusters(Graphics2D g, IContainer container, int panel) {
		g.setStroke(new BasicStroke(2f));
		for (Cluster cluster : eventData.clusters()) {
			if (cluster.panel() != panel) continue;
			Point point = screenPoint(container, cluster.x(), cluster.y());
			clusterLocations.put(cluster, point);
			g.setColor(Color.MAGENTA);
			g.drawLine(point.x - CLUSTER_RADIUS, point.y, point.x + CLUSTER_RADIUS, point.y);
			g.drawLine(point.x, point.y - CLUSTER_RADIUS, point.x, point.y + CLUSTER_RADIUS);
		}
	}

	private Polygon paddlePolygon(IContainer container, PaddleKey key) {
		Polygon polygon = new Polygon();
		Point point = new Point();
		for (Point3 vertex : geometry.frontFace(key.sector(), key.panel(), key.paddle())) {
			container.worldToLocal(point, vertex.x(), vertex.y());
			polygon.addPoint(point.x, point.y);
		}
		return polygon;
	}

	private static Point screenPoint(IContainer container, double x, double y) {
		Point point = new Point();
		container.worldToLocal(point, x, y);
		return point;
	}

	private int selectedPanel() {
		if (isDisplayed(CedDisplayOption.PANEL_1B)) return FTOFGeometry.PANEL_1B;
		if (isDisplayed(CedDisplayOption.PANEL_2)) return FTOFGeometry.PANEL_2;
		return FTOFGeometry.PANEL_1A;
	}

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint,
			Point2D.Double worldPoint, List<String> feedback) {
		super.getFeedbackStrings(container, screenPoint, worldPoint, feedback);
		for (Map.Entry<PaddleKey, Polygon> entry : paddlePolygons.entrySet()) {
			if (!entry.getValue().contains(screenPoint)) continue;
			PaddleKey key = entry.getKey();
			feedback.add(String.format("$cyan$FTOF %s sector %d paddle %d",
					PANEL_NAMES[key.panel()], key.sector(), key.paddle()));
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
				feedback.add(String.format("$cyan$occupancy %d / %d events",
						accumulation.count(key.sector(), key.panel(), key.paddle()),
						accumulation.eventCount()));
			} else for (AdcHit hit : eventData.adcHits()) {
				if (hit.sector() == key.sector() && hit.panel() == key.panel()
						&& hit.paddle() == key.paddle()) {
					feedback.add(String.format("$cyan$adc %d time %.3f order %d",
							hit.adc(), hit.time(), hit.order()));
				}
			}
			break;
		}
		for (Map.Entry<ReconHit, Point> entry : hitLocations.entrySet()) {
			if (entry.getValue().distance(screenPoint) > HIT_RADIUS + 2) continue;
			ReconHit hit = entry.getKey();
			feedback.add(String.format("$wheat$FTOF hit id %d xyz (%.2f, %.2f, %.2f) cm",
					hit.id(), hit.x(), hit.y(), hit.z()));
			break;
		}
		for (Map.Entry<Cluster, Point> entry : clusterLocations.entrySet()) {
			if (entry.getValue().distance(screenPoint) > CLUSTER_RADIUS + 3) continue;
			Cluster cluster = entry.getKey();
			feedback.add(String.format("$magenta$FTOF cluster xyz (%.3f, %.3f, %.3f) cm",
					cluster.x(), cluster.y(), cluster.z()));
			feedback.add(String.format("$magenta$energy %.3f GeV id %d status %d",
					cluster.energy(), cluster.id(), cluster.status()));
			break;
		}
	}

	private record PaddleKey(int sector, int panel, int paddle) { }
}
