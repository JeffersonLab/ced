package edu.cnu.ced.view3d;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A small labeled 0-255 slider controlling a {@link CedPanel3D}'s "volume
 * alpha" -- how opaque static detector geometry (as opposed to live event
 * data) is drawn. Direct port of legacy CED's {@code cnuphys.ced.ced3d.
 * AlphaSlider}, retargeted at {@link CedPanel3D} in place of legacy's own
 * {@code PlainPanel3D}.
 */
@SuppressWarnings("serial")
final class AlphaSlider extends JPanel implements ChangeListener {

	private static final int SLIDER_WIDTH = 140;
	private static final int INITIAL_VALUE = 24;

	private final CedPanel3D panel3D;
	private final JSlider slider;

	AlphaSlider(CedPanel3D panel3D, String prompt) {
		this.panel3D = panel3D;

		setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));

		JLabel label = new JLabel(prompt);
		label.setFont(Fonts.smallFont);
		add(label);

		slider = new JSlider(SwingConstants.HORIZONTAL, 0, 255, INITIAL_VALUE);
		slider.setMajorTickSpacing(50);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setFont(Fonts.tinyFont);
		slider.setFocusable(false); // avoid an ugly focus border
		slider.addChangeListener(this);

		Dimension d = slider.getPreferredSize();
		d.width = SLIDER_WIDTH;
		slider.setPreferredSize(d);

		add(slider);
	}

	/** Current alpha value, 0-255. */
	int getAlpha() {
		return slider.getValue();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		panel3D.refresh();
	}
}
