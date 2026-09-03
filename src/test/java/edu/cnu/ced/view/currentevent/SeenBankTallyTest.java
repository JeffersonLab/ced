package edu.cnu.ced.view.currentevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class SeenBankTallyTest {

	@Test void countsEachBankOncePerEventItAppearsIn() {
		SeenBankTally tally = new SeenBankTally();
		tally.accept(snapshot("BAND::adc", "CND::adc"));
		tally.accept(snapshot("BAND::adc"));
		tally.accept(snapshot("BAND::adc"));

		assertEquals(3, tally.count("BAND::adc"));
		assertEquals(1, tally.count("CND::adc"));
		assertEquals(0, tally.count("CTOF::adc"));
		assertEquals(List.of("[BAND::adc]  (3)", "[CND::adc]  (1)"), tally.summaries());
	}

	@Test void clearResetsTheTally() {
		SeenBankTally tally = new SeenBankTally();
		tally.accept(snapshot("BAND::adc"));
		tally.clear();
		assertEquals(0, tally.count("BAND::adc"));
		assertTrue(tally.summaries().isEmpty());
	}

	@Test void ignoresAnEmptySnapshot() {
		SeenBankTally tally = new SeenBankTally();
		tally.accept(EventSnapshot.empty());
		tally.accept(null);
		assertTrue(tally.summaries().isEmpty());
	}

	private static EventSnapshot snapshot(String... bankNames) {
		Map<String, DataBank> banks = new java.util.HashMap<>();
		for (String name : bankNames) {
			banks.put(name, (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
					new Class<?>[] { DataBank.class }, (proxy, method, args) -> null));
		}
		DataEvent event = (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
				new Class<?>[] { DataEvent.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getBankList" -> banks.keySet().toArray(String[]::new);
					case "hasBank" -> banks.containsKey(args[0]);
					case "getBank" -> banks.get(args[0]);
					default -> null;
				});
		return EventSnapshot.of(event);
	}
}
