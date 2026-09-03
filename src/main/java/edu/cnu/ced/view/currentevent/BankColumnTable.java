package edu.cnu.ced.view.currentevent;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * The Current Event view's central table: one row per present bank/column,
 * grouped by bank with an alternating background (matching legacy CED's
 * NodeTable), single selection driving the value list, and a
 * {@link #makeBankVisible(String)} hook for the present-banks list's
 * single-click behavior.
 */
@SuppressWarnings("serial")
public final class BankColumnTable extends JTable {

	public BankColumnTable() {
		super(new BankColumnTableModel());
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setShowGrid(true);
		setGridColor(Color.gray);
		setFont(Fonts.tweenFont);

		getTableHeader().setFont(Fonts.defaultBoldFont);
		getTableHeader().setBackground(X11Colors.getX11Color("wheat"));

		DefaultTableCellRenderer renderer = new BankGroupRenderer();
		renderer.setHorizontalAlignment(SwingConstants.CENTER);
		for (int column = 0; column < getColumnCount(); column++) {
			getColumnModel().getColumn(column).setCellRenderer(renderer);
		}
		getColumnModel().getColumn(0).setPreferredWidth(270);
		getColumnModel().getColumn(1).setPreferredWidth(90);
		getColumnModel().getColumn(2).setPreferredWidth(70);
	}

	/** @return this table's model, narrowed from {@link #getModel()} */
	public BankColumnTableModel bankColumnModel() {
		return (BankColumnTableModel) getModel();
	}

	/** @return the entry backing the selected row, or {@code null} if none is selected */
	public BankColumnEntry selectedEntry() {
		return bankColumnModel().entryAt(getSelectedRow());
	}

	/**
	 * Scroll to and select the first row belonging to {@code bankName}, as when
	 * the present-banks list is single-clicked.
	 */
	public void makeBankVisible(String bankName) {
		int row = bankColumnModel().rowForBank(bankName);
		if (row >= 0) {
			scrollRectToVisible(getCellRect(getRowCount() - 1, 0, true));
			scrollRectToVisible(getCellRect(row, 0, true));
			getSelectionModel().setSelectionInterval(row, row);
		}
	}

	/** Alternates row background by the entry's bank index; yellow when selected. */
	private final class BankGroupRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			Component cell = super.getTableCellRendererComponent(table, value, isSelected,
					hasFocus, row, column);
			BankColumnEntry entry = bankColumnModel().entryAt(row);
			if (entry == null) {
				cell.setBackground(Color.red);
				cell.setForeground(Color.white);
			} else if (isSelected) {
				cell.setBackground(Color.yellow);
				cell.setForeground(Color.black);
			} else if ((entry.bankIndex() % 2) == 0) {
				cell.setBackground(X11Colors.getX11Color("alice blue"));
				cell.setForeground(X11Colors.getX11Color("dark blue"));
			} else {
				cell.setBackground(X11Colors.getX11Color("misty rose"));
				cell.setForeground(X11Colors.getX11Color("dark red"));
			}
			return cell;
		}
	}
}
