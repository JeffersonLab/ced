package edu.cnu.ced.view.currentevent;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventStore;

class BankViewerOpenerTest {

	// Every entry point that can open a bank viewer -- the Current Event
	// view's present-banks list, and any detector view's own "banks" tab --
	// must reuse the very same cache for a given navigator, or a bank opened
	// from one place would not be recognized as already open from another.

	@Test void sameNavigatorAlwaysGetsTheSameOpener() {
		EventNavigator navigator = new EventNavigator(new EventStore());
		assertSame(BankViewerOpener.sharedFor(navigator), BankViewerOpener.sharedFor(navigator));
	}

	@Test void differentNavigatorsGetIndependentOpeners() {
		EventNavigator first = new EventNavigator(new EventStore());
		EventNavigator second = new EventNavigator(new EventStore());
		assertNotSame(BankViewerOpener.sharedFor(first), BankViewerOpener.sharedFor(second));
	}
}
