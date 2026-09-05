package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class FMTAccumulationTest {

	@Test void countsLayerStripOccupancyAndClears() {
		FMTAccumulation accumulation = new FMTAccumulation();
		FMTEventData.AdcHit hit = new FMTEventData.AdcHit(2, 500, 0, 1200, 12f);
		accumulation.add(new FMTEventData(List.of(hit), List.of(), List.of(), List.of(), 1200));
		accumulation.add(new FMTEventData(List.of(hit), List.of(), List.of(), List.of(), 1200));

		assertEquals(2, accumulation.count(2, 500));
		assertEquals(2, accumulation.maximumCount());
		assertEquals(2, accumulation.eventCount());

		accumulation.clear();
		assertEquals(0, accumulation.count(2, 500));
		assertEquals(0, accumulation.maximumCount());
		assertEquals(0, accumulation.eventCount());
	}

	@Test void ignoresOutOfRangeLayerOrStrip() {
		FMTAccumulation accumulation = new FMTAccumulation();
		accumulation.add(new FMTEventData(
				List.of(new FMTEventData.AdcHit(-1, 500, 0, 100, 1f),
						new FMTEventData.AdcHit(6, 500, 0, 100, 1f),
						new FMTEventData.AdcHit(2, 0, 0, 100, 1f),
						new FMTEventData.AdcHit(2, 1025, 0, 100, 1f)),
				List.of(), List.of(), List.of(), 100));
		assertEquals(0, accumulation.maximumCount());
	}

	@Test void nullDataIsANoOp() {
		FMTAccumulation accumulation = new FMTAccumulation();
		accumulation.add(null);
		assertEquals(0, accumulation.eventCount());
	}
}
