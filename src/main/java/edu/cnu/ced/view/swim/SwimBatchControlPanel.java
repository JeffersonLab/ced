package edu.cnu.ced.view.swim;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import edu.cnu.ced.view.swim.SwimTestPanel3D.SurfaceChoice;

/**
 * Randomized batch-swim testing for {@link SwimTestPanel3D}: how many
 * hypothetical particles to swim and with what random seed, a min/max
 * range for each of the six swim parameters (momentum, vertex x/y/z,
 * theta, phi) drawn uniformly at random per swim, a charge mode (positive
 * only, negative only, or randomized), the same reference-surface picker
 * {@link SwimTestControlPanel} uses (via the shared {@link
 * SurfaceChoicePanel}), and a show filter (successes/failures/all) that
 * re-filters the already-accumulated results without re-swimming.
 * Matches legacy CED's own {@code cnuphys.ced.ced3d.SwimmerControlPanel}.
 *
 * <p>
 * Results accumulate across repeated "Swim Trajectories" clicks (matching
 * legacy) until "Clear Trajectories" is pressed -- so a developer can
 * layer several differently-configured batches (e.g. one per charge mode)
 * onto the same view before comparing them.
 * </p>
 */
@SuppressWarnings("serial")
final class SwimBatchControlPanel extends JPanel {

	private enum ChargeMode { POSITIVE, NEGATIVE, RANDOM }

	private final JTextField swimCountField = new JTextField("100", 6);
	private final JTextField randomSeedField = new JTextField("0", 8);

	private final RangeField momentumRange = new RangeField("p from", "GeV/c", 0.4, 3.0);
	private final RangeField vxRange = new RangeField("x₀ from", "cm", 0, 0);
	private final RangeField vyRange = new RangeField("y₀ from", "cm", 0, 0);
	private final RangeField vzRange = new RangeField("z₀ from", "cm", 0, 0);
	private final RangeField thetaRange = new RangeField("θ from", "deg", 20, 45);
	private final RangeField phiRange = new RangeField("φ from", "deg", -180, 180);

	private final JRadioButton positiveChargeButton = new JRadioButton("+ only");
	private final JRadioButton negativeChargeButton = new JRadioButton("- only");
	private final JRadioButton randomChargeButton = new JRadioButton("Random", true);

	private final SurfaceChoicePanel surfacePanel = new SurfaceChoicePanel();

	private final JRadioButton showSuccessesButton = new JRadioButton("Successes");
	private final JRadioButton showFailuresButton = new JRadioButton("Failures");
	private final JRadioButton showAllButton = new JRadioButton("All", true);

	private final JLabel statusLabel = new JLabel(" ");

	SwimBatchControlPanel(SwimTestPanel3D panel) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Batch Swim Parameters"));

		add(row("Number of swims", swimCountField));
		add(row("Random seed (0 = unseeded)", randomSeedField));

		add(rangePanel());
		add(chargePanel());
		add(surfacePanel);
		add(showPanel(panel));

		JButton swim = new JButton("Swim Trajectories");
		swim.setAlignmentX(Component.CENTER_ALIGNMENT);
		swim.addActionListener(e -> doBatch(panel));
		add(swim);

		JButton clear = new JButton("Clear Trajectories");
		clear.setAlignmentX(Component.CENTER_ALIGNMENT);
		clear.addActionListener(e -> {
			panel.clearBatch();
			statusLabel.setText(" ");
		});
		add(clear);

		statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(statusLabel);
	}

	private JComponent rangePanel() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(BorderFactory.createTitledBorder("Ranges for randomized variables"));
		for (RangeField range : new RangeField[] { momentumRange, vxRange, vyRange, vzRange, thetaRange, phiRange }) {
			p.add(range);
		}
		return p;
	}

	private JComponent chargePanel() {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		p.setBorder(BorderFactory.createTitledBorder("Charge"));
		ButtonGroup group = new ButtonGroup();
		for (JRadioButton button : new JRadioButton[] { positiveChargeButton, negativeChargeButton, randomChargeButton }) {
			group.add(button);
			p.add(button);
		}
		return p;
	}

	private JComponent showPanel(SwimTestPanel3D panel) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		p.setBorder(BorderFactory.createTitledBorder("Show"));
		ButtonGroup group = new ButtonGroup();
		for (JRadioButton button : new JRadioButton[] { showSuccessesButton, showFailuresButton, showAllButton }) {
			group.add(button);
			p.add(button);
			button.addActionListener(e -> panel.setBatchShowMode(showMode()));
		}
		return p;
	}

	private SwimBatchShowMode showMode() {
		if (showSuccessesButton.isSelected()) {
			return SwimBatchShowMode.SUCCESSES;
		}
		return showFailuresButton.isSelected() ? SwimBatchShowMode.FAILURES : SwimBatchShowMode.ALL;
	}

	private void doBatch(SwimTestPanel3D panel) {
		try {
			int count = Integer.parseInt(swimCountField.getText().trim());
			if (count <= 0) {
				statusLabel.setText("Number of swims must be positive");
				return;
			}
			long seed = Long.parseLong(randomSeedField.getText().trim());
			Random random = (seed == 0) ? new Random() : new Random(seed);
			SurfaceChoice surface = surfacePanel.surfaceChoice();

			List<SwimSpec> specs = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				double p = momentumRange.nextRandom(random);
				double vx = vxRange.nextRandom(random);
				double vy = vyRange.nextRandom(random);
				double vz = vzRange.nextRandom(random);
				double theta = thetaRange.nextRandom(random);
				double phi = phiRange.nextRandom(random);
				specs.add(new SwimSpec(nextCharge(random), vx, vy, vz, p, theta, phi));
			}

			int successes = panel.runBatch(specs, surface);
			statusLabel.setText(successes + " / " + count + " reached the target");
		} catch (NumberFormatException ex) {
			statusLabel.setText("Invalid number: " + ex.getMessage());
		}
	}

	/** Matches legacy's own {@code SwimInputValues.selectCharge}: random splits 40% -/40% +/20% neutral. */
	private int nextCharge(Random random) {
		if (positiveChargeButton.isSelected()) {
			return 1;
		}
		if (negativeChargeButton.isSelected()) {
			return -1;
		}
		double value = random.nextDouble();
		if (value < 0.4) {
			return -1;
		}
		return value > 0.6 ? 1 : 0;
	}

	private static JComponent row(String label, JTextField field) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		row.add(new JLabel(label));
		row.add(field);
		return row;
	}
}
