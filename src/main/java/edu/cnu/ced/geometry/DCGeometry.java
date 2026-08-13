package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.geom.dc.DCGeantFactory;
import org.jlab.geom.component.DriftChamberWire;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable sector-1 wire volumes for the drift chamber. */
public final class DCGeometry implements CacheableGeometry {

	public static final int SECTOR_COUNT = 6;
	public static final int SUPERLAYER_COUNT = 6;
	public static final int LAYER_COUNT = 6;
	public static final int WIRE_COUNT = 112;
	private static final int VOLUME_POINT_COUNT = 12;

	public record Wire(int componentId, Point3 midpoint, Segment3 line, List<Point3> volume) {
		public Wire {
			volume = List.copyOf(volume);
			if (volume.size() != VOLUME_POINT_COUNT) {
				throw new IllegalArgumentException("A DC wire volume must have 12 vertices");
			}
		}
	}

	private volatile List<List<List<Wire>>> wires = List.of();
	private volatile double minWireX;
	private volatile double maxWireX;

	@Override
	public String name() {
		return "DC";
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
		var constants = GeometryFactory.getConstants(DetectorType.DC, 4013, variation);
		var sector = new DCGeantFactory().createDetectorCLAS(constants).getSector(0);
		ArrayList<List<List<Wire>>> loaded = new ArrayList<>();
		for (int superlayer = 0; superlayer < SUPERLAYER_COUNT; superlayer++) {
			ArrayList<List<Wire>> layers = new ArrayList<>();
			for (int layer = 0; layer < LAYER_COUNT; layer++) {
				ArrayList<Wire> layerWires = new ArrayList<>();
				for (int wire = 0; wire < WIRE_COUNT; wire++) {
					layerWires.add(copy(sector.getSuperlayer(superlayer).getLayer(layer).getComponent(wire)));
				}
				layers.add(List.copyOf(layerWires));
			}
			loaded.add(List.copyOf(layers));
		}
		publish(loaded);
	}

	/** Returns a wire using one-based CLAS12 detector indices. */
	public Wire wire(int superlayer, int layer, int wire) {
		check(superlayer, layer, wire);
		ensureInitialized();
		return wires.get(superlayer - 1).get(layer - 1).get(wire - 1);
	}

	/** Returns the sense-wire line rotated into the requested one-based sector. */
	public Segment3 wireLine(int sector, int superlayer, int layer, int wire) {
		checkSector(sector);
		Segment3 line = wire(superlayer, layer, wire).line();
		return rotate(line, Math.toRadians(60.0 * (sector - 1)));
	}

	/** Returns the twelve wire-volume vertices rotated into the requested sector. */
	public List<Point3> wireVolume(int sector, int superlayer, int layer, int wire) {
		checkSector(sector);
		double angle = Math.toRadians(60.0 * (sector - 1));
		ArrayList<Point3> result = new ArrayList<>(VOLUME_POINT_COUNT);
		for (Point3 point : wire(superlayer, layer, wire).volume()) {
			result.add(rotate(point, angle));
		}
		return List.copyOf(result);
	}

	public double absoluteMaxWireX() {
		ensureInitialized();
		return Math.max(Math.abs(minWireX), Math.abs(maxWireX));
	}

	@Override
	public void write(DataOutput output) throws IOException {
		ensureInitialized();
		output.writeInt(SUPERLAYER_COUNT);
		output.writeInt(LAYER_COUNT);
		output.writeInt(WIRE_COUNT);
		for (List<List<Wire>> superlayer : wires) {
			for (List<Wire> layer : superlayer) {
				for (Wire wire : layer) {
					output.writeInt(wire.componentId());
					writePoint(output, wire.midpoint());
					writePoint(output, wire.line().start());
					writePoint(output, wire.line().end());
					for (Point3 point : wire.volume()) {
						writePoint(output, point);
					}
				}
			}
		}
	}

