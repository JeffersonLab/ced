package edu.cnu.ced.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.junit.jupiter.api.Test;

class CedApplicationMenuHelpersTest {

	@Test void normalizedEventPeriodPassesThroughAnInRangeValue() {
		assertEquals(1.5f, CedApplication.normalizedEventPeriod("1.5", 2.0f));
	}

	@Test void normalizedEventPeriodClampsBelowMinimum() {
		assertEquals(0.001f, CedApplication.normalizedEventPeriod("0.0", 2.0f));
		assertEquals(0.001f, CedApplication.normalizedEventPeriod("-5", 2.0f));
	}

	@Test void normalizedEventPeriodClampsAboveMaximum() {
		assertEquals(60f, CedApplication.normalizedEventPeriod("120", 2.0f));
	}

	@Test void normalizedEventPeriodFallsBackOnUnparsableOrNonFiniteText() {
		assertEquals(2.0f, CedApplication.normalizedEventPeriod("not a number", 2.0f));
		assertEquals(2.0f, CedApplication.normalizedEventPeriod("NaN", 2.0f));
		assertEquals(2.0f, CedApplication.normalizedEventPeriod("Infinity", 2.0f));
	}

	@Test void removeInterpolationOptionsStripsTheGroupAndItsPrecedingSeparator() {
		JMenu menu = new JMenu("Field");
		menu.add(new JMenuItem("Torus"));
		menu.add(new JMenuItem("Solenoid"));
		menu.add(new JMenuItem("Composite (Torus and Solenoid)"));
		menu.add(new JMenuItem("No Field"));
		menu.addSeparator();
		menu.add(new JMenuItem("Interpolate"));
		menu.add(new JMenuItem("Nearest Neighbor"));
		menu.addSeparator();
		menu.add(new JMenuItem("Scale Torus"));

		CedApplication.removeInterpolationOptions(menu);

		assertEquals(6, menu.getMenuComponentCount());
		assertEquals("Torus", ((JMenuItem) menu.getMenuComponent(0)).getText());
		assertEquals("Solenoid", ((JMenuItem) menu.getMenuComponent(1)).getText());
		assertEquals("Composite (Torus and Solenoid)", ((JMenuItem) menu.getMenuComponent(2)).getText());
		assertEquals("No Field", ((JMenuItem) menu.getMenuComponent(3)).getText());
		assertTrue(menu.getMenuComponent(4) instanceof JSeparator, "expected the separator to survive");
		assertEquals("Scale Torus", ((JMenuItem) menu.getMenuComponent(5)).getText());
	}

	@Test void removeInterpolationOptionsLeavesAnUnexpectedLayoutAlone() {
		// No "Interpolate" item at all -- e.g. coatjava changed its menu, or
		// this is being handed a menu that was never the Field menu.
		JMenu menu = new JMenu("Field");
		menu.add(new JMenuItem("Torus"));
		menu.add(new JMenuItem("Solenoid"));

		CedApplication.removeInterpolationOptions(menu);

		assertEquals(2, menu.getMenuComponentCount());
	}

	@Test void indexOfMenuItemFindsAMatchingLabelOrReportsAbsent() {
		JMenu menu = new JMenu("Field");
		menu.add(new JMenuItem("Torus"));
		menu.add(new JMenuItem("Interpolate"));

		assertEquals(1, CedApplication.indexOfMenuItem(menu, "Interpolate"));
		assertEquals(-1, CedApplication.indexOfMenuItem(menu, "Nearest Neighbor"));
	}
}
