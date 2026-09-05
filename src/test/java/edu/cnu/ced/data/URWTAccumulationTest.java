package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class URWTAccumulationTest {

	@Test void countsHitOccupancyByAddressAndClears() {
		URWTAccumulation accumulation = new URWTAccumulation();
		URWTEventData.Hit hit = new URWTEventData.Hit(0, 3, 2, 44, 120f, 6.5f, 0, 0);
		accumulation.add(new URWTEventData(List.of(hit), List.of(), List.of()));
		accumulation.add(new URWTEventData(List.of(hit), List.of(), List.of()));

		assertEquals(2, accumulation.count(3, 2, 44));
		assertEquals(2, accumulation.maximumCount());
		assertEquals(2, accumulation.eventCount());

		accumulation.clear();
		assertEquals(0, accumulation.count(3, 2, 44));
		assertEquals(0, accumulation.maximumCount());
		assertEquals(0, accumulation.eventCount());
	}

	@Test void unaddressedCombinationsCountAsZero() {
		URWTAccumulation accumulation = new URWTAccumulation();
		assertEquals(0, accumulation.count(1, 1, 1));
	}

	@Test void nullDataIsANoOp() {
		URWTAccumulation accumulation = new URWTAccumulation();
		accumulation.add(null);
		assertEquals(0, accumulation.eventCount());
	}
}
