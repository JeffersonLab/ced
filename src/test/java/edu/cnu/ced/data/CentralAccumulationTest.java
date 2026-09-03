package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import edu.cnu.ced.data.CentralEventData.AdcHit;
import edu.cnu.ced.data.CentralEventData.Detector;

class CentralAccumulationTest {
	@Test void countsEachReadoutChannelOncePerEvent(){
		CentralAccumulation a=new CentralAccumulation();
		AdcHit left=new AdcHit(Detector.CND,10,2,1,0,100,1f);
		AdcHit duplicate=new AdcHit(Detector.CND,10,2,1,0,200,2f);
		AdcHit right=new AdcHit(Detector.CND,10,2,1,1,300,3f);
		a.add(new CentralEventData(List.of(left,duplicate,right),List.of(),List.of(),List.of(),List.of(),300));
		a.add(new CentralEventData(List.of(left),List.of(),List.of(),List.of(),List.of(),100));
		assertEquals(2,a.count(left)); assertEquals(1,a.count(right));
		assertEquals(2,a.maximumCount()); assertEquals(2,a.eventCount());
	}
}
