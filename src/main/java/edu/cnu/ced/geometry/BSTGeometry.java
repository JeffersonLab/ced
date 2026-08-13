package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.detector.geant4.v2.SVT.SVTStripFactory;
import org.jlab.geometry.prim.Line3d;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable strip-line geometry for the six-layer Barrel Silicon Tracker. */
public final class BSTGeometry implements CacheableGeometry {
	public static final int LAYER_COUNT=6, STRIP_COUNT=256;
	public static final int[] SECTORS_PER_LAYER={10,10,14,14,18,18};
	private static final double Z_GAP_MM=1.67;
	private volatile List<List<List<Segment3>>> strips=List.of();
	@Override public String name(){return "BST";} @Override public int formatVersion(){return 1;}
	@Override public void initializeFromSource(){initializeFromSource("default");}
	@Override public void initializeFromSource(String variation){DatabaseConstantProvider provider=new DatabaseConstantProvider(11,variation);SVTStripFactory factory=new SVTStripFactory(provider,true);ArrayList<List<List<Segment3>>> layers=new ArrayList<>();for(int layer=0;layer<LAYER_COUNT;layer++){ArrayList<List<Segment3>> sectors=new ArrayList<>();for(int sector=0;sector<SECTORS_PER_LAYER[layer];sector++){ArrayList<Segment3> layerStrips=new ArrayList<>();for(int strip=0;strip<STRIP_COUNT;strip++){Line3d line=factory.getStrip(layer,sector,strip);layerStrips.add(new Segment3(point(line.origin().x,line.origin().y,line.origin().z),point(line.end().x,line.end().y,line.end().z)));}sectors.add(List.copyOf(layerStrips));}layers.add(List.copyOf(sectors));}publish(layers);}
	public Segment3 strip(int sector,int layer,int strip){check(sector,layer,strip);ensure();return strips.get(layer).get(sector).get(strip);}
	public Point3 midpoint(int sector,int layer,int strip){Segment3 s=strip(sector,layer,strip);return new Point3((s.start().x()+s.end().x())/2,(s.start().y()+s.end().y())/2,(s.start().z()+s.end().z())/2);}
	/** Panel limits: x1,y1,x2,y2 followed by three active z intervals, in mm. */
	public double[] panelLimits(int sector,int layer){check(sector,layer,0);ensure();Segment3 first=strip(sector,layer,0),last=strip(sector,layer,STRIP_COUNT-1);double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;for(Segment3 s:strips.get(layer).get(sector)){min=Math.min(min,Math.min(s.start().z(),s.end().z()));max=Math.max(max,Math.max(s.start().z(),s.end().z()));}double d=(max-min)/3,z1=min+d-Z_GAP_MM/2,z2=z1+Z_GAP_MM,z3=max-d-Z_GAP_MM/2,z4=z3+Z_GAP_MM;return new double[]{first.start().x(),first.start().y(),last.end().x(),last.end().y(),min,z1,z2,z3,z4,max};}
	public float[] stripCm(int sector,int layer,int strip){Segment3 s=strip(sector,layer,strip);return new float[]{(float)(s.start().x()/10),(float)(s.start().y()/10),(float)(s.start().z()/10),(float)(s.end().x()/10),(float)(s.end().y()/10),(float)(s.end().z()/10)};}
	@Override public void write(DataOutput out)throws IOException{ensure();out.writeInt(LAYER_COUNT);out.writeInt(STRIP_COUNT);for(int layer=0;layer<LAYER_COUNT;layer++){out.writeInt(SECTORS_PER_LAYER[layer]);for(List<Segment3> sector:strips.get(layer))for(Segment3 s:sector){writePoint(out,s.start());writePoint(out,s.end());}}}
	@Override public void read(DataInput in)throws IOException{int layers=in.readInt(),count=in.readInt();if(layers!=LAYER_COUNT||count!=STRIP_COUNT)throw new IOException("Invalid BST dimensions");ArrayList<List<List<Segment3>>> restored=new ArrayList<>();for(int layer=0;layer<layers;layer++){int sectors=in.readInt();if(sectors!=SECTORS_PER_LAYER[layer])throw new IOException("Invalid BST sector count");ArrayList<List<Segment3>> ls=new ArrayList<>();for(int sector=0;sector<sectors;sector++){ArrayList<Segment3> ss=new ArrayList<>();for(int strip=0;strip<count;strip++)ss.add(new Segment3(readPoint(in),readPoint(in)));ls.add(List.copyOf(ss));}restored.add(List.copyOf(ls));}publish(restored);}
	private void publish(List<List<List<Segment3>>> value){if(value.size()!=LAYER_COUNT)throw new IllegalStateException("Invalid BST layer count");strips=List.copyOf(value);}private static Point3 point(double x,double y,double z){return new Point3(x,y,z);}private static void writePoint(DataOutput out,Point3 p)throws IOException{out.writeDouble(p.x());out.writeDouble(p.y());out.writeDouble(p.z());}private static Point3 readPoint(DataInput in)throws IOException{return new Point3(in.readDouble(),in.readDouble(),in.readDouble());}private static void check(int sector,int layer,int strip){if(layer<0||layer>=LAYER_COUNT||sector<0||sector>=SECTORS_PER_LAYER[layer]||strip<0||strip>=STRIP_COUNT)throw new IllegalArgumentException("Invalid BST address");}private void ensure(){if(strips.size()!=LAYER_COUNT)throw new IllegalStateException("BST geometry is not initialized");}
}
