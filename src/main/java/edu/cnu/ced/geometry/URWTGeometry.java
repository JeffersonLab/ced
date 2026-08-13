package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.geant4.v2.MPGD.URWT.URWTStripFactory;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable strip geometry for the six-sector, four-layer μrWT detector. */
public final class URWTGeometry implements CacheableGeometry {

	public static final int SECTOR_COUNT = 6;
	public static final int LAYER_COUNT = 4;

	public record Detector(int sector, int layer, List<Segment3> strips) {
		public Detector { strips = List.copyOf(strips); }
		public Point3 centroid() {
			double x = 0, y = 0, z = 0;
			for (Segment3 strip : strips) {
				x += (strip.start().x() + strip.end().x()) / 2;
				y += (strip.start().y() + strip.end().y()) / 2;
				z += (strip.start().z() + strip.end().z()) / 2;
			}
			return new Point3(x / strips.size(), y / strips.size(), z / strips.size());
		}
	}

	private volatile List<List<Detector>> detectors = List.of();

	@Override public String name() { return "μrWT"; }
	@Override public int formatVersion() { return 1; }
	@Override public void initializeFromSource() { initializeFromSource("default"); }

	@Override
	public void initializeFromSource(String variation) {
		URWTStripFactory factory = new URWTStripFactory(11, variation);
		ArrayList<List<Detector>> sectors = new ArrayList<>();
		for (int sector = 1; sector <= SECTOR_COUNT; sector++) {
			ArrayList<Detector> layers = new ArrayList<>();
			for (int layer = 1; layer <= LAYER_COUNT; layer++) {
				int count = factory.getNComponents(sector, layer);
				if (count < 1) throw new IllegalStateException("No μrWT strips for " + sector + "/" + layer);
				ArrayList<Segment3> strips = new ArrayList<>(count);
				for (int strip = 1; strip <= count; strip++) {
					var line = factory.getStrip(sector, layer, strip);
					if (line == null) throw new IllegalStateException("Missing μrWT strip " + sector + "/" + layer + "/" + strip);
					strips.add(new Segment3(point(line.origin()), point(line.end())));
				}
				layers.add(new Detector(sector, layer, strips));
			}
			sectors.add(List.copyOf(layers));
		}
		publish(sectors);
	}

	public Detector detector(int sector, int layer) {
		check(sector, layer); ensureInitialized();
		return detectors.get(sector - 1).get(layer - 1);
	}

	public Segment3 strip(int sector, int layer, int strip) {
		Detector detector = detector(sector, layer);
		if (strip < 1 || strip > detector.strips().size()) throw new IllegalArgumentException("Invalid μrWT strip: " + strip);
		return detector.strips().get(strip - 1);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		ensureInitialized(); output.writeInt(SECTOR_COUNT); output.writeInt(LAYER_COUNT);
		for (int sector = 1; sector <= SECTOR_COUNT; sector++) for (int layer = 1; layer <= LAYER_COUNT; layer++) {
			Detector detector = detector(sector, layer);
			output.writeInt(detector.sector()); output.writeInt(detector.layer()); output.writeInt(detector.strips().size());
			for (Segment3 strip : detector.strips()) { writePoint(output, strip.start()); writePoint(output, strip.end()); }
		}
	}

	@Override
	public void read(DataInput input) throws IOException {
		checkDimension(input.readInt(), SECTOR_COUNT, "sector"); checkDimension(input.readInt(), LAYER_COUNT, "layer");
		ArrayList<List<Detector>> sectors = new ArrayList<>();
		for (int sector = 1; sector <= SECTOR_COUNT; sector++) {
			ArrayList<Detector> layers = new ArrayList<>();
			for (int layer = 1; layer <= LAYER_COUNT; layer++) {
				int storedSector = input.readInt(), storedLayer = input.readInt(), count = input.readInt();
				if (storedSector != sector || storedLayer != layer || count < 1 || count > 100_000) throw new IOException("Invalid cached μrWT detector");
				ArrayList<Segment3> strips = new ArrayList<>(count);
				for (int strip = 0; strip < count; strip++) strips.add(new Segment3(readPoint(input), readPoint(input)));
				layers.add(new Detector(sector, layer, strips));
			}
			sectors.add(List.copyOf(layers));
		}
		publish(sectors);
	}

	private void publish(List<List<Detector>> value) {
		if (value.size() != SECTOR_COUNT) throw new IllegalStateException("Invalid μrWT sector count");
		for (List<Detector> layers : value) if (layers.size() != LAYER_COUNT) throw new IllegalStateException("Invalid μrWT layer count");
		detectors = List.copyOf(value);
	}
	private static void check(int sector, int layer) { if (sector < 1 || sector > SECTOR_COUNT || layer < 1 || layer > LAYER_COUNT) throw new IllegalArgumentException("Invalid μrWT address"); }
	private void ensureInitialized() { if (detectors.size() != SECTOR_COUNT) throw new IllegalStateException("μrWT geometry is not initialized"); }
	private static void checkDimension(int actual, int expected, String name) throws IOException { if (actual != expected) throw new IOException("Invalid μrWT " + name + " count: " + actual); }
	private static Point3 point(org.jlab.geom.prim.Point3D p) { return new Point3(p.x(), p.y(), p.z()); }
	private static void writePoint(DataOutput o, Point3 p) throws IOException { o.writeDouble(p.x()); o.writeDouble(p.y()); o.writeDouble(p.z()); }
	private static Point3 readPoint(DataInput i) throws IOException { return new Point3(i.readDouble(), i.readDouble(), i.readDouble()); }
}