	@Override
	public void read(DataInput input) throws IOException {
		checkDimension(input.readInt(), SUPERLAYER_COUNT, "superlayer");
		checkDimension(input.readInt(), LAYER_COUNT, "layer");
		checkDimension(input.readInt(), WIRE_COUNT, "wire");
		ArrayList<List<List<Wire>>> restored = new ArrayList<>();
		for (int superlayer = 0; superlayer < SUPERLAYER_COUNT; superlayer++) {
			ArrayList<List<Wire>> layers = new ArrayList<>();
			for (int layer = 0; layer < LAYER_COUNT; layer++) {
				ArrayList<Wire> layerWires = new ArrayList<>();
				for (int wire = 0; wire < WIRE_COUNT; wire++) {
					int id = input.readInt();
					Point3 midpoint = readPoint(input);
					Segment3 line = new Segment3(readPoint(input), readPoint(input));
					ArrayList<Point3> volume = new ArrayList<>(VOLUME_POINT_COUNT);
					for (int point = 0; point < VOLUME_POINT_COUNT; point++) {
						volume.add(readPoint(input));
					}
					layerWires.add(new Wire(id, midpoint, line, volume));
				}
				layers.add(List.copyOf(layerWires));
			}
			restored.add(List.copyOf(layers));
		}
		publish(restored);
	}

	private static Wire copy(DriftChamberWire source) {
		ArrayList<Point3> volume = new ArrayList<>(VOLUME_POINT_COUNT);
		for (int index = 0; index < VOLUME_POINT_COUNT; index++) {
			volume.add(point(source.getVolumePoint(index)));
		}
		return new Wire(source.getComponentId(), point(source.getMidpoint()),
				new Segment3(point(source.getLine().origin()), point(source.getLine().end())), volume);
	}

	private void publish(List<List<List<Wire>>> value) {
		if (value.size() != SUPERLAYER_COUNT) {
			throw new IllegalStateException("Invalid DC superlayer count");
		}
		double minimum = Double.POSITIVE_INFINITY;
		double maximum = Double.NEGATIVE_INFINITY;
		for (List<List<Wire>> superlayer : value) {
			for (List<Wire> layer : superlayer) {
				for (Wire wire : layer) {
					minimum = Math.min(minimum, Math.min(wire.line().start().x(), wire.line().end().x()));
					maximum = Math.max(maximum, Math.max(wire.line().start().x(), wire.line().end().x()));
				}
			}
		}
		wires = List.copyOf(value);
		minWireX = minimum;
		maxWireX = maximum;
	}

	private static Segment3 rotate(Segment3 line, double angle) {
		return new Segment3(rotate(line.start(), angle), rotate(line.end(), angle));
	}

	private static Point3 rotate(Point3 point, double angle) {
		double cosine = Math.cos(angle);
		double sine = Math.sin(angle);
		return new Point3(point.x() * cosine - point.y() * sine,
				point.x() * sine + point.y() * cosine, point.z());
	}

	private static Point3 point(org.jlab.geom.prim.Point3D point) {
		return new Point3(point.x(), point.y(), point.z());
	}

	private static void writePoint(DataOutput output, Point3 point) throws IOException {
		output.writeDouble(point.x());
		output.writeDouble(point.y());
		output.writeDouble(point.z());
	}

	private static Point3 readPoint(DataInput input) throws IOException {
		return new Point3(input.readDouble(), input.readDouble(), input.readDouble());
	}

	private static void checkDimension(int actual, int expected, String name) throws IOException {
		if (actual != expected) {
			throw new IOException("Invalid DC " + name + " count: " + actual);
		}
	}

	private static void checkSector(int sector) {
		if (sector < 1 || sector > SECTOR_COUNT) {
			throw new IllegalArgumentException("Invalid DC sector: " + sector);
		}
	}

	private static void check(int superlayer, int layer, int wire) {
		if (superlayer < 1 || superlayer > SUPERLAYER_COUNT
				|| layer < 1 || layer > LAYER_COUNT || wire < 1 || wire > WIRE_COUNT) {
			throw new IllegalArgumentException("Invalid DC wire address");
		}
	}

	private void ensureInitialized() {
		if (wires.size() != SUPERLAYER_COUNT) {
			throw new IllegalStateException("DC geometry is not initialized");
		}
	}
}
