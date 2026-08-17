package edu.cnu.ced.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class CedDisplayArrayTest {

	@Test
	void selectingSingleEventClearsAccumulationMode() {
		CedDisplayArray array = new CedDisplayArray(EnumSet.of(
				CedDisplayOption.SINGLE_EVENT, CedDisplayOption.ACCUMULATION),
				2, 0, 0, null);

		array.setSelected(CedDisplayOption.ACCUMULATION, true);
		assertFalse(array.isSelected(CedDisplayOption.SINGLE_EVENT));
		assertTrue(array.isSelected(CedDisplayOption.ACCUMULATION));

		array.setSelected(CedDisplayOption.SINGLE_EVENT, true);
		assertTrue(array.isSelected(CedDisplayOption.SINGLE_EVENT));
		assertFalse(array.isSelected(CedDisplayOption.ACCUMULATION));
	}
}
