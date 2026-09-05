package edu.cnu.ced.view.central;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.MagneticFieldChangeListener;
import cnuphys.magfield.MagneticFields;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.CentralAccumulation;
import edu.cnu.ced.data.CentralEventData;
import edu.cnu.ced.data.CentralEventData.AdcHit;
import edu.cnu.ced.data.CentralEventData.Cluster;
import edu.cnu.ced.data.CentralEventData.Cross;
import edu.cnu.ced.data.CentralEventData.Detector;
import edu.cnu.ced.data.CentralEventData.TdcHit;
import edu.cnu.ced.data.MonteCarloTracks;
import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.data.TrackRow;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.CNDGeometry;
import edu.cnu.ced.geometry.CTOFGeometry;
import edu.cnu.ced.geometry.Point2;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.style.CedDrawingStyle;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.swim.SwimmableParticle;
import edu.cnu.ced.view.CedXYView;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * Shared base for CLAS12's two central-region ring detectors, CND (neutron
 * barrel) and CTOF (time-of-flight barrel) -- present in this exact
 * transverse-XY form regardless of what tracker sits inside them: BST+BMT
 * for the nominal Central Detector configuration ({@link CentralXYView}), or
 * AHDC+ATOF for the ALERT configuration ({@code AlertXYView}). Per the user:
 * "For ALERT, it is essentially a copy of CentralXY with the BST and BMT
 * replaced by Alert" -- extracted here instead of duplicated, so both share
 * one CND/CTOF implementation (geometry, ADC/cluster/cross drawing, hover
 * feedback) and one swim-and-draw implementation for REC::Particle/Monte
 * Carlo tracks (not CVT tracks, which are Central-Detector-specific and stay
 * in {@link CentralXYView} itself).
 * <p>
 * A subclass supplies its own {@code initializeCedView} call (display
 * options and bank prefixes differ), its own {@code draw()} (so it controls
 * draw order against its own tracker's geometry), and its own {@code
 * eventChanged} override that calls {@code super.eventChanged(state)} first.
 * </p>
 */
@SuppressWarnings("serial")
public abstract class CndCtofXYView extends CedXYView implements MagneticFieldChangeListener {

	private static final Color EMPTY = new Color(245, 248, 248);
	private static final Color CND_LABEL = new Color(25, 70, 135, 145);
	private static final Color CTOF_LABEL = new Color(40, 75, 145, 150);
	private static final Color CLUSTER_COLOR = new Color(205, 0, 205);
	// CND::adc's own bank schema documents order 0=ADCL/1=ADCR; CND::tdc's documents
	// order 2=TDCL/3=TDCR -- a constant +2 offset, not the same raw values.
	private static final int TDC_ORDER_OFFSET = 2;

	private final CNDGeometry cnd;
	private final CTOFGeometry ctof;
	protected final CentralAccumulation accumulation;
	private final SwimTrajectoryCache swimCache;
	protected final Map<Element, Polygon> polygons = new HashMap<>();
	protected final Map<Object, Point> markers = new HashMap<>();
	private final List<ScreenParticle> screenParticles = new ArrayList<>();
	private final List<ScreenTrack> screenMcTracks = new ArrayList<>();
	protected volatile CentralEventData data = CentralEventData.from(null);
	private volatile RecEventData recData = RecEventData.from(null);
	private volatile List<TrackRow> mcTracks = List.of();
	protected volatile FieldProbe fieldProbe = FieldProbe.factory();

	protected CndCtofXYView(CNDGeometry cnd, CTOFGeometry ctof, EventNavigator navigator,
			CentralAccumulation accumulation, SwimTrajectoryCache swimCache, Object... properties) {
		super(navigator, properties);
		this.cnd = cnd;
		this.ctof = ctof;
		this.accumulation = accumulation;
		this.swimCache = swimCache;
		MagneticFields.getInstance().addMagneticFieldChangeListener(this);
	}

	@Override
	protected void eventChanged(EventNavigationState state) {
		data = CentralEventData.from(state.snapshot());
		recData = RecEventData.from(state.snapshot());
		mcTracks = MonteCarloTracks.from(state.snapshot()).tracks();
		swimCache.forEvent(state.snapshot());
	}

