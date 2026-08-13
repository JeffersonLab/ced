package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.geom.component.ScintillatorPaddle;
import org.jlab.geom.detector.ft.FTCALFactory;
import org.jlab.geom.prim.Point3D;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable primitive geometry for the sparse 332-component forward tagger calorimeter. */
public final class FTCALGeometry implements CacheableGeometry {

	public static final int MAX_COMPONENT_ID = 475;
	public static final float Z_OFFSET_CM = 200f;
	public static final double GRID_DELTA_CM = 2.5;
	private static final int EXPECTED_COMPONENTS = 332;
	private static final int CORNERS = 8;
	private static final double[] GRID_LIMITS = { -16.7, -15.2, -13.7, -12.2, -10.7,
			-9.15, -7.6, -6.1, -4.6, -3.05, -1.5, 0., 1.5, 3.05, 4.6, 6.1,
			7.6, 9.15, 10.7, 12.2, 13.7, 15.2, 16.7 };

	private volatile Map<Integer, List<Point3>> cornersById = Map.of();
	private volatile Map<Integer, GridIndex> gridById = Map.of();
	private volatile Map<GridIndex, Integer> idByGrid = Map.of();

	@Override public String name() { return "FTCAL"; }
	@Override public int formatVersion() { return 1; }

	@Override
	public void initializeFromSource() {
		var detector = new FTCALFactory().createDetectorCLAS(
				GeometryFactory.getConstants(DetectorType.FTCAL));
		var layer = detector.getSector(0).getSuperlayer(0).getLayer(0);
		Map<Integer, List<Point3>> newCorners = new LinkedHashMap<>();
		Map<Integer, GridIndex> newGrid = new HashMap<>();
		Map<GridIndex, Integer> newReverse = new HashMap<>();
		for (ScintillatorPaddle paddle : layer.getAllComponents()) {
			paddle.rotateZ(Math.PI);
			int id = paddle.getComponentId();
			ArrayList<Point3> corners = new ArrayList<>(CORNERS);
			for (int corner = 0; corner < CORNERS; corner++) {
				Point3D point = paddle.getVolumePoint(corner);
				corners.add(new Point3(point.x(), point.y(), point.z()));
			}
			List<Point3> immutableCorners = List.copyOf(corners);
			GridIndex grid = gridFor(center(immutableCorners));
			newCorners.put(id, immutableCorners);
			newGrid.put(id, grid);
			newReverse.put(grid, id);
		}
		publish(newCorners, newGrid, newReverse);
	}

	public List<Integer> componentIds() { return List.copyOf(cornersById.keySet()); }
	public boolean isValidComponent(int id) { return cornersById.containsKey(id); }
	public Optional<List<Point3>> corners(int id) { return Optional.ofNullable(cornersById.get(id)); }
	public Optional<GridIndex> gridIndex(int id) { return Optional.ofNullable(gridById.get(id)); }
	public OptionalInt componentAt(GridIndex grid) {
		Integer id = idByGrid.get(grid);
		return id == null ? OptionalInt.empty() : OptionalInt.of(id);
	}

	public Optional<Point2> centerXY(int id) {
		List<Point3> corners = cornersById.get(id);
		return corners == null ? Optional.empty() : Optional.of(center(corners));
	}

	/** Interleaved xyz corners with the display z offset applied. */
	public float[] verticesCm(int id) {
		List<Point3> corners = cornersById.get(id);
		if (corners == null) return new float[0];
		float[] values = new float[CORNERS * 3];
		for (int i = 0; i < CORNERS; i++) {
			Point3 point = corners.get(i);
			values[3 * i] = (float) point.x();
			values[3 * i + 1] = (float) point.y();
			values[3 * i + 2] = (float) point.z() - Z_OFFSET_CM;
		}
		return values;
	}

	public static int valueToGridIndex(double value) {
		if (value < GRID_LIMITS[0] || value > GRID_LIMITS[GRID_LIMITS.length - 1]) return 0;
		for (int i = 1; i < GRID_LIMITS.length; i++) {
			if (value < GRID_LIMITS[i]) {
				int index = -12 + i;
				return index < 0 ? index : index + 1;
			}
		}
		return 0;
	}

	@Override
	public void read(DataInput input) throws IOException {
		int count = input.readInt();
		if (count != EXPECTED_COMPONENTS) throw new IOException("Invalid FTCAL component count: " + count);
		Map<Integer, List<Point3>> newCorners = new LinkedHashMap<>();
		Map<Integer, GridIndex> newGrid = new HashMap<>();
		Map<GridIndex, Integer> newReverse = new HashMap<>();
		for (int component = 0; component < count; component++) {
			int id = input.readInt();
			if (id < 1 || id > MAX_COMPONENT_ID || newCorners.containsKey(id)) {
				throw new IOException("Invalid FTCAL component id: " + id);
			}
			ArrayList<Point3> corners = new ArrayList<>(CORNERS);
			for (int corner = 0; corner < CORNERS; corner++) {
				corners.add(new Point3(input.readDouble(), input.readDouble(), input.readDouble()));
			}
			GridIndex grid = new GridIndex(input.readInt(), input.readInt());
			newCorners.put(id, List.copyOf(corners));
			newGrid.put(id, grid);
			newReverse.put(grid, id);
		}
		publish(newCorners, newGrid, newReverse);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		if (cornersById.size() != EXPECTED_COMPONENTS) throw new IOException("FTCAL geometry is not initialized");
		output.writeInt(cornersById.size());
		for (int id : cornersById.keySet()) {
			output.writeInt(id);
			for (Point3 point : cornersById.get(id)) {
				output.writeDouble(point.x()); output.writeDouble(point.y()); output.writeDouble(point.z());
			}
			GridIndex grid = gridById.get(id);
			output.writeInt(grid.x()); output.writeInt(grid.y());
		}
	}

	private void publish(Map<Integer, List<Point3>> corners, Map<Integer, GridIndex> grid,
			Map<GridIndex, Integer> reverse) {
		if (corners.size() != EXPECTED_COMPONENTS) {
			throw new IllegalStateException("Expected " + EXPECTED_COMPONENTS + " FTCAL components, found " + corners.size());
		}
		cornersById = Map.copyOf(corners);
		gridById = Map.copyOf(grid);
		idByGrid = Map.copyOf(reverse);
	}

	private static Point2 center(List<Point3> corners) {
		double x = 0, y = 0;
		for (Point3 point : corners) { x += point.x(); y += point.y(); }
		return new Point2(x / corners.size(), y / corners.size());
	}

	private static GridIndex gridFor(Point2 center) {
		return new GridIndex(valueToGridIndex(center.x()), valueToGridIndex(center.y()));
	}
}
