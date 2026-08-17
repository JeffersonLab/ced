package edu.cnu.ced.view.central;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSlider;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.CentralAccumulation;
import edu.cnu.ced.data.CentralEventData;
import edu.cnu.ced.data.CentralEventData.AdcHit;
import edu.cnu.ced.data.CentralEventData.Cross;
import edu.cnu.ced.data.CentralEventData.Detector;
import edu.cnu.ced.data.CentralEventData.ReconHit;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.BMTGeometry;
import edu.cnu.ced.geometry.BSTGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.view.CedView;
import edu.cnu.mdi.component.AspectRatioPanel;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.PropertyUtils;

/** Longitudinal projection of the central tracking detectors. */
@SuppressWarnings("serial")
public final class CentralZView extends CedView {

	private static final Color BST_FILL = new Color(128, 128, 128, 28);
	private static final Color BST_EDGE = new Color(120, 120, 120, 105);
	private static final Color BMT_C = new Color(220, 255, 220, 145);
	private static final Color BMT_Z = new Color(242, 242, 242, 175);
	private static final Color RECON = new Color(225, 35, 25);
	private static final Color CROSS_COLOR = new Color(30, 145, 35);
	private static final int PHI_LIMIT = 25;

	private final BSTGeometry bst;
	private final BMTGeometry bmt;
	private final CentralAccumulation accumulation;
	private final Map<PanelAddress, List<Polygon>> panels = new HashMap<>();
	private final Map<Object, Point> markers = new HashMap<>();
	private final Map<Integer, Rectangle[]> bmtLayers = new HashMap<>();
	private double phiDegrees;
	private double cosPhi = 1;
	private double sinPhi;
	private volatile CentralEventData data = CentralEventData.from(null);

