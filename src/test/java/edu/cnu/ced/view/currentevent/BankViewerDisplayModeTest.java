package edu.cnu.ced.view.currentevent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

class BankViewerDisplayModeTest {

	@Test void defaultsToFloatingAndPersistsAChange() throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/ced/tests/bank-viewer-display-" + System.nanoTime());
		try {
			BankViewerDisplayMode mode = new BankViewerDisplayMode(preferences);
			assertTrue(mode.isFloating());

			mode.setFloating(false);
			assertFalse(mode.isFloating());
			assertFalse(new BankViewerDisplayMode(preferences).isFloating(),
					"a fresh instance over the same node sees the persisted state");

			mode.setFloating(true);
			assertTrue(mode.isFloating());
		} finally {
			preferences.removeNode();
		}
	}
}
