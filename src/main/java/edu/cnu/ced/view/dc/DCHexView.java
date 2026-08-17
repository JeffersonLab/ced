package edu.cnu.ced.view.dc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.DCAccumulation;
import edu.cnu.ced.data.DCEventData;
import edu.cnu.ced.data.DCEventData.RawHit;
import edu.cnu.ced.data.DCEventData.ReconHit;
import edu.cnu.ced.data.DCEventData.ReconKind;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.ced.view.CedView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.PropertyUtils;

/** Radial schematic of every drift-chamber wire cell. */
@SuppressWarnings("serial")
public final class DCHexView extends CedView {

	private static final int SECTORS = 6;
	private static final int SUPERLAYERS = 6;
	private static final int LAYERS = 6;
	private static final int WIRES = 112;
	private static final double TAN_30 = 1.0 / Math.sqrt(3.0);
	private static final double[] RADII = {129, 169, 214, 260, 311, 365};
	private static final double[] THICKNESSES = {36, 36, 42, 42, 50, 50};
	private static final double COLOR_CEILING_PERCENTILE = 0.95;
	private static final Color BACKGROUND = new Color(70, 96, 96);
	private static final Color[] LAYER_COLORS = {
			new Color(240, 255, 255), new Color(240, 248, 255)};
	private static final Color CELL_LINE = new Color(235, 210, 210);

	private final DCAccumulation accumulation;
	private final Map<Cell, Polygon> screenCells = new HashMap<>();
	private volatile DCEventData data = DCEventData.from(null);

