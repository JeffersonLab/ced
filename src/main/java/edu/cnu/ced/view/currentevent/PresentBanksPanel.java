package edu.cnu.ced.view.currentevent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * The Current Event view's east panel: every bank present in the current
 * event, in small red/italic labels -- matching legacy CED's
 * ClasIoPresentBankPanel. A single click scrolls the central table to that
 * bank; a double click opens its per-bank data viewer.
 */
@SuppressWarnings("serial")
public final class PresentBanksPanel extends JPanel {

	private final JPanel grid = new JPanel(new GridLayout(0, 1, 2, 0));
	private final Consumer<String> onSingleClick;
	private final Consumer<String> onDoubleClick;

	public PresentBanksPanel(Consumer<String> onSingleClick, Consumer<String> onDoubleClick) {
		this.onSingleClick = onSingleClick;
		this.onDoubleClick = onDoubleClick;
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Present Banks"));
		JScrollPane scrollPane = new JScrollPane(grid);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		add(scrollPane, BorderLayout.CENTER);
	}

	/** Replace the displayed banks, e.g. with a new event's {@code EventSnapshot.bankNames()}. */
	public void setBankNames(List<String> bankNames) {
		grid.removeAll();
		for (String bankName : bankNames) {
			grid.add(bankLabel(bankName));
		}
		grid.revalidate();
		grid.repaint();
	}

	private JLabel bankLabel(String bankName) {
		JLabel label = new JLabel(bankName);
		label.setFont(Fonts.commonFont(Font.ITALIC, 10));
		label.setForeground(Color.red);
		label.setOpaque(true);
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 1) {
					onSingleClick.accept(bankName);
				} else if (event.getClickCount() == 2) {
					onDoubleClick.accept(bankName);
				}
			}

			@Override
			public void mouseEntered(MouseEvent event) {
				label.setBackground(Color.yellow);
			}

			@Override
			public void mouseExited(MouseEvent event) {
				label.setBackground(null);
			}
		});
		return label;
	}
}
