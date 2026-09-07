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

	@Test
	void hoverPopupTogglesIndependentlyAndDefaultsOn() {
		// CedView.initializeCedView unions HOVER_POPUP into every view's own
		// options automatically; this only pins the checkbox's own behavior
		// once it's present, not that unioning (which needs a real CedView).
		CedDisplayArray array = new CedDisplayArray(EnumSet.of(
				CedDisplayOption.HOVER_POPUP, CedDisplayOption.RECON_HITS),
				2, 0, 0, null);

		assertTrue(array.isSelected(CedDisplayOption.HOVER_POPUP));

		array.setSelected(CedDisplayOption.HOVER_POPUP, false);
		assertFalse(array.isSelected(CedDisplayOption.HOVER_POPUP));
		// Unrelated options are untouched by the hover toggle.
		assertTrue(array.isSelected(CedDisplayOption.RECON_HITS));
	}
}
