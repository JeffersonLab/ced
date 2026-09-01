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
import edu.cnu.ced.data.CherenkovAccumulation;
import edu.cnu.ced.data.CherenkovEventData;
import edu.cnu.ced.data.DCEventData;
import edu.cnu.ced.data.DCEventData.RawHit;
import edu.cnu.ced.data.DCEventData.ReconHit;
import edu.cnu.ced.data.DCEventData.ReconKind;
import edu.cnu.ced.data.DCEventData.Cluster;
import edu.cnu.ced.data.DCEventData.Cross;
import edu.cnu.ced.data.DCEventData.Segment;
import edu.cnu.ced.data.ECalAccumulation;
import edu.cnu.ced.data.ECalEventData;
import edu.cnu.ced.data.FTOFEventData;
import edu.cnu.ced.data.FTOFEventData.AdcHit;
import edu.cnu.ced.data.PCalAccumulation;
import edu.cnu.ced.data.PCalEventData;
import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.DCGeometry;
import edu.cnu.ced.geometry.ECGeometry;
import edu.cnu.ced.geometry.FTOFGeometry;
import edu.cnu.ced.geometry.HTCCGeometry;
import edu.cnu.ced.geometry.LTCCGeometry;
import edu.cnu.ced.geometry.PCALGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.ced.swim.SwimTrajectoryCache;
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
	private static final String[] FTOF_PANEL_NAMES = {"Panel 1A", "Panel 1B", "Panel 2"};
	private static final Color FTOF_FILL = new Color(248, 252, 252);
	private static final Color FTOF_LINE = new Color(175, 185, 185);
	private static final Color CALORIMETER_FILL = new Color(248, 249, 246, 120);
	private static final Color CALORIMETER_LINE = new Color(185, 180, 170, 150);
	private static final String[] CALORIMETER_VIEWS = {"U", "V", "W"};
	private static final String[] ECAL_PLANES = {"inner", "outer"};
	private static final int PCAL = 0;
	private static final int ECAL = 1;
	private static final Color HTCC_FILL = new Color(245, 250, 238, 150);
	private static final Color LTCC_FILL = new Color(238, 248, 248, 145);

	private final DCGeometry geometry;
	private final FTOFGeometry ftofGeometry;
	private final PCALGeometry pcalGeometry;
	private final ECGeometry ecGeometry;
	private final DCAccumulation accumulation;
	private final PCalAccumulation pcalAccumulation;
	private final ECalAccumulation ecalAccumulation;
	private final CherenkovAccumulation htccAccumulation;
	private final CherenkovAccumulation ltccAccumulation;
	private final SwimTrajectoryCache swimCache;
	private final HTCCGeometry htccGeometry = new HTCCGeometry();
	private final LTCCGeometry ltccGeometry = new LTCCGeometry();
	private final Pair pair;
	private final Map<Cell, Polygon> screenCells = new HashMap<>();
	private final List<ScreenSegment> screenSegments = new ArrayList<>();
	private final List<ScreenCross> screenCrosses = new ArrayList<>();
	private final Map<FTOFCell, Polygon> ftofPaddles = new HashMap<>();
	private final Map<FTOFEventData.ReconHit, Point> ftofHitLocations = new HashMap<>();
	private final Map<FTOFEventData.Cluster, Point> ftofClusterLocations = new HashMap<>();
	private final Map<CalorimeterCell, Polygon> calorimeterStrips = new HashMap<>();
	private final Map<PCalEventData.ReconHit, Point> pcalHitLocations = new HashMap<>();
	private final Map<ECalEventData.ReconHit, Point> ecalHitLocations = new HashMap<>();
	private final Map<CherenkovCell, Polygon> cherenkovCells = new HashMap<>();
	private final Map<CherenkovMarker, Point> cherenkovMarkers = new HashMap<>();
	private final List<ScreenParticle> screenParticles = new ArrayList<>();
	private volatile DCEventData data = DCEventData.from(null);
	private volatile FTOFEventData ftofData = FTOFEventData.from(null);
	private volatile PCalEventData pcalData = PCalEventData.from(null);
	private volatile ECalEventData ecalData = ECalEventData.from(null);
	private volatile CherenkovEventData htccData = CherenkovEventData.from(null, "HTCC");
	private volatile CherenkovEventData ltccData = CherenkovEventData.from(null, "LTCC");
	private volatile RecEventData recData = RecEventData.from(null);
	private volatile FieldProbe fieldProbe = FieldProbe.factory();
	private volatile boolean showMagneticField;
	private ColorScaleBar fieldScale;
	private double phiOffsetDegrees;

	public SectorView(Pair pair, DCGeometry geometry, FTOFGeometry ftofGeometry,
			PCALGeometry pcalGeometry, ECGeometry ecGeometry, EventNavigator navigator,
			DCAccumulation accumulation, PCalAccumulation pcalAccumulation,
			ECalAccumulation ecalAccumulation, CherenkovAccumulation htccAccumulation,
			CherenkovAccumulation ltccAccumulation, SwimTrajectoryCache swimCache) {
		super(navigator, PropertyUtils.TITLE,
				"Sectors " + pair.upper + " and " + pair.lower,
				PropertyUtils.WIDTH, 940, PropertyUtils.HEIGHT, 760,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(-10, -450, 840, 900),
				PropertyUtils.BACKGROUND, BACKGROUND, PropertyUtils.TOOLBARBITS,
				ToolBits.NAVIGATIONTOOLS, PropertyUtils.WHEELZOOM, true,
				PropertyUtils.VISIBLE, true);
		this.pair = pair;
		this.geometry = geometry;
		this.ftofGeometry = ftofGeometry;
		this.pcalGeometry = pcalGeometry;
		this.ecGeometry = ecGeometry;
		this.accumulation = accumulation;
		this.pcalAccumulation = pcalAccumulation;
		this.ecalAccumulation = ecalAccumulation;
		this.htccAccumulation = htccAccumulation;
		this.ltccAccumulation = ltccAccumulation;
		this.swimCache = swimCache;
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT,
				CedDisplayOption.ACCUMULATION, CedDisplayOption.RAW_DATA,
				CedDisplayOption.RECON_HITS,
				CedDisplayOption.HB_HITS, CedDisplayOption.TB_HITS,
				CedDisplayOption.AI_HB_HITS, CedDisplayOption.AI_TB_HITS,
				CedDisplayOption.CLUSTERS, CedDisplayOption.CROSSES, CedDisplayOption.HB_SEGMENTS,
				CedDisplayOption.TB_SEGMENTS, CedDisplayOption.AI_HB_SEGMENTS,
				CedDisplayOption.AI_TB_SEGMENTS, CedDisplayOption.PARTICLES),
				List.of("DC::", "HitBasedTrkg::", "TimeBasedTrkg::", "FTOF::",
						"ECAL::", "REC::Calorimeter", "REC::Particle", "HTCC::", "LTCC::"),
				ScientificColorMap.TURBO, "Relative occupancy (95th-percentile ceiling)",
				340);
		addControlTab("phi", createPhiPanel());
		addControlTab("field", createFieldPanel());
		MagneticFields.getInstance().addMagneticFieldChangeListener(this);
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		data = DCEventData.from(state.snapshot());
		ftofData = FTOFEventData.from(state.snapshot());
		pcalData = PCalEventData.from(state.snapshot());
		ecalData = ECalEventData.from(state.snapshot());
		htccData = CherenkovEventData.from(state.snapshot(), "HTCC");
		ltccData = CherenkovEventData.from(state.snapshot(), "LTCC");
		recData = RecEventData.from(state.snapshot());
		swimCache.forEvent(state.snapshot());
	}

	private void draw(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			screenCells.clear();
			screenSegments.clear();
			screenCrosses.clear();
			ftofPaddles.clear();
			ftofHitLocations.clear();
			ftofClusterLocations.clear();
			calorimeterStrips.clear();
			pcalHitLocations.clear();
			ecalHitLocations.clear();
			cherenkovCells.clear();
			cherenkovMarkers.clear();
			screenParticles.clear();
			if (showMagneticField) drawMagneticField(g, container);
			drawBeamline(g, container);
			drawTarget(g, container);
			drawCherenkovFramework(g, container, pair.upper);
			drawCherenkovFramework(g, container, pair.lower);
			drawFramework(g, container, pair.upper);
			drawFramework(g, container, pair.lower);
			drawFTOFFramework(g, container, pair.upper);
			drawFTOFFramework(g, container, pair.lower);
			drawCalorimeterFramework(g, container, pair.upper);
			drawCalorimeterFramework(g, container, pair.lower);
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) drawAccumulation(g, container);
			else {
				if (isDisplayed(CedDisplayOption.RAW_DATA)) drawRaw(g, container);
				drawRecon(g, container);
				drawClusters(g);
				drawSegments(g, container);
				drawCrosses(g, container);
				drawFTOFEvent(g, container);
				drawCalorimeterEvent(g, container);
				drawCherenkovEvent(g, container);
				drawParticles(g, container);
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

	private void drawFTOFFramework(Graphics2D g, IContainer container, int sector) {
		for (int panel = 0; panel < FTOFGeometry.PANEL_COUNT; panel++) {
			for (int paddle = 1; paddle <= ftofGeometry.paddleCount(panel); paddle++) {
				Point2D.Double[] slice = SectorProjection.paddleSlice(
						ftofGeometry.paddle(sector, panel, paddle).projectionEdges(), sector,
						phiOffsetDegrees);
				if (slice.length == 0) continue;
				Polygon polygon = new Polygon();
				for (Point2D.Double world : slice) {
					Point point = local(container, world.x, world.y);
					polygon.addPoint(point.x, point.y);
				}
				ftofPaddles.put(new FTOFCell(sector, panel, paddle), polygon);
				g.setColor(FTOF_FILL);
				g.fillPolygon(polygon);
				g.setColor(FTOF_LINE);
				g.drawPolygon(polygon);
			}
		}
	}

	private void drawCherenkovFramework(Graphics2D g, IContainer container, int sector) {
		for (int half = 1; half <= HTCCGeometry.HALF_COUNT; half++) {
			for (int ring = 1; ring <= HTCCGeometry.RING_COUNT; ring++) {
				addCherenkovCell(g, container, new CherenkovCell("HTCC", sector, half, ring),
						htccGeometry.polygon(sector, ring, half, phiOffsetDegrees), HTCC_FILL);
			}
		}
		for (int half = 1; half <= LTCCGeometry.HALF_COUNT; half++) {
			for (int ring = 1; ring <= LTCCGeometry.RING_COUNT; ring++) {
				addCherenkovCell(g, container, new CherenkovCell("LTCC", sector, half, ring),
						ltccGeometry.polygon(sector, ring, half), LTCC_FILL);
			}
		}
	}

	private void addCherenkovCell(Graphics2D g, IContainer container, CherenkovCell cell,
			Point2D.Double[] world, Color fill) {
		if (world.length < 3) return;
		Polygon polygon = screenPolygon(container, world);
		cherenkovCells.put(cell, polygon);
		g.setColor(fill);
		g.fillPolygon(polygon);
		g.setColor(new Color(135, 150, 145));
		g.drawPolygon(polygon);
	}

	private void drawCherenkovEvent(Graphics2D g, IContainer container) {
		if (isDisplayed(CedDisplayOption.RAW_DATA)) {
			drawCherenkovRaw(g, htccData);
			drawCherenkovRaw(g, ltccData);
		}
		if (!isDisplayed(CedDisplayOption.RECON_HITS)) return;
		drawCherenkovRecon(g, container, htccData, new Color(255, 205, 30));
		drawCherenkovRecon(g, container, ltccData, new Color(65, 205, 160));
	}

	private void drawCherenkovRaw(Graphics2D g, CherenkovEventData event) {
		if (event.maximumAdc() <= 0) return;
		Map<CherenkovCell, Integer> maximumByCell = new HashMap<>();
		for (CherenkovEventData.AdcHit hit : event.adcHits()) {
			if (!displayedSector(hit.sector())) continue;
			maximumByCell.merge(new CherenkovCell(event.detector(), hit.sector(), hit.half(), hit.ring()),
					hit.adc(), Math::max);
		}
		for (Map.Entry<CherenkovCell, Integer> entry : maximumByCell.entrySet()) {
			fillCherenkov(g, entry.getKey(), ScientificColorMap.TURBO.colorAt(
					(double) entry.getValue() / event.maximumAdc()));
		}
	}

	private void drawCherenkovRecon(Graphics2D g, IContainer container,
			CherenkovEventData event, Color color) {
		for (CherenkovEventData.ReconHit hit : event.reconHits()) {
			if (!displayedSector(hit.sector())) continue;
			Point2D.Double world = SectorProjection.labPoint(new Point3(hit.x(), hit.y(), hit.z()),
					hit.sector(), phiOffsetDegrees);
			Point point = local(container, world.x, world.y);
			cherenkovMarkers.put(new CherenkovMarker(event.detector(), hit), point);
			g.setColor(color);
			g.fillOval(point.x - 5, point.y - 5, 11, 11);
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(1.5f));
			g.drawOval(point.x - 5, point.y - 5, 11, 11);
			g.drawLine(point.x - 7, point.y, point.x + 7, point.y);
			g.drawLine(point.x, point.y - 7, point.x, point.y + 7);
		}
	}

	private void fillCherenkov(Graphics2D g, CherenkovCell cell, Color color) {
		Polygon polygon = cherenkovCells.get(cell);
		if (polygon == null) return;
		g.setColor(color);
		g.fillPolygon(polygon);
		g.setColor(color.darker());
		g.drawPolygon(polygon);
	}

	private void drawCalorimeterFramework(Graphics2D g, IContainer container, int sector) {
		for (int view = 0; view < PCALGeometry.VIEW_COUNT; view++) {
			for (int strip = 1; strip <= PCALGeometry.STRIP_COUNTS[view]; strip++) {
				addCalorimeterStrip(g, container, new CalorimeterCell(PCAL, sector, 0, view, strip),
						pcalGeometry.stripVertices(sector, view, strip));
			}
		}
		for (int stack = 0; stack < ECGeometry.STACK_COUNT; stack++) {
			for (int view = 0; view < ECGeometry.VIEW_COUNT; view++) {
				for (int strip = 1; strip <= ECGeometry.STRIP_COUNT; strip++) {
					addCalorimeterStrip(g, container,
							new CalorimeterCell(ECAL, sector, stack, view, strip),
							ecGeometry.stripVertices(sector, stack, view, strip));
				}
			}
		}
	}

	private void addCalorimeterStrip(Graphics2D g, IContainer container,
			CalorimeterCell cell, List<Point3> vertices) {
		Point2D.Double[] slice = SectorProjection.volumeSlice(vertices, cell.sector,
				phiOffsetDegrees);
		if (slice.length < 3) return;
		Polygon polygon = screenPolygon(container, slice);
		calorimeterStrips.put(cell, polygon);
		g.setColor(CALORIMETER_FILL);
		g.fillPolygon(polygon);
		g.setColor(CALORIMETER_LINE);
		g.drawPolygon(polygon);
	}

	private void drawCalorimeterEvent(Graphics2D g, IContainer container) {
		if (isDisplayed(CedDisplayOption.RAW_DATA)) drawCalorimeterRaw(g);
		if (!isDisplayed(CedDisplayOption.RECON_HITS)) return;

		for (PCalEventData.ReconHit hit : pcalData.reconHits()) {
			if (!displayedSector(hit.sector())) continue;
			Point point = projectedPoint(container, hit.sector(), hit.x(), hit.y(), hit.z());
			pcalHitLocations.put(hit, point);
			drawCalorimeterFootprint(g, container, point, hit.radius());
			drawCalorimeterMarker(g, point, new Color(0, 210, 220), Color.RED);
		}
		for (ECalEventData.ReconHit hit : ecalData.reconHits()) {
			if (!displayedSector(hit.sector())) continue;
			Point point = projectedPoint(container, hit.sector(), hit.x(), hit.y(), hit.z());
			ecalHitLocations.put(hit, point);
			drawCalorimeterFootprint(g, container, point, hit.radius());
			drawCalorimeterMarker(g, point, new Color(255, 210, 0), new Color(120, 0, 110));
		}
	}

	private static void drawCalorimeterFootprint(Graphics2D g, IContainer container,
			Point center, double radius) {
		if (!(radius > 0.0) || !Double.isFinite(radius)) return;
		Point origin = local(container, 0.0, 0.0);
		Point horizontal = local(container, radius, 0.0);
		Point vertical = local(container, 0.0, radius);
		int rx = Math.abs(horizontal.x - origin.x);
		int ry = Math.abs(vertical.y - origin.y);
		if (rx < 1 || ry < 1) return;
		g.setColor(new Color(255, 0, 0, 64));
		g.fillOval(center.x - rx, center.y - ry, 2 * rx, 2 * ry);
	}

	private void drawCalorimeterRaw(Graphics2D g) {
		if (pcalData.maximumAdc() > 0) {
			for (PCalEventData.AdcHit hit : pcalData.adcHits()) {
				if (!displayedSector(hit.sector()) || hit.adc() <= 0) continue;
				fillCalorimeter(g, new CalorimeterCell(PCAL, hit.sector(), 0, hit.view(), hit.strip()),
						ScientificColorMap.TURBO.colorAt((double) hit.adc() / pcalData.maximumAdc()));
			}
		}
		if (ecalData.maximumAdc() > 0) {
			for (ECalEventData.AdcHit hit : ecalData.adcHits()) {
				if (!displayedSector(hit.sector()) || hit.adc() <= 0) continue;
				fillCalorimeter(g,
						new CalorimeterCell(ECAL, hit.sector(), hit.plane(), hit.view(), hit.strip()),
						ScientificColorMap.TURBO.colorAt((double) hit.adc() / ecalData.maximumAdc()));
			}
		}
	}

	private void fillCalorimeter(Graphics2D g, CalorimeterCell cell, Color color) {
		Polygon polygon = calorimeterStrips.get(cell);
		if (polygon == null) return;
		Color translucent = new Color(color.getRed(), color.getGreen(), color.getBlue(), 175);
		g.setColor(translucent);
		g.fillPolygon(polygon);
		g.setColor(color.darker());
		g.drawPolygon(polygon);
	}

	private Point projectedPoint(IContainer container, int sector,
			double x, double y, double z) {
		Point2D.Double world = SectorProjection.labPoint(new Point3(x, y, z), sector,
				phiOffsetDegrees);
		return local(container, world.x, world.y);
	}

	/**
	 * Like {@link #projectedPoint}, but for genuine lab-frame ("CLAS") data
	 * such as a swum particle trajectory -- see {@link
	 * SectorProjection#clasPoint} for the rotation this applies that {@link
	 * #projectedPoint} does not. Calorimeter/Cherenkov hits still go through
	 * {@link #projectedPoint}: their existing on-screen placement already
	 * matches truth, so this was deliberately left untouched rather than
	 * changed on the same unverified assumption that broke the particle
	 * trajectory.
	 */
	private Point projectedClasPoint(IContainer container, int sector,
			double x, double y, double z) {
		Point2D.Double world = SectorProjection.clasPoint(new Point3(x, y, z), sector,
				phiOffsetDegrees);
		return local(container, world.x, world.y);
	}

	private static void drawCalorimeterMarker(Graphics2D g, Point point,
			Color fill, Color line) {
		g.setColor(fill);
		g.fillOval(point.x - 5, point.y - 5, 11, 11);
		g.setColor(line);
		g.setStroke(new BasicStroke(1.5f));
		g.drawOval(point.x - 5, point.y - 5, 11, 11);
		g.drawLine(point.x - 7, point.y, point.x + 7, point.y);
		g.drawLine(point.x, point.y - 7, point.x, point.y + 7);
	}

	/**
	 * Fallback straight-line direction-stub length, in lab-frame cm, used
	 * only when swimming doesn't produce a usable trajectory (e.g. momentum
	 * below the swimmer's internal threshold, or the integration failed).
	 */
	private static final double PARTICLE_STUB_LENGTH_CM = 40.0;

	private void drawParticles(Graphics2D g, IContainer container) {
		if (!isDisplayed(CedDisplayOption.PARTICLES)) return;
		for (RecEventData.Particle particle : recData.particles()) {
			// The sector is fixed once, from the particle's starting momentum
			// direction, and every swum point is projected using that same
			// sector's midplane. A track that curls enough to actually cross
			// into a neighbouring sector will look somewhat approximate past
			// that point -- this view's projection convention (like every
			// other detector overlay here) is inherently per-sector.
			int sector = particle.sector();
			if (!displayedSector(sector)) continue;

			List<Point3> swum = swimCache.trajectory(particle, fieldProbe);
			List<Point> points = swum.size() >= 2
					? projectTrajectory(container, sector, swum)
					: stubTrajectory(container, sector, particle);
			if (points.size() < 2) continue;

			Color color = CedDrawingStyle.particleColor(particle.pid(), particle.charge());
			drawParticleTrajectory(g, points, color);
			screenParticles.add(new ScreenParticle(particle, points));
		}
	}

	private List<Point> projectTrajectory(IContainer container, int sector, List<Point3> world) {
		List<Point> screen = new ArrayList<>(world.size());
		for (Point3 point : world) {
			screen.add(projectedClasPoint(container, sector, point.x(), point.y(), point.z()));
		}
		return screen;
	}

	private List<Point> stubTrajectory(IContainer container, int sector, RecEventData.Particle particle) {
		float p = particle.p();
		if (!(p > 0f)) return List.of();
		double scale = PARTICLE_STUB_LENGTH_CM / p;
		Point vertex = projectedClasPoint(container, sector, particle.vx(), particle.vy(), particle.vz());
		Point tip = projectedClasPoint(container, sector,
				particle.vx() + scale * particle.px(),
				particle.vy() + scale * particle.py(),
				particle.vz() + scale * particle.pz());
		return List.of(vertex, tip);
	}

	private static void drawParticleTrajectory(Graphics2D g, List<Point> points, Color color) {
		g.setColor(color);
		g.setStroke(new BasicStroke(2f));
		for (int i = 1; i < points.size(); i++) {
			Point a = points.get(i - 1);
			Point b = points.get(i);
			g.drawLine(a.x, a.y, b.x, b.y);
		}
		Point vertex = points.get(0);
		g.setColor(color);
		g.fillOval(vertex.x - 3, vertex.y - 3, 6, 6);
		g.setColor(CedDrawingStyle.outline(color));
		g.drawOval(vertex.x - 3, vertex.y - 3, 6, 6);
	}

	private void drawFTOFEvent(Graphics2D g, IContainer container) {
		if (isDisplayed(CedDisplayOption.RAW_DATA) && ftofData.maximumAdc() > 0) {
			Map<FTOFCell, Integer> maximumByPaddle = new HashMap<>();
			for (AdcHit hit : ftofData.adcHits()) {
				if (!displayedSector(hit.sector())) continue;
				FTOFCell cell = new FTOFCell(hit.sector(), hit.panel(), hit.paddle());
				maximumByPaddle.merge(cell, hit.adc(), Math::max);
			}
			for (Map.Entry<FTOFCell, Integer> entry : maximumByPaddle.entrySet()) {
				double fraction = (double) entry.getValue() / ftofData.maximumAdc();
				fillFTOF(g, entry.getKey(), ScientificColorMap.TURBO.colorAt(fraction));
			}
		}

		if (isDisplayed(CedDisplayOption.RECON_HITS)) {
			for (FTOFEventData.ReconHit hit : ftofData.reconHits()) {
				if (!displayedSector(hit.sector())) continue;
				Point point = projectedFTOFPoint(container, hit.sector(), hit.x(), hit.y(), hit.z());
				ftofHitLocations.put(hit, point);
				g.setColor(Color.CYAN);
				g.fillOval(point.x - 5, point.y - 5, 11, 11);
				g.setColor(Color.RED);
				g.setStroke(new BasicStroke(1.5f));
				g.drawOval(point.x - 5, point.y - 5, 11, 11);
				g.drawLine(point.x - 7, point.y, point.x + 7, point.y);
				g.drawLine(point.x, point.y - 7, point.x, point.y + 7);
			}
		}

		if (isDisplayed(CedDisplayOption.CLUSTERS)) {
			g.setColor(Color.MAGENTA);
			g.setStroke(new BasicStroke(2f));
			for (FTOFEventData.Cluster cluster : ftofData.clusters()) {
				if (!displayedSector(cluster.sector())) continue;
				Point point = projectedFTOFPoint(container, cluster.sector(), cluster.x(),
						cluster.y(), cluster.z());
				ftofClusterLocations.put(cluster, point);
				g.drawLine(point.x - 6, point.y, point.x + 6, point.y);
				g.drawLine(point.x, point.y - 6, point.x, point.y + 6);
			}
		}
	}

	private Point projectedFTOFPoint(IContainer container, int sector,
			double x, double y, double z) {
		Point2D.Double world = SectorProjection.labPoint(new Point3(x, y, z), sector,
				phiOffsetDegrees);
		return local(container, world.x, world.y);
	}

	private void fillFTOF(Graphics2D g, FTOFCell cell, Color color) {
		Polygon polygon = ftofPaddles.get(cell);
		if (polygon == null) return;
		g.setColor(color);
		g.fillPolygon(polygon);
		g.setColor(color.darker());
		g.drawPolygon(polygon);
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
		drawCalorimeterAccumulation(g);
		drawCherenkovAccumulation(g, "HTCC", htccAccumulation, HTCCGeometry.RING_COUNT);
		drawCherenkovAccumulation(g, "LTCC", ltccAccumulation, LTCCGeometry.RING_COUNT);
	}

	private void drawCherenkovAccumulation(Graphics2D g, String detector,
			CherenkovAccumulation detectorAccumulation, int ringCount) {
		int maximum = detectorAccumulation.maximumCount();
		if (maximum <= 0) return;
		for (int sector : new int[] {pair.upper, pair.lower})
			for (int half = 1; half <= 2; half++)
				for (int ring = 1; ring <= ringCount; ring++) {
					int count = detectorAccumulation.count(sector, half, ring);
					if (count > 0) fillCherenkov(g,
							new CherenkovCell(detector, sector, half, ring),
							ScientificColorMap.TURBO.colorAt((double) count / maximum));
				}
	}

	private void drawCalorimeterAccumulation(Graphics2D g) {
		int pcalMaximum = pcalAccumulation.maximumCount();
		if (pcalMaximum > 0) {
			for (int sector : new int[] {pair.upper, pair.lower})
				for (int view = 0; view < PCALGeometry.VIEW_COUNT; view++)
					for (int strip = 1; strip <= PCALGeometry.STRIP_COUNTS[view]; strip++) {
						int count = pcalAccumulation.count(sector, view, strip);
						if (count > 0) fillCalorimeter(g,
								new CalorimeterCell(PCAL, sector, 0, view, strip),
								ScientificColorMap.TURBO.colorAt((double) count / pcalMaximum));
					}
		}
		for (int stack = 0; stack < ECGeometry.STACK_COUNT; stack++) {
			int maximum = ecalAccumulation.maximumCount(stack);
			if (maximum == 0) continue;
			for (int sector : new int[] {pair.upper, pair.lower})
				for (int view = 0; view < ECGeometry.VIEW_COUNT; view++)
					for (int strip = 1; strip <= ECGeometry.STRIP_COUNT; strip++) {
						int count = ecalAccumulation.count(sector, stack, view, strip);
						if (count > 0) fillCalorimeter(g,
								new CalorimeterCell(ECAL, sector, stack, view, strip),
								ScientificColorMap.TURBO.colorAt((double) count / maximum));
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

	/** @return {@code true} if {@code screenPoint} lies within {@code tolerance} pixels of any segment of {@code points} */
	private static boolean nearAnySegment(List<Point> points, Point screenPoint, double tolerance) {
		for (int i = 1; i < points.size(); i++) {
			Point a = points.get(i - 1);
			Point b = points.get(i);
			if (Line2D.ptSegDist(a.x, a.y, b.x, b.y, screenPoint.x, screenPoint.y) <= tolerance) {
				return true;
			}
		}
		return false;
	}

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
		for (ScreenParticle drawn : screenParticles) {
			if (nearAnySegment(drawn.points, screenPoint, 5.0)) {
				RecEventData.Particle particle = drawn.particle;
				feedback.add(String.format("$blue$%s (pid %d, q=%+d) sector %d", particle.displayName(),
						particle.pid(), particle.charge(), particle.sector()));
				feedback.add(String.format("$blue$p = %.3f GeV/c  theta = %.1f°  phi = %.1f°",
						particle.p(), Math.toDegrees(particle.theta()), Math.toDegrees(particle.phi())));
				feedback.add(String.format("$blue$vertex (%.2f, %.2f, %.2f) cm",
						particle.vx(), particle.vy(), particle.vz()));
				if (particle.beta() != 0f || particle.chi2pid() != 0f) {
					feedback.add(String.format("$blue$beta = %.3f  chi2pid = %.2f",
							particle.beta(), particle.chi2pid()));
				}
				break;
			}
		}
		for (Map.Entry<FTOFEventData.Cluster, Point> entry : ftofClusterLocations.entrySet()) {
			if (entry.getValue().distance(screenPoint) <= 8.0) {
				FTOFEventData.Cluster cluster = entry.getKey();
				feedback.add(String.format("$magenta$FTOF cluster xyz (%.2f, %.2f, %.2f) cm",
						cluster.x(), cluster.y(), cluster.z()));
				feedback.add(String.format("$magenta$FTOF cluster energy %.4f GeV",
						cluster.energy()));
				feedback.add(String.format("$magenta$FTOF cluster ID %d status %d",
						cluster.id(), cluster.status()));
				return;
			}
		}
		for (Map.Entry<FTOFEventData.ReconHit, Point> entry : ftofHitLocations.entrySet()) {
			if (entry.getValue().distance(screenPoint) <= 8.0) {
				FTOFEventData.ReconHit hit = entry.getKey();
				feedback.add(String.format("$cyan$FTOF hit xyz (%.2f, %.2f, %.2f) cm",
						hit.x(), hit.y(), hit.z()));
				feedback.add(String.format("$cyan$FTOF hit ID %d energy %.4f GeV time %.3f",
						hit.id(), hit.energy(), hit.time()));
				return;
			}
		}
		for (Map.Entry<PCalEventData.ReconHit, Point> entry : pcalHitLocations.entrySet()) {
			if (entry.getValue().distance(screenPoint) <= 8.0) {
				PCalEventData.ReconHit hit = entry.getKey();
				feedback.add(String.format("$cyan$PCAL recon sector %d %s", hit.sector(),
						CALORIMETER_VIEWS[hit.view()]));
				feedback.add(String.format("$cyan$PCAL xyz (%.2f, %.2f, %.2f) cm",
						hit.x(), hit.y(), hit.z()));
				feedback.add(String.format("$cyan$PCAL energy %.4f GeV time %.3f",
						hit.energy(), hit.time()));
				return;
			}
		}
		for (Map.Entry<ECalEventData.ReconHit, Point> entry : ecalHitLocations.entrySet()) {
			if (entry.getValue().distance(screenPoint) <= 8.0) {
				ECalEventData.ReconHit hit = entry.getKey();
				feedback.add(String.format("$orange$ECAL %s sector %d %s",
						ECAL_PLANES[hit.plane()], hit.sector(), CALORIMETER_VIEWS[hit.view()]));
				feedback.add(String.format("$orange$ECAL xyz (%.2f, %.2f, %.2f) cm",
						hit.x(), hit.y(), hit.z()));
				feedback.add(String.format("$orange$ECAL energy %.4f GeV time %.3f",
						hit.energy(), hit.time()));
				return;
			}
		}
		for (Map.Entry<CherenkovMarker, Point> entry : cherenkovMarkers.entrySet()) {
			if (entry.getValue().distance(screenPoint) <= 8.0) {
				CherenkovMarker marker = entry.getKey();
				CherenkovEventData.ReconHit hit = marker.hit();
				feedback.add(String.format("$orange$%s rec ID %d sector %d",
						marker.detector(), hit.id(), hit.sector()));
				feedback.add(String.format("$orange$%s xyz (%.2f, %.2f, %.2f) cm",
						marker.detector(), hit.x(), hit.y(), hit.z()));
				return;
			}
		}
		CherenkovCell cherenkovCell = cherenkovCells.entrySet().stream()
				.filter(e -> e.getValue().contains(screenPoint)).map(Map.Entry::getKey)
				.findFirst().orElse(null);
		if (cherenkovCell != null) {
			addCherenkovFeedback(cherenkovCell, feedback);
			return;
		}
		FTOFCell ftofCell = ftofPaddles.entrySet().stream()
				.filter(e -> e.getValue().contains(screenPoint)).map(Map.Entry::getKey)
				.findFirst().orElse(null);
		if (ftofCell != null) {
			feedback.add(String.format("$cyan$FTOF %s sector %d paddle %d",
					FTOF_PANEL_NAMES[ftofCell.panel], ftofCell.sector, ftofCell.paddle));
			if (!isDisplayed(CedDisplayOption.ACCUMULATION)) {
				for (AdcHit hit : ftofData.adcHits()) {
					if (ftofCell.matches(hit)) feedback.add(String.format(
							"$cyan$FTOF adc %d time %.3f order %d",
							hit.adc(), hit.time(), hit.order()));
				}
			}
			return;
		}
		CalorimeterCell calorimeterCell = calorimeterStrips.entrySet().stream()
				.filter(e -> e.getValue().contains(screenPoint)).map(Map.Entry::getKey)
				.findFirst().orElse(null);
		if (calorimeterCell != null) {
			addCalorimeterFeedback(calorimeterCell, feedback);
			return;
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

	private void addCherenkovFeedback(CherenkovCell cell, List<String> feedback) {
		feedback.add(String.format("$cyan$%s sector %d half %d ring %d",
				cell.detector(), cell.sector(), cell.half(), cell.ring()));
		CherenkovAccumulation detectorAccumulation = "HTCC".equals(cell.detector())
				? htccAccumulation : ltccAccumulation;
		if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
			feedback.add(String.format("$cyan$occupancy %d / %d events",
					detectorAccumulation.count(cell.sector(), cell.half(), cell.ring()),
					detectorAccumulation.eventCount()));
			return;
		}
		CherenkovEventData event = "HTCC".equals(cell.detector()) ? htccData : ltccData;
		for (CherenkovEventData.AdcHit hit : event.adcHits()) if (cell.matches(hit))
			feedback.add(String.format("$cyan$%s adc %d time %.3f order %d",
					cell.detector(), hit.adc(), hit.time(), hit.order()));
		for (CherenkovEventData.TdcHit hit : event.tdcHits()) if (cell.matches(hit))
			feedback.add(String.format("$cyan$%s tdc %d order %d",
					cell.detector(), hit.tdc(), hit.order()));
	}

	private void addCalorimeterFeedback(CalorimeterCell cell, List<String> feedback) {
		String view = CALORIMETER_VIEWS[cell.view()];
		if (cell.detector() == PCAL) {
			feedback.add(String.format("$cyan$PCAL sector %d %s strip %d",
					cell.sector(), view, cell.strip()));
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
				feedback.add(String.format("$cyan$occupancy %d / %d events",
						pcalAccumulation.count(cell.sector(), cell.view(), cell.strip()),
						pcalAccumulation.eventCount()));
			} else {
				for (PCalEventData.AdcHit hit : pcalData.adcHits()) if (cell.matches(hit))
					feedback.add(String.format("$cyan$PCAL adc %d time %.3f", hit.adc(), hit.time()));
			}
		} else {
			feedback.add(String.format("$orange$ECAL %s sector %d %s strip %d",
					ECAL_PLANES[cell.stack()], cell.sector(), view, cell.strip()));
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
				feedback.add(String.format("$orange$occupancy %d / %d events",
						ecalAccumulation.count(cell.sector(), cell.stack(), cell.view(), cell.strip()),
						ecalAccumulation.eventCount()));
			} else {
				for (ECalEventData.AdcHit hit : ecalData.adcHits()) if (cell.matches(hit))
					feedback.add(String.format("$orange$ECAL adc %d time %.3f", hit.adc(), hit.time()));
			}
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
	private record FTOFCell(int sector, int panel, int paddle) {
		boolean matches(AdcHit hit) { return sector == hit.sector() && panel == hit.panel()
				&& paddle == hit.paddle(); }
	}
	private record CalorimeterCell(int detector, int sector, int stack, int view, int strip) {
		boolean matches(PCalEventData.AdcHit hit) {
			return detector == PCAL && sector == hit.sector() && view == hit.view()
					&& strip == hit.strip();
		}
		boolean matches(ECalEventData.AdcHit hit) {
			return detector == ECAL && sector == hit.sector() && stack == hit.plane()
					&& view == hit.view() && strip == hit.strip();
		}
	}
	private record CherenkovCell(String detector, int sector, int half, int ring) {
		boolean matches(CherenkovEventData.AdcHit hit) {
			return sector == hit.sector() && half == hit.half() && ring == hit.ring();
		}
		boolean matches(CherenkovEventData.TdcHit hit) {
			return sector == hit.sector() && half == hit.half() && ring == hit.ring();
		}
	}
	private record CherenkovMarker(String detector, CherenkovEventData.ReconHit hit) { }

	private record ScreenSegment(Segment segment, Point start, Point end) { }
	private record ScreenCross(Cross cross, Point location) { }
	private record ScreenParticle(RecEventData.Particle particle, List<Point> points) { }

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
