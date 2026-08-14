package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class FTOFAccumulationTest {

	@Test
	void countsEachPaddleOnlyOncePerEvent() {
		FTOFAccumulation accumulation = new FTOFAccumulation();
		FTOFEventData.AdcHit left = new FTOFEventData.AdcHit(2, 0, 7, 0, 100, 10f);
		FTOFEventData.AdcHit right = new FTOFEventData.AdcHit(2, 0, 7, 1, 200, 11f);
		FTOFEventData.AdcHit panelTwo = new FTOFEventData.AdcHit(2, 2, 4, 0, 300, 12f);
		accumulation.add(new FTOFEventData(List.of(left, right, panelTwo), List.of(), List.of(), 300));
		accumulation.add(new FTOFEventData(List.of(left), List.of(), List.of(), 100));

		assertEquals(2, accumulation.count(2, 0, 7));
		assertEquals(1, accumulation.count(2, 2, 4));
		assertEquals(2, accumulation.maximumCount());
		assertEquals(2, accumulation.eventCount());
	}
}
