package edu.cnu.ced.view.ftcal;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import edu.cnu.ced.data.FTCalEventData;
import edu.cnu.ced.data.FTCalEventData.AdcHit;
import edu.cnu.ced.data.FTCalEventData.ReconHit;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.ced.event.EventStore;
import edu.cnu.ced.geometry.FTCALGeometry;
import edu.cnu.ced.geometry.GridIndex;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;

/** Forward Tagger calorimeter XY view backed directly by event banks. */
@SuppressWarnings("serial")
public final class FTCalXYView extends BaseView {

	private static final Color EMPTY_FILL = new Color(245, 248, 248);
	private static final int HIT_HALF_SIZE = 4;

	private final FTCALGeometry geometry;
	private final EventStore eventStore;
	private final Consumer<EventSnapshot> eventListener = this::acceptSnapshot;
	private final Map<Integer, Polygon> componentPolygons = new HashMap<>();
	private final Map<ReconHit, Point> reconLocations = new HashMap<>();
	private final JCheckBox showAdc = new JCheckBox("ADC data", true);
	private final JCheckBox showHits = new JCheckBox("Reconstructed hits", true);
	private volatile FTCalEventData eventData = FTCalEventData.from(EventSnapshot.empty());

	public FTCalXYView(FTCALGeometry geometry, EventStore eventStore) {
		super(PropertyUtils.TITLE, "FTCal XY",
				PropertyUtils.WIDTH, 820,
				PropertyUtils.HEIGHT, 720,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(20, -20, -40, 40),
				PropertyUtils.BACKGROUND, Color.WHITE,
				PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true,
				PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.eventStore = eventStore;
		setAfterDraw(this::drawDetector);
		initSidePanel();
		eventStore.addListener(eventListener);
	}

	private void initSidePanel() {
		JPanel side = new JPanel(new BorderLayout(4, 8));
		side.setPreferredSize(new Dimension(235, 300));
		JPanel choices = new JPanel();
		choices.setBorder(BorderFactory.createTitledBorder("Display"));
		choices.add(showAdc);
		choices.add(showHits);
		showAdc.addActionListener(event -> refresh());
		showHits.addActionListener(event -> refresh());
		side.add(choices, BorderLayout.NORTH);
		FeedbackPane feedback = initFeedback(Color.CYAN, Color.BLACK, 10);
		side.add(feedback, BorderLayout.CENTER);
		add(side, BorderLayout.EAST);
	}

	private void acceptSnapshot(EventSnapshot snapshot) {
		FTCalEventData next = FTCalEventData.from(snapshot);
		if (SwingUtilities.isEventDispatchThread()) {
			eventData = next;
			refresh();
		} else {
			SwingUtilities.invokeLater(() -> { eventData = next; refresh(); });
		}
	}

	private void drawDetector(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			componentPolygons.clear();
			reconLocations.clear();
			Map<Integer, AdcHit> adcByComponent = new HashMap<>();
			if (showAdc.isSelected()) {
				for (AdcHit hit : eventData.adcHits()) adcByComponent.put(hit.component(), hit);
			}
			for (int component : geometry.componentIds()) {
				Polygon polygon = polygon(container, component);
				componentPolygons.put(component, polygon);
				AdcHit hit = adcByComponent.get(component);
				double fraction = eventData.maximumAdc() == 0 || hit == null ? 0
						: (double) hit.adc() / eventData.maximumAdc();
				g.setColor(hit == null ? EMPTY_FILL : ScientificColorMap.TURBO.colorAt(fraction));
				g.fillPolygon(polygon);
				g.setColor(Color.DARK_GRAY);
				g.drawPolygon(polygon);
			}
			if (showHits.isSelected()) drawReconHits(g, container);
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
		if (worldPoint != null) {
			feedback.add(String.format("$yellow$(x, y) = (%6.2f, %6.2f) cm", worldPoint.x, worldPoint.y));
		}
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
			if (showAdc.isSelected()) {
				for (AdcHit hit : eventData.adcHits()) if (hit.component() == component) {
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

	@Override
	public void dispose() {
		eventStore.removeListener(eventListener);
		super.dispose();
	}
}
