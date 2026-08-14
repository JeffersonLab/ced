package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ECalAccumulationTest {
	@Test
	void separatesInnerAndOuterOccupancy() {
		ECalAccumulation accumulation = new ECalAccumulation();
		ECalEventData.AdcHit inner = new ECalEventData.AdcHit(2, 0, 1, 17, 250, 12f);
		ECalEventData.AdcHit outer = new ECalEventData.AdcHit(2, 1, 1, 17, 300, 13f);
		accumulation.add(new ECalEventData(List.of(inner, outer), List.of(), 300));
		accumulation.add(new ECalEventData(List.of(inner), List.of(), 250));
		assertEquals(2, accumulation.count(2, 0, 1, 17));
		assertEquals(1, accumulation.count(2, 1, 1, 17));
		assertEquals(2, accumulation.maximumCount(0));
		assertEquals(1, accumulation.maximumCount(1));
		assertEquals(2, accumulation.eventCount());
	}
}
