package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.geom.detector.ec.ECFactory;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Triangle3D;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable primitive geometry for PCAL U, V, and W strip views. */
public final class PCALGeometry implements CacheableGeometry {
	public static final int VIEW_COUNT = 3;
	public static final int SECTOR_COUNT = 6;
	public static final int[] STRIP_COUNTS = { 68, 62, 62 };
	private static final double DELTA_K_CM = 14.94;

	private volatile Point3 reference;
	private volatile double cosTheta;
	private volatile double sinTheta;
	private volatile double slope;
	private volatile Affine3 localToSector;
	private volatile Affine3 sectorToLocal;
	private volatile List<List<List<Point3>>> localFaces = List.of();
	private volatile List<List<List<Segment3>>> projectionEdges = List.of();
	private volatile List<List<List<Point3>>> viewTriangles = List.of();
	private volatile List<List<List<List<Point3>>>> stripVertices = List.of();

	@Override public String name() { return "PCAL"; }
	@Override public int formatVersion() { return 1; }

	@Override
	public void initializeFromSource() {
		var provider = GeometryFactory.getConstants(DetectorType.ECAL);
		ECFactory factory = new ECFactory();
		var clasSuperlayer = factory.createDetectorCLAS(provider).getSector(0).getSuperlayer(0);
		var localSuperlayer = factory.createDetectorLocal(provider).getSector(0).getSuperlayer(0);
		var sectorLayer = factory.createDetectorSector(provider).getSector(0).getSuperlayer(0).getLayer(0);
		Affine3 forward = Affine3.sample(sectorLayer.getTransformation());
		Affine3 inverse = Affine3.sample(sectorLayer.getTransformation().inverse());
		Point3 r0 = forward.apply(new Point3(0, 0, 0));
		double theta = Math.atan2(r0.x(), r0.z());

		ArrayList<List<List<Point3>>> faces = new ArrayList<>(VIEW_COUNT);
		ArrayList<List<List<Segment3>>> edgesByView = new ArrayList<>(VIEW_COUNT);
		ArrayList<List<List<Point3>>> trianglesBySector = new ArrayList<>(SECTOR_COUNT);
		ArrayList<List<List<List<Point3>>>> verticesBySector = new ArrayList<>(SECTOR_COUNT);
		for (int sector = 0; sector < SECTOR_COUNT; sector++) {
			trianglesBySector.add(new ArrayList<>(VIEW_COUNT));
			verticesBySector.add(new ArrayList<>(VIEW_COUNT));
		}
		double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
		for (int view = 0; view < VIEW_COUNT; view++) {
			var clasLayer = clasSuperlayer.getLayer(view);
			var localLayer = localSuperlayer.getLayer(view);
			ArrayList<List<Point3>> viewFaces = new ArrayList<>(STRIP_COUNTS[view]);
			ArrayList<List<Segment3>> viewEdges = new ArrayList<>(STRIP_COUNTS[view]);
			for (int strip = 0; strip < STRIP_COUNTS[view]; strip++) {
				var local = localLayer.getComponent(strip);
				List<Point3> face = List.of(point(local.getVolumePoint(4)), point(local.getVolumePoint(5)),
						point(local.getVolumePoint(1)), point(local.getVolumePoint(0)));
				viewFaces.add(face);
				for (Point3 point : face) { minX = Math.min(minX, point.x()); maxX = Math.max(maxX, point.x()); }
				var paddle = clasLayer.getComponent(strip);
				ArrayList<Segment3> stripEdges = new ArrayList<>(4);
				for (int edge = 0; edge < 4; edge++) { Line3D line = paddle.getVolumeEdge(6 + edge); stripEdges.add(new Segment3(point(line.origin()), point(line.end()))); }
				viewEdges.add(List.copyOf(stripEdges));
			}
			faces.add(List.copyOf(viewFaces)); edgesByView.add(List.copyOf(viewEdges));
			Triangle3D triangle = (Triangle3D) clasLayer.getBoundary().face(0);
			double distance = view * (DELTA_K_CM / 3.0);
			for (int sector = 0; sector < SECTOR_COUNT; sector++) {
				ArrayList<Point3> tri = new ArrayList<>(3);
				for (int corner = 0; corner < 3; corner++) tri.add(transform(point(triangle.point(corner)), distance, sector));
				((ArrayList<List<Point3>>) trianglesBySector.get(sector)).add(List.copyOf(tri));
				ArrayList<List<Point3>> strips = new ArrayList<>(STRIP_COUNTS[view]);
				for (int strip = 0; strip < STRIP_COUNTS[view]; strip++) {
					ArrayList<Point3> corners = new ArrayList<>(8);
					for (int corner = 0; corner < 8; corner++) corners.add(transform(point(clasLayer.getComponent(strip).getVolumePoint(corner)), distance, sector));
					strips.add(List.copyOf(corners));
				}
				((ArrayList<List<List<Point3>>>) verticesBySector.get(sector)).add(List.copyOf(strips));
			}
		}
		Point3 p0 = forward.apply(new Point3(minX, 0, 0));
		Point3 p3 = forward.apply(new Point3(maxX, 0, 0));
		publish(r0, Math.cos(theta), Math.sin(theta), (p0.x() - p3.x()) / (p0.z() - p3.z()),
				forward, inverse, faces, edgesByView, trianglesBySector, verticesBySector);
	}