	/** Call at the start of a subclass's own draw(), before its screen-space caches are populated. */
	protected final void clearSharedDrawState() {
		polygons.clear();
		markers.clear();
		screenParticles.clear();
		screenMcTracks.clear();
	}

	protected final void drawCTOF(Graphics2D g, IContainer c) {
		for (int paddle = 1; paddle <= CTOFGeometry.PADDLE_COUNT; paddle++) {
			Element e = new Element(Detector.CTOF, 1, 1, paddle, 0);
			List<Point2> quad = ctof.quad(paddle);
			Polygon p = polygon(c, quad, .1);
			polygons.put(e, p);
			fill(g, p, elementColor(e));
			drawCenteredLabel(g, centroid(c, quad, .1), Integer.toString(paddle), CTOF_LABEL);
		}
	}

	protected final void drawCND(Graphics2D g, IContainer c) {
		for (int layer = 1; layer <= 3; layer++) {
			for (int paddle = 1; paddle <= 48; paddle++) {
				int[] address = CNDGeometry.databaseToDetector(layer, paddle);
				Element e = new Element(Detector.CND, address[0], layer, 1, address[2] - 1);
				Polygon p = polygon(c, cnd.xyCorners(layer, paddle), 1);
				polygons.put(e, p);
				fill(g, p, elementColor(e));
			}
		}
		drawCNDSectorLabels(g, c);
	}

	private void drawCNDSectorLabels(Graphics2D g, IContainer c) {
		Font old = g.getFont();
		g.setFont(old.deriveFont(Font.BOLD, Math.max(10f, old.getSize2D())));
		g.setColor(CND_LABEL);
		for (int sector = 1; sector <= 24; sector++) {
			List<Point2> q = cnd.xyCorners(2, CNDGeometry.databasePaddle(sector, 2, 1));
			double x = 0, y = 0;
			for (Point2 p : q) { x += p.x(); y += p.y(); }
			Point s = screen(c, x / q.size(), y / q.size());
			String label = Integer.toString(sector);
			g.drawString(label, s.x - g.getFontMetrics().stringWidth(label) / 2, s.y + 4);
		}
		g.setFont(old);
	}

