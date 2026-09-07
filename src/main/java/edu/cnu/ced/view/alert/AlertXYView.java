package edu.cnu.ced.view.alert;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.AlertAccumulation;
import edu.cnu.ced.data.AlertEventData;
import edu.cnu.ced.data.AlertEventData.DcAdcHit;
import edu.cnu.ced.data.AlertEventData.DcCluster;
import edu.cnu.ced.data.AlertEventData.DcHit;
import edu.cnu.ced.data.AlertEventData.TofCluster;
import edu.cnu.ced.data.AlertEventData.TofHit;
import edu.cnu.ced.data.CentralAccumulation;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.AlertGeometry;
import edu.cnu.ced.geometry.AlertGeometry.Paddle;
import edu.cnu.ced.geometry.CNDGeometry;
import edu.cnu.ced.geometry.CTOFGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.geometry.Segment3;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.view.central.CndCtofXYView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * ALERT (AHDC drift chamber + ATOF time-of-flight) laboratory XY display
 * backed directly by CLAS banks -- {@link CndCtofXYView} (CND + CTOF, shared
 * with {@link edu.cnu.ced.view.central.CentralXYView}) plus AHDC + ATOF, the
 * same default world system as Central XY, and the same
 * RECON_TRACKS/MC_TRACKS swim-and-draw pattern (but not CVT_TRACKS, which
 * stays Central-Detector-specific -- ALERT has no CVT detector; legacy's own
 * ALERT view keeping literal "CVTRec Trajectory" labels is an artifact of
 * copy-pasting CentralXYView's control panel wholesale, not something to
 * reproduce).
 * <p>
 * AHDC has one real sector (its wires are drawn colored by superlayer, 0-4).
 * ATOF has 15 real sectors; each (sector, superlayer, layer) triple's paddle
 * tiles together with its neighbors to fill out one sector's wedge (matching
 * CND/PCAL's own many-small-adjacent-polygon convention), confirmed
 * empirically: a "bar" (superlayer 0) layer's 4 components sit at the same
 * radius, 6 degrees apart in phi, while a "wedge" (superlayer 1) layer's own
 * 10 sub-components differ only in z (irrelevant for this transverse view,
 * so only the first is drawn). See {@link AlertEventData}'s class doc for
 * why ATOF hits/clusters are plotted at their own bank-given position rather
 * than resolved through this same geometry, and why accumulation only
 * covers AHDC wires.
 * </p>
 */
@SuppressWarnings("serial")
public final class AlertXYView extends CndCtofXYView {

	private static final Color[] SUPERLAYER_COLORS = {
			new Color(220, 20, 60), new Color(255, 140, 0), new Color(184, 134, 11),
			new Color(34, 139, 34), new Color(30, 144, 255)
	};
	// Superlayer 0 (the "bar" ring, one real paddle per sector/layer) is
	// always this one neutral, near-white fill, regardless of sector --
	// legacy's own fixed "superlayer0Color". Superlayer 1 (the 10 "wedge"
	// sub-components, artificially spread into concentric rings -- see
	// drawTofCells) alternates 3 background hues across the 15 sectors
	// (sector % 3) purely to help the eye group sectors, each hue in turn
	// alternating between two shades radially (paddle % 2) for the
	// "zebra stripe" look -- none of this carries physical meaning, it is
	// legacy's own deliberately non-geometric way to show more of the
	// {sector, superlayer, layer, paddle} address than a faithful 3D
	// projection could (real geometry has all 10 paddles of one
	// superlayer-1 layer sharing an identical x, y -- they differ only in
	// z, invisible to an XY view; see AlertGeometry's own scratch-verified
	// paddle dump).
	private static final Color TOF_SL0_FILL = new Color(240, 248, 255);
	private static final Color TOF_SL0_OUTLINE = new Color(190, 200, 215);
	private static final Color[][] TOF_SL1_FILL = {
			{ new Color(250, 235, 215), new Color(222, 184, 135, 150) }, // antique white / burlywood
			{ new Color(224, 255, 255), new Color(173, 216, 230, 150) }, // light cyan / light blue
			{ new Color(127, 255, 212), new Color(144, 238, 144, 150) }, // aquamarine / light green
	};
	private static final Color TOF_LABEL = new Color(40, 75, 145, 170);
	private static final Color TOF_HIT_FILL = new Color(220, 20, 20, 190);
	private static final Color RECON_COLOR = new Color(225, 35, 25);
	private static final Color CLUSTER_COLOR = new Color(205, 0, 205);
	private static final int MARKER = 4;
	private static final int TOF_ARC_STEPS = 6;

