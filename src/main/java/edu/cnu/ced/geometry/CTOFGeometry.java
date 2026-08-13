package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable analytic geometry for the 48 central time-of-flight paddles. */
public final class CTOFGeometry implements CacheableGeometry {

	public static final double INNER_RADIUS_MM = 251.1;
	public static final double OUTER_RADIUS_MM = INNER_RADIUS_MM + 30.226;
	public static final int PADDLE_COUNT = 48;
	private static final double DELTA_THETA_DEGREES = 7.5;
	private static final float LENGTH_CM = (float) (35.4 * 2.54);

	private volatile List<List<Point2>> quads = List.of();

	@Override public String name() { return "CTOF"; }
	@Override public int formatVersion() { return 1; }

	@Override
	public void initializeFromSource() {
		ArrayList<List<Point2>> initialized = new ArrayList<>(PADDLE_COUNT);
		for (int paddle = 0; paddle < PADDLE_COUNT; paddle++) {
			double theta1 = Math.toRadians(paddle * DELTA_THETA_DEGREES);
			double theta2 = Math.toRadians((paddle + 1) * DELTA_THETA_DEGREES);
			initialized.add(List.of(
					point(INNER_RADIUS_MM, theta1), point(OUTER_RADIUS_MM, theta1),
					point(OUTER_RADIUS_MM, theta2), point(INNER_RADIUS_MM, theta2)));
		}
		quads = List.copyOf(initialized);
	}

	/** Return the immutable four-corner XY quad for a one-based paddle number. */
	public List<Point2> quad(int paddle) {
		checkPaddle(paddle);
		ensureInitialized();
		return quads.get(paddle - 1);
	}

	/** Return the eight 3D paddle corners as interleaved xyz values in centimetres. */
	public float[] verticesCm(int paddle) {
		List<Point2> quad = quad(paddle);
		float z1 = -LENGTH_CM / 2;
		float z2 = LENGTH_CM / 2;
		float[] values = new float[24];
		set(values, 0, quad.get(0), z1);
		set(values, 1, quad.get(3), z1);
		set(values, 2, quad.get(2), z1);
		set(values, 3, quad.get(1), z1);
		set(values, 4, quad.get(0), z2);
		set(values, 5, quad.get(3), z2);
		set(values, 6, quad.get(2), z2);
		set(values, 7, quad.get(1), z2);
		return values;
	}

	@Override
	public void read(DataInput input) throws IOException {
		int count = input.readInt();
		if (count != PADDLE_COUNT) {
			throw new IOException("Expected " + PADDLE_COUNT + " CTOF paddles, found " + count);
		}
		ArrayList<List<Point2>> restored = new ArrayList<>(count);
		for (int paddle = 0; paddle < count; paddle++) {
			int corners = input.readInt();
			if (corners != 4) {
				throw new IOException("Expected 4 CTOF corners, found " + corners);
			}
			ArrayList<Point2> quad = new ArrayList<>(corners);
			for (int corner = 0; corner < corners; corner++) {
				quad.add(new Point2(input.readDouble(), input.readDouble()));
			}
			restored.add(List.copyOf(quad));
		}
		quads = List.copyOf(restored);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		ensureInitialized();
		output.writeInt(quads.size());
		for (List<Point2> quad : quads) {
			output.writeInt(quad.size());
			for (Point2 point : quad) {
				output.writeDouble(point.x());
				output.writeDouble(point.y());
			}
		}
	}

	private static Point2 point(double radius, double angle) {
		return new Point2(radius * Math.cos(angle), radius * Math.sin(angle));
	}

	private static void set(float[] values, int corner, Point2 point, float z) {
		int offset = corner * 3;
		values[offset] = (float) (point.x() / 10.0);
		values[offset + 1] = (float) (point.y() / 10.0);
		values[offset + 2] = z;
	}

	private static void checkPaddle(int paddle) {
		if (paddle < 1 || paddle > PADDLE_COUNT) {
			throw new IllegalArgumentException("CTOF paddle must be in [1, 48]: " + paddle);
		}
	}

	private void ensureInitialized() {
		if (quads.size() != PADDLE_COUNT) {
			throw new IllegalStateException("CTOF geometry is not initialized");
		}
	}
}
