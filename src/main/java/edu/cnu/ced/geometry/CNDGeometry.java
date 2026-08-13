package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.geom.detector.cnd.CNDFactory;
import org.jlab.geom.prim.Point3D;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable primitive geometry for the three-layer Central Neutron Detector. */
public final class CNDGeometry implements CacheableGeometry {

	public static final int LAYER_COUNT = 3;
	public static final int PADDLE_COUNT = 48;
	private static final int CORNER_COUNT = 8;

	private volatile List<List<List<Point3>>> corners = List.of();

	@Override public String name() { return "CND"; }
	@Override public int formatVersion() { return 1; }

	@Override
	public void initializeFromSource() {
		var detector = new CNDFactory().createDetectorCLAS(
				GeometryFactory.getConstants(DetectorType.CND));
		var superlayer = detector.getSector(0).getSuperlayer(0);
		ArrayList<List<List<Point3>>> initialized = new ArrayList<>(LAYER_COUNT);
		for (int layer = 0; layer < LAYER_COUNT; layer++) {
			ArrayList<List<Point3>> paddles = new ArrayList<>(PADDLE_COUNT);
			for (int paddle = 0; paddle < PADDLE_COUNT; paddle++) {
				var component = superlayer.getLayer(layer).getComponent(paddle);
				component.rotateZ(Math.toRadians(7.5));
				ArrayList<Point3> points = new ArrayList<>(CORNER_COUNT);
				for (int corner = 0; corner < CORNER_COUNT; corner++) {
					Point3D point = component.getVolumePoint(corner);
					points.add(new Point3(point.x(), point.y(), point.z()));
				}
				paddles.add(List.copyOf(points));
			}
			initialized.add(List.copyOf(paddles));
		}
		publish(initialized);
	}

	/** Return eight immutable 3D corners for one-based layer and paddle numbers. */
	public List<Point3> corners(int layer, int paddle) {
		checkIndices(layer, paddle);
		ensureInitialized();
		return corners.get(layer - 1).get(paddle - 1);
	}

	/** Return the four transverse corners used by the central XY view. */
	public List<Point2> xyCorners(int layer, int paddle) {
		List<Point3> points = corners(layer, paddle);
		return List.of(
				new Point2(points.get(0).x(), points.get(0).y()),
				new Point2(points.get(1).x(), points.get(1).y()),
				new Point2(points.get(2).x(), points.get(2).y()),
				new Point2(points.get(3).x(), points.get(3).y()));
	}

	/** Return eight interleaved xyz vertices in centimetres. */
	public float[] verticesCm(int layer, int paddle) {
		List<Point3> points = corners(layer, paddle);
		float[] values = new float[CORNER_COUNT * 3];
		for (int corner = 0; corner < CORNER_COUNT; corner++) {
			Point3 point = points.get(corner);
			values[3 * corner] = (float) point.x();
			values[3 * corner + 1] = (float) point.y();
			values[3 * corner + 2] = (float) point.z();
		}
		return values;
	}

	/** Convert database numbering to sector/layer/component numbering. */
	public static int[] databaseToDetector(int layer, int paddle) {
		checkIndices(layer, paddle);
		int shifted = 1 + (paddle % PADDLE_COUNT);
		return new int[] { 1 + ((shifted - 1) / 2), layer,
				(shifted % 2 == 0) ? 2 : 1 };
	}

	/** Convert sector/layer/component numbering to database paddle numbering. */
	public static int databasePaddle(int sector, int layer, int component) {
		if (sector < 1 || sector > 24 || layer < 1 || layer > LAYER_COUNT
				|| component < 1 || component > 2) {
			throw new IllegalArgumentException("Invalid CND detector indices");
		}
		int paddle = (2 * (sector - 1) + component - 1) % PADDLE_COUNT;
		return paddle == 0 ? PADDLE_COUNT : paddle;
	}

	@Override
	public void read(DataInput input) throws IOException {
		int layers = input.readInt();
		int paddles = input.readInt();
		int cornerCount = input.readInt();
		if (layers != LAYER_COUNT || paddles != PADDLE_COUNT || cornerCount != CORNER_COUNT) {
			throw new IOException("Invalid CND geometry dimensions");
		}
		ArrayList<List<List<Point3>>> restored = new ArrayList<>(layers);
		for (int layer = 0; layer < layers; layer++) {
			ArrayList<List<Point3>> layerPaddles = new ArrayList<>(paddles);
			for (int paddle = 0; paddle < paddles; paddle++) {
				ArrayList<Point3> points = new ArrayList<>(cornerCount);
				for (int corner = 0; corner < cornerCount; corner++) {
					points.add(new Point3(input.readDouble(), input.readDouble(), input.readDouble()));
				}
				layerPaddles.add(List.copyOf(points));
			}
			restored.add(List.copyOf(layerPaddles));
		}
		publish(restored);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		ensureInitialized();
		output.writeInt(LAYER_COUNT);
		output.writeInt(PADDLE_COUNT);
		output.writeInt(CORNER_COUNT);
		for (List<List<Point3>> layer : corners) {
			for (List<Point3> paddle : layer) {
				for (Point3 point : paddle) {
					output.writeDouble(point.x());
					output.writeDouble(point.y());
					output.writeDouble(point.z());
				}
			}
		}
	}

	private void publish(List<List<List<Point3>>> value) {
		if (value.size() != LAYER_COUNT
				|| value.stream().anyMatch(layer -> layer.size() != PADDLE_COUNT)) {
			throw new IllegalStateException("Invalid CND geometry dimensions");
		}
		corners = List.copyOf(value);
	}

	private static void checkIndices(int layer, int paddle) {
		if (layer < 1 || layer > LAYER_COUNT || paddle < 1 || paddle > PADDLE_COUNT) {
			throw new IllegalArgumentException("CND layer/paddle out of range: " + layer + "/" + paddle);
		}
	}

	private void ensureInitialized() {
		if (corners.size() != LAYER_COUNT) {
			throw new IllegalStateException("CND geometry is not initialized");
		}
	}
}