	public CentralZView(BSTGeometry bst, BMTGeometry bmt, EventNavigator navigator,
			CentralAccumulation accumulation) {
		super(navigator, PropertyUtils.TITLE, "Central Z", PropertyUtils.WIDTH, 900,
				PropertyUtils.HEIGHT, 780, PropertyUtils.WORLDSYSTEM,
				new Rectangle2D.Double(-24, -23, 52, 46), PropertyUtils.BACKGROUND,
				Color.WHITE, PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.bst = bst;
		this.bmt = bmt;
		this.accumulation = accumulation;
		installAspectRatioCanvas(52.0 / 46.0);
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT,
				CedDisplayOption.ACCUMULATION, CedDisplayOption.RAW_DATA,
				CedDisplayOption.RECON_HITS, CedDisplayOption.CROSSES),
				List.of("BST", "BMT", "CND", "CTOF", "CVT"),
				ScientificColorMap.TURBO, "Relative ADC / accumulation");
		addDisplayControl(createPhiControl());
	}

	private JPanel createPhiControl() {
		JSlider slider = new JSlider(-PHI_LIMIT, PHI_LIMIT, 0);
		slider.setMajorTickSpacing(5);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.addChangeListener(event -> {
			setPhi(slider.getValue());
			refresh();
		});
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Projection Δφ relative to midplane (deg)"));
		panel.add(slider, BorderLayout.CENTER);
		panel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,
				slider.getPreferredSize().height + 24));
		return panel;
	}

	private void setPhi(double degrees) {
		phiDegrees = degrees;
		double radians = Math.toRadians(degrees);
		cosPhi = Math.cos(radians);
		sinPhi = Math.sin(radians);
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		data = CentralEventData.from(state.snapshot());
	}

	private void draw(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			panels.clear();
			markers.clear();
			bmtLayers.clear();
			drawBMT(g, container);
			drawBST(g, container);
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) drawAccumulation(g, container);
			else {
				if (isDisplayed(CedDisplayOption.RAW_DATA)) drawAdc(g, container);
				if (isDisplayed(CedDisplayOption.RECON_HITS)) drawRecon(g, container);
				if (isDisplayed(CedDisplayOption.CROSSES)) drawCrosses(g, container);
			}
			drawAxes(g, container);
		} finally {
			g.dispose();
		}
	}

	private void drawBMT(Graphics2D g, IContainer c) {
		for (int layer = 1; layer <= BMTGeometry.LAYER_COUNT; layer++) {
			BMTGeometry.Layer info = bmt.layer(layer);
			double zMin = info.zMinMm() / 10, zMax = info.zMaxMm() / 10;
			double radius = info.radiusMm() / 10;
			Color fill = info.axis() == 1 ? BMT_Z : BMT_C;
			Rectangle top = drawWorldRectangle(g, c, zMin, radius, zMax - zMin, .45, fill, Color.GRAY);
			Rectangle bottom = drawWorldRectangle(g, c, zMin, -radius - .45, zMax - zMin, .45, fill, Color.GRAY);
			bmtLayers.put(layer, new Rectangle[] {top, bottom});
		}
	}

	private void drawBST(Graphics2D g, IContainer c) {
		for (int layer = 0; layer < BSTGeometry.LAYER_COUNT; layer++) {
			for (int sector = 0; sector < BSTGeometry.SECTORS_PER_LAYER[layer]; sector++) {
				double[] limits = bst.panelLimits(sector, layer);
				double y1 = projected(limits[0] / 10, limits[1] / 10);
				double y2 = projected(limits[2] / 10, limits[3] / 10);
				List<Polygon> pieces = List.of(
						quad(c, limits[4] / 10, limits[5] / 10, y1, y2),
						quad(c, limits[6] / 10, limits[7] / 10, y1, y2),
						quad(c, limits[8] / 10, limits[9] / 10, y1, y2));
				panels.put(new PanelAddress(sector + 1, layer + 1), pieces);
				for (Polygon polygon : pieces) {
					g.setColor(BST_FILL);
					g.fillPolygon(polygon);
					g.setColor(BST_EDGE);
					g.drawPolygon(polygon);
				}
			}
		}
	}

	private void drawAdc(Graphics2D g, IContainer c) {
		int max = maximumTrackerAdc();
		if (max == 0) return;
		for (AdcHit hit : data.adcHits()) {
			Color color = ScientificColorMap.TURBO.colorAt((double) hit.adc() / max);
			if (hit.detector() == Detector.BST && validBST(hit))
				drawBSTStrip(g, c, hit.sector(), hit.layer(), hit.component(), color, hit);
			else if (hit.detector() == Detector.BMT && validBMT(hit))
				drawBMTStrip(g, c, hit, color);
		}
	}

	private void drawBMTStrip(Graphics2D g, IContainer c, AdcHit hit, Color color) {
		Point[] ends = bmtStripProjection(c, hit.sector(), hit.layer(), hit.component());
		if (ends == null) return;
		Point a = ends[0], b = ends[1];
		g.setStroke(new BasicStroke(2.2f));
		g.setColor(color);
		g.drawLine(a.x, a.y, b.x, b.y);
		markers.put(hit, new Point((a.x + b.x) / 2, (a.y + b.y) / 2));
	}

	private Point[] bmtStripProjection(IContainer c, int sector, int layerNumber,
			int component) {
		if (layerNumber < 1 || layerNumber > BMTGeometry.LAYER_COUNT) return null;
		BMTGeometry.Layer layer = bmt.layer(layerNumber);
		double radius = layer.radiusMm() / 10;
		double zMin = layer.zMinMm() / 10, zMax = layer.zMaxMm() / 10;
		double phiStart = layer.phiMinDeg() + 120 * bmtSector(sector);
		double phiEnd = layer.phiMaxDeg() + 120 * bmtSector(sector);
		double fraction = (component - .5) / layer.stripCount();
		Point a;
		Point b;
		if (layer.axis() == 1) {
			double phi = phiStart + fraction * angularExtent(phiStart, phiEnd);
			double transverse = projectedPolar(radius, phi);
			a = screen(c, zMin, transverse);
			b = screen(c, zMax, transverse);
		} else {
			double z = zMin + fraction * (zMax - zMin);
			double phi = phiStart + angularExtent(phiStart, phiEnd) / 2;
			Point center = screen(c, z, projectedPolar(radius, phi));
			// A C strip is an azimuthal arc at fixed z. Drawing the entire projected
			// arc overwhelms this view, so represent its localized readout at the
			// projected strip midpoint with a compact transverse marker.
			a = new Point(center.x, center.y - 5);
			b = new Point(center.x, center.y + 5);
		}
		return new Point[] {a, b};
	}

	private void drawAccumulation(Graphics2D g, IContainer c) {
		int max = accumulation.maximumCount();
		if (max == 0) return;
		for (var entry : accumulation.counts().entrySet()) {
			var hit = entry.getKey();
			AdcHit adcHit = new AdcHit(hit.detector(), hit.sector(), hit.layer(),
					hit.component(), hit.order(), 0, Float.NaN);
			Color color = ScientificColorMap.TURBO.colorAt((double) entry.getValue() / max);
			if (hit.detector() == Detector.BST && validBST(adcHit))
				drawBSTStrip(g, c, hit.sector(), hit.layer(), hit.component(), color, hit);
			else if (hit.detector() == Detector.BMT && validBMT(adcHit))
				drawBMTStrip(g, c, adcHit, color);
		}
	}

	private void drawRecon(Graphics2D g, IContainer c) {
		for (ReconHit hit : data.reconHits()) {
			Point p;
			if (hit.detector() == Detector.BST) {
				if (hit.layer() < 1 || hit.layer() > BSTGeometry.LAYER_COUNT
						|| hit.sector() < 1
						|| hit.sector() > BSTGeometry.SECTORS_PER_LAYER[hit.layer() - 1]
						|| hit.strip() < 1 || hit.strip() > BSTGeometry.STRIP_COUNT) continue;
				Point3 midpoint = bst.midpoint(hit.sector() - 1, hit.layer() - 1,
						hit.strip() - 1);
				p = screen(c, midpoint.z() / 10,
						projected(midpoint.x() / 10, midpoint.y() / 10));
			} else if (hit.detector() == Detector.BMT) {
				AdcHit address = new AdcHit(Detector.BMT, hit.sector(), hit.layer(),
						hit.strip(), 0, 0, Float.NaN);
				if (!validBMT(address)) continue;
				Point[] ends = bmtStripProjection(c, hit.sector(), hit.layer(), hit.strip());
				if (ends == null) continue;
				p = new Point((ends[0].x + ends[1].x) / 2,
						(ends[0].y + ends[1].y) / 2);
			} else continue;
			markers.put(hit, p);
			g.setColor(RECON);
			g.setStroke(new BasicStroke(1.6f));
			g.drawLine(p.x - 5, p.y - 5, p.x + 5, p.y + 5);
			g.drawLine(p.x - 5, p.y + 5, p.x + 5, p.y - 5);
		}
	}

	private void drawCrosses(Graphics2D g, IContainer c) {
		g.setStroke(new BasicStroke(2f));
		g.setColor(CROSS_COLOR);
		for (Cross cross : data.crosses()) {
			if (Float.isNaN(cross.x()) || Float.isNaN(cross.y()) || Float.isNaN(cross.z())) continue;
			Point p = screen(c, cross.z(), projected(cross.x(), cross.y()));
			markers.put(cross, p);
			g.drawOval(p.x - 6, p.y - 6, 12, 12);
			g.drawLine(p.x - 8, p.y, p.x + 8, p.y);
			g.drawLine(p.x, p.y - 8, p.x, p.y + 8);
		}
	}

	private void drawBSTStrip(Graphics2D g, IContainer c, int sector, int layer,
			int strip, Color color, Object feedbackKey) {
		if (layer < 1 || layer > BSTGeometry.LAYER_COUNT || sector < 1
				|| sector > BSTGeometry.SECTORS_PER_LAYER[layer - 1] || strip < 1
				|| strip > BSTGeometry.STRIP_COUNT) return;
		var segment = bst.strip(sector - 1, layer - 1, strip - 1);
		Point a = screen(c, segment.start().z() / 10,
				projected(segment.start().x() / 10, segment.start().y() / 10));
		Point b = screen(c, segment.end().z() / 10,
				projected(segment.end().x() / 10, segment.end().y() / 10));
		g.setStroke(new BasicStroke(2f));
		g.setColor(color);
		g.drawLine(a.x, a.y, b.x, b.y);
		markers.put(feedbackKey, new Point((a.x + b.x) / 2, (a.y + b.y) / 2));
	}

	@Override
	public void getFeedbackStrings(IContainer c, Point sp, Point2D.Double wp,
			List<String> feedback) {
		feedback.add(String.format("$yellow$(z, transverse) = (%6.2f, %6.2f) cm", wp.x, wp.y));
		feedback.add(String.format("$purple$projection φ = %.1f°", phiDegrees));
		for (var entry : panels.entrySet()) {
			if (entry.getValue().stream().anyMatch(p -> p.contains(sp))) {
				PanelAddress panel = entry.getKey();
				feedback.add("$red$BST layer " + panel.layer());
				feedback.add("$red$BST region " + ((panel.layer() + 1) / 2));
				feedback.add("$red$BST sector " + panel.sector());
				break;
			}
		}
		for (var entry : bmtLayers.entrySet()) {
			if (entry.getValue()[0].contains(sp) || entry.getValue()[1].contains(sp)) {
				BMTGeometry.Layer layer = bmt.layer(entry.getKey());
				feedback.add("$darkgreen$BMT layer " + layer.number() + " region "
						+ layer.region() + " type " + (layer.axis() == 1 ? "Z" : "C"));
				feedback.add(String.format("$darkgreen$radius %.2f cm z [%.2f, %.2f] cm",
						layer.radiusMm() / 10, layer.zMinMm() / 10, layer.zMaxMm() / 10));
				break;
			}
		}
		for (var entry : markers.entrySet()) if (entry.getValue().distance(sp) <= 9) {
			if (entry.getKey() instanceof AdcHit hit) feedback.add(String.format(
					"$cyan$%s sector %d layer %d component %d adc %d time %.3f order %d",
					hit.detector(), hit.sector(), hit.layer(), hit.component(), hit.adc(),
					hit.time(), hit.order()));
			else if (entry.getKey() instanceof ReconHit hit) {
				feedback.add(String.format("$wheat$%s recon hit sector %d layer %d strip %d id %d",
						hit.detector(), hit.sector(), hit.layer(), hit.strip(), hit.id()));
				feedback.add(String.format("$wheat$energy %.4f time %.3f cluster %d track %d",
						hit.energy(), hit.time(), hit.clusterId(), hit.trackId()));
			}
			else if (entry.getKey() instanceof Cross cross)
				feedback.add(String.format("$green$%s cross id %d sector %d region %d xyz (%.3f, %.3f, %.3f) cm",
						cross.detector(), cross.id(), cross.sector(), cross.region(),
						cross.x(), cross.y(), cross.z()));
		}
	}

	private void drawAxes(Graphics2D g, IContainer c) {
		Rectangle screen = new Rectangle(0, 0, c.getComponent().getWidth() - 1,
				c.getComponent().getHeight() - 1);
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, screen.width, screen.height);
		g.setFont(Fonts.mediumFont);
		FontMetrics fm = g.getFontMetrics();
		Point origin = screen(c, 0, 0);
		g.setColor(new Color(110, 0, 90));
		g.drawLine(0, origin.y, screen.width, origin.y);
		g.drawString("z", screen.width - fm.stringWidth("z") - 6, origin.y - 4);
		g.drawString(String.format("projection φ = %.1f°", phiDegrees), 10, 18);
		drawTicks(g, c, fm);
		drawCompass(g, screen, fm);
	}

	private void drawTicks(Graphics2D g, IContainer c, FontMetrics fm) {
		Rectangle2D.Double world = c.getWorldSystem();
		g.setColor(Color.BLACK);
		for (int i = 0; i <= 50; i++) {
			double z = world.x + i * world.width / 50.0;
			Point bottom = screen(c, z, world.y);
			int length = i % 5 == 0 ? 10 : 5;
			g.drawLine(bottom.x, bottom.y, bottom.x, bottom.y - length);
			if (i % 5 == 0) {
				String label = integerLabel(z);
				g.drawString(label, bottom.x - fm.stringWidth(label) / 2, bottom.y - 13);
			}
		}
		for (int i = 0; i <= 40; i++) {
			double transverse = world.y + i * world.height / 40.0;
			Point left = screen(c, world.x, transverse);
			int length = i % 5 == 0 ? 10 : 5;
			g.drawLine(left.x, left.y, left.x + length, left.y);
			if (i % 5 == 0) {
				String label = integerLabel(transverse);
				g.drawString(label, left.x + 13, left.y + fm.getAscent() / 2);
			}
		}
	}

	private void drawCompass(Graphics2D g, Rectangle screen, FontMetrics fm) {
		int size = 82;
		int left = 18;
		int top = screen.height - size - 18;
		g.setColor(new Color(225, 225, 225, 205));
		g.fillRect(left, top, size, size);
		int cx = left + size / 2, cy = top + size / 2;
		g.setStroke(new BasicStroke(1.7f));
		g.setColor(new Color(120, 0, 0));
		g.drawLine(cx, cy, cx + 31, cy);
		g.drawString("z", cx + 34, cy + fm.getAscent() / 2);
		int xScale = (int) Math.round(Math.abs(31 * cosPhi));
		int xx = (int) Math.round(sinPhi * xScale);
		int xy = (int) Math.round(cosPhi * xScale);
		g.drawLine(cx, cy, cx + xx, cy - xy);
		g.drawString("x", cx + xx - 3, cy - xy - 3);
		int yScale = (int) Math.round(Math.abs(31 * sinPhi));
		int yx = (int) Math.round(-cosPhi * yScale);
		int yy = (int) Math.round(sinPhi * yScale);
		g.drawLine(cx, cy, cx + yx, cy - yy);
		g.drawString("y", cx + yx - 3, cy - yy - 3);
	}

	private static String integerLabel(double value) {
		return Integer.toString((int) Math.round(value));
	}

	private static Rectangle drawWorldRectangle(Graphics2D g, IContainer c, double x,
			double y, double width, double height, Color fill, Color edge) {
		Point a = screen(c, x, y), b = screen(c, x + width, y + height);
		int left = Math.min(a.x, b.x), top = Math.min(a.y, b.y);
		int w = Math.abs(a.x - b.x), h = Math.abs(a.y - b.y);
		g.setColor(fill);
		g.fillRect(left, top, w, h);
		g.setColor(edge);
		g.drawRect(left, top, w, h);
		return new Rectangle(left, top, w, h);
	}

	private static Polygon quad(IContainer c, double z1, double z2, double y1, double y2) {
		Polygon p = new Polygon();
		for (double[] q : new double[][] {{z1, y1}, {z1, y2}, {z2, y2}, {z2, y1}}) {
			Point s = screen(c, q[0], q[1]);
			p.addPoint(s.x, s.y);
		}
		return p;
	}

	private double projected(double x, double y) {
		return -(x * cosPhi + y * sinPhi);
	}

	private int maximumTrackerAdc() {
		int max = 0;
		for (AdcHit hit : data.adcHits())
			if (hit.detector() == Detector.BST || hit.detector() == Detector.BMT)
			max = Math.max(max, hit.adc());
		return max;
	}

	private double projectedPolar(double radius, double phiDegrees) {
		double radians = Math.toRadians(phiDegrees);
		return projected(radius * Math.cos(radians), radius * Math.sin(radians));
	}

	private static double angularExtent(double start, double end) {
		double extent = end - start;
		while (extent < 0) extent += 360;
		while (extent > 360) extent -= 360;
		return extent;
	}

	private static int bmtSector(int dataSector) {
		return dataSector == 2 ? 0 : dataSector == 1 ? 1 : 2;
	}

	private boolean validBMT(AdcHit hit) {
		return hit.layer() >= 1 && hit.layer() <= BMTGeometry.LAYER_COUNT
				&& hit.sector() >= 1 && hit.sector() <= 3 && hit.component() >= 1
				&& hit.component() <= bmt.layer(hit.layer()).stripCount();
	}

	private static boolean validBST(AdcHit hit) {
		return hit.layer() >= 1 && hit.layer() <= BSTGeometry.LAYER_COUNT
				&& hit.sector() >= 1
				&& hit.sector() <= BSTGeometry.SECTORS_PER_LAYER[hit.layer() - 1]
				&& hit.component() >= 1 && hit.component() <= BSTGeometry.STRIP_COUNT;
	}

	private static Point screen(IContainer c, double x, double y) {
		Point p = new Point();
		c.worldToLocal(p, x, y);
		return p;
	}

	private void installAspectRatioCanvas(double aspectRatio) {
		IContainer drawingContainer = getIContainer();
		if (drawingContainer == null) return;
		Component canvas = drawingContainer.getComponent();
		Container parent = canvas == null ? null : canvas.getParent();
		if (parent == null || !(parent.getLayout() instanceof BorderLayout)) return;
		parent.remove(canvas);
		parent.add(new AspectRatioPanel(canvas, aspectRatio), BorderLayout.CENTER);
	}

	private record PanelAddress(int sector, int layer) { }
}
