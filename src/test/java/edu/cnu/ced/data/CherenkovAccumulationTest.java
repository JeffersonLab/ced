package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class CherenkovAccumulationTest {

	@Test
	void countsEachCellOncePerEventAndClears() {
		CherenkovAccumulation accumulation = new CherenkovAccumulation(4);
		CherenkovEventData.AdcHit left = new CherenkovEventData.AdcHit(2, 1, 3, 0, 100, 10f);
		CherenkovEventData.AdcHit right = new CherenkovEventData.AdcHit(2, 1, 3, 1, 200, 11f);
		CherenkovEventData.AdcHit other = new CherenkovEventData.AdcHit(2, 2, 4, 0, 300, 12f);
		accumulation.add(new CherenkovEventData("HTCC", List.of(left, right, other),
				List.of(), List.of(), 300));
		accumulation.add(new CherenkovEventData("HTCC", List.of(left),
				List.of(), List.of(), 100));

		assertEquals(2, accumulation.count(2, 1, 3));
		assertEquals(1, accumulation.count(2, 2, 4));
		assertEquals(2, accumulation.maximumCount());
		assertEquals(2, accumulation.eventCount());

		accumulation.clear();
		assertEquals(0, accumulation.count(2, 1, 3));
		assertEquals(0, accumulation.maximumCount());
		assertEquals(0, accumulation.eventCount());
	}
}
