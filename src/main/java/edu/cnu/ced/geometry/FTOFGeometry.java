package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.geom.component.ScintillatorPaddle;
import org.jlab.geom.detector.ftof.FTOFFactory;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Point3D;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable primitive geometry for all Forward Time-of-Flight paddles. */
public final class FTOFGeometry implements CacheableGeometry {

	public static final int SECTOR_COUNT = 6;
	public static final int PANEL_COUNT = 3;
	public static final int PANEL_1A = 0;
	public static final int PANEL_1B = 1;
	public static final int PANEL_2 = 2;
	private static final int CORNER_COUNT = 8;
	private static final int PROJECTION_EDGE_COUNT = 4;

	private volatile int[] paddleCounts = new int[0];
	private volatile List<List<List<Paddle>>> paddles = List.of();

	/** Complete immutable primitive payload for one paddle. */
	public record Paddle(List<Point3> corners, List<Segment3> projectionEdges, double lengthCm) {
		public Paddle {
			corners = List.copyOf(corners);
			projectionEdges = List.copyOf(projectionEdges);
		}
	}

	@Override public String name() { return "FTOF"; }
	@Override public int formatVersion() { return 1; }

	@Override
	public void initializeFromSource() {
		var detector = new FTOFFactory().createDetectorCLAS(
				GeometryFactory.getConstants(DetectorType.FTOF));
		int[] counts = new int[PANEL_COUNT];
		ArrayList<List<List<Paddle>>> sectors = new ArrayList<>(SECTOR_COUNT);
		for (int sector = 0; sector < SECTOR_COUNT; sector++) {
			ArrayList<List<Paddle>> panels = new ArrayList<>(PANEL_COUNT);
			for (int panel = 0; panel < PANEL_COUNT; panel++) {
				var layer = detector.getSector(sector).getSuperlayer(panel).getLayer(0);
				if (sector == 0) counts[panel] = layer.getNumComponents();
				ArrayList<Paddle> panelPaddles = new ArrayList<>(layer.getNumComponents());
				for (ScintillatorPaddle source : layer.getAllComponents()) {
					ArrayList<Point3> corners = new ArrayList<>(CORNER_COUNT);
					for (int corner = 0; corner < CORNER_COUNT; corner++) {
						corners.add(point(source.getVolumePoint(corner)));
					}
					ArrayList<Segment3> edges = new ArrayList<>(PROJECTION_EDGE_COUNT);
					for (int edge = 0; edge < PROJECTION_EDGE_COUNT; edge++) {
						Line3D line = source.getVolumeEdge(6 + edge);
						edges.add(new Segment3(point(line.origin()), point(line.end())));
					}
					panelPaddles.add(new Paddle(corners, edges, source.getLength()));
				}
				panels.add(List.copyOf(panelPaddles));
			}
			sectors.add(List.copyOf(panels));
		}
		publish(counts, sectors);
	}

	public int paddleCount(int panel) {
		checkPanel(panel);
		ensureInitialized();
		return paddleCounts[panel];
	}

	public Paddle paddle(int sector, int panel, int paddle) {
		checkSector(sector);
		checkPanel(panel);
		ensureInitialized();
		if (paddle < 1 || paddle > paddleCounts[panel]) {
			throw new IllegalArgumentException("FTOF paddle out of range: " + paddle);
		}
		return paddles.get(sector - 1).get(panel).get(paddle - 1);
	}

	public List<Point3> frontFace(int sector, int panel, int paddle) {
		List<Point3> corners = paddle(sector, panel, paddle).corners();
		return List.of(corners.get(0), corners.get(1), corners.get(5), corners.get(4));
	}

	public float[] verticesCm(int sector, int panel, int paddle) {
		List<Point3> corners = paddle(sector, panel, paddle).corners();
		float[] values = new float[CORNER_COUNT * 3];
		for (int corner = 0; corner < CORNER_COUNT; corner++) {
			Point3 point = corners.get(corner);
			values[3 * corner] = (float) point.x();
			values[3 * corner + 1] = (float) point.y();
			values[3 * corner + 2] = (float) point.z();
		}
		return values;
	}

