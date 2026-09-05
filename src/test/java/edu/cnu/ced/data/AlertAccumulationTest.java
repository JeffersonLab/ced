package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class AlertAccumulationTest {

	@Test void countsWireOccupancyAndClears() {
		AlertAccumulation accumulation = new AlertAccumulation();
		AlertEventData.DcAdcHit hit = new AlertEventData.DcAdcHit(0, 2, 1, 14, 800, 5f);
		accumulation.add(new AlertEventData(List.of(hit), List.of(), List.of(), List.of(), List.of(), 800));
		accumulation.add(new AlertEventData(List.of(hit), List.of(), List.of(), List.of(), List.of(), 800));

		assertEquals(2, accumulation.count(2, 1, 14));
		assertEquals(2, accumulation.maximumCount());
		assertEquals(2, accumulation.eventCount());

		accumulation.clear();
		assertEquals(0, accumulation.count(2, 1, 14));
		assertEquals(0, accumulation.maximumCount());
		assertEquals(0, accumulation.eventCount());
	}

	@Test void unaddressedWireCountsAsZero() {
		AlertAccumulation accumulation = new AlertAccumulation();
		assertEquals(0, accumulation.count(0, 0, 0));
	}

	@Test void nullDataIsANoOp() {
		AlertAccumulation accumulation = new AlertAccumulation();
		accumulation.add(null);
		assertEquals(0, accumulation.eventCount());
	}
}
