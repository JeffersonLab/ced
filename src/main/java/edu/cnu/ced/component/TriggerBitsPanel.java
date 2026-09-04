package edu.cnu.ced.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import edu.cnu.ced.event.RunTrigger;
import edu.cnu.mdi.ui.colors.X11Colors;

/**
 * The main menu bar's trigger-bit status row: a "trigger" label, 32 small
 * boxes (bit 31 down to bit 0, left to right) shaded to show which bits are
 * set in the current event's {@code RUN::trigger} word, and the word's
 * decimal value -- matching legacy CED's own main-window trigger status bar
 * (its {@code cnuphys.ced.trigger.TriggerMenuPanel}), including its exact
 * on/off colors. Unlike legacy's dual-purpose {@code TriggerPanel} (also
 * reused, editable, as a click-to-filter widget in a separate dialog), this
 * is read-only -- CED 2.0 has no trigger-filter dialog yet.
 */
@SuppressWarnings("serial")
public final class TriggerBitsPanel extends JPanel {

	private static final int CELL_SIZE = 15;
	private static final int BIT_COUNT = 32;
	private static final Color ON_COLOR = X11Colors.getX11Color("dark red");
	private static final Color OFF_COLOR = new Color(224, 224, 224);
	private static final Font BIT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 9);
	private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);
	private static final Font VALUE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

	private final JLabel valueLabel = new JLabel("0", SwingConstants.CENTER);
	private long triggerWord;

	public TriggerBitsPanel() {
		super(new BorderLayout(4, 0));

		JLabel triggerLabel = new JLabel("trigger");
		triggerLabel.setFont(LABEL_FONT);
		triggerLabel.setForeground(X11Colors.getX11Color("dark red"));
		add(triggerLabel, BorderLayout.WEST);

		JPanel bits = new JPanel(new GridLayout(1, BIT_COUNT, 1, 1));
		for (int index = BIT_COUNT - 1; index >= 0; index--) {
			bits.add(new BitBox(index));
		}
		add(bits, BorderLayout.CENTER);

		valueLabel.setFont(VALUE_FONT);
		valueLabel.setOpaque(true);
		valueLabel.setBackground(Color.black);
		valueLabel.setForeground(Color.yellow);
		valueLabel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.cyan), BorderFactory.createEmptyBorder(1, 6, 1, 6)));
		add(valueLabel, BorderLayout.EAST);

		setTrigger(null);
	}

	/** Show {@code trigger}'s bits, or an all-off row if the bank was absent for this event. */
	public void setTrigger(RunTrigger trigger) {
		triggerWord = (trigger == null) ? 0L : trigger.trigger();
		valueLabel.setText(Long.toString(triggerWord));
		repaint();
	}

	/** One 32nd of the row: a fixed-size box painting its own bit index and on/off state. */
	private final class BitBox extends JComponent {

		private final int index;

		BitBox(int index) {
			this.index = index;
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(CELL_SIZE, CELL_SIZE);
		}

		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		@Override
		protected void paintComponent(Graphics g) {
			int width = getWidth();
			int height = getHeight();
			boolean bitOn = (triggerWord & (1L << index)) != 0;

			g.setColor(bitOn ? ON_COLOR : OFF_COLOR);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.black);
			g.drawLine(0, 0, width - 1, 0);
			g.drawLine(0, 0, 0, height - 1);
			g.setColor(Color.white);
			g.drawLine(width - 1, 0, width - 1, height - 1);
			g.drawLine(0, height - 1, width - 1, height - 1);

			g.setFont(BIT_FONT);
			g.setColor(bitOn ? Color.white : Color.black);
			FontMetrics metrics = g.getFontMetrics(BIT_FONT);
			String label = Integer.toString(index);
			int textX = (width - metrics.stringWidth(label)) / 2;
			int textY = (height + metrics.getAscent()) / 2 - 1;
			g.drawString(label, textX, textY);
		}
	}
}
