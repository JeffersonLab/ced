package edu.cnu.ced.view.ftcal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.FTCalAccumulation;
import edu.cnu.ced.data.FTCalEventData;
import edu.cnu.ced.data.FTCalEventData.AdcHit;
import edu.cnu.ced.data.FTCalEventData.ReconHit;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.FTCALGeometry;
import edu.cnu.ced.geometry.GridIndex;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.view.CedXYView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;

/** Forward Tagger calorimeter XY view backed directly by event banks. */
@SuppressWarnings("serial")
public final class FTCalXYView extends CedXYView {

	private static final Color EMPTY_FILL = new Color(245, 248, 248);
	private static final int HIT_HALF_SIZE = 4;

	private final FTCALGeometry geometry;
	private final FTCalAccumulation accumulation;
	private final Map<Integer, Polygon> componentPolygons = new HashMap<>();
	private final Map<ReconHit, Point> reconLocations = new HashMap<>();
	private volatile FTCalEventData eventData = FTCalEventData.from(null);

	public FTCalXYView(FTCALGeometry geometry, EventNavigator navigator,
			FTCalAccumulation accumulation) {
		super(navigator, PropertyUtils.TITLE, "FTCal XY",
				PropertyUtils.WIDTH, 820,
				PropertyUtils.HEIGHT, 720,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(20, -20, -40, 40),
				PropertyUtils.BACKGROUND, Color.WHITE,
				PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true,
				PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.accumulation = accumulation;
		setAfterDraw(this::drawDetector);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT,
				CedDisplayOption.ACCUMULATION, CedDisplayOption.RAW_DATA,
				CedDisplayOption.RECON_HITS), List.of("FTCAL::"),
				ScientificColorMap.TURBO, "Relative ADC / accumulation");
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		eventData = FTCalEventData.from(state.snapshot());
	}

	private void drawDetector(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			componentPolygons.clear();
			reconLocations.clear();
			Map<Integer, AdcHit> adcByComponent = new HashMap<>();
			boolean accumulated = isDisplayed(CedDisplayOption.ACCUMULATION);
			if (isDisplayed(CedDisplayOption.RAW_DATA) && !accumulated) {
				for (AdcHit hit : eventData.adcHits()) adcByComponent.put(hit.component(), hit);
			}
			for (int component : geometry.componentIds()) {
				Polygon polygon = polygon(container, component);
				componentPolygons.put(component, polygon);
				AdcHit hit = adcByComponent.get(component);
				int count = accumulated ? accumulation.count(component) : 0;
				double fraction = accumulated
						? (accumulation.maximumCount() == 0 ? 0.0 : (double) count / accumulation.maximumCount())
						: (eventData.maximumAdc() == 0 || hit == null ? 0.0
								: (double) hit.adc() / eventData.maximumAdc());
				boolean active = accumulated ? count > 0 : hit != null;
				g.setColor(active ? ScientificColorMap.TURBO.colorAt(fraction) : EMPTY_FILL);
				g.fillPolygon(polygon);
				g.setColor(Color.DARK_GRAY);
				g.drawPolygon(polygon);
			}
			if (!accumulated && isDisplayed(CedDisplayOption.RECON_HITS)) drawReconHits(g, container);
			drawXYAxes(g, container);
		} finally {
			g.dispose();
		}
	}

	private Polygon polygon(IContainer container, int component) {
		List<Point3> corners = geometry.corners(component).orElse(List.of());
		double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		for (Point3 corner : corners) {
			minX = Math.min(minX, corner.x()); maxX = Math.max(maxX, corner.x());
			minY = Math.min(minY, corner.y()); maxY = Math.max(maxY, corner.y());
		}
		Polygon polygon = new Polygon();
		Point point = new Point();
		container.worldToLocal(point, minX, minY); polygon.addPoint(point.x, point.y);
		container.worldToLocal(point, maxX, minY); polygon.addPoint(point.x, point.y);
		container.worldToLocal(point, maxX, maxY); polygon.addPoint(point.x, point.y);
		container.worldToLocal(point, minX, maxY); polygon.addPoint(point.x, point.y);
		return polygon;
	}

	private void drawReconHits(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(1.5f));
		for (ReconHit hit : eventData.reconHits()) {
			Point point = new Point();
			container.worldToLocal(point, hit.x(), hit.y());
			reconLocations.put(hit, point);
			g.setColor(Color.RED);
			g.fillRect(point.x - HIT_HALF_SIZE, point.y - HIT_HALF_SIZE,
					2 * HIT_HALF_SIZE + 1, 2 * HIT_HALF_SIZE + 1);
			g.setColor(Color.BLACK);
			g.drawRect(point.x - HIT_HALF_SIZE, point.y - HIT_HALF_SIZE,
					2 * HIT_HALF_SIZE + 1, 2 * HIT_HALF_SIZE + 1);
		}
	}

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint,
			Point2D.Double worldPoint, List<String> feedback) {
		super.getFeedbackStrings(container, screenPoint, worldPoint, feedback);
		for (Map.Entry<ReconHit, Point> entry : reconLocations.entrySet()) {
			Point p = entry.getValue();
			if (Math.abs(p.x - screenPoint.x) <= HIT_HALF_SIZE + 2
					&& Math.abs(p.y - screenPoint.y) <= HIT_HALF_SIZE + 2) {
				ReconHit hit = entry.getKey();
				feedback.add(String.format("$orange red$FTCAL hit %d xyz (%5.2f, %5.2f, %5.2f) cm",
						hit.id(), hit.x(), hit.y(), hit.z()));
			}
		}
		for (Map.Entry<Integer, Polygon> entry : componentPolygons.entrySet()) {
			if (!entry.getValue().contains(screenPoint)) continue;
			int component = entry.getKey();
			feedback.add("$red$FTCAL component " + component);
			geometry.gridIndex(component).ifPresent(grid -> addGridFeedback(grid, feedback));
			if (isDisplayed(CedDisplayOption.RAW_DATA)) {
				if (isDisplayed(CedDisplayOption.ACCUMULATION)
						&& accumulation.count(component) > 0) {
					feedback.add("$cyan$FTCAL occupancy " + accumulation.count(component)
							+ " / " + accumulation.eventCount() + " events");
				}
				for (AdcHit hit : eventData.adcHits()) if (!isDisplayed(CedDisplayOption.ACCUMULATION)
						&& hit.component() == component) {
					feedback.add(String.format("$cyan$FTCAL adc %d time %6.3f order %d",
							hit.adc(), hit.time(), hit.order()));
				}
			}
			break;
		}
	}

	private static void addGridFeedback(GridIndex grid, List<String> feedback) {
		feedback.add("$red$grid indices [" + grid.x() + ", " + grid.y() + "]");
	}

}
