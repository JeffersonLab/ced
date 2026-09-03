package edu.cnu.ced.view.currentevent;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.EventNavigator;

/**
 * A free-floating window holding one bank's {@link BankViewerPanel} --
 * matching legacy CED's CedDataWindow. Disposed (not merely hidden) on
 * close, so {@code onClosed} can evict it from its owner's cache and let a
 * later double-click build a fresh one.
 */
@SuppressWarnings("serial")
public final class BankFloatingWindow extends JFrame {

	private final BankViewerPanel panel;

	public BankFloatingWindow(EventNavigator navigator, String bankName, DataBank initialBank,
			BankColumnVisibility visibility, Runnable onClosed) {
		super(bankName);
		panel = new BankViewerPanel(navigator, bankName, initialBank, visibility);
		setContentPane(panel);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent event) {
				panel.dispose();
				onClosed.run();
			}
		});
		pack();
	}

	/** Show this window and bring it to the front of the desktop. */
	public void showAndFront() {
		setVisible(true);
		toFront();
		requestFocus();
	}
}
