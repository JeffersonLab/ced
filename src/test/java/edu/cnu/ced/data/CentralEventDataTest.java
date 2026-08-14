package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;
import edu.cnu.ced.event.EventSnapshot;

class CentralEventDataTest {
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
