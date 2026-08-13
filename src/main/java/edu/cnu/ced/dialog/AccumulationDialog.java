package edu.cnu.ced.dialog;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import edu.cnu.ced.event.AccumulationService;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.mdi.log.Log;

/** Modal, cancellable event-accumulation workflow shared by detector views. */
@SuppressWarnings("serial")
public final class AccumulationDialog extends JDialog {

	private final EventNavigator navigator;
	private final AccumulationService accumulation;
	private final int remaining;
	private final JTextField countField;
	private final JCheckBox clear = new JCheckBox("Clear existing accumulated data", true);
	private final JProgressBar progress = new JProgressBar();
	private final JButton start = new JButton("Accumulate");
	private final JButton cancel = new JButton("Cancel");
	private SwingWorker<Integer, Integer> worker;

	public AccumulationDialog(Window owner, EventNavigator navigator,
			AccumulationService accumulation) {
		super(owner, "Accumulate Events", Dialog.ModalityType.APPLICATION_MODAL);
		this.navigator = navigator;
		this.accumulation = accumulation;
		EventNavigationState state = navigator.state();
		remaining = Math.max(0, state.eventCount() - state.sequenceNumber());
		countField = new JTextField(Integer.toString(Math.min(1000, remaining)), 8);
		buildUi(state);
	}

	private void buildUi(EventNavigationState state) {
		JPanel source = new JPanel(new GridLayout(0, 1, 2, 2));
		source.setBorder(BorderFactory.createTitledBorder("Event source"));
		source.add(new JLabel(state.isOpen() ? state.source() : "No event source open"));
		source.add(new JLabel("Total events: " + state.eventCount()));
		source.add(new JLabel("Remaining after current event: " + remaining));

		JPanel settings = new JPanel(new FlowLayout(FlowLayout.LEFT));
		settings.add(new JLabel("Number to accumulate:"));
		settings.add(countField);
		settings.add(clear);

		progress.setStringPainted(true);
		progress.setString("Waiting");

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		start.setEnabled(remaining > 0);
		start.addActionListener(event -> start());
		cancel.addActionListener(event -> cancel());
		buttons.add(start);
		buttons.add(cancel);

		JPanel center = new JPanel(new BorderLayout(6, 6));
		center.add(settings, BorderLayout.NORTH);
		center.add(progress, BorderLayout.SOUTH);
		setLayout(new BorderLayout(8, 8));
		getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		add(source, BorderLayout.NORTH);
		add(center, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(start);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		pack();
		setLocationRelativeTo(getOwner());
	}

	private void start() {
		final int requested;
		try {
			requested = Math.min(remaining, Integer.parseInt(countField.getText().trim()));
			if (requested < 1) throw new NumberFormatException();
		} catch (NumberFormatException exception) {
			JOptionPane.showMessageDialog(this, "Enter a positive number of events.",
					"Accumulate Events", JOptionPane.ERROR_MESSAGE);
			return;
		}
		accumulation.begin(clear.isSelected());
		progress.setMinimum(0);
		progress.setMaximum(requested);
		progress.setValue(0);
		progress.setString("0 / " + requested);
		start.setEnabled(false);
		countField.setEnabled(false);
		clear.setEnabled(false);
		worker = new SwingWorker<>() {
			@Override protected Integer doInBackground() {
				return navigator.scanNext(requested, accumulation::accumulate,
						this::publish, this::isCancelled);
			}
			@Override protected void process(List<Integer> values) {
				int value = values.get(values.size() - 1);
				progress.setValue(value);
				progress.setString(value + " / " + requested);
			}
			@Override protected void done() {
				if (!isCancelled()) {
					try { Log.getInstance().info("Accumulated " + get() + " events."); }
					catch (Exception exception) { Log.getInstance().exception(exception); }
				}
				dispose();
			}
		};
		worker.execute();
	}

	private void cancel() {
		if (worker != null && !worker.isDone()) worker.cancel(false);
		else dispose();
	}
}
