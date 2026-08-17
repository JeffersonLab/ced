package edu.cnu.ced.style;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.data.DCEventData.ReconKind;

class CedDrawingStyleTest {

	@Test
	void reconstructionKindsHaveStableSemanticColors() {
		assertEquals(Color.YELLOW, CedDrawingStyle.reconstructionColor(ReconKind.HB));
		assertEquals(new Color(255, 140, 0),
				CedDrawingStyle.reconstructionColor(ReconKind.TB));
		assertEquals(new Color(0, 255, 127),
				CedDrawingStyle.reconstructionColor(ReconKind.AI_HB));
		assertEquals(Color.MAGENTA,
				CedDrawingStyle.reconstructionColor(ReconKind.AI_TB));
	}

	@Test
	void derivedColorsPreserveTheSemanticHue() {
		Color translucent = CedDrawingStyle.translucent(CedDrawingStyle.TIME_BASED, 120);
		assertEquals(255, translucent.getRed());
		assertEquals(140, translucent.getGreen());
		assertEquals(0, translucent.getBlue());
		assertEquals(120, translucent.getAlpha());
		assertEquals(CedDrawingStyle.HIT_BASED.darker(),
				CedDrawingStyle.outline(CedDrawingStyle.HIT_BASED));
	}
}
