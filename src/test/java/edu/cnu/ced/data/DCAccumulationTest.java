package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import java.util.List;

import org.junit.jupiter.api.Test;

class DCAccumulationTest {
	@Test
	void countsWireOccupancyAndClears() {
		DCAccumulation accumulation = new DCAccumulation();
		var hit = new DCEventData.RawHit(0, 2, 3, 4, 55, 0, 120);
		accumulation.add(new DCEventData(List.of(hit), List.of()));
		accumulation.add(new DCEventData(List.of(hit), List.of()));
		assertEquals(2, accumulation.count(2, 3, 4, 55));
		assertEquals(2, accumulation.eventCount());
		assertEquals(2, accumulation.maximumCount());
		assertEquals(2, accumulation.maximumCount(3));
		assertEquals(0, accumulation.maximumCount(4));
		accumulation.clear();
		assertEquals(0, accumulation.count(2, 3, 4, 55));
		assertEquals(0, accumulation.eventCount());
		assertEquals(0, accumulation.maximumCount(3));
	}

	@Test
	void percentileCeilingIsNotDominatedByOneHotWire() {
		DCAccumulation accumulation = new DCAccumulation();
		List<DCEventData.RawHit> hits = new ArrayList<>();
		for (int wire = 1; wire <= 19; wire++)
			hits.add(new DCEventData.RawHit(wire, 1, 2, 1, wire, 0, 100));
		for (int occurrence = 0; occurrence < 100; occurrence++)
			hits.add(new DCEventData.RawHit(1000 + occurrence, 1, 2, 1, 112, 0, 100));
		accumulation.add(new DCEventData(hits, List.of()));

		assertEquals(1, accumulation.percentileCount(2, 0.95));
		assertEquals(100, accumulation.maximumCount(2));
		assertEquals(0, accumulation.percentileCount(3, 0.95));
	}
}
