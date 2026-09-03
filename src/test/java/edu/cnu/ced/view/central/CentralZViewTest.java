package edu.cnu.ced.view.central;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CentralZViewTest {

	@Test
	void atZeroPhiTransverseEqualsXNotItsNegation() {
		// Regression guard: this used to be negated (-(x*cosPhi + y*sinPhi)),
		// which put every object at the mirror image of its own reported
		// coordinates -- caught against a real BST cross whose own "xyz"
		// feedback read x = -6.139 while the (z, transverse) readout for
		// that same point showed +6.139.
		double cosPhi = Math.cos(Math.toRadians(0.0));
		double sinPhi = Math.sin(Math.toRadians(0.0));
		assertEquals(-6.139, CentralZView.projectTransverse(-6.139, -2.282, cosPhi, sinPhi), 1.0e-9);
		assertEquals(6.139, CentralZView.projectTransverse(6.139, -2.282, cosPhi, sinPhi), 1.0e-9);
	}

	@Test
	void atNinetyDegreesTransverseComesFromYNotX() {
		double cosPhi = Math.cos(Math.toRadians(90.0));
		double sinPhi = Math.sin(Math.toRadians(90.0));
		assertEquals(5.0, CentralZView.projectTransverse(100.0, 5.0, cosPhi, sinPhi), 1.0e-9);
	}

	@Test
	void isTheDotProductOfXyWithTheProjectionDirection() {
		double phiDeg = 37.0;
		double cosPhi = Math.cos(Math.toRadians(phiDeg));
		double sinPhi = Math.sin(Math.toRadians(phiDeg));
		double x = 3.0, y = -4.0;
		double expected = x * cosPhi + y * sinPhi;
		assertEquals(expected, CentralZView.projectTransverse(x, y, cosPhi, sinPhi), 1.0e-12);
	}
}