	private final AlertGeometry geometry;
	private final AlertAccumulation accumulation;
	// Screen-space endpoints for every drawn AHDC wire, cached from the draw
	// pass so getFeedbackStrings doesn't redo geometry lookups and
	// world-to-local transforms on every mouse move -- same reasoning as
	// FMTXYView/URWTXYView's identical caches.
	private final Map<WireAddress, Point[]> wireScreenPoints = new HashMap<>();
	private final Map<TofCell, Polygon> tofCells = new HashMap<>();
	private volatile AlertEventData eventData = AlertEventData.from(null);

	public AlertXYView(AlertGeometry geometry, CNDGeometry cnd, CTOFGeometry ctof, EventNavigator navigator,
			AlertAccumulation accumulation, CentralAccumulation centralAccumulation, SwimTrajectoryCache swimCache) {
		super(cnd, ctof, navigator, centralAccumulation, swimCache,
				PropertyUtils.TITLE, "ALERT XY",
				PropertyUtils.WIDTH, 860, PropertyUtils.HEIGHT, 760,
				PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(40, -40, -80, 80),
				PropertyUtils.BACKGROUND, Color.WHITE,
				PropertyUtils.TOOLBARBITS, ToolBits.NAVIGATIONTOOLS,
				PropertyUtils.WHEELZOOM, true, PropertyUtils.VISIBLE, true);
		this.geometry = geometry;
		this.accumulation = accumulation;
		setAfterDraw(this::draw);
		initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT, CedDisplayOption.ACCUMULATION,
				CedDisplayOption.RAW_DATA, CedDisplayOption.RECON_HITS, CedDisplayOption.CLUSTERS,
				CedDisplayOption.CROSSES, CedDisplayOption.CONNECT_CLUSTER_ENDPOINTS,
				CedDisplayOption.RECON_TRACKS, CedDisplayOption.MC_TRACKS),
				List.of("AHDC::", "ATOF::", "CND", "CTOF"), ScientificColorMap.TURBO,
				"Relative ADC / accumulation");
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		super.eventChanged(state);
		eventData = AlertEventData.from(state.snapshot());
	}

	private void draw(Graphics2D graphics, IContainer container) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			clearSharedDrawState();
			wireScreenPoints.clear();
			tofCells.clear();
			drawCTOF(g, container);
			drawCND(g, container);
			drawTofCells(g, container);
			if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
				drawAccumulatedWires(g, container);
			} else {
				drawWires(g, container);
				if (isDisplayed(CedDisplayOption.RAW_DATA)) drawDcAdcHits(g, container);
				if (isDisplayed(CedDisplayOption.RECON_HITS)) {
					drawDcHits(g, container);
					drawTofHits(g, container);
				}
				if (isDisplayed(CedDisplayOption.CLUSTERS)) {
					drawClusters(g, container);
					drawDcClusters(g, container);
					drawTofClusters(g, container);
				}
				if (isDisplayed(CedDisplayOption.CROSSES)) drawCrosses(g, container);
				if (isDisplayed(CedDisplayOption.RECON_TRACKS)) drawParticles(g, container);
				if (isDisplayed(CedDisplayOption.MC_TRACKS)) drawMcTracks(g, container);
			}
			drawXYAxes(g, container);
		} finally {
			g.dispose();
		}
	}

	/**
	 * ATOF's schematic "cell grid" -- deliberately <em>not</em> a faithful
	 * projection of the real paddle volumes. Every {sector, superlayer,
	 * layer, paddle} combination gets its own rectangular (in r-phi space)
	 * cell: the innermost ring per sector is superlayer 0 (one real "bar"
	 * paddle per layer, always the same neutral fill), surrounded by 10
	 * concentric zebra-striped rings for superlayer 1's "wedge"
	 * sub-components (paddle 0 innermost, paddle 9 outermost), each
	 * sector's own 4 layers occupying adjacent angular slices going
	 * clockwise (matching real geometry's own phi ordering -- see
	 * radialPhiBounds). This mirrors legacy CED's own choice to sacrifice
	 * geometric fidelity here in exchange for showing the full address
	 * space: superlayer 1's 10 real sub-components differ only in z (not
	 * r or phi -- confirmed against AlertGeometry itself), so a true XY
	 * projection would draw all 10 on top of each other.
	 */
	private void drawTofCells(Graphics2D g, IContainer container) {
		for (int sector = 0; sector < 15; sector++) {
			for (int layer = 0; layer < 4; layer++) {
				List<Paddle> sl0 = geometry.tofPaddles(sector, 0, layer);
				if (!sl0.isEmpty()) {
					RadialPhiBounds bounds = radialPhiBounds(sl0.get(0));
					Polygon polygon = annulusWedgePolygon(container, bounds.innerR(), bounds.outerR(),
							bounds.phiStart(), bounds.phiEnd());
					tofCells.put(new TofCell(sector, 0, layer, 0), polygon);
					g.setColor(TOF_SL0_FILL);
					g.fillPolygon(polygon);
					g.setColor(TOF_SL0_OUTLINE);
					g.drawPolygon(polygon);
				}
				List<Paddle> sl1 = geometry.tofPaddles(sector, 1, layer);
				if (!sl1.isEmpty()) {
					RadialPhiBounds bounds = radialPhiBounds(sl1.get(0));
					double deltaR = (bounds.outerR() - bounds.innerR()) / 10.0;
					for (int paddle = 0; paddle < 10; paddle++) {
						double innerR = bounds.innerR() + paddle * deltaR;
						Polygon polygon = annulusWedgePolygon(container, innerR, innerR + deltaR,
								bounds.phiStart(), bounds.phiEnd());
						tofCells.put(new TofCell(sector, 1, layer, paddle), polygon);
						Color fill = TOF_SL1_FILL[sector % 3][paddle % 2];
						g.setColor(fill);
						g.fillPolygon(polygon);
						g.setColor(fill.darker());
						g.drawPolygon(polygon);
					}
				}
			}
			drawTofSectorLabel(g, container, sector);
		}
	}

	/**
	 * One light sector number, 0-based (matching the raw bank/geometry
	 * addressing, and legacy's own 0-14 labels -- not the earlier,
	 * off-by-one "1-15" this replaced), placed just beyond the outermost
	 * zebra ring at that sector's angular center -- legacy's own outside
	 * placement, not the earlier inner-layer centroid this replaced.
	 */
	private void drawTofSectorLabel(Graphics2D g, IContainer container, int sector) {
		List<Paddle> first = geometry.tofPaddles(sector, 1, 0);
		List<Paddle> last = geometry.tofPaddles(sector, 1, 3);
		if (first.isEmpty() || last.isEmpty()) return;
		RadialPhiBounds firstBounds = radialPhiBounds(first.get(0));
		RadialPhiBounds lastBounds = radialPhiBounds(last.get(0));
		double phiStart = firstBounds.phiStart();
		double phiEnd = lastBounds.phiEnd();
		if (phiEnd < phiStart) phiEnd += 360;
		double phiCenter = Math.toRadians((phiStart + phiEnd) / 2);
		double labelR = firstBounds.outerR() + 15;
		Point p = screenMm(container, labelR * Math.cos(phiCenter), labelR * Math.sin(phiCenter));
		Font old = g.getFont();
		g.setFont(old.deriveFont(Font.BOLD, Math.max(10f, old.getSize2D())));
		g.setColor(TOF_LABEL);
		String label = Integer.toString(sector);
		g.drawString(label, p.x - g.getFontMetrics().stringWidth(label) / 2, p.y + 4);
		g.setFont(old);
	}

	/**
	 * A cell's real radial/angular extent, read directly off its underlying
	 * paddle's own 4 front-face vertices -- confirmed empirically (dumped
	 * against the running geometry) that vertex 0 sits at (innerR, phiStart)
	 * and vertex 2 at (outerR, phiEnd), i.e. every paddle is already a
	 * rectangle in r-phi space.
	 */
	private static RadialPhiBounds radialPhiBounds(Paddle paddle) {
		List<Point3> vertices = paddle.vertices();
		Point3 innerNear = vertices.get(0), outerFar = vertices.get(2);
		double innerR = Math.hypot(innerNear.x(), innerNear.y());
		double outerR = Math.hypot(outerFar.x(), outerFar.y());
		double phiStart = Math.toDegrees(Math.atan2(innerNear.y(), innerNear.x()));
		double phiEnd = Math.toDegrees(Math.atan2(outerFar.y(), outerFar.x()));
		return new RadialPhiBounds(innerR, outerR, phiStart, phiEnd);
	}

	/** An annulus-wedge cell (schematic, not a true paddle outline), approximated with a short polyline per arc. */
	private static Polygon annulusWedgePolygon(IContainer container, double innerRMm, double outerRMm,
			double phiStartDeg, double phiEndDeg) {
		Polygon polygon = new Polygon();
		for (int step = 0; step <= TOF_ARC_STEPS; step++) {
			double phi = Math.toRadians(phiStartDeg + (phiEndDeg - phiStartDeg) * step / TOF_ARC_STEPS);
			addArcPoint(container, polygon, innerRMm, phi);
		}
		for (int step = TOF_ARC_STEPS; step >= 0; step--) {
			double phi = Math.toRadians(phiStartDeg + (phiEndDeg - phiStartDeg) * step / TOF_ARC_STEPS);
			addArcPoint(container, polygon, outerRMm, phi);
		}
		return polygon;
	}

	private static void addArcPoint(IContainer container, Polygon polygon, double radiusMm, double phiRad) {
		Point p = screenMm(container, radiusMm * Math.cos(phiRad), radiusMm * Math.sin(phiRad));
		polygon.addPoint(p.x, p.y);
	}

	/**
	 * {@link edu.cnu.ced.geometry.AlertGeometry}'s own points turned out to be
	 * in mm, not cm like FMTGeometry/URWTGeometry -- confirmed against the
	 * running app (the earlier "already cm" read was wrong: comparing this
	 * detector's actual scale, which is compact and central, not against
	 * BST/BMT's own already-mm convention). AlertEventData's bank-derived
	 * positions (DcCluster/TofHit/TofCluster) already convert mm to cm
	 * themselves and use {@link #screen} directly; only positions read
	 * straight from AlertGeometry need this.
	 */
	private static Point screenMm(IContainer container, double xMm, double yMm) {
		return screen(container, xMm / 10, yMm / 10);
	}

	private void drawWires(Graphics2D g, IContainer container) {
		g.setStroke(new BasicStroke(1f));
		for (int superlayer = 0; superlayer < 5; superlayer++) {
			g.setColor(SUPERLAYER_COLORS[superlayer]);
			for (int layer = 0; layer < 2; layer++) {
				List<Segment3> wires = geometry.dcWires(0, superlayer, layer);
				for (int wire = 0; wire < wires.size(); wire++) {
					drawWireLine(g, container, superlayer, layer, wire, wires.get(wire));
				}
			}
		}
	}

	private void drawAccumulatedWires(Graphics2D g, IContainer container) {
		int max = accumulation.maximumCount();
		g.setStroke(new BasicStroke(1f));
		for (int superlayer = 0; superlayer < 5; superlayer++) {
			for (int layer = 0; layer < 2; layer++) {
				List<Segment3> wires = geometry.dcWires(0, superlayer, layer);
				for (int wire = 0; wire < wires.size(); wire++) {
					int count = accumulation.count(superlayer, layer, wire);
					g.setColor(count == 0 || max == 0
							? new Color(230, 230, 230)
							: ScientificColorMap.TURBO.colorAt((double) count / max));
					drawWireLine(g, container, superlayer, layer, wire, wires.get(wire));
				}
			}
		}
	}

	private void drawWireLine(Graphics2D g, IContainer container, int superlayer, int layer, int wire,
			Segment3 line) {
		Point a = screenMm(container, line.start().x(), line.start().y());
		Point b = screenMm(container, line.end().x(), line.end().y());
		wireScreenPoints.put(new WireAddress(superlayer, layer, wire), new Point[] { a, b });
		g.drawLine(a.x, a.y, b.x, b.y);
	}

	private void drawDcAdcHits(Graphics2D g, IContainer container) {
		int max = eventData.maximumDcAdc();
		if (max == 0) return;
		g.setStroke(new BasicStroke(3f));
		for (DcAdcHit hit : eventData.dcAdcHits()) {
			List<Segment3> wires = geometry.dcWires(hit.sector(), hit.superlayer(), hit.layer());
			if (hit.wire() < 0 || hit.wire() >= wires.size()) continue;
			Segment3 line = wires.get(hit.wire());
			Point a = screenMm(container, line.start().x(), line.start().y());
			Point b = screenMm(container, line.end().x(), line.end().y());
			g.setColor(ScientificColorMap.TURBO.colorAt((double) hit.adc() / max));
			g.drawLine(a.x, a.y, b.x, b.y);
		}
	}

	private void drawDcHits(Graphics2D g, IContainer container) {
		g.setColor(RECON_COLOR);
		for (DcHit hit : eventData.dcHits()) {
			List<Segment3> wires = geometry.dcWires(0, hit.superlayer(), hit.layer());
			if (hit.wire() < 0 || hit.wire() >= wires.size()) continue;
			Segment3 line = wires.get(hit.wire());
			Point p = screenMm(container, (line.start().x() + line.end().x()) / 2,
					(line.start().y() + line.end().y()) / 2);
			markers.put(hit, p);
			g.fillOval(p.x - MARKER, p.y - MARKER, 2 * MARKER, 2 * MARKER);
		}
	}

	/**
	 * Highlights the schematic cell(s) a raw ATOF hit belongs to, rather than
	 * plotting its bank-given (x, y) as a dot -- real superlayer-1 (wedge)
	 * components all share the same (x, y) and would otherwise collide into
	 * a single indistinguishable point (see drawTofCells). The
	 * component -> (superlayer, paddle) decode is legacy CED's own
	 * (AlertTOFGeometryNumbering.fromHipoNumbering): component 10 is the
	 * lone superlayer-0 bar; components 0-9 are superlayer 1's own paddle
	 * index.
	 */
	private void drawTofHits(Graphics2D g, IContainer container) {
		g.setColor(TOF_HIT_FILL);
		for (TofHit hit : eventData.tofHits()) {
			int superlayer = hit.component() == 10 ? 0 : 1;
			int paddle = hit.component() % 10;
			Polygon cell = tofCells.get(new TofCell(hit.sector(), superlayer, hit.layer(), paddle));
			if (cell == null) continue;
			g.fillPolygon(cell);
			g.setColor(TOF_HIT_FILL.darker());
			g.drawPolygon(cell);
			g.setColor(TOF_HIT_FILL);
			markers.put(hit, cellCenter(cell));
		}
	}

	private static Point cellCenter(Polygon cell) {
		java.awt.Rectangle bounds = cell.getBounds();
		return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
	}

	private void drawDcClusters(Graphics2D g, IContainer container) {
		g.setColor(CLUSTER_COLOR);
		g.setStroke(new BasicStroke(1.6f));
		for (DcCluster cluster : eventData.dcClusters()) {
			Point p = screen(container, cluster.x(), cluster.y());
			markers.put(cluster, p);
			g.drawLine(p.x - 7, p.y, p.x + 7, p.y);
			g.drawLine(p.x, p.y - 7, p.x, p.y + 7);
		}
	}

	private void drawTofClusters(Graphics2D g, IContainer container) {
		g.setColor(CLUSTER_COLOR.darker());
		g.setStroke(new BasicStroke(1.6f));
		for (TofCluster cluster : eventData.tofClusters()) {
			Point p = screen(container, cluster.x(), cluster.y());
			markers.put(cluster, p);
			g.drawOval(p.x - 6, p.y - 6, 12, 12);
		}
	}

	private record WireAddress(int superlayer, int layer, int wire) { }
	private record TofCell(int sector, int superlayer, int layer, int paddle) { }
	private record RadialPhiBounds(double innerR, double outerR, double phiStart, double phiEnd) { }

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint, Point2D.Double worldPoint,
			List<String> feedback) {
		super.getFeedbackStrings(container, screenPoint, worldPoint, feedback);
		addXYFeedback(worldPoint, "cm", feedback);
		boolean found = addBarrelPolygonFeedback(screenPoint, feedback);
		if (!found) {
			for (Map.Entry<TofCell, Polygon> entry : tofCells.entrySet()) {
				if (entry.getValue().contains(screenPoint)) {
					TofCell cell = entry.getKey();
					feedback.add(cell.superlayer() == 0
							? String.format("$wheat$ATOF sector %d layer %d (superlayer 0)",
									cell.sector(), cell.layer())
							: String.format("$wheat$ATOF sector %d layer %d superlayer 1 paddle %d",
									cell.sector(), cell.layer(), cell.paddle()));
					found = true;
					break;
				}
			}
		}
		if (!found) {
			WireAddress closest = null;
			double best = 6.0;
			for (Map.Entry<WireAddress, Point[]> entry : wireScreenPoints.entrySet()) {
				Point[] points = entry.getValue();
				double distance = Line2D.ptSegDist(points[0].x, points[0].y, points[1].x, points[1].y,
						screenPoint.x, screenPoint.y);
				if (distance < best) {
					best = distance;
					closest = entry.getKey();
				}
			}
			if (closest != null) {
				feedback.add(String.format("$wheat$AHDC superlayer %d layer %d wire %d",
						closest.superlayer(), closest.layer(), closest.wire()));
			}
		}
		addMarkersAndTrackFeedback(screenPoint, feedback);
		for (Map.Entry<Object, Point> entry : markers.entrySet()) {
			if (entry.getValue().distance(screenPoint) <= 9 && addMarkerFeedback(entry.getKey(), feedback)) break;
		}
	}

	private static boolean addMarkerFeedback(Object marker, List<String> feedback) {
		if (marker instanceof DcHit hit) {
			feedback.add(String.format("$deep sky blue$AHDC hit superlayer %d layer %d wire %d",
					hit.superlayer(), hit.layer(), hit.wire()));
			feedback.add(String.format("$deep sky blue$time %.3f ns  doca %.3f cm", hit.time(), hit.doca()));
			return true;
		} else if (marker instanceof TofHit hit) {
			feedback.add(String.format("$deep sky blue$ATOF hit sector %d layer %d component %d",
					hit.sector(), hit.layer(), hit.component()));
			feedback.add(String.format("$deep sky blue$energy %.3f MeV  time %.3f ns", hit.energy(), hit.time()));
			return true;
		} else if (marker instanceof DcCluster cluster) {
			feedback.add(String.format("$magenta$AHDC cluster xyz (%.3f, %.3f, %.3f) cm",
					cluster.x(), cluster.y(), cluster.z()));
			return true;
		} else if (marker instanceof TofCluster cluster) {
			feedback.add(String.format("$magenta$ATOF cluster xyz (%.3f, %.3f, %.3f) cm  energy %.3f MeV",
					cluster.x(), cluster.y(), cluster.z(), cluster.energy()));
			return true;
		}
		return false;
	}
}
