package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataDescriptor;
import org.junit.jupiter.api.Test;

class BankColumnsTest {

	@Test void readsEveryTypeDataBankExposesGenerically() {
		DataBank bank = bank(Map.of("sector", BankColumns.INT8, "component", BankColumns.INT16,
				"id", BankColumns.INT32, "timestamp", BankColumns.INT64,
				"time", BankColumns.FLOAT32, "energy", BankColumns.FLOAT64),
				Map.of("sector", new byte[] { 3 }, "component", new short[] { 12 },
						"id", new int[] { 555 }, "timestamp", new long[] { 99L },
						"time", new float[] { 1.5f }, "energy", new double[] { 2.5 }));

		assertEquals(BankColumns.INT8, BankColumns.type(bank, "sector"));
		assertEquals("byte", BankColumns.typeName(bank, "sector"));
		assertEquals("3", BankColumns.formattedValue(bank, "sector", 0));
		assertEquals(3.0, BankColumns.numericValue(bank, "sector", 0));

		assertEquals("12", BankColumns.formattedValue(bank, "component", 0));
		assertEquals("555", BankColumns.formattedValue(bank, "id", 0));
		assertEquals("99", BankColumns.formattedValue(bank, "timestamp", 0));
		assertTrue(BankColumns.formattedValue(bank, "time", 0).startsWith("1.5"));
		assertTrue(BankColumns.formattedValue(bank, "energy", 0).startsWith("2.5"));
	}

	@Test void formattedValuesCoversEveryRowInOrder() {
		DataBank bank = bank(Map.of("ADC", BankColumns.INT32),
				Map.of("ADC", new int[] { 10, 20, 30 }));
		List<String> values = BankColumns.formattedValues(bank, "ADC");
		assertEquals(List.of("10", "20", "30"), values);
	}

	@Test void unreadableTypesReportNaAndUnknownColumnsReportUnknown() {
		DataBank bank = bank(Map.of("label", BankColumns.STRING), Map.of("label", new int[] { 0 }));
		assertEquals("n/a", BankColumns.formattedValue(bank, "label", 0));
		assertTrue(Double.isNaN(BankColumns.numericValue(bank, "label", 0)));

		assertEquals(BankColumns.UNKNOWN, BankColumns.type(bank, "noSuchColumn"));
		assertEquals("unknown", BankColumns.typeName(bank, "noSuchColumn"));
		// an unreadable column (unknown type, same as a schema miss) reads as "n/a",
		// like any other type this class cannot generically read -- "" is reserved
		// for a genuinely unavailable bank or an out-of-range row (see below).
		assertEquals("n/a", BankColumns.formattedValue(bank, "noSuchColumn", 0));
	}

	@Test void missingBankOrOutOfRangeRowIsHandledSafely() {
		assertEquals(BankColumns.UNKNOWN, BankColumns.type(null, "x"));
		assertEquals("", BankColumns.formattedValue(null, "x", 0));
		assertTrue(BankColumns.formattedValues(null, "x").isEmpty());

		DataBank bank = bank(Map.of("id", BankColumns.INT32), Map.of("id", new int[] { 1 }));
		assertEquals("", BankColumns.formattedValue(bank, "id", 5));
		assertTrue(Double.isNaN(BankColumns.numericValue(bank, "id", -1)));
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
					case "getDescriptor" -> descriptor;
					case "rows" -> rows;
					case "getByte" -> (args.length == 1) ? values.get(args[0])
							: ((byte[]) values.get(args[0]))[(int) args[1]];
					case "getShort" -> (args.length == 1) ? values.get(args[0])
							: ((short[]) values.get(args[0]))[(int) args[1]];
					case "getInt" -> (args.length == 1) ? values.get(args[0])
							: ((int[]) values.get(args[0]))[(int) args[1]];
					case "getLong" -> (args.length == 1) ? values.get(args[0])
							: ((long[]) values.get(args[0]))[(int) args[1]];
					case "getFloat" -> (args.length == 1) ? values.get(args[0])
							: ((float[]) values.get(args[0]))[(int) args[1]];
					case "getDouble" -> (args.length == 1) ? values.get(args[0])
							: ((double[]) values.get(args[0]))[(int) args[1]];
					default -> null;
				});
	}
}
