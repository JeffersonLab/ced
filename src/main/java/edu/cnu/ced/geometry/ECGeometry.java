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

/** Immutable inner- and outer-stack electromagnetic calorimeter geometry. */
public final class ECGeometry implements CacheableGeometry {
	public static final int INNER=0, OUTER=1, STACK_COUNT=2, VIEW_COUNT=3, SECTOR_COUNT=6, STRIP_COUNT=36;
	private volatile List<Stack> stacks=List.of();
	private volatile List<List<List<List<Point3>>>> triangles=List.of();
	private volatile List<List<List<List<List<Point3>>>>> vertices=List.of();
	public record Stack(Point3 reference,double deltaK,double slope,Affine3 localToSector,Affine3 sectorToLocal,List<List<List<Point3>>> localFaces,List<List<List<Segment3>>> edges){public Stack{localFaces=List.copyOf(localFaces);edges=List.copyOf(edges);}}
	@Override public String name(){return "EC";} @Override public int formatVersion(){return 1;}
	@Override public void initializeFromSource(){
		var provider=GeometryFactory.getConstants(DetectorType.ECAL);ECFactory factory=new ECFactory();
		var clas=factory.createDetectorCLAS(provider).getSector(0);var local=factory.createDetectorLocal(provider).getSector(0);var sector=factory.createDetectorSector(provider).getSector(0);
		Affine3[] forward=new Affine3[2],inverse=new Affine3[2];Point3[] r0=new Point3[2];
		for(int stack=0;stack<2;stack++){var transform=sector.getSuperlayer(stack+1).getLayer(0).getTransformation();forward[stack]=Affine3.sample(transform);inverse[stack]=Affine3.sample(transform.inverse());r0[stack]=forward[stack].apply(new Point3(0,0,0));}
		double innerDelta=distance(r0[1])-distance(r0[0]);double[] delta={innerDelta,1.5*innerDelta};ArrayList<Stack> result=new ArrayList<>();
		ArrayList<List<List<List<Point3>>>> allTriangles=new ArrayList<>();ArrayList<List<List<List<List<Point3>>>>> allVertices=new ArrayList<>();for(int sectorIndex=0;sectorIndex<6;sectorIndex++){allTriangles.add(new ArrayList<>());allVertices.add(new ArrayList<>());}
		for(int stack=0;stack<2;stack++){ArrayList<List<List<Point3>>> faces=new ArrayList<>();ArrayList<List<List<Segment3>>> edges=new ArrayList<>();double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
			for(int view=0;view<3;view++){var cl=clas.getSuperlayer(stack+1).getLayer(view);var ll=local.getSuperlayer(stack+1).getLayer(view);ArrayList<List<Point3>> vf=new ArrayList<>();ArrayList<List<Segment3>> ve=new ArrayList<>();
				for(int strip=0;strip<36;strip++){var lp=ll.getComponent(strip);List<Point3> face=List.of(point(lp.getVolumePoint(4)),point(lp.getVolumePoint(5)),point(lp.getVolumePoint(1)),point(lp.getVolumePoint(0)));vf.add(face);for(Point3 p:face){min=Math.min(min,p.x());max=Math.max(max,p.x());}var cp=cl.getComponent(strip);ArrayList<Segment3> se=new ArrayList<>();for(int e=0;e<4;e++){Line3D line=cp.getVolumeEdge(6+e);se.add(new Segment3(point(line.origin()),point(line.end())));}ve.add(List.copyOf(se));}faces.add(List.copyOf(vf));edges.add(List.copyOf(ve));
				Triangle3D boundary=(Triangle3D)cl.getBoundary().face(0);double d=view*delta[stack]/3;
				for(int s=0;s<6;s++){while(allTriangles.get(s).size()<=stack)((ArrayList<List<List<Point3>>>)allTriangles.get(s)).add(new ArrayList<>());while(allVertices.get(s).size()<=stack)((ArrayList<List<List<List<Point3>>>>)allVertices.get(s)).add(new ArrayList<>());ArrayList<Point3> tri=new ArrayList<>();for(int i=0;i<3;i++)tri.add(transform(point(boundary.point(i)),d,s));((ArrayList<List<Point3>>)allTriangles.get(s).get(stack)).add(List.copyOf(tri));ArrayList<List<Point3>> strips=new ArrayList<>();for(int strip=0;strip<36;strip++){ArrayList<Point3> cs=new ArrayList<>();for(int i=0;i<8;i++)cs.add(transform(point(cl.getComponent(strip).getVolumePoint(i)),d,s));strips.add(List.copyOf(cs));}((ArrayList<List<List<Point3>>>)allVertices.get(s).get(stack)).add(List.copyOf(strips));}
			}Point3 a=forward[stack].apply(new Point3(min,0,0)),b=forward[stack].apply(new Point3(max,0,0));result.add(new Stack(r0[stack],delta[stack],(a.x()-b.x())/(a.z()-b.z()),forward[stack],inverse[stack],faces,edges));}
		publish(result,allTriangles,allVertices);
	}
	public Stack stack(int stack){checkStack(stack);ensure();return stacks.get(stack);}public double zFromX(int stack,double x){Stack s=stack(stack);return s.reference().z()+(x-s.reference().x())/s.slope();}
	public List<Point3> viewTriangle(int sector,int stack,int view){check(sector,stack,view,1);ensure();return triangles.get(sector-1).get(stack).get(view);}public List<Point3> stripVertices(int sector,int stack,int view,int strip){check(sector,stack,view,strip);ensure();return vertices.get(sector-1).get(stack).get(view).get(strip-1);}
	@Override public void write(DataOutput out)throws IOException{ensure();for(Stack s:stacks){writePoint(out,s.reference());out.writeDouble(s.deltaK());out.writeDouble(s.slope());s.localToSector().write(out);s.sectorToLocal().write(out);for(int v=0;v<3;v++)for(int p=0;p<36;p++){for(Point3 q:s.localFaces().get(v).get(p))writePoint(out,q);for(Segment3 e:s.edges().get(v).get(p)){writePoint(out,e.start());writePoint(out,e.end());}}}for(int sec=1;sec<=6;sec++)for(int st=0;st<2;st++)for(int v=0;v<3;v++){for(Point3 p:viewTriangle(sec,st,v))writePoint(out,p);for(int strip=1;strip<=36;strip++)for(Point3 p:stripVertices(sec,st,v,strip))writePoint(out,p);}}
	@Override public void read(DataInput in)throws IOException{ArrayList<Stack> ss=new ArrayList<>();for(int st=0;st<2;st++){Point3 r=readPoint(in);double d=in.readDouble(),m=in.readDouble();Affine3 f=Affine3.read(in),iv=Affine3.read(in);ArrayList<List<List<Point3>>> fs=new ArrayList<>();ArrayList<List<List<Segment3>>> es=new ArrayList<>();for(int v=0;v<3;v++){ArrayList<List<Point3>> vf=new ArrayList<>();ArrayList<List<Segment3>> ve=new ArrayList<>();for(int p=0;p<36;p++){ArrayList<Point3> face=new ArrayList<>();for(int i=0;i<4;i++)face.add(readPoint(in));ArrayList<Segment3> edge=new ArrayList<>();for(int i=0;i<4;i++)edge.add(new Segment3(readPoint(in),readPoint(in)));vf.add(List.copyOf(face));ve.add(List.copyOf(edge));}fs.add(List.copyOf(vf));es.add(List.copyOf(ve));}ss.add(new Stack(r,d,m,f,iv,fs,es));}ArrayList<List<List<List<Point3>>>> ts=new ArrayList<>();ArrayList<List<List<List<List<Point3>>>>> vs=new ArrayList<>();for(int sec=0;sec<6;sec++){ArrayList<List<List<Point3>>> stt=new ArrayList<>();ArrayList<List<List<List<Point3>>>> stv=new ArrayList<>();for(int st=0;st<2;st++){ArrayList<List<Point3>> vt=new ArrayList<>();ArrayList<List<List<Point3>>> vv=new ArrayList<>();for(int v=0;v<3;v++){ArrayList<Point3> tri=new ArrayList<>();for(int i=0;i<3;i++)tri.add(readPoint(in));vt.add(List.copyOf(tri));ArrayList<List<Point3>> strips=new ArrayList<>();for(int p=0;p<36;p++){ArrayList<Point3> cs=new ArrayList<>();for(int i=0;i<8;i++)cs.add(readPoint(in));strips.add(List.copyOf(cs));}vv.add(List.copyOf(strips));}stt.add(List.copyOf(vt));stv.add(List.copyOf(vv));}ts.add(List.copyOf(stt));vs.add(List.copyOf(stv));}publish(ss,ts,vs);}
	private void publish(List<Stack>s,List<List<List<List<Point3>>>>t,List<List<List<List<List<Point3>>>>>v){stacks=List.copyOf(s);triangles=List.copyOf(t);vertices=List.copyOf(v);}private static Point3 transform(Point3 p,double d,int s){double x=p.x()+d*Math.sin(Math.toRadians(25)),z=p.z()+d*Math.cos(Math.toRadians(25)),a=Math.toRadians(60*s);return new Point3(x*Math.cos(a)-p.y()*Math.sin(a),x*Math.sin(a)+p.y()*Math.cos(a),z);}private static double distance(Point3 p){return Math.sqrt(p.x()*p.x()+p.y()*p.y()+p.z()*p.z());}private static Point3 point(Point3D p){return new Point3(p.x(),p.y(),p.z());}private static void writePoint(DataOutput o,Point3 p)throws IOException{o.writeDouble(p.x());o.writeDouble(p.y());o.writeDouble(p.z());}private static Point3 readPoint(DataInput i)throws IOException{return new Point3(i.readDouble(),i.readDouble(),i.readDouble());}private static void checkStack(int s){if(s<0||s>1)throw new IllegalArgumentException("Invalid EC stack");}private static void check(int sec,int st,int v,int strip){if(sec<1||sec>6||st<0||st>1||v<0||v>2||strip<1||strip>36)throw new IllegalArgumentException("Invalid EC address");}private void ensure(){if(stacks.size()!=2)throw new IllegalStateException("EC geometry is not initialized");}
}
