package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AccumulationServiceTest {

	@Test
	void clearResetsEveryDetectorAccumulator() {
		AccumulationService service = new AccumulationService();
		EventSnapshot emptyEvent = EventSnapshot.empty();
		service.accumulate(emptyEvent);

		assertEquals(1, service.ftcal().eventCount());
		assertEquals(1, service.pcal().eventCount());
		assertEquals(1, service.ecal().eventCount());
		assertEquals(1, service.ftof().eventCount());
		assertEquals(1, service.central().eventCount());
		assertEquals(1, service.dc().eventCount());

		service.clear();

		assertEquals(0, service.ftcal().eventCount());
		assertEquals(0, service.pcal().eventCount());
		assertEquals(0, service.ecal().eventCount());
		assertEquals(0, service.ftof().eventCount());
		assertEquals(0, service.central().eventCount());
		assertEquals(0, service.dc().eventCount());
	}
}
