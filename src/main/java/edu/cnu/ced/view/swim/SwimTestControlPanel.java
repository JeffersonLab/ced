package edu.cnu.ced.view.swim;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Manual swim-parameter input for {@link SwimTestPanel3D}: a charge,
 * vertex, and momentum (magnitude/theta/phi) a developer can set by hand,
 * a "Swim" button that swims exactly that hypothetical particle through
 * the field, and a "Clear" button. Matches legacy CED's own {@code
 * cnuphys.ced.ced3d.SwimmerControlPanel} in spirit -- a bare parameter
 * form for testing the swimmer/field, independent of any physics event --
 * though not its optional reference-surface (constant-z plane / constant-
 * rho or arbitrary cylinder / arbitrary plane) picker, deferred as a
 * follow-up.
 */
@SuppressWarnings("serial")
final class SwimTestControlPanel extends JPanel {

	private final JTextField chargeField = new JTextField("1", 4);
	private final JTextField vxField = new JTextField("0", 6);
	private final JTextField vyField = new JTextField("0", 6);
	private final JTextField vzField = new JTextField("0", 6);
	private final JTextField momentumField = new JTextField("1.0", 6);
	private final JTextField thetaField = new JTextField("25", 6);
	private final JTextField phiField = new JTextField("0", 6);
	private final JLabel statusLabel = new JLabel(" ");

	SwimTestControlPanel(SwimTestPanel3D panel) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Swim Parameters"));

		add(row("Charge (e)", chargeField));
		add(row("Vertex x (cm)", vxField));
		add(row("Vertex y (cm)", vyField));
		add(row("Vertex z (cm)", vzField));
		add(row("Momentum p (GeV/c)", momentumField));
		add(row("Theta (deg)", thetaField));
		add(row("Phi (deg)", phiField));

		JButton swim = new JButton("Swim");
		swim.setAlignmentX(Component.CENTER_ALIGNMENT);
		swim.addActionListener(e -> doSwim(panel));
		add(swim);

		JButton clear = new JButton("Clear");
		clear.setAlignmentX(Component.CENTER_ALIGNMENT);
		clear.addActionListener(e -> {
			panel.clearTrajectory();
			statusLabel.setText(" ");
		});
		add(clear);

		statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(statusLabel);
	}

	private void doSwim(SwimTestPanel3D panel) {
		try {
			int charge = Integer.parseInt(chargeField.getText().trim());
			double vx = Double.parseDouble(vxField.getText().trim());
			double vy = Double.parseDouble(vyField.getText().trim());
			double vz = Double.parseDouble(vzField.getText().trim());
			double p = Double.parseDouble(momentumField.getText().trim());
			double theta = Double.parseDouble(thetaField.getText().trim());
			double phi = Double.parseDouble(phiField.getText().trim());
			boolean ok = panel.swim(charge, vx, vy, vz, p, theta, phi);
			statusLabel.setText(ok ? "Swum OK" : "No trajectory (check momentum/field)");
		} catch (NumberFormatException ex) {
			statusLabel.setText("Invalid number: " + ex.getMessage());
		}
	}

	private static JComponent row(String label, JTextField field) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		row.add(new JLabel(label));
		row.add(field);
		return row;
	}
}