	public DCHexView(EventNavigator navigator, DCAccumulation accumulation) {
		super(navigator, PropertyUtils.TITLE, "DC Hex", PropertyUtils.WIDTH, 820,
				PropertyUtils.HEIGHT, 790, PropertyUtils.WORLDSYSTEM,
				new Rectangle2D.Double(430, -495, -860, 990), PropertyUtils.BACKGROUND,
				BACKGROUND, PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.accumulation = accumulation;
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT,
				CedDisplayOption.ACCUMULATION, CedDisplayOption.RAW_DATA,
				CedDisplayOption.HB_HITS, CedDisplayOption.TB_HITS,
				CedDisplayOption.AI_HB_HITS, CedDisplayOption.AI_TB_HITS),
				List.of("DC::", "HitBasedTrkg::", "TimeBasedTrkg::"),
				ScientificColorMap.TURBO, "Relative occupancy (95th-percentile ceiling)");
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		data = DCEventData.from(state.snapshot());
	}

	private void draw(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			screenCells.clear();
			drawFramework(g, container);
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) drawAccumulation(g, container);
			else {
				if (isDisplayed(CedDisplayOption.RAW_DATA)) drawRaw(g, container);
				drawRecon(g, container);
			}
			drawSectorNumbers(g, container);
		} finally {
			g.dispose();
		}
	}

	private void drawFramework(Graphics2D g, IContainer container) {
		for (int sector = 1; sector <= SECTORS; sector++)
			for (int superlayer = 1; superlayer <= SUPERLAYERS; superlayer++)
				for (int layer = 1; layer <= LAYERS; layer++) {
					Polygon polygon = screenPolygon(container,
							layerPolygon(sector, superlayer, layer));
					g.setColor(LAYER_COLORS[layer & 1]);
					g.fillPolygon(polygon);
					g.setColor(CELL_LINE);
					g.drawPolygon(polygon);
				}
	}

	private void drawRaw(Graphics2D g, IContainer container) {
		for (RawHit hit : data.rawHits()) fillCell(g, container,
				new Cell(hit.sector(), hit.superlayer(), hit.layer(), hit.wire()),
				CedDrawingStyle.RAW_HIT);
	}

	private void drawRecon(Graphics2D g, IContainer container) {
		for (ReconHit hit : data.reconHits()) if (show(hit.kind()))
			fillCell(g, container, new Cell(hit.sector(), hit.superlayer(), hit.layer(), hit.wire()),
					CedDrawingStyle.reconstructionColor(hit.kind()));
	}

	private void drawAccumulation(Graphics2D g, IContainer container) {
		for (int superlayer = 1; superlayer <= SUPERLAYERS; superlayer++) {
			int ceiling = accumulation.percentileCount(superlayer,
					COLOR_CEILING_PERCENTILE);
			if (ceiling == 0) continue;
			for (int sector = 1; sector <= SECTORS; sector++)
				for (int layer = 1; layer <= LAYERS; layer++)
					for (int wire = 1; wire <= WIRES; wire++) {
						int count = accumulation.count(sector, superlayer, layer, wire);
						if (count > 0) fillCell(g, container,
								new Cell(sector, superlayer, layer, wire),
								ScientificColorMap.TURBO.colorAt(Math.min(1.0,
										(double) count / ceiling)));
					}
		}
	}

	private void fillCell(Graphics2D g, IContainer container, Cell cell, Color color) {
		Polygon polygon = screenCells.computeIfAbsent(cell, ignored -> screenPolygon(container,
				wirePolygon(cell.sector, cell.superlayer, cell.layer, cell.wire)));
		g.setColor(color);
		g.fillPolygon(polygon);
		g.setColor(color.darker());
		g.setStroke(new BasicStroke(1f));
		g.drawPolygon(polygon);
	}

	private void drawSectorNumbers(Graphics2D g, IContainer container) {
		g.setFont(Fonts.defaultBoldFont);
		g.setColor(new Color(35, 220, 220, 135));
		for (int sector = 1; sector <= SECTORS; sector++) {
			double angle = Math.toRadians(60 * (sector - 1));
			Point point = local(container, 78 * Math.cos(angle), 78 * Math.sin(angle));
			String text = Integer.toString(sector);
			g.drawString(text, point.x - g.getFontMetrics().stringWidth(text) / 2,
					point.y + g.getFontMetrics().getAscent() / 2);
		}
	}

	private boolean show(ReconKind kind) {
		return switch (kind) {
			case HB -> isDisplayed(CedDisplayOption.HB_HITS);
			case TB -> isDisplayed(CedDisplayOption.TB_HITS);
			case AI_HB -> isDisplayed(CedDisplayOption.AI_HB_HITS);
			case AI_TB -> isDisplayed(CedDisplayOption.AI_TB_HITS);
		};
	}

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint,
			Point2D.Double worldPoint, List<String> feedback) {
		Cell cell = screenCells.entrySet().stream()
				.filter(entry -> entry.getValue().contains(screenPoint))
				.map(Map.Entry::getKey).findFirst().orElse(null);
		if (cell == null) cell = findCell(worldPoint);
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

	static Cell findCell(Point2D.Double worldPoint) {
		for (int sector = 1; sector <= SECTORS; sector++)
			for (int superlayer = 1; superlayer <= SUPERLAYERS; superlayer++)
				for (int layer = 1; layer <= LAYERS; layer++) {
					if (!contains(layerPolygon(sector, superlayer, layer), worldPoint)) continue;
					for (int wire = 1; wire <= WIRES; wire++)
						if (contains(wirePolygon(sector, superlayer, layer, wire), worldPoint))
							return new Cell(sector, superlayer, layer, wire);
					return null;
				}
		return null;
	}

	private static boolean contains(Point2D.Double[] polygon, Point2D point) {
		Path2D.Double path = new Path2D.Double();
		path.moveTo(polygon[0].x, polygon[0].y);
		for (int index = 1; index < polygon.length; index++)
			path.lineTo(polygon[index].x, polygon[index].y);
		path.closePath();
		return path.contains(point);
	}

	static Point2D.Double[] wirePolygon(int sector, int superlayer, int layer, int wire) {
		if (sector < 1 || sector > SECTORS || superlayer < 1 || superlayer > SUPERLAYERS
				|| layer < 1 || layer > LAYERS || wire < 1 || wire > WIRES)
			throw new IllegalArgumentException("Invalid DC wire address");
		Point2D.Double[] layerPoints = layerPolygon(sector, superlayer, layer);
		double first = (wire - 1.0) / WIRES;
		double second = wire / (double) WIRES;
		return new Point2D.Double[] {interpolate(layerPoints[1], layerPoints[2], first),
				interpolate(layerPoints[1], layerPoints[2], second),
				interpolate(layerPoints[0], layerPoints[3], second),
				interpolate(layerPoints[0], layerPoints[3], first)};
	}

	private static Point2D.Double[] layerPolygon(int sector, int superlayer, int layer) {
		Point2D.Double[] shell = shell(sector, superlayer);
		double first = (layer - 1.0) / LAYERS;
		double second = layer / (double) LAYERS;
		return new Point2D.Double[] {interpolate(shell[0], shell[1], first),
				interpolate(shell[0], shell[1], second),
				interpolate(shell[3], shell[2], second),
				interpolate(shell[3], shell[2], first)};
	}

	private static Point2D.Double[] shell(int sector, int superlayer) {
		double radius = RADII[superlayer - 1];
		double outer = radius + THICKNESSES[superlayer - 1];
		double shrink = 0.95 + 0.005 * (superlayer - 1);
		Point2D.Double[] points = {
				new Point2D.Double(-TAN_30 * radius * shrink, radius),
				new Point2D.Double(-TAN_30 * outer * shrink, outer),
				new Point2D.Double(TAN_30 * outer * shrink, outer),
				new Point2D.Double(TAN_30 * radius * shrink, radius)};
		double angle = Math.toRadians(-90 + 60 * (sector - 1));
		for (int index = 0; index < points.length; index++) points[index] = rotate(points[index], angle);
		return points;
	}

	private static Point2D.Double interpolate(Point2D.Double a, Point2D.Double b, double fraction) {
		return new Point2D.Double(a.x + fraction * (b.x - a.x),
				a.y + fraction * (b.y - a.y));
	}

	private static Point2D.Double rotate(Point2D.Double point, double angle) {
		double cosine = Math.cos(angle), sine = Math.sin(angle);
		return new Point2D.Double(cosine * point.x - sine * point.y,
				sine * point.x + cosine * point.y);
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

	record Cell(int sector, int superlayer, int layer, int wire) {
		boolean matches(RawHit hit) { return sector == hit.sector() && superlayer == hit.superlayer()
				&& layer == hit.layer() && wire == hit.wire(); }
		boolean matches(ReconHit hit) { return sector == hit.sector() && superlayer == hit.superlayer()
				&& layer == hit.layer() && wire == hit.wire(); }
	}
}
