package edu.cnu.ced.view.sector;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.MagneticFieldChangeListener;
import cnuphys.magfield.MagneticFields;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.DCAccumulation;
import edu.cnu.ced.data.DCEventData;
import edu.cnu.ced.data.DCEventData.RawHit;
import edu.cnu.ced.data.DCEventData.ReconHit;
import edu.cnu.ced.data.DCEventData.ReconKind;
import edu.cnu.ced.data.DCEventData.Cluster;
import edu.cnu.ced.data.DCEventData.Cross;
import edu.cnu.ced.data.DCEventData.Segment;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.DCGeometry;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.ced.view.CedView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ColorScaleBar;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.PropertyUtils;

/** Midplane projection of one pair of opposite CLAS12 sectors. */
@SuppressWarnings("serial")
public final class SectorView extends CedView implements MagneticFieldChangeListener {

	public enum Pair {
		SECTORS_1_4(1, 4), SECTORS_2_5(2, 5), SECTORS_3_6(3, 6);
		private final int upper;
		private final int lower;
		Pair(int upper, int lower) { this.upper = upper; this.lower = lower; }
	}

	private static final double COLOR_CEILING_PERCENTILE = 0.95;
	private static final double DEFAULT_MAX_FIELD_TESLA = 6.5;
	private static final double FIELD_SCALE_SPEEDUP = 6.0;
	private static final Color BACKGROUND = new Color(220, 232, 238);
	private static final Color[] CELL_FILL = {new Color(248, 251, 252), new Color(235, 242, 246)};
	private static final Color CELL_LINE = new Color(170, 180, 185);
	private static final double[] WIRE_THRESHOLD = {Double.NaN, 2.0, 2.0, 1.7, 1.7, 1.6, 1.6};
	private static final double[] HEX_THRESHOLD = {Double.NaN, 16.0, 16.0, 12.0, 12.0, 7.0, 7.0};

	private final DCGeometry geometry;
	private final DCAccumulation accumulation;
	private final Pair pair;
	private final Map<Cell, Polygon> screenCells = new HashMap<>();
	private final List<ScreenSegment> screenSegments = new ArrayList<>();
	private final List<ScreenCross> screenCrosses = new ArrayList<>();
	private volatile DCEventData data = DCEventData.from(null);
	private volatile FieldProbe fieldProbe = FieldProbe.factory();
	private volatile boolean showMagneticField;
	private ColorScaleBar fieldScale;
	private double phiOffsetDegrees;