	protected final Color elementColor(Element e) {
		if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
			int max = accumulation.maximumCount();
			int n = accumulation.count(e.detector(), e.sector(), e.layer(), e.component(), e.order());
			return n == 0 || max == 0 ? EMPTY : ScientificColorMap.TURBO.colorAt((double) n / max);
		}
		if (!isDisplayed(CedDisplayOption.RAW_DATA)) return EMPTY;
		int detectorMax = maximumAdc(e.detector());
		int value = 0;
		for (AdcHit h : data.adcHits()) if (matches(e, h)) value = Math.max(value, h.adc());
		return value == 0 || detectorMax == 0 ? EMPTY : ScientificColorMap.TURBO.colorAt((double) value / detectorMax);
	}

	/** @return the largest ADC value seen this event for the given detector, across every channel. */
	protected final int maximumAdc(Detector detector) {
		int max = 0;
		for (AdcHit h : data.adcHits()) if (h.detector() == detector) max = Math.max(max, h.adc());
		return max;
	}

	protected final void drawClusters(Graphics2D g, IContainer c) {
		g.setStroke(new BasicStroke(1.6f));
		for (Cluster x : data.clusters()) {
			Point p = screen(c, x.x1(), x.y1());
			markers.put(x, p);
			drawClusterMarker(g, p);
			if (!Float.isNaN(x.x2())) {
				Point q = screen(c, x.x2(), x.y2());
				drawClusterMarker(g, q);
				if (isDisplayed(CedDisplayOption.CONNECT_CLUSTER_ENDPOINTS)) {
					g.setColor(new Color(CLUSTER_COLOR.getRed(), CLUSTER_COLOR.getGreen(),
							CLUSTER_COLOR.getBlue(), 150));
					g.drawLine(p.x, p.y, q.x, q.y);
				}
			}
		}
	}

	protected final void drawCrosses(Graphics2D g, IContainer c) {
		g.setColor(new Color(20, 145, 35));
		g.setStroke(new BasicStroke(2f));
		for (Cross x : data.crosses()) {
			if (Float.isNaN(x.x()) || Float.isNaN(x.y())) continue;
			Point p = screen(c, x.x(), x.y());
			markers.put(x, p);
			g.drawOval(p.x - 6, p.y - 6, 12, 12);
			g.drawLine(p.x - 8, p.y, p.x + 8, p.y);
			g.drawLine(p.x, p.y - 8, p.x, p.y + 8);
		}
	}

	/**
	 * REC::Particle, swum and drawn species-colored -- a plain world-coordinate
	 * projection, unlike SectorView's sector-rotated one, since this view
	 * already shows every sector at once.
	 */
	protected final void drawParticles(Graphics2D g, IContainer c) {
		for (RecEventData.Particle particle : recData.particles()) {
			List<Point3> swum = swimCache.trajectory(SwimmableParticle.of(particle), fieldProbe);
			if (swum.size() < 2) continue;
			Color color = CedDrawingStyle.particleColor(particle.pid(), particle.charge());
			Stroke stroke = CedDrawingStyle.particleStroke(particle.pid(), particle.charge());
			List<Point> points = new ArrayList<>(swum.size());
			for (Point3 p : swum) points.add(screen(c, p.x(), p.y()));
			drawTrajectory(g, points, color, stroke);
			screenParticles.add(new ScreenParticle(particle, points));
		}
	}

	protected final void drawMcTracks(Graphics2D g, IContainer c) {
		drawTrackRows(g, c, mcTracks, screenMcTracks);
	}

	/**
	 * Draws a group of {@link TrackRow}s (Monte Carlo truth, or -- from
	 * {@link CentralXYView}, which calls this too -- CVT track candidates)
	 * the same way {@link #drawParticles} draws REC::Particle. No stub
	 * fallback when swimming fails, matching drawParticles's own behavior
	 * here (unlike SectorView, which does fall back to a direction stub).
	 */
	protected final void drawTrackRows(Graphics2D g, IContainer c, List<TrackRow> tracks, List<ScreenTrack> sink) {
		for (TrackRow t : tracks) {
			List<Point3> swum = swimCache.trajectory(SwimmableParticle.of(t), fieldProbe);
			if (swum.size() < 2) continue;
			Color color = CedDrawingStyle.particleColor(t.pid(), t.charge());
			Stroke stroke = CedDrawingStyle.particleStroke(t.pid(), t.charge());
			List<Point> points = new ArrayList<>(swum.size());
			for (Point3 p : swum) points.add(screen(c, p.x(), p.y()));
			drawTrajectory(g, points, color, stroke);
			sink.add(new ScreenTrack(t, points));
		}
	}

	private static void drawTrajectory(Graphics2D g, List<Point> points, Color color, Stroke stroke) {
		g.setColor(color);
		g.setStroke(stroke);
		for (int i = 1; i < points.size(); i++) {
			Point a = points.get(i - 1), b = points.get(i);
			g.drawLine(a.x, a.y, b.x, b.y);
		}
		Point vertex = points.get(0);
		g.fillOval(vertex.x - 3, vertex.y - 3, 6, 6);
		g.setColor(CedDrawingStyle.outline(color));
		g.setStroke(new BasicStroke(1f));
		g.drawOval(vertex.x - 3, vertex.y - 3, 6, 6);
	}

	/**
	 * Polygon hover feedback for CND/CTOF, plus the markers/particle/MC-track
	 * feedback shared regardless of which tracker geometry a subclass adds.
	 * A subclass calls this first; if it returns {@code false}, the subclass
	 * tries its own geometry fallback (e.g. BST/BMT, or AHDC's closest wire),
	 * then should still let unmatched hover fall through to markers/tracks --
	 * see {@link #addMarkersAndTrackFeedback}, called unconditionally.
	 */
	protected final boolean addBarrelPolygonFeedback(Point sp, List<String> feedback) {
		for (Map.Entry<Element, Polygon> entry : polygons.entrySet()) {
			if (entry.getValue().contains(sp)) {
				Element e = entry.getKey();
				if (e.detector() == Detector.CTOF) feedback.add("$cyan$CTOF paddle " + e.component());
				else feedback.add("$cyan$" + e.detector() + " sector " + e.sector() + " layer " + e.layer()
						+ " component " + e.component());
				addAdcFeedback(e, feedback);
				return true;
			}
		}
		return false;
	}

	private void addAdcFeedback(Element e, List<String> feedback) {
		for (AdcHit h : data.adcHits()) {
			if (matches(e, h)) feedback.add(String.format("$cyan$adc %d time %.3f order %d",
					h.adc(), h.time(), h.order()));
		}
		if (e.detector() == Detector.CND) {
			for (TdcHit h : data.tdcHits()) {
				if (matchesTdc(e, h)) feedback.add(String.format("$cyan$CND tdc %d order %d", h.tdc(), h.order()));
			}
		}
		if (isDisplayed(CedDisplayOption.ACCUMULATION)) {
			feedback.add("$cyan$occupancy " + accumulation.count(e.detector(), e.sector(), e.layer(),
					e.component(), e.order()) + " / " + accumulation.eventCount() + " events");
		}
	}

	/** Markers (recon hits/clusters/crosses) and swum-track hover feedback -- call unconditionally, after any geometry-fallback feedback. */
	protected final void addMarkersAndTrackFeedback(Point sp, List<String> feedback) {
		for (Map.Entry<Object, Point> entry : markers.entrySet()) {
			if (entry.getValue().distance(sp) <= 9) {
				addMarkerFeedback(entry.getKey(), feedback);
				break;
			}
		}
		for (ScreenParticle drawn : screenParticles) {
			if (nearAnySegment(drawn.points(), sp, 5.0)) {
				addParticleFeedback(drawn.particle(), feedback);
				break;
			}
		}
		for (ScreenTrack drawn : screenMcTracks) {
			if (nearAnySegment(drawn.points(), sp, 5.0)) {
				addTrackFeedback(drawn.track(), "MC", "orange red", feedback);
				break;
			}
		}
	}

	private static void addMarkerFeedback(Object o, List<String> f) {
		if (o instanceof CentralEventData.ReconHit h) {
			f.add("$wheat$" + h.detector() + " recon hit sector " + h.sector() + " layer " + h.layer()
					+ " strip " + h.strip());
			f.add(String.format("$wheat$energy %.3f time %.3f cluster %d track %d",
					h.energy(), h.time(), h.clusterId(), h.trackId()));
		} else if (o instanceof Cluster x) {
			f.add(String.format("$magenta$%s cluster xy (%.3f, %.3f) cm", x.detector(), x.x1(), x.y1()));
			if (!Float.isNaN(x.energy())) {
				f.add(String.format("$magenta$energy %.3f id %d status %d", x.energy(), x.id(), x.status()));
			}
		} else if (o instanceof Cross x) {
			f.add(String.format("$green$%s cross id %d sector %d region %d xyz (%.3f, %.3f, %.3f) cm",
					x.detector(), x.id(), x.sector(), x.region(), x.x(), x.y(), x.z()));
		}
	}

	/** Hover feedback for anywhere along a drawn trajectory, not just its vertex marker -- matches SectorView's own particle hover. */
	private static void addParticleFeedback(RecEventData.Particle p, List<String> f) {
		f.add(String.format("$deep sky blue$%s (pid %d, q=%+d)", p.displayName(), p.pid(), p.charge()));
		f.add(String.format("$deep sky blue$p = %.3f GeV/c  theta = %.1f°  phi = %.1f°",
				p.p(), Math.toDegrees(p.theta()), Math.toDegrees(p.phi())));
		f.add(String.format("$deep sky blue$vertex (%.2f, %.2f, %.2f) cm", p.vx(), p.vy(), p.vz()));
		if (p.beta() != 0f || p.chi2pid() != 0f) {
			f.add(String.format("$deep sky blue$beta = %.3f  chi2pid = %.2f", p.beta(), p.chi2pid()));
		}
	}

	/** Hover feedback shared by every {@link ScreenTrack}-backed source (MC here, MC+CVT in {@link CentralXYView}); "color" matches each source's own drawn color. */
	protected static void addTrackFeedback(TrackRow t, String label, String color, List<String> f) {
		f.add(String.format("$%s$%s %s (pid %d, q=%+d)", color, label, t.name(), t.pid(), t.charge()));
		f.add(String.format("$%s$p = %.3f GeV/c  theta = %.1f°  phi = %.1f°", color,
				t.momentumMeV() / 1000.0, t.thetaDeg(), t.phiDeg()));
		f.add(String.format("$%s$vertex (%.2f, %.2f, %.2f) cm", color, t.x0(), t.y0(), t.z0()));
	}

	/** @return true if screenPoint lies within tolerance pixels of any segment of the polyline points. */
	protected static boolean nearAnySegment(List<Point> points, Point screenPoint, double tolerance) {
		for (int i = 1; i < points.size(); i++) {
			Point a = points.get(i - 1), b = points.get(i);
			if (Line2D.ptSegDist(a.x, a.y, b.x, b.y, screenPoint.x, screenPoint.y) <= tolerance) return true;
		}
		return false;
	}

	protected static void drawMarker(Graphics2D g, Point p, Color color, int halfSize) {
		g.setColor(color);
		g.fillRect(p.x - halfSize, p.y - halfSize, 2 * halfSize, 2 * halfSize);
		g.setColor(Color.DARK_GRAY);
		g.drawRect(p.x - halfSize, p.y - halfSize, 2 * halfSize, 2 * halfSize);
	}

	private static void drawClusterMarker(Graphics2D g, Point p) {
		g.setColor(CLUSTER_COLOR);
		g.drawLine(p.x - 7, p.y, p.x + 7, p.y);
		g.drawLine(p.x, p.y - 7, p.x, p.y + 7);
		g.fillRect(p.x - 2, p.y - 2, 5, 5);
		g.setColor(Color.DARK_GRAY);
		g.drawRect(p.x - 2, p.y - 2, 5, 5);
	}

	private static void fill(Graphics2D g, Polygon p, Color color) {
		g.setColor(color);
		g.fillPolygon(p);
		g.setColor(Color.DARK_GRAY);
		g.drawPolygon(p);
	}

	private static Polygon polygon(IContainer c, List<Point2> points, double scale) {
		Polygon p = new Polygon();
		for (Point2 q : points) {
			Point s = screen(c, q.x() * scale, q.y() * scale);
			p.addPoint(s.x, s.y);
		}
		return p;
	}

	private static Point centroid(IContainer c, List<Point2> points, double scale) {
		double x = 0, y = 0;
		for (Point2 q : points) { x += q.x(); y += q.y(); }
		return screen(c, scale * x / points.size(), scale * y / points.size());
	}

	protected static void drawCenteredLabel(Graphics2D g, Point p, String text, Color color) {
		Font old = g.getFont();
		g.setFont(old.deriveFont(Font.BOLD, Math.max(10f, old.getSize2D())));
		g.setColor(color);
		g.drawString(text, p.x - g.getFontMetrics().stringWidth(text) / 2, p.y + g.getFontMetrics().getAscent() / 3);
		g.setFont(old);
	}

	protected static Point screen(IContainer c, double x, double y) {
		Point p = new Point();
		c.worldToLocal(p, x, y);
		return p;
	}

	private static boolean matches(Element e, AdcHit h) {
		return e.detector() == h.detector() && e.sector() == h.sector() && e.layer() == h.layer()
				&& e.component() == h.component() && (e.detector() != Detector.CND || e.order() == h.order());
	}

	// e.order was derived to match CND::adc's (0/1) convention; see TDC_ORDER_OFFSET.
	/** Package-private (not private) so a test can exercise this directly. */
	static boolean matchesTdc(Element e, TdcHit h) {
		return e.sector() == h.sector() && e.layer() == h.layer() && e.component() == h.component()
				&& (e.order() + TDC_ORDER_OFFSET) == h.order();
	}

	/** Package-private (not private) so a test can construct one to exercise matchesTdc directly. */
	record Element(Detector detector, int sector, int layer, int component, int order) { }

	protected record ScreenParticle(RecEventData.Particle particle, List<Point> points) { }
	protected record ScreenTrack(TrackRow track, List<Point> points) { }

	@Override
	public void magneticFieldChanged() {
		fieldProbe = FieldProbe.factory();
		refresh();
	}

	@Override
	public void dispose() {
		MagneticFields.getInstance().removeMagneticFieldChangeListener(this);
		super.dispose();
	}
}
