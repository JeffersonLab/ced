package edu.cnu.ced.view.swim;

import java.awt.FlowLayout;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * One randomized-parameter range for {@link SwimBatchControlPanel}: a
 * labeled "min to max" pair of text fields, plus a random draw uniformly
 * between them -- matching legacy CED's own {@code
 * cnuphys.ced.component.VariableRange} in spirit, minus its per-field
 * last-good-value auto-recovery on bad input: this codebase's own {@link
 * SwimTestControlPanel} already handles a bad number by aborting the
 * whole swim with a status message instead, and {@link
 * SwimBatchControlPanel} follows that same, simpler convention rather
 * than mixing the two.
 */
@SuppressWarnings("serial")
final class RangeField extends JPanel {

	private final JTextField minField;
	private final JTextField maxField;

	RangeField(String label, String units, double minValue, double maxValue) {
		super(new FlowLayout(FlowLayout.LEFT, 4, 2));
		minField = new JTextField(Double.toString(minValue), 6);
		maxField = new JTextField(Double.toString(maxValue), 6);
		add(new JLabel(label));
		add(minField);
		add(new JLabel("to"));
		add(maxField);
		add(new JLabel(units));
	}

	/** @throws NumberFormatException if either bound doesn't parse */
	double nextRandom(Random random) {
		double min = Double.parseDouble(minField.getText().trim());
		double max = Double.parseDouble(maxField.getText().trim());
		if (min == max) {
			return min;
		}
		return min + (max - min) * random.nextDouble();
	}
}
