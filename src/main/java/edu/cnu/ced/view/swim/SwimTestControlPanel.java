package edu.cnu.ced.view.swim;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import edu.cnu.ced.swim.ParticleSwimmer;
import edu.cnu.ced.view.swim.SwimTestPanel3D.SurfaceChoice;
import edu.cnu.ced.view.swim.SwimTestPanel3D.SurfaceType;

/**
 * Manual swim-parameter input for {@link SwimTestPanel3D}: a charge,
 * vertex, and momentum (magnitude/theta/phi) a developer can set by hand;
 * a choice of reference surface to stop the swim at (the full path, a
 * fixed z, a fixed rho, an arbitrary plane, or an arbitrary cylinder); and
 * "Swim"/"Clear" buttons. Matches legacy CED's own {@code cnuphys.ced.
 * ced3d.SwimmerControlPanel} in spirit -- a bare parameter form for
 * testing the swimmer/field, independent of any physics event -- though
 * not its much larger randomized batch-testing apparatus (swim counts,
 * random seeds, charge randomization, success/failure filtering), which
 * this doesn't attempt to reproduce.
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

	private final JRadioButton fullPathButton = new JRadioButton("Full path", true);
	private final JRadioButton fixedZButton = new JRadioButton("Fixed z");
	private final JRadioButton fixedRhoButton = new JRadioButton("Fixed ρ (rho)");
	private final JRadioButton planeButton = new JRadioButton("Plane");
	private final JRadioButton cylinderButton = new JRadioButton("Cylinder");

	private final JTextField accuracyField = new JTextField(Double.toString(ParticleSwimmer.DEFAULT_SURFACE_ACCURACY_CM), 6);
	private final JTextField fixedZField = new JTextField("500", 6);
	private final JTextField fixedRhoField = new JTextField("100", 6);
	private final JTextField planeNxField = new JTextField("0", 4);
	private final JTextField planeNyField = new JTextField("0", 4);
	private final JTextField planeNzField = new JTextField("1", 4);
	private final JTextField planePxField = new JTextField("0", 4);
	private final JTextField planePyField = new JTextField("0", 4);
	private final JTextField planePzField = new JTextField("500", 4);
	private final JTextField cylinderP1xField = new JTextField("0", 4);
	private final JTextField cylinderP1yField = new JTextField("0", 4);
	private final JTextField cylinderP1zField = new JTextField("-100", 4);
	private final JTextField cylinderP2xField = new JTextField("0", 4);
	private final JTextField cylinderP2yField = new JTextField("0", 4);
	private final JTextField cylinderP2zField = new JTextField("600", 4);
	private final JTextField cylinderRadiusField = new JTextField("100", 6);

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

		add(surfacePanel());

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

	private JComponent surfacePanel() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(BorderFactory.createTitledBorder("Stop at"));

		ButtonGroup group = new ButtonGroup();
		JPanel choices = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		for (JRadioButton button : new JRadioButton[] { fullPathButton, fixedZButton, fixedRhoButton, planeButton, cylinderButton }) {
			group.add(button);
			choices.add(button);
			button.addActionListener(e -> fixEnabledState());
		}
		p.add(choices);

		p.add(row("Accuracy (cm)", accuracyField));
		p.add(row("z (cm)", fixedZField));
		p.add(row("ρ (cm)", fixedRhoField));
		p.add(vectorRow("Plane normal (x,y,z)", planeNxField, planeNyField, planeNzField));
		p.add(vectorRow("Plane point (cm)", planePxField, planePyField, planePzField));
		p.add(vectorRow("Cylinder p1 (cm)", cylinderP1xField, cylinderP1yField, cylinderP1zField));
		p.add(vectorRow("Cylinder p2 (cm)", cylinderP2xField, cylinderP2yField, cylinderP2zField));
		p.add(row("Cylinder radius (cm)", cylinderRadiusField));

		fixEnabledState();
		return p;
	}

	/** Enables only the fields relevant to the currently selected surface, matching legacy's own fixState(). */
	private void fixEnabledState() {
		boolean surfaceChosen = !fullPathButton.isSelected();
		accuracyField.setEnabled(surfaceChosen);
		fixedZField.setEnabled(fixedZButton.isSelected());
		fixedRhoField.setEnabled(fixedRhoButton.isSelected());
		for (JTextField field : new JTextField[] { planeNxField, planeNyField, planeNzField, planePxField, planePyField, planePzField }) {
			field.setEnabled(planeButton.isSelected());
		}
		for (JTextField field : new JTextField[] { cylinderP1xField, cylinderP1yField, cylinderP1zField,
				cylinderP2xField, cylinderP2yField, cylinderP2zField, cylinderRadiusField }) {
			field.setEnabled(cylinderButton.isSelected());
		}
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
			SurfaceChoice surface = readSurfaceChoice();

			boolean ok = panel.swim(charge, vx, vy, vz, p, theta, phi, surface);
			statusLabel.setText(ok ? "Swum OK" : "No trajectory (check momentum/field/surface)");
		} catch (NumberFormatException ex) {
			statusLabel.setText("Invalid number: " + ex.getMessage());
		}
	}

	private SurfaceChoice readSurfaceChoice() {
		double accuracy = fullPathButton.isSelected() ? 0 : Double.parseDouble(accuracyField.getText().trim());
		if (fixedZButton.isSelected()) {
			return new SurfaceChoice(SurfaceType.FIXED_Z, Double.parseDouble(fixedZField.getText().trim()),
					0, null, null, null, null, 0, accuracy);
		}
		if (fixedRhoButton.isSelected()) {
			return new SurfaceChoice(SurfaceType.FIXED_RHO, 0,
					Double.parseDouble(fixedRhoField.getText().trim()), null, null, null, null, 0, accuracy);
		}
		if (planeButton.isSelected()) {
			double[] normal = vector(planeNxField, planeNyField, planeNzField);
			double[] point = vector(planePxField, planePyField, planePzField);
			return new SurfaceChoice(SurfaceType.PLANE, 0, 0, normal, point, null, null, 0, accuracy);
		}
		if (cylinderButton.isSelected()) {
			double[] p1 = vector(cylinderP1xField, cylinderP1yField, cylinderP1zField);
			double[] p2 = vector(cylinderP2xField, cylinderP2yField, cylinderP2zField);
			double radius = Double.parseDouble(cylinderRadiusField.getText().trim());
			return new SurfaceChoice(SurfaceType.CYLINDER, 0, 0, null, null, p1, p2, radius, accuracy);
		}
		return SurfaceChoice.fullPath();
	}

	private static double[] vector(JTextField x, JTextField y, JTextField z) {
		return new double[] {
				Double.parseDouble(x.getText().trim()),
				Double.parseDouble(y.getText().trim()),
				Double.parseDouble(z.getText().trim()) };
	}

	private static JComponent row(String label, JTextField field) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		row.add(new JLabel(label));
		row.add(field);
		return row;
	}

	private static JComponent vectorRow(String label, JTextField x, JTextField y, JTextField z) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		row.add(new JLabel(label));
		row.add(x);
		row.add(y);
		row.add(z);
		return row;
	}
}
