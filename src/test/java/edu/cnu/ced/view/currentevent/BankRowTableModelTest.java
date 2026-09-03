package edu.cnu.ced.view.currentevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataDescriptor;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.data.BankColumns;
import edu.cnu.ced.event.EventSnapshot;

class BankRowTableModelTest {

	@Test void exposesAnIndexColumnPlusOnePerSchemaColumn() {
		DataBank bank = bank(Map.of("sector", BankColumns.INT8, "ADC", BankColumns.INT32),
				Map.of("sector", new byte[] { 3, 5 }, "ADC", new int[] { 100, 200 }));
		BankRowTableModel model = new BankRowTableModel("CND::adc", bank);

		assertEquals(3, model.getColumnCount());
		assertEquals(2, model.getRowCount());
		assertEquals("", model.getColumnName(0));
		assertEquals(0, model.getValueAt(0, 0));
		// columns sort alphabetically: "ADC" ('A'=65) before "sector" ('s'=115)
		assertEquals("ADC", model.getColumnName(1));
		assertEquals("100", model.getValueAt(0, 1));
		assertEquals("sector", model.getColumnName(2));
		assertEquals("3", model.getValueAt(0, 2));
	}

	@Test void columnsSurviveAnEmptyOrMissingBankInALaterSnapshot() {
		DataBank bank = bank(Map.of("sector", BankColumns.INT8), Map.of("sector", new byte[] { 3 }));
		BankRowTableModel model = new BankRowTableModel("CND::adc", bank);
		assertEquals(2, model.getColumnCount());

		model.setSnapshot(EventSnapshot.empty());
		assertEquals(2, model.getColumnCount(), "column set is fixed at construction");
		assertEquals(0, model.getRowCount());
		assertNull(model.getValueAt(0, 1));
	}

	@Test void sortReordersByANumericColumnAndTogglesDirection() {
		DataBank bank = bank(Map.of("ADC", BankColumns.INT32),
				Map.of("ADC", new int[] { 30, 10, 20 }));
		BankRowTableModel model = new BankRowTableModel("CND::adc", bank);

		model.sort(1);
		assertEquals(List.of("10", "20", "30"), values(model, 1));
		model.sort(1);
		assertEquals(List.of("30", "20", "10"), values(model, 1));
	}

	@Test void sortingTheIndexColumnReversesRowOrderWithoutTouchingData() {
		DataBank bank = bank(Map.of("ADC", BankColumns.INT32),
				Map.of("ADC", new int[] { 30, 10, 20 }));
		BankRowTableModel model = new BankRowTableModel("CND::adc", bank);

		model.sort(0);
		assertEquals(List.of(0, 1, 2), List.of(model.getValueAt(0, 0), model.getValueAt(1, 0),
				model.getValueAt(2, 0)));
		model.sort(0);
		assertEquals(List.of(2, 1, 0), List.of(model.getValueAt(0, 0), model.getValueAt(1, 0),
				model.getValueAt(2, 0)));
	}

	@Test void sortingAnUnreadableColumnTracksTheClickButLeavesOrderUnchanged() {
		DataBank bank = bank(Map.of("label", BankColumns.STRING),
				Map.of("label", new int[] { 0, 1 }));
		BankRowTableModel model = new BankRowTableModel("X::bank", bank);
		model.sort(1);
		assertEquals(List.of(0, 1), List.of(model.getValueAt(0, 0), model.getValueAt(1, 0)));
	}

	private static List<String> values(BankRowTableModel model, int column) {
		return List.of(String.valueOf(model.getValueAt(0, column)),
				String.valueOf(model.getValueAt(1, column)), String.valueOf(model.getValueAt(2, column)));
	}

	private static DataBank bank(Map<String, Integer> types, Map<String, Object> values) {
		DataDescriptor descriptor = (DataDescriptor) Proxy.newProxyInstance(
				DataDescriptor.class.getClassLoader(), new Class<?>[] { DataDescriptor.class },
				(proxy, method, args) -> switch (method.getName()) {
					case "hasEntry" -> types.containsKey(args[0]);
					case "getProperty" -> (args.length == 2) ? types.getOrDefault(args[1], 0) : 0;
					default -> null;
				});
		int rows = values.values().stream().mapToInt(v -> java.lang.reflect.Array.getLength(v)).max()
				.orElse(0);
		return (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class },
				(proxy, method, args) -> switch (method.getName()) {
					case "getColumnList" -> types.keySet().toArray(String[]::new);
					case "getDescriptor" -> descriptor;
					case "rows" -> rows;
					case "getByte" -> (args.length == 1) ? values.get(args[0])
							: ((byte[]) values.get(args[0]))[(int) args[1]];
					case "getInt" -> (args.length == 1) ? values.get(args[0])
							: ((int[]) values.get(args[0]))[(int) args[1]];
					default -> null;
				});
	}
}
