package edu.cnu.ced.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;

import org.junit.jupiter.api.Test;

import cnuphys.lund.LundId;
import cnuphys.lund.LundStyle;
import cnuphys.lund.LundSupport;

import edu.cnu.ced.data.DCEventData.ReconKind;

class CedDrawingStyleTest {

	@Test
	void particleColorAndStrokeDelegateToLundStyle() {
		// Not hardcoding an RGB literal here: the point of delegating to
		// cnuphys.lund.LundStyle is to always match whatever that shared
		// library actually says, not to independently re-derive and
		// hardcode its current answer for one species.
		LundId electronId = LundSupport.getInstance().get(11, -1);
		LundStyle electronStyle = LundStyle.getStyle(electronId);
		assertEquals(electronStyle.getLineColor(), CedDrawingStyle.particleColor(11, -1));
		assertEquals(electronStyle.getStroke(), CedDrawingStyle.particleStroke(11, -1));
	}

	@Test
	void neutralParticlesGetADashedStroke() {
		Stroke photonStroke = CedDrawingStyle.particleStroke(22, 0);
		assertTrue(photonStroke instanceof BasicStroke);
		assertNotNull(((BasicStroke) photonStroke).getDashArray());
	}

	@Test
	void chargedParticlesGetASolidStroke() {
		Stroke protonStroke = CedDrawingStyle.particleStroke(2212, 1);
		assertTrue(protonStroke instanceof BasicStroke);
		assertNull(((BasicStroke) protonStroke).getDashArray());
	}

	@Test
	void unrecognizedPidFallsBackToChargeBasedColor() {
		// pid 0 (reconstruction couldn't assign one), one color per charge
		// sign -- LundStyle's own unknown+/-/0 fallback, keyed by charge
		// alone, distinct from every real species' color.
		assertNotEquals(CedDrawingStyle.particleColor(0, 1), CedDrawingStyle.particleColor(0, -1));
		assertNotEquals(CedDrawingStyle.particleColor(0, 1), CedDrawingStyle.particleColor(0, 0));
		assertNotEquals(CedDrawingStyle.particleColor(0, -1), CedDrawingStyle.particleColor(0, 0));
	}

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