	public Point3 reference() { ensureInitialized(); return reference; }
	public double cosTheta() { ensureInitialized(); return cosTheta; }
	public double sinTheta() { ensureInitialized(); return sinTheta; }
	public double zFromX(double x) { ensureInitialized(); return reference.z() + (x - reference.x()) / slope; }
	public Point3 localToSector(Point3 point) { ensureInitialized(); return localToSector.apply(point); }
	public Point3 sectorToLocal(Point3 point) { ensureInitialized(); return sectorToLocal.apply(point); }
	public List<Point3> localFace(int view, int strip) { checkStrip(view, strip); ensureInitialized(); return localFaces.get(view).get(strip - 1); }
	public List<Segment3> projectionEdges(int view, int strip) { checkStrip(view, strip); ensureInitialized(); return projectionEdges.get(view).get(strip - 1); }
	public List<Point3> viewTriangle(int sector, int view) { checkSectorView(sector, view); ensureInitialized(); return viewTriangles.get(sector - 1).get(view); }
	public List<Point3> stripVertices(int sector, int view, int strip) { checkSectorView(sector, view); checkStrip(view, strip); ensureInitialized(); return stripVertices.get(sector - 1).get(view).get(strip - 1); }

	@Override public void write(DataOutput out) throws IOException {
		ensureInitialized(); writePoint(out, reference); out.writeDouble(cosTheta); out.writeDouble(sinTheta); out.writeDouble(slope); localToSector.write(out); sectorToLocal.write(out);
		for (int view = 0; view < VIEW_COUNT; view++) { out.writeInt(STRIP_COUNTS[view]); for (int strip = 1; strip <= STRIP_COUNTS[view]; strip++) { for (Point3 p : localFace(view, strip)) writePoint(out, p); for (Segment3 e : projectionEdges(view, strip)) { writePoint(out, e.start()); writePoint(out, e.end()); } } }
		for (int sector = 1; sector <= SECTOR_COUNT; sector++) for (int view = 0; view < VIEW_COUNT; view++) { for (Point3 p : viewTriangle(sector, view)) writePoint(out, p); for (int strip = 1; strip <= STRIP_COUNTS[view]; strip++) for (Point3 p : stripVertices(sector, view, strip)) writePoint(out, p); }
	}

