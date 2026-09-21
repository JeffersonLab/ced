package edu.cnu.ced.swim;

import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.geom.DetectorHit;
import org.jlab.geom.base.Layer;
import org.jlab.geom.detector.dc.DCFactory;
import org.jlab.geom.prim.Path3D;

import edu.cnu.ced.geometry.Point3;

/**
 * Turns a swum trajectory into the same kind of "fast Monte Carlo"
 * geometric hits {@code ~/bCNU/fastmCED} computes: where the trajectory
 * actually crosses DC's own sensing components, via coatjava's own {@link
 * Layer#getHits(Path3D)} -- not a reimplementation, the same coatjava
 * capability fastmCED itself calls through {@code
 * org.jlab.geom.base.Layer#getHits}. No new dependency: {@code
 * org.jlab.geom.DetectorHit}/{@code Layer} are already inside the {@code
 * coat-libs} jar this project already depends on.
 *
 * <p>
 * {@code edu.cnu.ced.geometry.DCGeometry} already builds coatjava {@code
 * Layer} objects internally (see its own {@code initializeFromSource()}),
 * but discards them immediately after extracting lightweight, cacheable
 * drawing data -- deliberately, since {@code CacheableGeometry}'s whole
 * point is skipping that (slow, DB-backed) construction entirely on a
 * cache hit. This class independently rebuilds its own {@code Layer}
 * objects (uncached, lazily, once per application run, only if a caller
 * actually asks for hits) rather than changing that contract.
 * </p>
 * <p>
 * This deliberately does <em>not</em> reuse {@code DCGeometry}'s own
 * factory ({@code org.jlab.detector.geom.dc.DCGeantFactory}, chosen there
 * for its detailed GEANT-style wire volumes, ideal for 3D drawing):
 * confirmed empirically that factory's own {@code Layer.getPlane()}
 * comes back with a zero-length normal vector, and {@code getHits}/
 * {@code getLayerHits} silently find nothing no matter the trajectory.
 * {@code org.jlab.geom.detector.dc.DCFactory} builds a properly
 * populated plane and finds hits correctly.
 * </p>
 * <p>
 * Scoped to DC only: FTOF/PCAL/ECAL were tried the same way (their own
 * {@code org.jlab.geom.detector.{ftof,ec}} factories, confirmed non-
 * degenerate planes) and consistently found zero hits regardless of
 * trajectory density or path length -- traced to {@code
 * AbstractLayer#getHits} delegating to {@code
 * ScintillatorPaddle#getVolumeIntersection}, which appears to not work
 * correctly for those detectors' own component shape. That's a coatjava
 * library issue, not something to work around here; left as a follow-up
 * if it's ever fixed upstream or a workaround is found.
 * </p>
 */
public final class FastMcHitFinder {

	private static final int SECTOR_COUNT = 6;
	private static final int DC_SUPERLAYER_COUNT = 6;
	private static final int DC_LAYER_COUNT = 6;

	private volatile List<Layer<?>> dcLayers;

	/** Finds every DC hit along {@code trajectory} (lab-frame points, same as {@link ParticleSwimmer}'s own output). */
	public List<DetectorHit> findDcHits(List<Point3> trajectory) {
		if (trajectory == null || trajectory.size() < 2) {
			return List.of();
		}
		Path3D path = toPath3D(trajectory);
		List<DetectorHit> hits = new ArrayList<>();
		for (Layer<?> layer : dcLayers()) {
			hits.addAll(layer.getHits(path));
		}
		return hits;
	}

	private static Path3D toPath3D(List<Point3> trajectory) {
		Path3D path = new Path3D();
		for (Point3 point : trajectory) {
			path.addPoint(point.x(), point.y(), point.z());
		}
		return path;
	}

	private synchronized List<Layer<?>> dcLayers() {
		if (dcLayers == null) {
			var constants = GeometryFactory.getConstants(DetectorType.DC, 4013, "default");
			var detector = new DCFactory().createDetectorCLAS(constants);
			List<Layer<?>> layers = new ArrayList<>();
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
}
