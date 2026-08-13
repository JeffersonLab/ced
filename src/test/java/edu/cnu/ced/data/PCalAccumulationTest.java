package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PCalAccumulationTest {
	@Test
	void countsSectorViewStripOccupancy() {
		PCalAccumulation accumulation = new PCalAccumulation();
		PCalEventData.AdcHit hit = new PCalEventData.AdcHit(2, 1, 17, 250, 12f);
		accumulation.add(new PCalEventData(List.of(hit), List.of(), 250));
		accumulation.add(new PCalEventData(List.of(hit), List.of(), 250));
		assertEquals(2, accumulation.count(2, 1, 17));
		assertEquals(2, accumulation.maximumCount());
		assertEquals(2, accumulation.eventCount());
		accumulation.clear();
		assertEquals(0, accumulation.count(2, 1, 17));
	}
}
