package edu.cnu.ced.view.currentevent;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.jlab.io.base.DataBank;

import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * The per-bank data viewer's table: an index column plus one column per
 * schema column, header-click-to-sort, per-column show/hide for the
 * viewer's Visibility checkbox row, and an alternating row background so a
 * wide, many-column bank stays easy to read across.
 */
@SuppressWarnings("serial")
public final class BankRowTable extends JTable {

	public static final int COLUMN_WIDTH = 100;

	// deliberately pale -- Color.LIGHT_GRAY (192,192,192) reads as a heavy
	// stripe against black text; this is a subtle zebra background instead.
	private static final Color ALTERNATE_ROW_BACKGROUND = new Color(235, 235, 235);

	public BankRowTable(String bankName, DataBank initialBank) {
		super(new BankRowTableModel(bankName, initialBank));
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setFont(Fonts.tweenFont);
		setShowGrid(true);
		setGridColor(Color.gray);

		JTableHeader header = getTableHeader();
		header.setResizingAllowed(true);
		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				int column = columnAtPoint(event.getPoint());
				if (column >= 0) {
					rowModel().sort(column);
				}
			}
		});
	}

	/** @return this table's model, narrowed from {@link #getModel()} */
	public BankRowTableModel rowModel() {
		return (BankRowTableModel) getModel();
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component cell = super.prepareRenderer(renderer, row, column);
		if (!isRowSelected(row)) {
			cell.setBackground((row % 2 == 0) ? Color.white : ALTERNATE_ROW_BACKGROUND);
		}
		return cell;
	}

	/** Show or hide the table column backing schema column {@code columnIndex} (1-based). */
	public void setColumnVisible(int columnIndex, boolean visible) {
		TableColumn column = getColumnModel().getColumn(columnIndex);
		if (visible) {
			column.setMinWidth(20);
			column.setMaxWidth(Integer.MAX_VALUE);
			column.setPreferredWidth(COLUMN_WIDTH);
			column.setResizable(true);
		} else {
			column.setMinWidth(0);
			column.setMaxWidth(0);
			column.setPreferredWidth(0);
			column.setResizable(false);
		}
		revalidate();
	}
}
