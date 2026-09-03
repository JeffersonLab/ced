package edu.cnu.ced.view.currentevent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

class BankColumnVisibilityTest {

	@Test void columnsAreVisibleByDefaultAndHidingPersists() throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/ced/tests/bank-column-visibility-" + System.nanoTime());
		try {
			BankColumnVisibility visibility = new BankColumnVisibility(preferences);
			assertTrue(visibility.isVisible("CND::adc", "sector"));

			visibility.setVisible("CND::adc", "sector", false);
			assertFalse(visibility.isVisible("CND::adc", "sector"));
			assertTrue(visibility.isVisible("CND::adc", "layer"), "only the hidden column is affected");

			// a fresh instance over the same preferences node sees the persisted state
			assertFalse(new BankColumnVisibility(preferences).isVisible("CND::adc", "sector"));

			visibility.setVisible("CND::adc", "sector", true);
			assertTrue(visibility.isVisible("CND::adc", "sector"));
		} finally {
			preferences.removeNode();
		}
	}

	@Test void differentBanksAreIndependent() throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/ced/tests/bank-column-visibility-" + System.nanoTime());
		try {
			BankColumnVisibility visibility = new BankColumnVisibility(preferences);
			visibility.setVisible("CND::adc", "sector", false);
			assertTrue(visibility.isVisible("CTOF::adc", "sector"));
		} finally {
			preferences.removeNode();
		}
	}
}
