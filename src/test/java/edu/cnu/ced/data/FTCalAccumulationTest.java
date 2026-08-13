package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class FTCalAccumulationTest {
	@Test
	void countsPositiveAdcOccupancy() {
		FTCalAccumulation accumulation = new FTCalAccumulation();
		accumulation.add(data(10, 11));
		accumulation.add(data(10));
		assertEquals(2, accumulation.eventCount());
		assertEquals(2, accumulation.count(10));
		assertEquals(1, accumulation.count(11));
		assertEquals(2, accumulation.maximumCount());
		accumulation.clear();
		assertEquals(0, accumulation.eventCount());
		assertEquals(0, accumulation.count(10));
		assertEquals(0, accumulation.maximumCount());
	}

	private static FTCalEventData data(int... components) {
		return new FTCalEventData(java.util.Arrays.stream(components)
				.mapToObj(id -> new FTCalEventData.AdcHit(id, 10, 1, 0)).toList(),
				List.of(), 10);
	}
}
