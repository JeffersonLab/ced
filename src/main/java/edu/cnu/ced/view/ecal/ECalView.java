package edu.cnu.ced.view.ecal;

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
import edu.cnu.ced.data.ECalAccumulation;
import edu.cnu.ced.data.ECalEventData;
import edu.cnu.ced.data.ECalEventData.AdcHit;
import edu.cnu.ced.data.ECalEventData.ReconHit;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.ECGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.view.CedHexView;
import edu.cnu.ced.view.calorimeter.CalorimeterDrawingSupport;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;

/** Six-sector ECAL inner/outer-stack laboratory XY display. */
@SuppressWarnings("serial")
public final class ECalView extends CedHexView {
	private static final Color EMPTY_FILL = new Color(250, 250, 250, 180);
	private static final Color STRIP_OUTLINE = new Color(190, 190, 190);
	private static final Color HIT_EXTENSION_FILL = new Color(255, 230, 35, 210);
	private static final Color HIT_OUTLINE = new Color(120, 25, 20);
	private static final double EXTENSION_GAP = 1.0;
	private static final double MAX_EXTENSION_LENGTH = 32.0;
	private static final int RECON_RADIUS = 7;

	private final ECGeometry geometry;
	private final ECalAccumulation accumulation;
	private final Map<StripKey, Polygon> stripPolygons = new HashMap<>();
	private final Map<ReconHit, Point> reconLocations = new HashMap<>();
	private volatile ECalEventData eventData = ECalEventData.from(null);

