package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;
import edu.cnu.ced.data.CentralEventData.Detector;
import edu.cnu.ced.event.EventSnapshot;

class CentralEventDataTest {
	// CTOF adc/clusters had no dedicated coverage: the one existing test above
	// only touches "CND::adc" incidentally, as a generic ADC bank for a
	// BST/BMT-cluster test, without asserting the CND tag or reading any CTOF
	// bank at all.
	@Test void extractsCtofAdcHits(){
		DataBank adc=bank(new String[]{"sector","layer","component","order","ADC","time"},1,Map.of(
				"sector",new byte[]{3},"layer",new byte[]{0},"component",new short[]{12},
				"order",new byte[]{0},"ADC",new int[]{555},"time",new float[]{12.5f}));
		CentralEventData data=CentralEventData.from(EventSnapshot.of(event(Map.of("CTOF::adc",adc))));
		assertEquals(1,data.adcHits().size());
		CentralEventData.AdcHit hit=data.adcHits().getFirst();
		assertEquals(Detector.CTOF,hit.detector()); assertEquals(3,hit.sector()); assertEquals(12,hit.component());
		assertEquals(555,hit.adc()); assertEquals(555,data.maximumAdc());
	}
	@Test void extractsCndAndCtofPointClustersDistinctFromEachOther(){
		DataBank cndClusters=bank(new String[]{"x","y","z","id","status","energy"},1,Map.of(
				"x",new float[]{1.5f},"y",new float[]{2.5f},"z",new float[]{3.5f},
				"id",new short[]{7},"status",new short[]{1},"energy",new float[]{0.8f}));
		DataBank ctofClusters=bank(new String[]{"x","y","z"},1,Map.of(
				"x",new float[]{4f},"y",new float[]{5f},"z",new float[]{6f}));
		CentralEventData data=CentralEventData.from(EventSnapshot.of(event(Map.of(
				"CND::clusters",cndClusters,"CTOF::clusters",ctofClusters))));
		assertEquals(2,data.clusters().size());
		CentralEventData.Cluster cnd=data.clusters().stream()
				.filter(c->c.detector()==Detector.CND).findFirst().orElseThrow();
		assertEquals(1.5f,cnd.x1()); assertEquals(2.5f,cnd.y1()); assertEquals(0.8f,cnd.energy());
		assertTrue(Float.isNaN(cnd.x2()), "point clusters have no second endpoint");
		CentralEventData.Cluster ctof=data.clusters().stream()
				.filter(c->c.detector()==Detector.CTOF).findFirst().orElseThrow();
		assertEquals(4f,ctof.x1()); assertEquals(6f,ctof.z());
		assertTrue(Float.isNaN(ctof.x2()), "point clusters have no second endpoint");
	}
	@Test void extractsCndTdcHits(){
		DataBank tdc=bank(new String[]{"sector","layer","component","order","TDC"},2,Map.of(
				"sector",new byte[]{5,5},"layer",new byte[]{2,2},"component",new short[]{1,1},
				"order",new byte[]{2,3},"TDC",new int[]{1234,1250}));
		CentralEventData data=CentralEventData.from(EventSnapshot.of(event(Map.of("CND::tdc",tdc))));
		assertEquals(2,data.tdcHits().size());
		CentralEventData.TdcHit left=data.tdcHits().stream()
				.filter(h->h.order()==2).findFirst().orElseThrow();
		assertEquals(5,left.sector()); assertEquals(2,left.layer()); assertEquals(1234,left.tdc());
		CentralEventData.TdcHit right=data.tdcHits().stream()
				.filter(h->h.order()==3).findFirst().orElseThrow();
		assertEquals(1250,right.tdc());
	}
	@Test void extractsPositiveAdcAndUsesPreferredClusterBankWithoutDuplication(){
		DataBank adc=bank(new String[]{"sector","layer","component","order","ADC","time"},2,Map.of(
				"sector",new byte[]{10,10},"layer",new byte[]{2,2},"component",new short[]{1,1},
				"order",new byte[]{0,1},"ADC",new int[]{0,4853},"time",new float[]{0,94.4f}));
		DataBank modern=bank(new String[]{"sector","x1","y1","x2","y2"},1,Map.of(
				"sector",new byte[]{2},"x1",new float[]{1},"y1",new float[]{2},"x2",new float[]{3},"y2",new float[]{4}));
		DataBank legacy=bank(new String[]{"sector","x1","y1","x2","y2"},2,Map.of(
				"sector",new byte[]{2,2},"x1",new float[]{5,6},"y1",new float[]{7,8},"x2",new float[]{9,10},"y2",new float[]{11,12}));
		CentralEventData data=CentralEventData.from(EventSnapshot.of(event(Map.of(
				"CND::adc",adc,"BMT::Clusters",modern,"BMTRec::Clusters",legacy))));
		assertEquals(1,data.adcHits().size()); assertEquals(4853,data.maximumAdc());
		assertEquals(1,data.clusters().size()); assertEquals(1f,data.clusters().getFirst().x1());
	}
	private static DataEvent event(Map<String,DataBank> banks){return(DataEvent)Proxy.newProxyInstance(DataEvent.class.getClassLoader(),new Class<?>[]{DataEvent.class},(o,m,a)->switch(m.getName()){case"getBankList"->banks.keySet().toArray(String[]::new);case"hasBank"->banks.containsKey(a[0]);case"getBank"->banks.get(a[0]);default->null;});}
	private static DataBank bank(String[]columns,int rows,Map<String,Object> values){return(DataBank)Proxy.newProxyInstance(DataBank.class.getClassLoader(),new Class<?>[]{DataBank.class},(o,m,a)->switch(m.getName()){case"getColumnList"->columns;case"rows"->rows;case"getShort"->((short[])values.get(a[0]))[(int)a[1]];case"getInt"->((int[])values.get(a[0]))[(int)a[1]];case"getFloat"->((float[])values.get(a[0]))[(int)a[1]];case"getByte"->((byte[])values.get(a[0]))[(int)a[1]];default->null;});}
}
