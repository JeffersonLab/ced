package edu.cnu.ced.view.dc;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
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
import edu.cnu.ced.geometry.DCGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.view.CedView;
import edu.cnu.mdi.component.AspectRatioPanel;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.PropertyUtils;

/** Compact, geometrically schematic display of all six drift-chamber sectors. */
@SuppressWarnings("serial")
public final class AllDCView extends CedView {

	private static final int SECTORS = 6;
	private static final int SUPERLAYERS = 6;
	private static final int LAYERS = 6;
	private static final int WIRES = 112;
	private static final double COLOR_CEILING_PERCENTILE = 0.95;
	private static final double SUPER_WIDTH = .92;
	private static final double X_MARGIN = .04;
	private static final double BOTTOM_MARGIN = .03;
	private static final double TOP_MARGIN = .06;
	private static final double SMALL_GAP = .02;
	private static final double REGION_GAP = .04;
	private static final double SUPER_HEIGHT = (1 - BOTTOM_MARGIN - TOP_MARGIN
			- 3 * SMALL_GAP - 2 * REGION_GAP) / SUPERLAYERS;
	private static final Color SECTOR_FILL = new Color(47, 79, 79);
	private static final Color SHELL = new Color(245, 247, 247);
	private static final Color LAYER_SHADE = new Color(155, 155, 155, 48);
	private static final Color RAW = new Color(225, 35, 25);
	private static final Map<ReconKind, Color> RECON_COLORS = Map.of(
			ReconKind.HB, new Color(255, 190, 0), ReconKind.TB, new Color(20, 155, 235),
			ReconKind.AI_HB, new Color(255, 80, 190), ReconKind.AI_TB, new Color(50, 180, 75));

	private final DCGeometry geometry;
	private final DCAccumulation accumulation;
	private final Map<Cell, Rectangle> cells = new HashMap<>();
	private volatile DCEventData data = DCEventData.from(null);

