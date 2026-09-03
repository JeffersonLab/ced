package edu.cnu.ced.view.currentevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class BankColumnTableModelTest {

	@Test void exposesNameTypeAndCountColumns() {
		BankColumnTableModel model = new BankColumnTableModel();
		model.setEntries(List.of(new BankColumnEntry("CND::adc", "sector", 0, "byte", 4)));

		assertEquals(1, model.getRowCount());
		assertEquals(3, model.getColumnCount());
		assertEquals("CND::adc.sector", model.getValueAt(0, 0));
		assertEquals("byte", model.getValueAt(0, 1));
		assertEquals("4", model.getValueAt(0, 2));
		assertEquals("", model.getValueAt(5, 0), "out-of-range row is blank, not an exception");
	}

	@Test void rowForBankFindsTheFirstMatchingRow() {
		BankColumnTableModel model = new BankColumnTableModel();
		model.setEntries(List.of(
				new BankColumnEntry("CND::adc", "sector", 0, "byte", 4),
				new BankColumnEntry("CND::adc", "layer", 0, "byte", 4),
				new BankColumnEntry("CTOF::adc", "sector", 1, "byte", 2)));

		assertEquals(0, model.rowForBank("CND::adc"));
		assertEquals(2, model.rowForBank("CTOF::adc"));
		assertEquals(-1, model.rowForBank("BAND::adc"));
		assertEquals(-1, model.rowForBank(null));
	}

	@Test void entryAtIsNullOutOfRange() {
		BankColumnTableModel model = new BankColumnTableModel();
		assertNull(model.entryAt(-1));
		assertNull(model.entryAt(0));
	}
}