	public ECalView(ECGeometry geometry, EventNavigator navigator, ECalAccumulation accumulation) {
		super(navigator, PropertyUtils.TITLE, "ECAL",
				PropertyUtils.WIDTH, 780, PropertyUtils.HEIGHT, 780,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(430, -496.53562, -860, 993.07124),
				PropertyUtils.BACKGROUND, new Color(235, 245, 250),
				PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.accumulation = accumulation;
		setAfterDraw(this::drawDetector);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT,
				CedDisplayOption.ACCUMULATION, CedDisplayOption.RAW_DATA,
				CedDisplayOption.RECON_CAL, CedDisplayOption.INNER_PLANE,
				CedDisplayOption.OUTER_PLANE, CedDisplayOption.U_STRIPS,
				CedDisplayOption.V_STRIPS, CedDisplayOption.W_STRIPS),
				List.of("ECAL::", "REC::Calorimeter"), ScientificColorMap.TURBO,
				"Relative ADC / accumulation");
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		eventData = ECalEventData.from(state.snapshot());
	}

	private int displayedPlane() {
		return isDisplayed(CedDisplayOption.INNER_PLANE) ? ECGeometry.INNER : ECGeometry.OUTER;
	}

	private void drawDetector(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			stripPolygons.clear();
			reconLocations.clear();
			int plane = displayedPlane();
			boolean accumulated = isDisplayed(CedDisplayOption.ACCUMULATION);
			Map<StripKey, AdcHit> adc = new HashMap<>();
			if (!accumulated && isDisplayed(CedDisplayOption.RAW_DATA)) {
				for (AdcHit hit : eventData.adcHits()) if (hit.plane() == plane)
					adc.put(new StripKey(hit.sector(), hit.view(), hit.strip()), hit);
			}
			for (int sector = 1; sector <= 6; sector++) {
				for (int view = 0; view < 3; view++) {
					if (!showView(view)) continue;
					for (int strip = 1; strip <= ECGeometry.STRIP_COUNT; strip++) {
						StripKey key = new StripKey(sector, view, strip);
						Polygon polygon = polygon(container, plane, key);
						stripPolygons.put(key, polygon);
						g.setColor(EMPTY_FILL);
						g.fillPolygon(polygon);
					}
				}
			}
			g.setColor(STRIP_OUTLINE);
			for (Polygon polygon : stripPolygons.values()) g.drawPolygon(polygon);
			for (Map.Entry<StripKey, Polygon> entry : stripPolygons.entrySet()) {
				StripKey key = entry.getKey();
				AdcHit hit = adc.get(key);
				int count = accumulated ? accumulation.count(key.sector(), plane, key.view(), key.strip()) : 0;
				double fraction = accumulated
						? (accumulation.maximumCount(plane) == 0 ? 0.0
								: (double) count / accumulation.maximumCount(plane))
						: (hit == null || eventData.maximumAdc() == 0 ? 0.0
								: (double) hit.adc() / eventData.maximumAdc());
				if ((accumulated && count == 0) || (!accumulated && hit == null)) continue;
				Color color = ScientificColorMap.TURBO.colorAt(fraction);
				g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 125));
				g.fillPolygon(entry.getValue());
				g.setColor(HIT_OUTLINE);
				g.drawPolygon(entry.getValue());
				Polygon extension = extensionPolygon(container, plane, key,
						Math.max(0.15, fraction));
				g.setColor(HIT_EXTENSION_FILL);
				g.fillPolygon(extension);
				g.setColor(HIT_OUTLINE);
				g.drawPolygon(extension);
			}
			if (!accumulated && isDisplayed(CedDisplayOption.RECON_CAL)) drawRecon(g, container, plane);
			drawXYAxes(g, container);
			CalorimeterDrawingSupport.drawDetectorLabel(g, "ECAL");
		} finally {
			g.dispose();
		}
	}

	private Polygon polygon(IContainer container, int plane, StripKey key) {
		return polygon(container, worldStrip(plane, key));
	}

	private Point2D.Double[] worldStrip(int plane, StripKey key) {
		Point2D.Double[] world = new Point2D.Double[4];
		int index = 0;
		List<Point3> vertices = geometry.stripVertices(key.sector(), plane, key.view(), key.strip());
		for (int corner : new int[] { 4, 5, 1, 0 }) {
			Point3 vertex = vertices.get(corner);
			world[index++] = new Point2D.Double(vertex.x(), vertex.y());
		}
		return world;
	}

	private static Polygon polygon(IContainer container, Point2D.Double[] world) {
		Polygon polygon = new Polygon();
		Point point = new Point();
		for (Point2D.Double vertex : world) {
			container.worldToLocal(point, vertex.x, vertex.y);
			polygon.addPoint(point.x, point.y);
		}
		return polygon;
	}

	private Polygon extensionPolygon(IContainer container, int plane, StripKey key,
			double fraction) {
		Point2D.Double[] strip = worldStrip(plane, key);
		double length = MAX_EXTENSION_LENGTH * Math.max(0, Math.min(1, fraction));
		double d1 = strip[1].distance(strip[2]);
		double d2 = strip[0].distance(strip[3]);
		Point2D.Double[] extension = {
				extend(strip[2], strip[1], 1 + EXTENSION_GAP / d1),
				extend(strip[2], strip[1], 1 + (EXTENSION_GAP + length) / d1),
				extend(strip[3], strip[0], 1 + (EXTENSION_GAP + length) / d2),
				extend(strip[3], strip[0], 1 + EXTENSION_GAP / d2)
		};
		return polygon(container, extension);
	}

	private static Point2D.Double extend(Point2D.Double from, Point2D.Double toward,
			double scale) {
		return new Point2D.Double(from.x + scale * (toward.x - from.x),
				from.y + scale * (toward.y - from.y));
	}

	private void drawRecon(Graphics2D g, IContainer container, int plane) {
		g.setStroke(new BasicStroke(2f));
		for (ReconHit hit : eventData.reconHits()) {
			if (hit.plane() != plane) continue;
			Point point = new Point();
			container.worldToLocal(point, hit.x(), hit.y());
			reconLocations.put(hit, point);
			g.setColor(new Color(220, 30, 30, 90));
			g.fillOval(point.x - RECON_RADIUS, point.y - RECON_RADIUS,
					2 * RECON_RADIUS, 2 * RECON_RADIUS);
			g.setColor(Color.RED.darker());
			g.drawOval(point.x - RECON_RADIUS, point.y - RECON_RADIUS,
					2 * RECON_RADIUS, 2 * RECON_RADIUS);
			CalorimeterDrawingSupport.drawReconCenter(g, point);
		}
	}

	private boolean showView(int view) {
		return isDisplayed(switch (view) {
			case 0 -> CedDisplayOption.U_STRIPS;
			case 1 -> CedDisplayOption.V_STRIPS;
			default -> CedDisplayOption.W_STRIPS;
		});
	}

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint,
			Point2D.Double worldPoint, List<String> feedback) {
		feedback.add(displayedPlane() == ECGeometry.INNER ? "$white$INNER plane" : "$white$OUTER plane");
		super.getFeedbackStrings(container, screenPoint, worldPoint, feedback);
		int sector = sectorAt(worldPoint);
		int plane = displayedPlane();
		int[] uvw = { -1, -1, -1 };
		for (int view = 0; view < 3; view++) {
			for (int strip = 1; strip <= ECGeometry.STRIP_COUNT; strip++) {
				if (polygon(container, plane, new StripKey(sector, view, strip)).contains(screenPoint)) {
					uvw[view] = strip;
					break;
				}
			}
		}
		if (uvw[0] > 0 || uvw[1] > 0 || uvw[2] > 0) {
			feedback.add(String.format("$lime green$U V W [%d, %d, %d]", uvw[0], uvw[1], uvw[2]));
			for (int view = 0; view < 3; view++) {
				if (uvw[view] < 1) continue;
				if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
					int count = accumulation.count(sector, plane, view, uvw[view]);
					if (count > 0) feedback.add(String.format("$cyan$%c occupancy %d / %d events",
							"UVW".charAt(view), count, accumulation.eventCount()));
				} else {
					for (AdcHit hit : eventData.adcHits()) if (hit.sector() == sector
							&& hit.plane() == plane && hit.view() == view && hit.strip() == uvw[view]) {
						feedback.add(String.format("$cyan$%c strip %d adc %d time %7.3f",
								"UVW".charAt(view), hit.strip(), hit.adc(), hit.time()));
					}
				}
			}
		}
		for (Map.Entry<ReconHit, Point> entry : reconLocations.entrySet()) {
			if (entry.getValue().distance(screenPoint) > RECON_RADIUS + 2) continue;
			ReconHit hit = entry.getKey();
			feedback.add(String.format("$magenta$REC::Calorimeter row %d energy %.4f GeV time %.3f",
					hit.row(), hit.energy(), hit.time()));
			break;
		}
	}

	private record StripKey(int sector, int view, int strip) { }
}
