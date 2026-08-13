package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.geom.detector.alert.AHDC.AlertDCFactory;
import org.jlab.geom.detector.alert.ATOF.AlertTOFFactory;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable ALERT drift-chamber wires and time-of-flight paddle volumes. */
public final class AlertGeometry implements CacheableGeometry {

	public record Address(int sector, int superlayer, int layer) { }
	public record Paddle(int componentId, List<Point3> vertices) {
		public Paddle { vertices = List.copyOf(vertices); }
	}

	private volatile Map<Address, List<Segment3>> dcLayers = Map.of();
	private volatile Map<Address, List<Paddle>> tofLayers = Map.of();

	@Override public String name() { return "ALERT"; }
	@Override public int formatVersion() { return 1; }
	@Override public void initializeFromSource() { initializeFromSource("default"); }

	@Override
	public void initializeFromSource(String variation) {
		DatabaseConstantProvider provider = new DatabaseConstantProvider(11, variation);
		AlertDCFactory dcFactory = new AlertDCFactory();
		var dc = dcFactory.createDetectorCLAS(provider);
		LinkedHashMap<Address, List<Segment3>> wires = new LinkedHashMap<>();
		for (int sector = 0; sector < dc.getNumSectors(); sector++) {
			var dcSector = dcFactory.createSector(provider, sector);
			for (int superlayer = 0; superlayer < dcSector.getNumSuperlayers(); superlayer++) {
				var dcSuperlayer = dcFactory.createSuperlayer(provider, sector, superlayer);
				for (int layer = 0; layer < dcSuperlayer.getNumLayers(); layer++) {
					var dcLayer = dcFactory.createLayer(provider, sector, superlayer, layer);
					ArrayList<Segment3> lines = new ArrayList<>();
					for (var wire : dcLayer.getAllComponents()) {
						lines.add(new Segment3(point(wire.getLine().origin()), point(wire.getLine().end())));
					}
					wires.put(new Address(sector, superlayer, layer), List.copyOf(lines));
				}
			}
		}

		AlertTOFFactory tofFactory = new AlertTOFFactory();
		var tof = tofFactory.createDetectorCLAS(provider);
		LinkedHashMap<Address, List<Paddle>> paddles = new LinkedHashMap<>();
		for (int sector = 0; sector < tof.getNumSectors(); sector++) {
			var tofSector = tofFactory.createSector(provider, sector);
			for (int superlayer = 0; superlayer < tofSector.getNumSuperlayers(); superlayer++) {
				var tofSuperlayer = tofFactory.createSuperlayer(provider, sector, superlayer);
				for (int layer = 0; layer < tofSuperlayer.getNumLayers(); layer++) {
					var tofLayer = tofFactory.createLayer(provider, sector, superlayer, layer);
					ArrayList<Paddle> layerPaddles = new ArrayList<>();
					for (var paddle : tofLayer.getAllComponents()) {
						ArrayList<Point3> vertices = new ArrayList<>(8);
						for (int vertex = 0; vertex < 8; vertex++) vertices.add(point(paddle.getVolumePoint(vertex)));
						layerPaddles.add(new Paddle(paddle.getComponentId(), vertices));
					}
					paddles.put(new Address(sector, superlayer, layer), List.copyOf(layerPaddles));
				}
			}
		}
		provider.disconnect();
		publish(wires, paddles);
	}

	public List<Segment3> dcWires(int sector, int superlayer, int layer) {
		ensureInitialized();
		return dcLayers.getOrDefault(new Address(sector, superlayer, layer), List.of());
	}

	public List<Paddle> tofPaddles(int sector, int superlayer, int layer) {
		ensureInitialized();
		return tofLayers.getOrDefault(new Address(sector, superlayer, layer), List.of());
	}

	public int dcLayerCount() { ensureInitialized(); return dcLayers.size(); }
	public int tofLayerCount() { ensureInitialized(); return tofLayers.size(); }

	@Override
	public void write(DataOutput output) throws IOException {
		ensureInitialized();
		output.writeInt(dcLayers.size());
		for (var entry : dcLayers.entrySet()) {
			writeAddress(output, entry.getKey()); output.writeInt(entry.getValue().size());
			for (Segment3 wire : entry.getValue()) { writePoint(output, wire.start()); writePoint(output, wire.end()); }
		}
		output.writeInt(tofLayers.size());
		for (var entry : tofLayers.entrySet()) {
			writeAddress(output, entry.getKey()); output.writeInt(entry.getValue().size());
			for (Paddle paddle : entry.getValue()) {
				output.writeInt(paddle.componentId());
				for (Point3 vertex : paddle.vertices()) writePoint(output, vertex);
			}
		}
	}

	@Override
	public void read(DataInput input) throws IOException {
		LinkedHashMap<Address, List<Segment3>> wires = new LinkedHashMap<>();
		int dcCount = checkedCount(input.readInt(), "DC layer");
		for (int index = 0; index < dcCount; index++) {
			Address address = readAddress(input); int count = checkedCount(input.readInt(), "wire");
			ArrayList<Segment3> lines = new ArrayList<>();
			for (int wire = 0; wire < count; wire++) lines.add(new Segment3(readPoint(input), readPoint(input)));
			wires.put(address, List.copyOf(lines));
		}
		LinkedHashMap<Address, List<Paddle>> paddles = new LinkedHashMap<>();
		int tofCount = checkedCount(input.readInt(), "TOF layer");
		for (int index = 0; index < tofCount; index++) {
			Address address = readAddress(input); int count = checkedCount(input.readInt(), "paddle");
			ArrayList<Paddle> values = new ArrayList<>();
			for (int paddle = 0; paddle < count; paddle++) {
				int id = input.readInt(); ArrayList<Point3> vertices = new ArrayList<>(8);
				for (int vertex = 0; vertex < 8; vertex++) vertices.add(readPoint(input));
				values.add(new Paddle(id, vertices));
			}
			paddles.put(address, List.copyOf(values));
		}
		publish(wires, paddles);
	}

	private void publish(Map<Address, List<Segment3>> wires, Map<Address, List<Paddle>> paddles) {
		if (wires.isEmpty() || paddles.isEmpty()) throw new IllegalStateException("Incomplete ALERT geometry");
		dcLayers = Map.copyOf(wires); tofLayers = Map.copyOf(paddles);
	}
	private void ensureInitialized() { if (dcLayers.isEmpty() || tofLayers.isEmpty()) throw new IllegalStateException("ALERT geometry is not initialized"); }
	private static int checkedCount(int count, String name) throws IOException { if (count < 0 || count > 100_000) throw new IOException("Invalid ALERT " + name + " count: " + count); return count; }
	private static Point3 point(org.jlab.geom.prim.Point3D p) { return new Point3(p.x(), p.y(), p.z()); }
	private static void writePoint(DataOutput o, Point3 p) throws IOException { o.writeDouble(p.x()); o.writeDouble(p.y()); o.writeDouble(p.z()); }
	private static Point3 readPoint(DataInput i) throws IOException { return new Point3(i.readDouble(), i.readDouble(), i.readDouble()); }
	private static void writeAddress(DataOutput o, Address a) throws IOException { o.writeInt(a.sector()); o.writeInt(a.superlayer()); o.writeInt(a.layer()); }
	private static Address readAddress(DataInput i) throws IOException { return new Address(i.readInt(), i.readInt(), i.readInt()); }
}