	public AllDCView(DCGeometry geometry, EventNavigator navigator,
			DCAccumulation accumulation) {
		super(navigator, PropertyUtils.TITLE, "All Drift Chambers", PropertyUtils.WIDTH, 1120,
				PropertyUtils.HEIGHT, 760, PropertyUtils.WORLDSYSTEM,
				new Rectangle2D.Double(0, 0, 3, 2), PropertyUtils.BACKGROUND,
				Color.WHITE, PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.accumulation = accumulation;
		installAspectRatioCanvas(1.5);
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT,
				CedDisplayOption.ACCUMULATION, CedDisplayOption.RAW_DATA,
				CedDisplayOption.HB_HITS, CedDisplayOption.TB_HITS,
				CedDisplayOption.AI_HB_HITS, CedDisplayOption.AI_TB_HITS),
				List.of("DC::", "HitBasedTrkg::", "TimeBasedTrkg::"),
				ScientificColorMap.TURBO, "Relative occupancy (95th-percentile ceiling)");
	}

	private void installAspectRatioCanvas(double ratio) {
		IContainer drawingContainer = getIContainer();
		if (drawingContainer == null) return;
		Component canvas = drawingContainer.getComponent();
		Container parent = canvas == null ? null : canvas.getParent();
		if (parent == null || !(parent.getLayout() instanceof BorderLayout)) return;
		parent.remove(canvas);
		parent.add(new AspectRatioPanel(canvas, ratio), BorderLayout.CENTER);
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
			cells.clear();
			drawFramework(g, container);
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) drawAccumulation(g, container);
			else {
				if (isDisplayed(CedDisplayOption.RAW_DATA)) drawRaw(g, container);
				drawRecon(g, container);
			}
		} finally {
			g.dispose();
		}
	}

	private void drawFramework(Graphics2D g, IContainer container) {
		g.setFont(Fonts.mediumFont);
		FontMetrics fm = g.getFontMetrics();
		for (int sector = 1; sector <= SECTORS; sector++) {
			Rectangle sectorRect = screenRect(container, (sector - 1) % 3,
					sector <= 3 ? 1 : 0, 1, 1);
			g.setColor(SECTOR_FILL);
			g.fillRect(sectorRect.x, sectorRect.y, sectorRect.width, sectorRect.height);
			g.setColor(Color.LIGHT_GRAY);
			g.drawRect(sectorRect.x, sectorRect.y, sectorRect.width, sectorRect.height);
			g.setColor(new Color(255, 110, 80));
			g.drawString("Sector " + sector, sectorRect.x + 8,
					sectorRect.y + fm.getAscent() + 5);
			for (int superlayer = 1; superlayer <= SUPERLAYERS; superlayer++) {
				Rectangle shell = cellRect(container, sector, superlayer, 1, 1, true);
				g.setColor(SHELL);
				g.fillRect(shell.x, shell.y, shell.width, shell.height);
				for (int layer = 1; layer <= LAYERS; layer += 2) {
					Rectangle stripe = layerRect(container, sector, superlayer, layer);
					g.setColor(LAYER_SHADE);
					g.fillRect(stripe.x, stripe.y, stripe.width, stripe.height);
				}
				g.setColor(Color.BLACK);
				g.drawRect(shell.x, shell.y, shell.width, shell.height);
				g.setColor(Color.CYAN.darker());
				String label = Integer.toString(superlayer);
				g.drawString(label, shell.x - fm.stringWidth(label) - 3,
						shell.y + (shell.height + fm.getAscent() - fm.getDescent()) / 2);
				for (int layer = 1; layer <= LAYERS; layer++) for (int wire = 1; wire <= WIRES; wire++)
					cells.put(new Cell(sector, superlayer, layer, wire),
							cellRect(container, sector, superlayer, layer, wire, false));
			}
		}
	}

	private void drawRaw(Graphics2D g, IContainer container) {
		for (RawHit hit : data.rawHits()) fillCell(g, container,
				new Cell(hit.sector(), hit.superlayer(), hit.layer(), hit.wire()), RAW, 0);
	}

	private void drawRecon(Graphics2D g, IContainer container) {
		for (ReconHit hit : data.reconHits()) {
			if (!show(hit.kind())) continue;
			fillCell(g, container, new Cell(hit.sector(), hit.superlayer(), hit.layer(), hit.wire()),
					RECON_COLORS.get(hit.kind()), 1);
		}
	}

	private void drawAccumulation(Graphics2D g, IContainer container) {
		for (int sector = 1; sector <= SECTORS; sector++)
			for (int superlayer = 1; superlayer <= SUPERLAYERS; superlayer++) {
				int ceiling = accumulation.percentileCount(superlayer,
						COLOR_CEILING_PERCENTILE);
				if (ceiling == 0) continue;
				for (int layer = 1; layer <= LAYERS; layer++)
					for (int wire = 1; wire <= WIRES; wire++) {
						int count = accumulation.count(sector, superlayer, layer, wire);
						if (count > 0) fillCell(g, container, new Cell(sector, superlayer, layer, wire),
								ScientificColorMap.TURBO.colorAt(Math.min(1.0,
										(double) count / ceiling)), 0);
					}
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

	private void fillCell(Graphics2D g, IContainer c, Cell key, Color color, int inset) {
		Rectangle cell = cells.computeIfAbsent(key,
				ignored -> cellRect(c, key.sector, key.superlayer, key.layer, key.wire, false));
		int x = cell.x + inset, y = cell.y + inset;
		int width = Math.max(1, cell.width - 2 * inset), height = Math.max(1, cell.height - 2 * inset);
		g.setColor(color);
		g.fillRect(x, y, width, height);
		g.setColor(color.darker());
		g.setStroke(new BasicStroke(1f));
		g.drawRect(x, y, width, height);
	}

	private static Rectangle layerRect(IContainer c, int sector, int superlayer, int layer) {
		double y = superlayerY(sector, superlayer);
		int row = sector <= 3 ? layer - 1 : LAYERS - layer;
		return screenRect(c, ((sector - 1) % 3) + X_MARGIN,
				y + row * SUPER_HEIGHT / LAYERS, SUPER_WIDTH, SUPER_HEIGHT / LAYERS);
	}

	private static Rectangle cellRect(IContainer c, int sector, int superlayer,
			int layer, int wire, boolean wholeSuperlayer) {
		double x = ((sector - 1) % 3) + X_MARGIN;
		double y = superlayerY(sector, superlayer);
		if (wholeSuperlayer) return screenRect(c, x, y, SUPER_WIDTH, SUPER_HEIGHT);
		int row = sector <= 3 ? layer - 1 : LAYERS - layer;
		double cellWidth = SUPER_WIDTH / WIRES;
		return screenRect(c, x + (WIRES - wire) * cellWidth,
				y + row * SUPER_HEIGHT / LAYERS, cellWidth, SUPER_HEIGHT / LAYERS);
	}

	private static double superlayerY(int sector, int superlayer) {
		int position = sector <= 3 ? superlayer - 1 : SUPERLAYERS - superlayer;
		double y = (sector <= 3 ? 1 : 0) + BOTTOM_MARGIN;
		for (int index = 0; index < position; index++)
			y += SUPER_HEIGHT + (index % 2 == 0 ? SMALL_GAP : REGION_GAP);
		return y;
	}

	private static Rectangle screenRect(IContainer c, double x, double y, double width, double height) {
		Point a = new Point(), b = new Point();
		c.worldToLocal(a, x, y);
		c.worldToLocal(b, x + width, y + height);
		return new Rectangle(Math.min(a.x, b.x), Math.min(a.y, b.y),
				Math.max(1, Math.abs(b.x - a.x)), Math.max(1, Math.abs(b.y - a.y)));
	}

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint,
			Point2D.Double worldPoint, List<String> feedback) {
		Cell cell = cells.entrySet().stream().filter(entry -> entry.getValue().contains(screenPoint))
				.map(Map.Entry::getKey).findFirst().orElse(null);
		if (cell == null) return;
		feedback.add(String.format("$cyan$DC sector %d superlayer %d layer %d wire %d",
				cell.sector, cell.superlayer, cell.layer, cell.wire));
		if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
			feedback.add(String.format("$cyan$occupancy %d / %d events",
					accumulation.count(cell.sector, cell.superlayer, cell.layer, cell.wire),
					accumulation.eventCount()));
			feedback.add(String.format("$cyan$superlayer color ceiling %d (95th percentile); true maximum %d",
					accumulation.percentileCount(cell.superlayer, COLOR_CEILING_PERCENTILE),
					accumulation.maximumCount(cell.superlayer)));
		}
		else {
			for (RawHit hit : data.rawHits()) if (cell.matches(hit))
				feedback.add(String.format("$orange$tdc %d order %d", hit.tdc(), hit.order()));
			for (ReconHit hit : data.reconHits()) if (show(hit.kind()) && cell.matches(hit)) {
				feedback.add(String.format("$red$%s hit id %d status %d cluster %d",
						hit.kind(), hit.id(), hit.status(), hit.clusterId()));
				if (!Float.isNaN(hit.trackDoca())) feedback.add(String.format(
						"$red$trkDoca %.3f cm", hit.trackDoca()));
			}
		}
		long sectorHits = data.rawHits().stream().filter(hit -> hit.sector() == cell.sector).count();
		feedback.add(String.format("$cyan$total DC occupancy %.2f%%  sector %d occupancy %.2f%%",
				100.0 * data.rawHits().size() / 24192.0, cell.sector,
				100.0 * sectorHits / 4032.0));
		var wireLine = geometry.wireLine(cell.sector, cell.superlayer, cell.layer,
				cell.wire);
		Point3 midpoint = new Point3((wireLine.start().x() + wireLine.end().x()) / 2,
				(wireLine.start().y() + wireLine.end().y()) / 2,
				(wireLine.start().z() + wireLine.end().z()) / 2);
		feedback.add(String.format("$cyan$wire midpoint (%.1f, %.1f, %.1f) cm",
				midpoint.x(), midpoint.y(), midpoint.z()));
	}

	private record Cell(int sector, int superlayer, int layer, int wire) {
		boolean matches(RawHit hit) { return sector == hit.sector() && superlayer == hit.superlayer()
				&& layer == hit.layer() && wire == hit.wire(); }
		boolean matches(ReconHit hit) { return sector == hit.sector() && superlayer == hit.superlayer()
				&& layer == hit.layer() && wire == hit.wire(); }
	}
}
