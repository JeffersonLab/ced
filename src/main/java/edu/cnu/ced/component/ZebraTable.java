package edu.cnu.ced.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * A {@link JTable} that alternates row background between white and a pale
 * gray, with an explicit border on every cell -- the shared "zebra table"
 * look this app uses for any table dense enough that keeping a row straight
 * across many columns is otherwise hard (originally built for the per-bank
 * data viewer's {@code BankRowTable}, reused as-is for the Monte Carlo
 * Tracks and Reconstructed Tracks tables).
 */
@SuppressWarnings("serial")
public class ZebraTable extends JTable {

	// deliberately pale -- Color.LIGHT_GRAY (192,192,192) reads as a heavy
	// stripe against black text; this is a subtle zebra background instead.
	private static final Color ALTERNATE_ROW_BACKGROUND = new Color(235, 235, 235);

	// a border painted on every cell's renderer, so each cell reads as a
	// distinct boxed field rather than relying on JTable's built-in grid
	// lines (easy to lose against the zebra background at 1px).
	private static final Border CELL_BORDER = BorderFactory.createLineBorder(Color.gray);

	public ZebraTable(TableModel model) {
		super(model);
		setShowGrid(false);
		setIntercellSpacing(new Dimension(0, 0));
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component cell = super.prepareRenderer(renderer, row, column);
		if (!isRowSelected(row)) {
			cell.setBackground((row % 2 == 0) ? Color.white : ALTERNATE_ROW_BACKGROUND);
		}
		if (cell instanceof JComponent jComponent) {
			jComponent.setBorder(CELL_BORDER);
		}
		return cell;
	}
}
