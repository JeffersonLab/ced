package edu.cnu.ced.swim;

import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.geom.DetectorHit;
import org.jlab.geom.base.Layer;
import org.jlab.geom.component.PrismaticComponent;
import org.jlab.geom.detector.dc.DCFactory;
import org.jlab.geom.detector.ec.ECFactory;
import org.jlab.geom.detector.ftof.FTOFFactory;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Path3D;
import org.jlab.geom.prim.Point3D;

import edu.cnu.ced.geometry.Point3;

/**
 * Turns a swum trajectory into the same kind of "fast Monte Carlo"
 * geometric hits {@code ~/bCNU/fastmCED} computes: where the trajectory
 * actually crosses DC/FTOF/PCAL/ECAL's own sensing components. No new
 * dependency: {@code org.jlab.geom.DetectorHit}/{@code Layer} are already
 * inside the {@code coat-libs} jar this project already depends on.
 *
 * <p>
 * Deliberately does <em>not</em> call {@link Layer#getHits(Path3D)}
 * itself. Confirmed empirically, by testing directly against the real
 * geometry rather than trusting the API on paper: {@code
 * org.jlab.geom.abs.AbstractLayer}'s own default {@code getHits}
 * implementation (inherited by FTOF's and EC's {@code Layer}s, since
 * neither overrides it) only ever examines a single path segment --
 * either the boundary-crossing one for a layer built with {@code
 * useBoundaryAsHitFilter=true} (EC), or unconditionally the path's very
 * first segment for one built with it {@code false} (FTOF) -- and even
 * that one segment only registers a hit if <em>both</em> of its endpoints
 * fall strictly outside the target component (a precondition {@link
 * org.jlab.geom.component.PrismaticComponent#getVolumeIntersection}'s own
 * javadoc documents), which a real trajectory's own point spacing has no
 * reason to satisfy near a specific paddle. DC's own {@code Layer} avoids
 * all of this by overriding {@code getHits} with a different strategy:
 * scan every path segment for a crossing of the layer's overall boundary
 * (a coarse, forgiving test, unrelated to individual components), then
 * report whichever component's own centerline is closest to that
 * crossing. {@link #findLayerHit} is that same strategy, generalized to
 * any {@link PrismaticComponent}-based layer -- DC's {@code
 * DriftChamberWire} and FTOF/PCAL/ECAL's {@code ScintillatorPaddle} both
 * extend it -- so it works uniformly across all four detectors, confirmed
 * against both synthetic paths and this class's own {@link
 * ParticleSwimmer}-produced trajectories.
 * </p>
 *
 * <p>
 * {@code edu.cnu.ced.geometry.DCGeometry} already builds coatjava {@code
 * Layer} objects internally (see its own {@code initializeFromSource()}),
 * but discards them immediately after extracting lightweight, cacheable
 * drawing data -- deliberately, since {@code CacheableGeometry}'s whole
 * point is skipping that (slow, DB-backed) construction entirely on a
 * cache hit. This class independently rebuilds its own {@code Layer}
 * objects (uncached, lazily, once per application run, only if a caller
 * actually asks for hits) rather than changing that contract. For DC
 * specifically this also means <em>not</em> reusing {@code DCGeometry}'s
 * own factory ({@code org.jlab.detector.geom.dc.DCGeantFactory}, chosen
 * there for its detailed GEANT-style wire volumes, ideal for 3D drawing):
 * confirmed empirically that factory's own {@code Layer.getPlane()} comes
 * back with a zero-length normal vector, and {@code getBoundary()} is
 * similarly unusable. {@code org.jlab.geom.detector.dc.DCFactory} -- the
 * same {@code org.jlab.geom.detector.*} family {@link FTOFFactory}/{@link
 * ECFactory} already belong to -- builds a properly populated one.
 * </p>
 */
public final class FastMcHitFinder {

	private static final int SECTOR_COUNT = 6;
	private static final int DC_SUPERLAYER_COUNT = 6;
	private static final int DC_LAYER_COUNT = 6;
	private static final int FTOF_PANEL_COUNT = 3;
	private static final int EC_VIEW_COUNT = 3;
	private static final int PCAL_SUPERLAYER = 0;
	private static final int EC_STACK_COUNT = 2;

	private volatile List<Layer<? extends PrismaticComponent>> dcLayers;
	private volatile List<Layer<? extends PrismaticComponent>> ftofLayers;
	private volatile List<Layer<? extends PrismaticComponent>> pcalLayers;
	private volatile List<Layer<? extends PrismaticComponent>> ecalLayers;

	/** One trajectory's hits, grouped by detector -- PCAL and ECAL are both {@code DetectorId.EC} internally, so kept separate here by which layers produced them, not by re-reading that id. */
	public record Hits(List<DetectorHit> dc, List<DetectorHit> ftof, List<DetectorHit> pcal, List<DetectorHit> ecal) {
		public static final Hits EMPTY = new Hits(List.of(), List.of(), List.of(), List.of());
	}

