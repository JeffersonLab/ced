package edu.cnu.ced.app;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/** Compact, scalable CED mark used by the optional startup window. */
final class CedStartupIcon implements Icon {

	private static final Color NAVY = new Color(8, 54, 96);
	private static final Color CYAN = new Color(40, 183, 211);
	private static final Color GOLD = new Color(245, 174, 32);
	private static final int SIZE = 76;

	@Override
	public int getIconWidth() {
		return SIZE;
	}

	@Override
	public int getIconHeight() {
		return SIZE;
	}

	@Override
	public void paintIcon(Component component, Graphics graphics, int x, int y) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(NAVY);
			g.fillRoundRect(x, y, SIZE, SIZE, 16, 16);
			g.setColor(CYAN);
			g.fillArc(x + 9, y + 10, 56, 56, 55, 250);
			g.setColor(NAVY);
			g.fillOval(x + 22, y + 23, 30, 30);
			g.setColor(GOLD);
			g.fillOval(x + 33, y + 34, 8, 8);

			Font font = component.getFont().deriveFont(Font.BOLD, 15f);
			g.setFont(font);
			FontMetrics metrics = g.getFontMetrics();
			String label = "CED";
			int labelX = x + (SIZE - metrics.stringWidth(label)) / 2;
			g.setColor(Color.WHITE);
			g.drawString(label, labelX, y + SIZE - 8);
		} finally {
			g.dispose();
		}
	}
}