	@Override
	public void read(DataInput input) throws IOException {
		int sectors = input.readInt();
		int panels = input.readInt();
		if (sectors != SECTOR_COUNT || panels != PANEL_COUNT) {
			throw new IOException("Invalid FTOF geometry dimensions");
		}
		int[] counts = new int[PANEL_COUNT];
		for (int panel = 0; panel < PANEL_COUNT; panel++) {
			counts[panel] = input.readInt();
			if (counts[panel] < 1 || counts[panel] > 100) {
				throw new IOException("Invalid FTOF paddle count: " + counts[panel]);
			}
		}
		ArrayList<List<List<Paddle>>> restored = new ArrayList<>(sectors);
		for (int sector = 0; sector < sectors; sector++) {
			ArrayList<List<Paddle>> sectorPanels = new ArrayList<>(panels);
			for (int panel = 0; panel < panels; panel++) {
				ArrayList<Paddle> panelPaddles = new ArrayList<>(counts[panel]);
				for (int paddle = 0; paddle < counts[panel]; paddle++) {
					ArrayList<Point3> corners = new ArrayList<>(CORNER_COUNT);
					for (int corner = 0; corner < CORNER_COUNT; corner++) corners.add(readPoint(input));
					ArrayList<Segment3> edges = new ArrayList<>(PROJECTION_EDGE_COUNT);
					for (int edge = 0; edge < PROJECTION_EDGE_COUNT; edge++) {
						edges.add(new Segment3(readPoint(input), readPoint(input)));
					}
					panelPaddles.add(new Paddle(corners, edges, input.readDouble()));
				}
				sectorPanels.add(List.copyOf(panelPaddles));
			}
			restored.add(List.copyOf(sectorPanels));
		}
		publish(counts, restored);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		ensureInitialized();
		output.writeInt(SECTOR_COUNT);
		output.writeInt(PANEL_COUNT);
		for (int count : paddleCounts) output.writeInt(count);
		for (List<List<Paddle>> sector : paddles) {
			for (List<Paddle> panel : sector) {
				for (Paddle paddle : panel) {
					for (Point3 point : paddle.corners()) writePoint(output, point);
					for (Segment3 edge : paddle.projectionEdges()) {
						writePoint(output, edge.start());
						writePoint(output, edge.end());
					}
					output.writeDouble(paddle.lengthCm());
				}
			}
		}
	}

	private void publish(int[] counts, List<List<List<Paddle>>> value) {
		if (counts.length != PANEL_COUNT || value.size() != SECTOR_COUNT) {
			throw new IllegalStateException("Invalid FTOF geometry dimensions");
		}
		for (List<List<Paddle>> sector : value) {
			if (sector.size() != PANEL_COUNT) throw new IllegalStateException("Invalid FTOF panel count");
			for (int panel = 0; panel < PANEL_COUNT; panel++) {
				if (sector.get(panel).size() != counts[panel]) {
					throw new IllegalStateException("Inconsistent FTOF paddle count");
				}
			}
		}
		paddleCounts = counts.clone();
		paddles = List.copyOf(value);
	}

	private static Point3 point(Point3D point) { return new Point3(point.x(), point.y(), point.z()); }
	private static Point3 readPoint(DataInput input) throws IOException {
		return new Point3(input.readDouble(), input.readDouble(), input.readDouble());
	}
	private static void writePoint(DataOutput output, Point3 point) throws IOException {
		output.writeDouble(point.x()); output.writeDouble(point.y()); output.writeDouble(point.z());
	}
	private static void checkSector(int sector) {
		if (sector < 1 || sector > SECTOR_COUNT) throw new IllegalArgumentException("FTOF sector out of range: " + sector);
	}
	private static void checkPanel(int panel) {
		if (panel < 0 || panel >= PANEL_COUNT) throw new IllegalArgumentException("FTOF panel out of range: " + panel);
	}
	private void ensureInitialized() {
		if (paddles.size() != SECTOR_COUNT) throw new IllegalStateException("FTOF geometry is not initialized");
	}
}