	@Override public void read(DataInput in) throws IOException {
		Point3 r0 = readPoint(in); double cos = in.readDouble(), sin = in.readDouble(), m = in.readDouble(); Affine3 forward = Affine3.read(in), inverse = Affine3.read(in);
		ArrayList<List<List<Point3>>> faces = new ArrayList<>(); ArrayList<List<List<Segment3>>> edges = new ArrayList<>();
		for (int view = 0; view < VIEW_COUNT; view++) { int count = in.readInt(); if (count != STRIP_COUNTS[view]) throw new IOException("Invalid PCAL strip count"); ArrayList<List<Point3>> vf = new ArrayList<>(); ArrayList<List<Segment3>> ve = new ArrayList<>(); for (int strip = 0; strip < count; strip++) { ArrayList<Point3> face = new ArrayList<>(); for (int i=0;i<4;i++) face.add(readPoint(in)); ArrayList<Segment3> se = new ArrayList<>(); for(int i=0;i<4;i++) se.add(new Segment3(readPoint(in),readPoint(in))); vf.add(List.copyOf(face)); ve.add(List.copyOf(se)); } faces.add(List.copyOf(vf)); edges.add(List.copyOf(ve)); }
		ArrayList<List<List<Point3>>> triangles = new ArrayList<>(); ArrayList<List<List<List<Point3>>>> vertices = new ArrayList<>();
		for(int sector=0;sector<SECTOR_COUNT;sector++){ ArrayList<List<Point3>> st=new ArrayList<>(); ArrayList<List<List<Point3>>> sv=new ArrayList<>(); for(int view=0;view<VIEW_COUNT;view++){ ArrayList<Point3> tri=new ArrayList<>(); for(int i=0;i<3;i++)tri.add(readPoint(in)); st.add(List.copyOf(tri)); ArrayList<List<Point3>> strips=new ArrayList<>(); for(int strip=0;strip<STRIP_COUNTS[view];strip++){ArrayList<Point3> corners=new ArrayList<>();for(int i=0;i<8;i++)corners.add(readPoint(in));strips.add(List.copyOf(corners));}sv.add(List.copyOf(strips));}triangles.add(List.copyOf(st));vertices.add(List.copyOf(sv));}
		publish(r0,cos,sin,m,forward,inverse,faces,edges,triangles,vertices);
	}

	private void publish(Point3 r0,double cos,double sin,double m,Affine3 forward,Affine3 inverse,List<List<List<Point3>>> faces,List<List<List<Segment3>>> edges,List<List<List<Point3>>> triangles,List<List<List<List<Point3>>>> vertices){reference=r0;cosTheta=cos;sinTheta=sin;slope=m;localToSector=forward;sectorToLocal=inverse;localFaces=List.copyOf(faces);projectionEdges=List.copyOf(edges);viewTriangles=List.copyOf(triangles);stripVertices=List.copyOf(vertices);}
	private static Point3 transform(Point3 p,double distance,int sector){double x=p.x()+distance*Math.sin(Math.toRadians(25)),z=p.z()+distance*Math.cos(Math.toRadians(25)),a=Math.toRadians(60*sector);return new Point3(x*Math.cos(a)-p.y()*Math.sin(a),x*Math.sin(a)+p.y()*Math.cos(a),z);}
	private static Point3 point(Point3D p){return new Point3(p.x(),p.y(),p.z());}
	private static void writePoint(DataOutput out,Point3 p)throws IOException{out.writeDouble(p.x());out.writeDouble(p.y());out.writeDouble(p.z());}
	private static Point3 readPoint(DataInput in)throws IOException{return new Point3(in.readDouble(),in.readDouble(),in.readDouble());}
	private static void checkSectorView(int sector,int view){if(sector<1||sector>SECTOR_COUNT||view<0||view>=VIEW_COUNT)throw new IllegalArgumentException("Invalid PCAL sector/view");}
	private static void checkStrip(int view,int strip){if(view<0||view>=VIEW_COUNT||strip<1||strip>STRIP_COUNTS[view])throw new IllegalArgumentException("Invalid PCAL view/strip");}
	private void ensureInitialized(){if(reference==null||stripVertices.size()!=SECTOR_COUNT)throw new IllegalStateException("PCAL geometry is not initialized");}
}
