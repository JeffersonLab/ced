package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;

import org.jlab.io.base.DataBank;
import org.junit.jupiter.api.Test;

class RunConfigTest {

	@Test
	void readsRequiredAndOptionalFirstRowValues() {
		String[] columns = { "run", "event", "trigger", "solenoid", "torus" };
		DataBank bank = (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (object, method, args) -> switch (method.getName()) {
					case "rows" -> 1;
					case "getColumnList" -> columns;
					case "getInt" -> "run".equals(args[0]) ? 19210 : 5228740;
					case "getLong" -> 0x4257L;
					case "getFloat" -> "solenoid".equals(args[0]) ? -1.0f : 1.0f;
					default -> null;
				});

		RunConfig values = RunConfig.fromBank(bank).orElseThrow();
		assertEquals(19210, values.run());
		assertEquals(5228740, values.event());
		assertEquals(0x4257L, values.trigger());
		assertEquals(-1, values.timestamp());
		assertEquals(-1, values.type());
		assertEquals(-1.0f, values.solenoid());
	}

	@Test
	void rejectsIncompleteRows() {
		DataBank bank = (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (object, method, args) -> switch (method.getName()) {
					case "rows" -> 1;
					case "getColumnList" -> new String[] { "run", "event" };
					default -> null;
				});
		assertTrue(RunConfig.fromBank(bank).isEmpty());
	}
}
