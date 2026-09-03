package edu.cnu.ced.view.currentevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataDescriptor;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.data.BankColumns;
import edu.cnu.ced.event.EventSnapshot;

class BankColumnCatalogTest {

	@Test void emptySnapshotYieldsNoEntries() {
		assertTrue(BankColumnCatalog.build(EventSnapshot.empty()).isEmpty());
		assertTrue(BankColumnCatalog.build(null).isEmpty());
	}

	@Test void banksAreAlphabeticalAndColumnsWithinABankAreAlphabetical() {
		// "Hit10_ID" sorts before "Hit1_ID" lexicographically ('0' < '_'),
		// which is what makes legacy CED's column order look schema-defined
		// when it is really just a plain string sort.
		DataBank hb = bank(new String[] { "Hit1_ID", "Hit10_ID" }, 3);
		DataBank band = bank(new String[] { "adc" }, 2);
		EventSnapshot snapshot = EventSnapshot.of(
				event(Map.of("HitBasedTrkg::HBSegments", hb, "BAND::adc", band)));

		List<BankColumnEntry> entries = BankColumnCatalog.build(snapshot);
		assertEquals(3, entries.size());
		assertEquals("BAND::adc.adc", entries.get(0).fullName());
		assertEquals(0, entries.get(0).bankIndex());
		assertEquals(2, entries.get(0).rowCount());
		assertEquals("HitBasedTrkg::HBSegments.Hit10_ID", entries.get(1).fullName());
		assertEquals("HitBasedTrkg::HBSegments.Hit1_ID", entries.get(2).fullName());
		assertEquals(1, entries.get(1).bankIndex());
		assertEquals(1, entries.get(2).bankIndex());
	}

	private static DataBank bank(String[] columns, int rows) {
		DataDescriptor descriptor = (DataDescriptor) Proxy.newProxyInstance(
				DataDescriptor.class.getClassLoader(), new Class<?>[] { DataDescriptor.class },
				(proxy, method, args) -> switch (method.getName()) {
					case "hasEntry" -> List.of(columns).contains(args[0]);
					case "getProperty" -> BankColumns.INT32;
					default -> null;
				});
		return (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class },
				(proxy, method, args) -> switch (method.getName()) {
					case "getColumnList" -> columns;
					case "getDescriptor" -> descriptor;
					case "rows" -> rows;
					default -> null;
				});
	}

	private static DataEvent event(Map<String, DataBank> banks) {
		return (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
				new Class<?>[] { DataEvent.class },
				(proxy, method, args) -> switch (method.getName()) {
					case "getBankList" -> banks.keySet().toArray(String[]::new);
					case "hasBank" -> banks.containsKey(args[0]);
					case "getBank" -> banks.get(args[0]);
					default -> null;
				});
	}
}
