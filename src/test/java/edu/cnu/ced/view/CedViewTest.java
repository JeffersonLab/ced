package edu.cnu.ced.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class CedViewTest {

	@Test
	void stripsStyleAndJoinsNonEmptyLines() {
		String text = CedView.hoverText(List.of("$cyan$sector 3", "$deep sky blue$p = 1.234 GeV/c"));
		assertEquals("sector 3\np = 1.234 GeV/c", text);
	}

	@Test
	void skipsLinesThatAreEmptyAfterStrippingStyle() {
		// A style tag with nothing after it strips down to an empty line,
		// which shouldn't leave a blank line in the popup.
		String text = CedView.hoverText(List.of("$cyan$", "$mono$1.234"));
		assertEquals("1.234", text);
	}

	@Test
	void emptyFeedbackListYieldsNoPopupText() {
		assertNull(CedView.hoverText(List.of()));
	}

	@Test
	void allBlankLinesYieldsNoPopupText() {
		assertNull(CedView.hoverText(List.of("$cyan$", "$mono$")));
	}
}
