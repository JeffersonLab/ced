package edu.cnu.ced.view.swim;

import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import edu.cnu.ced.swim.ParticleSwimmer;
import edu.cnu.ced.view.swim.SwimTestPanel3D.SurfaceChoice;
import edu.cnu.ced.view.swim.SwimTestPanel3D.SurfaceType;

/**
 * The reference-surface picker shared by {@link SwimTestControlPanel}
 * (manual single swim) and {@link SwimBatchControlPanel} (randomized batch
 * swims): a choice of the full path, a fixed z, a fixed rho, an arbitrary
 * plane, or an arbitrary cylinder, plus the parameter fields each choice
 * needs -- extracted once a second caller needed the identical UI, matching
 * this codebase's own established pattern (e.g. {@code
 * TrackTrajectoryDrawer3D}) of factoring out shared logic at the point a
 * second caller actually needs it, not before.
 */
@SuppressWarnings("serial")
final class SurfaceChoicePanel extends JPanel {

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

	SurfaceChoicePanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Stop at"));

		ButtonGroup group = new ButtonGroup();
		JPanel choices = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		for (JRadioButton button : new JRadioButton[] { fullPathButton, fixedZButton, fixedRhoButton, planeButton, cylinderButton }) {
			group.add(button);
			choices.add(button);
			button.addActionListener(e -> fixEnabledState());
		}
		add(choices);

		add(row("Accuracy (cm)", accuracyField));
		add(row("z (cm)", fixedZField));
		add(row("ρ (cm)", fixedRhoField));
		add(vectorRow("Plane normal (x,y,z)", planeNxField, planeNyField, planeNzField));
		add(vectorRow("Plane point (cm)", planePxField, planePyField, planePzField));
		add(vectorRow("Cylinder p1 (cm)", cylinderP1xField, cylinderP1yField, cylinderP1zField));
		add(vectorRow("Cylinder p2 (cm)", cylinderP2xField, cylinderP2yField, cylinderP2zField));
		add(row("Cylinder radius (cm)", cylinderRadiusField));

		fixEnabledState();
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

	/** @throws NumberFormatException if any relevant field doesn't parse -- caller shows the message */
	SurfaceChoice surfaceChoice() {
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