	public SectorView(Pair pair, DCGeometry geometry, EventNavigator navigator,
			DCAccumulation accumulation) {
		super(navigator, PropertyUtils.TITLE,
				"Sectors " + pair.upper + " and " + pair.lower,
				PropertyUtils.WIDTH, 940, PropertyUtils.HEIGHT, 760,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(-10, -450, 840, 900),
				PropertyUtils.BACKGROUND, BACKGROUND, PropertyUtils.TOOLBARBITS,
				ToolBits.NAVIGATIONTOOLS, PropertyUtils.WHEELZOOM, true,
				PropertyUtils.VISIBLE, true);
		this.pair = pair;
		this.geometry = geometry;
		this.accumulation = accumulation;
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT,
				CedDisplayOption.ACCUMULATION, CedDisplayOption.RAW_DATA,
				CedDisplayOption.HB_HITS, CedDisplayOption.TB_HITS,
				CedDisplayOption.AI_HB_HITS, CedDisplayOption.AI_TB_HITS,
				CedDisplayOption.CLUSTERS, CedDisplayOption.CROSSES, CedDisplayOption.HB_SEGMENTS,
				CedDisplayOption.TB_SEGMENTS, CedDisplayOption.AI_HB_SEGMENTS,
				CedDisplayOption.AI_TB_SEGMENTS),
				List.of("DC::", "HitBasedTrkg::", "TimeBasedTrkg::"),
				ScientificColorMap.TURBO, "Relative occupancy (95th-percentile ceiling)",
				340);
		addControlTab("phi", createPhiPanel());
		addControlTab("field", createFieldPanel());
		MagneticFields.getInstance().addMagneticFieldChangeListener(this);
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		data = DCEventData.from(state.snapshot());
	}

	private void draw(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			screenCells.clear();
			screenSegments.clear();
			screenCrosses.clear();
			if (showMagneticField) drawMagneticField(g, container);
			drawBeamline(g, container);
			drawTarget(g, container);
			drawFramework(g, container, pair.upper);
			drawFramework(g, container, pair.lower);
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) drawAccumulation(g, container);
			else {
				if (isDisplayed(CedDisplayOption.RAW_DATA)) drawRaw(g, container);
				drawRecon(g, container);
				drawClusters(g);
				drawSegments(g, container);
				drawCrosses(g, container);
			}
			drawLabels(g, container);
			drawScale(g, container);
		} finally {
			g.dispose();
		}
	}

	private JPanel createPhiPanel() {
		JSlider slider = new JSlider(-25, 25, 0);
		slider.setMajorTickSpacing(5);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.addChangeListener(event -> {
			phiOffsetDegrees = slider.getValue();
			refresh();
		});
		JPanel panel = new JPanel(new java.awt.BorderLayout(4, 4));
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JLabel label = new JLabel("Δφ relative to sector midplane (degrees)");
		label.setFont(Fonts.smallFont);
		panel.add(label, java.awt.BorderLayout.NORTH);
		panel.add(slider, java.awt.BorderLayout.CENTER);
		return panel;
	}

	private JPanel createFieldPanel() {
		JCheckBox magnitude = new JCheckBox("B magnitude");
		magnitude.setFont(Fonts.defaultFont);
		magnitude.setSelected(showMagneticField);
		magnitude.addActionListener(event -> {
			showMagneticField = magnitude.isSelected();
			refresh();
		});

		JPanel options = new JPanel();
		options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
		options.setBorder(new CommonBorder("Field display"));
		magnitude.setAlignmentX(LEFT_ALIGNMENT);
		options.add(magnitude);

		fieldScale = new ColorScaleBar(ScientificColorMap.VIRIDIS);
		updateFieldScaleLabels();
		fieldScale.setBarHeight(18);
		fieldScale.setBorder(new CommonBorder("Magnetic field magnitude (T)"));

		JPanel panel = new JPanel(new java.awt.BorderLayout(4, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(options, java.awt.BorderLayout.NORTH);
		panel.add(fieldScale, java.awt.BorderLayout.CENTER);
		return panel;
	}

	private void drawMagneticField(Graphics2D g, IContainer container) {
		FieldProbe probe = fieldProbe;
		if (probe == null) return;
		Rectangle bounds = container.getComponent().getBounds();
		int step = 4;
		double angle = Math.toRadians(60.0 * (pair.upper - 1) + phiOffsetDegrees);
		double maximum = maximumFieldTesla();
		Point pixel = new Point();
		Point2D.Double world = new Point2D.Double();
		for (int x = step / 2; x < bounds.width; x += step) {
			for (int y = step / 2; y < bounds.height; y += step) {
				pixel.setLocation(x, y);
				container.localToWorld(pixel, world);
				double labX = world.y * Math.cos(angle);
				double labY = world.y * Math.sin(angle);
				if (!probe.contains(labX, labY, world.x)) continue;
				double tesla = probe.fieldMagnitude((float) labX, (float) labY,
						(float) world.x) / 10.0;
				Color base = ScientificColorMap.VIRIDIS.colorAt(fieldColorFraction(tesla, maximum));
				g.setColor(base);
				g.fillRect(x - step / 2, y - step / 2, step, step);
			}
		}
	}

	private double maximumFieldTesla() {
		FieldProbe probe = fieldProbe;
		if (probe == null) return DEFAULT_MAX_FIELD_TESLA;
		double maximum = probe.getMaxFieldMagnitude() / 10.0;
		return maximum > 0.0 ? maximum : DEFAULT_MAX_FIELD_TESLA;
	}

	static double fieldColorFraction(double tesla, double maximum) {
		if (tesla <= 0.0 || maximum <= 0.0) return 0.0;
		double ratio = Math.min(1.0, tesla / maximum);
		return Math.log1p(Math.expm1(FIELD_SCALE_SPEEDUP) * ratio) / FIELD_SCALE_SPEEDUP;
	}

	static double fieldValueAtColorFraction(double fraction, double maximum) {
		if (fraction <= 0.0 || maximum <= 0.0) return 0.0;
		double bounded = Math.min(1.0, fraction);
		return maximum * Math.expm1(FIELD_SCALE_SPEEDUP * bounded)
				/ Math.expm1(FIELD_SCALE_SPEEDUP);
	}

	private void updateFieldScaleLabels() {
		if (fieldScale == null) return;
		double maximum = maximumFieldTesla();
		fieldScale.setTickLabels("0", formatFieldTick(fieldValueAtColorFraction(0.25, maximum)),
				formatFieldTick(fieldValueAtColorFraction(0.50, maximum)),
				formatFieldTick(fieldValueAtColorFraction(0.75, maximum)),
				formatFieldTick(maximum));
	}

	private static String formatFieldTick(double tesla) {
		return String.format("%.2f", tesla);
	}

	private void drawBeamline(Graphics2D g, IContainer container) {
		Point start = local(container, -10, 0);
		Point end = local(container, 830, 0);
		g.setColor(new Color(120, 0, 110));
		g.setStroke(new BasicStroke(2f));
		g.drawLine(start.x, start.y, end.x, end.y);
	}

	private void drawTarget(Graphics2D g, IContainer container) {
		Point target = local(container, 0, 0);
		g.setColor(Color.WHITE);
		g.fillOval(target.x - 5, target.y - 5, 10, 10);
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(1.5f));
		g.drawOval(target.x - 5, target.y - 5, 10, 10);
		g.drawLine(target.x - 8, target.y, target.x + 8, target.y);
		g.drawLine(target.x, target.y - 8, target.x, target.y + 8);
	}

	private void drawFramework(Graphics2D g, IContainer container, int sector) {
		double density = meanPixelDensity(container);
		for (int superlayer = 1; superlayer <= DCGeometry.SUPERLAYER_COUNT; superlayer++) {
			boolean drawWires = density > WIRE_THRESHOLD[superlayer];
			boolean drawHexagons = density > HEX_THRESHOLD[superlayer];
			List<Point> superlayerPoints = new ArrayList<>();
			List<Polygon> layerOutlines = new ArrayList<>();
			List<Polygon> cellPolygons = new ArrayList<>();
			List<Integer> cellLayers = new ArrayList<>();
			List<Point> wireCenters = new ArrayList<>();
			for (int layer = 1; layer <= DCGeometry.LAYER_COUNT; layer++) {
				List<Point> layerPoints = new ArrayList<>();
				for (int wire = 1; wire <= DCGeometry.WIRE_COUNT; wire++) {
					Cell cell = new Cell(sector, superlayer, layer, wire);
					Polygon polygon = screenPolygon(container, SectorProjection.cell(geometry,
							sector, superlayer, layer, wire, phiOffsetDegrees));
					screenCells.put(cell, polygon);
					if (wire == 1 || wire == DCGeometry.WIRE_COUNT) addPoints(layerPoints, polygon);
					if (drawHexagons) {
						cellPolygons.add(polygon);
						cellLayers.add(layer);
					} else if (drawWires) wireCenters.add(polygonCenter(polygon));
				}
				Polygon layerOutline = convexHull(layerPoints);
				layerOutlines.add(layerOutline);
				addPoints(superlayerPoints, layerOutline);
			}
			Polygon superlayerOutline = convexHull(superlayerPoints);
			g.setColor(CELL_FILL[0]);
			g.fillPolygon(superlayerOutline);
			if (drawHexagons) {
				for (int i = 0; i < cellPolygons.size(); i++) {
					g.setColor(CELL_FILL[cellLayers.get(i) & 1]);
					g.fillPolygon(cellPolygons.get(i));
					g.setColor(CELL_LINE);
					g.drawPolygon(cellPolygons.get(i));
				}
			} else {
				g.setColor(new Color(175, 180, 182));
				for (Polygon layerOutline : layerOutlines) g.drawPolygon(layerOutline);
				if (drawWires) {
					g.setColor(CELL_LINE);
					for (Point center : wireCenters) g.fillRect(center.x, center.y, 1, 1);
				}
			}
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(1.5f));
			g.drawPolygon(superlayerOutline);
		}
	}

	private static double meanPixelDensity(IContainer container) {
		Rectangle2D.Double world = container.getWorldSystem();
		if (world == null || world.width == 0.0 || world.height == 0.0) return 0.0;
		double xDensity = container.getComponent().getWidth() / Math.abs(world.width);
		double yDensity = container.getComponent().getHeight() / Math.abs(world.height);
		return Math.sqrt(xDensity * yDensity);
	}

	private static Point polygonCenter(Polygon polygon) {
		int x = 0;
		int y = 0;
		for (int i = 0; i < polygon.npoints; i++) {
			x += polygon.xpoints[i];
			y += polygon.ypoints[i];
		}
		return polygon.npoints == 0 ? new Point() : new Point(x / polygon.npoints, y / polygon.npoints);
	}

	private static void addPoints(List<Point> points, Polygon polygon) {
		for (int i = 0; i < polygon.npoints; i++) points.add(new Point(polygon.xpoints[i], polygon.ypoints[i]));
	}

	private static Polygon convexHull(List<Point> points) {
		if (points.size() < 3) {
			Polygon polygon = new Polygon();
			for (Point point : points) polygon.addPoint(point.x, point.y);
			return polygon;
		}
		List<Point> sorted = points.stream().distinct()
				.sorted(Comparator.comparingInt((Point p) -> p.x).thenComparingInt(p -> p.y)).toList();
		List<Point> hull = new ArrayList<>();
		for (Point point : sorted) {
			while (hull.size() >= 2 && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), point) <= 0)
				hull.remove(hull.size() - 1);
			hull.add(point);
		}
		int lowerSize = hull.size();
		for (int i = sorted.size() - 2; i >= 0; i--) {
			Point point = sorted.get(i);
			while (hull.size() > lowerSize && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), point) <= 0)
				hull.remove(hull.size() - 1);
			hull.add(point);
		}
		if (hull.size() > 1) hull.remove(hull.size() - 1);
		Polygon polygon = new Polygon();
		for (Point point : hull) polygon.addPoint(point.x, point.y);
		return polygon;
	}

	private static long cross(Point a, Point b, Point c) {
		return (long) (b.x - a.x) * (c.y - a.y) - (long) (b.y - a.y) * (c.x - a.x);
	}

	private void drawRaw(Graphics2D g, IContainer container) {
		for (RawHit hit : data.rawHits()) if (displayedSector(hit.sector()))
			fill(g, new Cell(hit.sector(), hit.superlayer(), hit.layer(), hit.wire()),
					CedDrawingStyle.RAW_HIT);
	}

	private void drawRecon(Graphics2D g, IContainer container) {
		for (ReconHit hit : data.reconHits()) if (displayedSector(hit.sector()) && show(hit.kind()))
			fill(g, new Cell(hit.sector(), hit.superlayer(), hit.layer(), hit.wire()),
					CedDrawingStyle.reconstructionColor(hit.kind()));
	}

	private void drawClusters(Graphics2D g) {
		if (!isDisplayed(CedDisplayOption.CLUSTERS)) return;
		g.setStroke(new BasicStroke(2.5f));
		for (Cluster cluster : data.clusters()) {
			if (!displayedSector(cluster.sector()) || !show(cluster.kind())) continue;
			Color color = CedDrawingStyle.outline(
					CedDrawingStyle.reconstructionColor(cluster.kind()));
			for (int hitId : cluster.hitIds()) {
				ReconHit hit = reconHit(cluster.kind(), hitId);
				if (hit == null) continue;
				Polygon polygon = screenCells.get(new Cell(hit.sector(), hit.superlayer(),
						hit.layer(), hit.wire()));
				if (polygon != null) {
					g.setColor(color);
					g.drawPolygon(polygon);
				}
			}
		}
		g.setStroke(new BasicStroke(1f));
	}

	private ReconHit reconHit(ReconKind kind, int id) {
		for (ReconHit hit : data.reconHits())
			if (hit.kind() == kind && hit.id() == id) return hit;
		return null;
	}

	private void drawSegments(Graphics2D g, IContainer container) {
		for (Segment segment : data.segments()) {
			if (!displayedSector(segment.sector()) || !showSegment(segment.kind())) continue;
			Point2D.Double w1 = SectorProjection.sectorPoint(segment.x1(), segment.z1(),
					segment.sector(), phiOffsetDegrees);
			Point2D.Double w2 = SectorProjection.sectorPoint(segment.x2(), segment.z2(),
					segment.sector(), phiOffsetDegrees);
			Point p1 = local(container, w1.x, w1.y);
			Point p2 = local(container, w2.x, w2.y);
			Color color = CedDrawingStyle.reconstructionColor(segment.kind());
			g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(new Color(245, 245, 210, 180));
			g.drawLine(p1.x, p1.y, p2.x, p2.y);
			g.setStroke(new BasicStroke(1.5f));
			g.setColor(color.darker());
			g.drawLine(p1.x, p1.y, p2.x, p2.y);
			drawEndpoint(g, p1, color);
			drawEndpoint(g, p2, color);
			screenSegments.add(new ScreenSegment(segment, p1, p2));
		}
	}

	private static void drawEndpoint(Graphics2D g, Point point, Color color) {
		g.setColor(color);
		g.fillOval(point.x - 3, point.y - 3, 7, 7);
		g.setColor(Color.BLACK);
		g.drawOval(point.x - 3, point.y - 3, 7, 7);
	}

	private void drawCrosses(Graphics2D g, IContainer container) {
		if (!isDisplayed(CedDisplayOption.CROSSES)) return;
		for (Cross cross : data.crosses()) {
			if (!displayedSector(cross.sector()) || !show(cross.kind())) continue;
			Point2D.Double world = SectorProjection.tiltedPoint(cross.x(), cross.y(), cross.z(),
					cross.sector(), phiOffsetDegrees);
			Point center = local(container, world.x, world.y);
			Point2D.Double directionWorld = SectorProjection.tiltedPoint(
					cross.x() + cross.directionX(), cross.y() + cross.directionY(),
					cross.z() + cross.directionZ(), cross.sector(), phiOffsetDegrees);
			Point direction = local(container, directionWorld.x, directionWorld.y);
			double dx = direction.x - center.x;
			double dy = direction.y - center.y;
			double length = Math.hypot(dx, dy);
			Point arrow = length < 1.0 ? center : new Point(
					(int) Math.round(center.x + 30.0 * dx / length),
					(int) Math.round(center.y + 30.0 * dy / length));
			drawCrossSymbol(g, center, arrow, cross.kind());
			screenCrosses.add(new ScreenCross(cross, center));
		}
	}

	private static void drawCrossSymbol(Graphics2D g, Point center, Point arrow,
			ReconKind kind) {
		Color color = CedDrawingStyle.reconstructionColor(kind);
		g.setStroke(new BasicStroke(1.5f));
		g.setColor(new Color(255, 165, 0));
		g.drawLine(center.x + 1, center.y, arrow.x + 1, arrow.y);
		g.setColor(Color.DARK_GRAY);
		g.drawLine(center.x, center.y, arrow.x, arrow.y);
		g.setColor(color);
		g.fillOval(center.x - 6, center.y - 6, 13, 13);
		g.setColor(Color.BLACK);
		g.drawOval(center.x - 6, center.y - 6, 13, 13);
		g.drawLine(center.x - 8, center.y, center.x + 8, center.y);
		g.drawLine(center.x, center.y - 8, center.x, center.y + 8);
	}

	private void drawAccumulation(Graphics2D g, IContainer container) {
		for (int superlayer = 1; superlayer <= DCGeometry.SUPERLAYER_COUNT; superlayer++) {
			int ceiling = accumulation.percentileCount(superlayer, COLOR_CEILING_PERCENTILE);
			if (ceiling == 0) continue;
			for (int sector : new int[] {pair.upper, pair.lower})
				for (int layer = 1; layer <= DCGeometry.LAYER_COUNT; layer++)
					for (int wire = 1; wire <= DCGeometry.WIRE_COUNT; wire++) {
						int count = accumulation.count(sector, superlayer, layer, wire);
						if (count > 0) fill(g, new Cell(sector, superlayer, layer, wire),
								ScientificColorMap.TURBO.colorAt(Math.min(1.0, (double) count / ceiling)));
					}
		}
	}

	private void fill(Graphics2D g, Cell cell, Color color) {
		Polygon polygon = screenCells.get(cell);
		if (polygon == null) return;
		g.setColor(color);
		g.fillPolygon(polygon);
		g.setColor(color.darker());
		g.drawPolygon(polygon);
	}

	private void drawLabels(Graphics2D g, IContainer container) {
		g.setFont(Fonts.defaultBoldFont);
		g.setColor(new Color(30, 70, 120, 180));
		Point upper = local(container, 30, 420);
		Point lower = local(container, 30, -420);
		g.drawString("Sector " + pair.upper, upper.x, upper.y);
		g.drawString("Sector " + pair.lower, lower.x, lower.y);
		Point phi = local(container, 15, 440);
		g.setColor(new Color(110, 0, 90));
		g.drawString(String.format("projection φ = %.1f°", phiOffsetDegrees), phi.x, phi.y);
	}

	private void drawScale(Graphics2D g, IContainer container) {
		int width = container.getComponent().getWidth();
		int height = container.getComponent().getHeight();
		if (width < 100 || height < 80) return;
		Point p0 = new Point(0, height - 28);
		Point p1 = new Point(Math.max(60, width / 5), height - 28);
		Point2D.Double w0 = new Point2D.Double();
		Point2D.Double w1 = new Point2D.Double();
		container.localToWorld(p0, w0);
		container.localToWorld(p1, w1);
		double length = niceLength(Math.abs(w1.x - w0.x));
		Point start = new Point(28, height - 28);
		container.localToWorld(start, w0);
		Point end = local(container, w0.x + length, w0.y);
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(2f));
		g.drawLine(start.x, start.y, end.x, end.y);
		g.drawLine(start.x, start.y - 5, start.x, start.y + 5);
		g.drawLine(end.x, end.y - 5, end.x, end.y + 5);
		String label = String.format("%s cm", length >= 10 ? String.format("%.0f", length)
				: String.format("%.1f", length));
		g.setFont(Fonts.smallFont);
		int textWidth = g.getFontMetrics().stringWidth(label);
		g.drawString(label, (start.x + end.x - textWidth) / 2, start.y + 17);
	}

	private static double niceLength(double target) {
		if (!(target > 0.0)) return 1.0;
		double power = Math.pow(10.0, Math.floor(Math.log10(target)));
		double scaled = target / power;
		double nice = scaled >= 5.0 ? 5.0 : scaled >= 2.0 ? 2.0 : 1.0;
		return nice * power;
	}

	private boolean displayedSector(int sector) { return sector == pair.upper || sector == pair.lower; }

	private boolean show(ReconKind kind) {
		return switch (kind) {
			case HB -> isDisplayed(CedDisplayOption.HB_HITS);
			case TB -> isDisplayed(CedDisplayOption.TB_HITS);
			case AI_HB -> isDisplayed(CedDisplayOption.AI_HB_HITS);
			case AI_TB -> isDisplayed(CedDisplayOption.AI_TB_HITS);
		};
	}

	private boolean showSegment(ReconKind kind) {
		return switch (kind) {
			case HB -> isDisplayed(CedDisplayOption.HB_SEGMENTS);
			case TB -> isDisplayed(CedDisplayOption.TB_SEGMENTS);
			case AI_HB -> isDisplayed(CedDisplayOption.AI_HB_SEGMENTS);
			case AI_TB -> isDisplayed(CedDisplayOption.AI_TB_SEGMENTS);
		};
	}

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint,
			Point2D.Double worldPoint, List<String> feedback) {
		feedback.add(String.format("$yellow$(z, transverse) = (%6.2f, %6.2f) cm",
				worldPoint.x, worldPoint.y));
		addFieldFeedback(worldPoint, feedback);
		for (ScreenCross drawn : screenCrosses) {
			if (drawn.location.distance(screenPoint) <= 9.0) {
				Cross cross = drawn.cross;
				feedback.add(String.format("$green$%s cross ID %d sector %d region %d",
						cross.kind(), cross.id(), cross.sector(), cross.region()));
				feedback.add(String.format("$green$cross xyz (%.3f, %.3f, %.3f) cm",
						cross.x(), cross.y(), cross.z()));
				feedback.add(String.format("$green$cross error (%.3f, %.3f, %.3f) cm",
						cross.errorX(), cross.errorY(), cross.errorZ()));
				feedback.add(String.format("$green$cross direction (%.3f, %.3f, %.3f)",
						cross.directionX(), cross.directionY(), cross.directionZ()));
				feedback.add(String.format("$green$segment IDs %d, %d",
						cross.segment1Id(), cross.segment2Id()));
				break;
			}
		}
		for (ScreenSegment drawn : screenSegments) {
			if (Line2D.ptSegDist(drawn.start.x, drawn.start.y, drawn.end.x,
					drawn.end.y, screenPoint.x, screenPoint.y) <= 5.0) {
				Segment segment = drawn.segment;
				feedback.add(String.format("$orange$%s segment sector %d superlayer %d",
						segment.kind(), segment.sector(), segment.superlayer()));
				feedback.add(String.format("$orange$endpoints (x,z) (%.2f, %.2f) to (%.2f, %.2f) cm",
						segment.x1(), segment.z1(), segment.x2(), segment.z2()));
				break;
			}
		}
		Cell cell = screenCells.entrySet().stream().filter(e -> e.getValue().contains(screenPoint))
				.map(Map.Entry::getKey).findFirst().orElse(null);
		if (cell == null) return;
		feedback.add(String.format("$cyan$DC sector %d superlayer %d layer %d wire %d",
				cell.sector, cell.superlayer, cell.layer, cell.wire));
		if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
			feedback.add(String.format("$cyan$occupancy %d / %d events",
					accumulation.count(cell.sector, cell.superlayer, cell.layer, cell.wire),
					accumulation.eventCount()));
			feedback.add(String.format("$cyan$color ceiling %d; true maximum %d",
					accumulation.percentileCount(cell.superlayer, COLOR_CEILING_PERCENTILE),
					accumulation.maximumCount(cell.superlayer)));
		} else {
			for (RawHit hit : data.rawHits()) if (cell.matches(hit))
				feedback.add(String.format("$red$raw hit tdc %d order %d", hit.tdc(), hit.order()));
			for (ReconHit hit : data.reconHits()) if (show(hit.kind()) && cell.matches(hit))
				feedback.add(String.format("$orange$%s hit cluster %d status %d trkDOCA %.3f",
						hit.kind(), hit.clusterId(), hit.status(), hit.trackDoca()));
		}
	}

	private void addFieldFeedback(Point2D.Double worldPoint, List<String> feedback) {
		if (!showMagneticField) return;
		FieldProbe probe = fieldProbe;
		if (probe == null) return;
		double angle = Math.toRadians(60.0 * (pair.upper - 1) + phiOffsetDegrees);
		float labX = (float) (worldPoint.y * Math.cos(angle));
		float labY = (float) (worldPoint.y * Math.sin(angle));
		float labZ = (float) worldPoint.x;
		if (!probe.contains(labX, labY, labZ)) return;
		float[] field = new float[3];
		probe.field(labX, labY, labZ, field);
		double bx = field[0] / 10.0;
		double by = field[1] / 10.0;
		double bz = field[2] / 10.0;
		double magnitude = Math.sqrt(bx * bx + by * by + bz * bz);
		feedback.add(String.format("$green$|B| = %.4f T", magnitude));
		feedback.add(String.format("$green$B = (%.4f, %.4f, %.4f) T", bx, by, bz));
	}

	private static Polygon screenPolygon(IContainer container, Point2D.Double[] world) {
		Polygon polygon = new Polygon();
		for (Point2D.Double point : world) {
			Point local = local(container, point.x, point.y);
			polygon.addPoint(local.x, local.y);
		}
		return polygon;
	}

	private static Point local(IContainer container, double x, double y) {
		Point point = new Point();
		container.worldToLocal(point, x, y);
		return point;
	}

	private record Cell(int sector, int superlayer, int layer, int wire) {
		boolean matches(RawHit hit) { return sector == hit.sector() && superlayer == hit.superlayer()
				&& layer == hit.layer() && wire == hit.wire(); }
		boolean matches(ReconHit hit) { return sector == hit.sector() && superlayer == hit.superlayer()
				&& layer == hit.layer() && wire == hit.wire(); }
	}

	private record ScreenSegment(Segment segment, Point start, Point end) { }
	private record ScreenCross(Cross cross, Point location) { }

	@Override
	public void magneticFieldChanged() {
		fieldProbe = FieldProbe.factory();
		updateFieldScaleLabels();
		refresh();
	}

	@Override
	public void dispose() {
		MagneticFields.getInstance().removeMagneticFieldChangeListener(this);
		super.dispose();
	}
}
