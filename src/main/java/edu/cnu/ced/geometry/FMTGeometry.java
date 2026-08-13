package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.geom.component.TrackerStrip;
import org.jlab.geom.detector.fmt.FMTFactory;
import org.jlab.geom.detector.fmt.FMTLayer;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable six-layer Forward Micromegas Tracker strip geometry. */
public final class FMTGeometry implements CacheableGeometry {

	public static final int LAYER_COUNT = 6;
	public static final int STRIP_COUNT = 1024;
	private static final int VERTEX_COUNT = 8;

	public record Layer(int sector, int superlayer, int layer, Affine3 localToGlobal,
			List<List<Point3>> strips) {
		public Layer {
			strips = List.copyOf(strips);
		}
	}

	private volatile List<Layer> layers = List.of();

	@Override
	public String name() {
		return "FMT";
	}

	@Override
	public int formatVersion() {
		return 1;
	}

	@Override
	public void initializeFromSource() {
		initializeFromSource("default");
	}

	@Override
	public void initializeFromSource(String variation) {
		var constants = GeometryFactory.getConstants(DetectorType.FMT, 11, variation);
		FMTFactory factory = new FMTFactory();
		var superlayer = factory.createDetectorCLAS(constants).getSector(0).getSuperlayer(0);
		ArrayList<Layer> loaded = new ArrayList<>();
		for (int layer = 0; layer < LAYER_COUNT; layer++) {
			loaded.add(copy(superlayer.getLayer(layer)));
		}
		publish(loaded);
	}

	public Layer layer(int layer) {
		ensureInitialized();
		if (layer < 0 || layer >= LAYER_COUNT) {
			throw new IllegalArgumentException("Invalid FMT layer: " + layer);
		}
		return layers.get(layer);
	}

	public List<Point3> stripVertices(int layer, int strip) {
		checkStrip(strip);
		return layer(layer).strips().get(strip);
	}

	/** Center line between the two four-corner faces of a strip volume. */
	public Segment3 stripLine(int layer, int strip) {
		List<Point3> vertices = stripVertices(layer, strip);
		return new Segment3(faceCenter(vertices, 0), faceCenter(vertices, 4));
	}

	public Point3 localToGlobal(int layer, Point3 local) {
		return layer(layer).localToGlobal().apply(local);
	}

	/** Returns the legacy one-based disconnected strip region (1..4). */
	public static int region(int strip) {
		if (strip < 1 || strip > STRIP_COUNT) {
			return 0;
		}
		if (strip <= 320) return 1;
		if (strip <= 512) return 2;
		if (strip <= 832) return 3;
		return 4;
	}

	@Override
	public void write(DataOutput output) throws IOException {
		ensureInitialized();
		output.writeInt(layers.size());
		for (Layer layer : layers) {
			output.writeInt(layer.sector());
			output.writeInt(layer.superlayer());
			output.writeInt(layer.layer());
			layer.localToGlobal().write(output);
			output.writeInt(layer.strips().size());
			for (List<Point3> strip : layer.strips()) {
				for (Point3 point : strip) {
					writePoint(output, point);
				}
			}
		}
	}

	@Override
	public void read(DataInput input) throws IOException {
		int count = input.readInt();
		if (count != LAYER_COUNT) {
			throw new IOException("Invalid FMT layer count: " + count);
		}
		ArrayList<Layer> restored = new ArrayList<>();
		for (int index = 0; index < count; index++) {
			int sector = input.readInt();
			int superlayer = input.readInt();
			int layer = input.readInt();
			Affine3 transform = Affine3.read(input);
			int strips = input.readInt();
			if (strips != STRIP_COUNT) {
				throw new IOException("Invalid FMT strip count: " + strips);
			}
			ArrayList<List<Point3>> stripVertices = new ArrayList<>();
			for (int strip = 0; strip < strips; strip++) {
				ArrayList<Point3> vertices = new ArrayList<>(VERTEX_COUNT);
				for (int vertex = 0; vertex < VERTEX_COUNT; vertex++) {
					vertices.add(readPoint(input));
				}
				stripVertices.add(List.copyOf(vertices));
			}
			restored.add(new Layer(sector, superlayer, layer, transform, stripVertices));
		}
		publish(restored);
	}

	private static Layer copy(FMTLayer source) {
		ArrayList<List<Point3>> strips = new ArrayList<>();
		for (int strip = 0; strip < source.getNumComponents(); strip++) {
			TrackerStrip component = source.getComponent(strip);
			ArrayList<Point3> vertices = new ArrayList<>(VERTEX_COUNT);
			for (int vertex = 0; vertex < VERTEX_COUNT; vertex++) {
				vertices.add(point(component.getVolumePoint(vertex)));
			}
			strips.add(List.copyOf(vertices));
		}
		return new Layer(source.getSectorId(), source.getSuperlayerId(), source.getLayerId(),
				Affine3.sample(source.getTransformation()), strips);
	}

	private void publish(List<Layer> value) {
		if (value.size() != LAYER_COUNT) {
			throw new IllegalStateException("Invalid FMT layer count");
		}
		for (Layer layer : value) {
			if (layer.strips().size() != STRIP_COUNT) {
				throw new IllegalStateException("Invalid FMT strip count");
			}
		}
		layers = List.copyOf(value);
	}

	private static Point3 faceCenter(List<Point3> vertices, int first) {
		double x = 0, y = 0, z = 0;
		for (int index = first; index < first + 4; index++) {
			Point3 point = vertices.get(index);
			x += point.x(); y += point.y(); z += point.z();
		}
		return new Point3(x / 4, y / 4, z / 4);
	}

	private static Point3 point(org.jlab.geom.prim.Point3D point) {
		return new Point3(point.x(), point.y(), point.z());
	}

	private static void writePoint(DataOutput output, Point3 point) throws IOException {
		output.writeDouble(point.x()); output.writeDouble(point.y()); output.writeDouble(point.z());
	}

	private static Point3 readPoint(DataInput input) throws IOException {
		return new Point3(input.readDouble(), input.readDouble(), input.readDouble());
	}

	private static void checkStrip(int strip) {
		if (strip < 0 || strip >= STRIP_COUNT) {
			throw new IllegalArgumentException("Invalid FMT strip: " + strip);
		}
	}

	private void ensureInitialized() {
		if (layers.size() != LAYER_COUNT) {
			throw new IllegalStateException("FMT geometry is not initialized");
		}
	}
}
