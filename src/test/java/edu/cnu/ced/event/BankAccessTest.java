package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;

import org.jlab.io.base.DataBank;
import org.junit.jupiter.api.Test;

class BankAccessTest {

	@Test
	void checksColumnsWithoutMutatingSourceOrder() {
		String[] columns = { "torus", "event", "run" };
		DataBank bank = bank(columns);
		assertTrue(BankAccess.hasColumn(bank, "event"));
		assertFalse(BankAccess.hasColumn(bank, "missing"));
		assertArrayEquals(new String[] { "event", "run", "torus" }, BankAccess.columns(bank));
		assertArrayEquals(new String[] { "torus", "event", "run" }, columns);
	}

	private static DataBank bank(String[] columns) {
		return (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (object, method, args) ->
						"getColumnList".equals(method.getName()) ? columns : null);
	}
}
