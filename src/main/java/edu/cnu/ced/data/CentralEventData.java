package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;
import org.jlab.io.base.DataBank;
import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/** Immutable central-detector data extracted directly from one event snapshot. */
public record CentralEventData(List<AdcHit> adcHits, List<ReconHit> reconHits,
		List<Cluster> clusters, List<Cross> crosses, List<TdcHit> tdcHits, int maximumAdc) {

	public enum Detector { BST, BMT, CND, CTOF }
	private static final CentralEventData EMPTY =
			new CentralEventData(List.of(), List.of(), List.of(), List.of(), List.of(), 0);

	public CentralEventData {
		adcHits=List.copyOf(adcHits); reconHits=List.copyOf(reconHits);
		clusters=List.copyOf(clusters); crosses=List.copyOf(crosses); tdcHits=List.copyOf(tdcHits);
	}

	public static CentralEventData from(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) return EMPTY;
		ArrayList<AdcHit> adc=new ArrayList<>(); ArrayList<ReconHit> hits=new ArrayList<>();
		ArrayList<Cluster> clusters=new ArrayList<>(); ArrayList<Cross> crosses=new ArrayList<>();
		ArrayList<TdcHit> tdc=new ArrayList<>();
		int max=0;
		max=Math.max(max, readAdc(snapshot, Detector.BST, "BST::adc", adc));
		max=Math.max(max, readAdc(snapshot, Detector.BMT, "BMT::adc", adc));
		max=Math.max(max, readAdc(snapshot, Detector.CND, "CND::adc", adc));
		max=Math.max(max, readAdc(snapshot, Detector.CTOF, "CTOF::adc", adc));
		readHits(snapshot, Detector.BST, "BSTRec::Hits", hits);
		readHits(snapshot, Detector.BMT, "BMTRec::Hits", hits);
		readPointClusters(snapshot, Detector.CND, "CND::clusters", clusters);
		readPointClusters(snapshot, Detector.CTOF, "CTOF::clusters", clusters);
		readEndpointClusters(snapshot, Detector.BST, "BST::Clusters", "BSTRec::Clusters", clusters);
		readEndpointClusters(snapshot, Detector.BMT, "BMT::Clusters", "BMTRec::Clusters", clusters);
		readCrosses(snapshot, Detector.BST, "BSTRec::Crosses", crosses);
		readCrosses(snapshot, Detector.BMT, "BMTRec::Crosses", crosses);
		readTdc(snapshot, "CND::tdc", tdc);
		return new CentralEventData(adc,hits,clusters,crosses,tdc,max);
	}

	private static int readAdc(EventSnapshot s, Detector d, String name, List<AdcHit> out) {
		DataBank b=s.bank(name).orElse(null); if (!has(b,"sector","layer","component","order","ADC","time")) return 0;
		int max=0; for(int r=0;r<b.rows();r++){int adc=b.getInt("ADC",r); if(adc<=0)continue;
			out.add(new AdcHit(d,b.getByte("sector",r),b.getByte("layer",r),b.getShort("component",r),b.getByte("order",r),adc,b.getFloat("time",r))); max=Math.max(max,adc);}
		return max;
	}
	private static void readHits(EventSnapshot s,Detector d,String name,List<ReconHit> out){DataBank b=s.bank(name).orElse(null);if(!has(b,"sector","layer","strip"))return;
		for(int r=0;r<b.rows();r++)out.add(new ReconHit(d,r,b.getByte("sector",r),b.getByte("layer",r),b.getShort("strip",r),shortValue(b,"ID",r),floatValue(b,"energy",r),floatValue(b,"time",r),shortValue(b,"clusterID",r),shortValue(b,"trkID",r)));}
	private static void readPointClusters(EventSnapshot s,Detector d,String name,List<Cluster> out){DataBank b=s.bank(name).orElse(null);if(!has(b,"x","y","z"))return;
		for(int r=0;r<b.rows();r++)out.add(new Cluster(d,r,floatValue(b,"x",r),floatValue(b,"y",r),floatValue(b,"z",r),Float.NaN,Float.NaN,shortValue(b,"id",r),shortValue(b,"status",r),floatValue(b,"energy",r)));}
	private static void readEndpointClusters(EventSnapshot s,Detector d,String preferred,String legacy,List<Cluster> out){DataBank b=s.bank(preferred).orElseGet(()->s.bank(legacy).orElse(null));if(!has(b,"x1","y1","x2","y2"))return;
		for(int r=0;r<b.rows();r++)out.add(new Cluster(d,r,b.getFloat("x1",r),b.getFloat("y1",r),0,b.getFloat("x2",r),b.getFloat("y2",r),0,0,0));}
	private static void readCrosses(EventSnapshot s,Detector d,String name,List<Cross> out){DataBank b=s.bank(name).orElse(null);if(!has(b,"x","y","z"))return;
		for(int r=0;r<b.rows();r++)out.add(new Cross(d,r,floatValue(b,"x",r),floatValue(b,"y",r),floatValue(b,"z",r),shortValue(b,"ID",r),byteValue(b,"sector",r),byteValue(b,"region",r)));}
	/**
	 * {@code CND::tdc} -- raw TDC-only channels, CND-specific (unlike
	 * {@code AdcHit}, which is shared across BST/BMT/CND/CTOF); there is
	 * no analogous TDC bank for the other central detectors. Same
	 * (sector, layer, component, order) addressing convention as
	 * {@code CND::adc}, so a TdcHit and an AdcHit for the same physical
	 * channel share those four values.
	 */
	private static void readTdc(EventSnapshot s,String name,List<TdcHit> out){DataBank b=s.bank(name).orElse(null);if(!has(b,"sector","layer","component","order","TDC"))return;
		for(int r=0;r<b.rows();r++)out.add(new TdcHit(b.getByte("sector",r),b.getByte("layer",r),b.getShort("component",r),b.getByte("order",r),b.getInt("TDC",r)));}
	private static boolean has(DataBank b,String...n){if(b==null)return false;for(String x:n)if(!BankAccess.hasColumn(b,x))return false;return true;}
	private static float floatValue(DataBank b,String n,int r){return BankAccess.hasColumn(b,n)?b.getFloat(n,r):Float.NaN;}
	private static short shortValue(DataBank b,String n,int r){return BankAccess.hasColumn(b,n)?b.getShort(n,r):0;}
	private static byte byteValue(DataBank b,String n,int r){return BankAccess.hasColumn(b,n)?b.getByte(n,r):0;}

	public record AdcHit(Detector detector,int sector,int layer,int component,int order,int adc,float time){}
	public record ReconHit(Detector detector,int row,int sector,int layer,int strip,int id,float energy,float time,int clusterId,int trackId){}
	/** Point clusters have x2/y2 NaN; endpoint clusters contain both transverse endpoints. */
	public record Cluster(Detector detector,int row,float x1,float y1,float z,float x2,float y2,int id,int status,float energy){}
	public record Cross(Detector detector,int row,float x,float y,float z,int id,int sector,int region){}
	public record TdcHit(int sector,int layer,int component,int order,int tdc){}
}