	/** Finds every DC/FTOF/PCAL/ECAL hit along {@code trajectory} (lab-frame points, same as {@link ParticleSwimmer}'s own output). */
	public Hits findHits(List<Point3> trajectory) {
		if (trajectory == null || trajectory.size() < 2) {
			return Hits.EMPTY;
		}
		Path3D path = toPath3D(trajectory);
		return new Hits(collectHits(dcLayers(), path), collectHits(ftofLayers(), path),
				collectHits(pcalLayers(), path), collectHits(ecalLayers(), path));
	}

	private static List<DetectorHit> collectHits(List<Layer<? extends PrismaticComponent>> layers, Path3D path) {
		List<DetectorHit> hits = new ArrayList<>();
		for (Layer<? extends PrismaticComponent> layer : layers) {
			DetectorHit hit = findLayerHit(layer, path);
			if (hit != null) {
				hits.add(hit);
			}
		}
		return hits;
	}

	/**
	 * @return the first path segment that crosses {@code layer}'s own
	 *         boundary, reported at that crossing point and attributed to
	 *         whichever component's own centerline is closest to it -- or
	 *         {@code null} if the path never crosses this layer at all
	 */
	private static DetectorHit findLayerHit(Layer<? extends PrismaticComponent> layer, Path3D path) {
		for (int i = 0; i < path.size() - 1; i++) {
			Line3D line = path.getLine(i);
			if (!layer.getBoundary().hasIntersectionSegment(line)) {
				continue;
			}
			List<Point3D> intersections = new ArrayList<>();
			if (layer.getBoundary().intersection(line, intersections) == 0) {
				continue;
			}
			int closestComponentId = -1;
			double closestDistance = Double.MAX_VALUE;
			for (PrismaticComponent component : layer.getAllComponents()) {
				double distance = component.getLine().distance(line).length();
				if (distance < closestDistance) {
					closestDistance = distance;
					closestComponentId = component.getComponentId();
				}
			}
			return new DetectorHit(layer.getDetectorId(), layer.getSectorId(), layer.getSuperlayerId(),
					layer.getLayerId(), closestComponentId, intersections.get(0));
		}
		return null;
	}

	private static Path3D toPath3D(List<Point3> trajectory) {
		Path3D path = new Path3D();
		for (Point3 point : trajectory) {
			path.addPoint(point.x(), point.y(), point.z());
		}
		return path;
	}

	private synchronized List<Layer<? extends PrismaticComponent>> dcLayers() {
		if (dcLayers == null) {
			var constants = GeometryFactory.getConstants(DetectorType.DC, 4013, "default");
			var detector = new DCFactory().createDetectorCLAS(constants);
			List<Layer<? extends PrismaticComponent>> layers = new ArrayList<>();
			for (int sector = 0; sector < SECTOR_COUNT; sector++) {
				for (int superlayer = 0; superlayer < DC_SUPERLAYER_COUNT; superlayer++) {
					for (int layer = 0; layer < DC_LAYER_COUNT; layer++) {
						layers.add(detector.getSector(sector).getSuperlayer(superlayer).getLayer(layer));
					}
				}
			}
			dcLayers = List.copyOf(layers);
		}
		return dcLayers;
	}

	private synchronized List<Layer<? extends PrismaticComponent>> ftofLayers() {
		if (ftofLayers == null) {
			var detector = new FTOFFactory().createDetectorCLAS(GeometryFactory.getConstants(DetectorType.FTOF));
			List<Layer<? extends PrismaticComponent>> layers = new ArrayList<>();
			for (int sector = 0; sector < SECTOR_COUNT; sector++) {
				for (int panel = 0; panel < FTOF_PANEL_COUNT; panel++) {
					layers.add(detector.getSector(sector).getSuperlayer(panel).getLayer(0));
				}
			}
			ftofLayers = List.copyOf(layers);
		}
		return ftofLayers;
	}

	private synchronized List<Layer<? extends PrismaticComponent>> pcalLayers() {
		if (pcalLayers == null) {
			var detector = new ECFactory().createDetectorCLAS(GeometryFactory.getConstants(DetectorType.ECAL));
			List<Layer<? extends PrismaticComponent>> layers = new ArrayList<>();
			for (int sector = 0; sector < SECTOR_COUNT; sector++) {
				for (int view = 0; view < EC_VIEW_COUNT; view++) {
					layers.add(detector.getSector(sector).getSuperlayer(PCAL_SUPERLAYER).getLayer(view));
				}
			}
			pcalLayers = List.copyOf(layers);
		}
		return pcalLayers;
	}

	private synchronized List<Layer<? extends PrismaticComponent>> ecalLayers() {
		if (ecalLayers == null) {
			var detector = new ECFactory().createDetectorCLAS(GeometryFactory.getConstants(DetectorType.ECAL));
			List<Layer<? extends PrismaticComponent>> layers = new ArrayList<>();
			for (int sector = 0; sector < SECTOR_COUNT; sector++) {
				for (int stack = 0; stack < EC_STACK_COUNT; stack++) {
					for (int view = 0; view < EC_VIEW_COUNT; view++) {
						layers.add(detector.getSector(sector).getSuperlayer(stack + 1).getLayer(view));
					}
				}
			}
			ecalLayers = List.copyOf(layers);
		}
		return ecalLayers;
	}
}
