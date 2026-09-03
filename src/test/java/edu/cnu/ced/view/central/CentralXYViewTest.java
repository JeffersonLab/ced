package edu.cnu.ced.view.central;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.data.CentralEventData.Detector;
import edu.cnu.ced.data.CentralEventData.TdcHit;
import edu.cnu.ced.view.central.CentralXYView.Element;

class CentralXYViewTest {

	@Test
	void matchesTdcOffsetsTheAdcDerivedOrderByTwo() {
		// Real event data caught this: hovering a CND paddle with a matching
		// ADC hit reporting "order 0" showed no TDC feedback at all, because
		// CND::adc's own bank schema documents order 0=ADCL/1=ADCR while
		// CND::tdc's documents order 2=TDCL/3=TDCR -- not the same raw
		// values, just the same physical left/right sides.
		Element leftPaddle = new Element(Detector.CND, 10, 3, 1, 0);
		TdcHit leftTdc = new TdcHit(10, 3, 1, 2, 1234);
		assertTrue(CentralXYView.matchesTdc(leftPaddle, leftTdc));

		Element rightPaddle = new Element(Detector.CND, 10, 3, 1, 1);
		TdcHit rightTdc = new TdcHit(10, 3, 1, 3, 1250);
		assertTrue(CentralXYView.matchesTdc(rightPaddle, rightTdc));
	}

	@Test
	void matchesTdcRejectsTheAdcRawOrderValueDirectly() {
		// The bug this guards against: comparing e.order directly against
		// h.order() (no offset) would make THIS assertion fail instead.
		Element leftPaddle = new Element(Detector.CND, 10, 3, 1, 0);
		TdcHit wrongOrderConvention = new TdcHit(10, 3, 1, 0, 1234);
		assertFalse(CentralXYView.matchesTdc(leftPaddle, wrongOrderConvention));
	}

	@Test
	void matchesTdcStillRequiresSectorLayerAndComponentToAgree() {
		Element paddle = new Element(Detector.CND, 10, 3, 1, 0);
		assertFalse(CentralXYView.matchesTdc(paddle, new TdcHit(11, 3, 1, 2, 1234)));
		assertFalse(CentralXYView.matchesTdc(paddle, new TdcHit(10, 4, 1, 2, 1234)));
		assertFalse(CentralXYView.matchesTdc(paddle, new TdcHit(10, 3, 2, 2, 1234)));
	}
}
