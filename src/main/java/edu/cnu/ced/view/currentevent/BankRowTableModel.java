package edu.cnu.ced.view.currentevent;

import java.util.Arrays;
import java.util.Comparator;

import javax.swing.table.AbstractTableModel;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.data.BankColumns;
import edu.cnu.ced.event.BankAccess;
import edu.cnu.ced.event.EventSnapshot;

/**
 * Table model for one bank's per-bank data viewer: an index column plus one
 * column per schema column, sortable by clicking a header.
 * <p>
 * The column set is established once, from the {@link DataBank} present when
 * the viewer is opened, and never changes afterward -- matching legacy CED's
 * BankTableModel, whose columns come from the schema rather than any one
 * event. Only row data refreshes as the shared navigator moves to a new
 * event, so column widths and the Visibility selections a viewer's owner
 * sets stay put across navigation.
 */
public final class BankRowTableModel extends AbstractTableModel {

	private final String bankName;
	private final String[] columnNames;

	private DataBank bank;
	private int[] rowOrder = new int[0];
	private boolean ascending = true;
	private int lastSortColumn = -1;

	public BankRowTableModel(String bankName, DataBank initialBank) {
		this.bankName = bankName;
		this.columnNames = BankAccess.columns(initialBank);
		acceptBank(initialBank);
	}

	/** @return the bank name this model displays */
	public String bankName() {
		return bankName;
	}

	/** Refresh row data for a new event; the column set is unaffected. */
	public void setSnapshot(EventSnapshot snapshot) {
		acceptBank(snapshot == null ? null : snapshot.bank(bankName).orElse(null));
	}

	private void acceptBank(DataBank bank) {
		this.bank = bank;
		int rows = (bank == null) ? 0 : bank.rows();
		rowOrder = new int[rows];
		for (int i = 0; i < rows; i++) {
			rowOrder[i] = i;
		}
		lastSortColumn = -1;
		ascending = true;
		fireTableDataChanged();
	}

	@Override
	public int getRowCount() {
		return rowOrder.length;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length + 1;
	}

	@Override
	public String getColumnName(int column) {
		return (column == 0) ? "" : columnNames[column - 1];
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override
	public Object getValueAt(int row, int column) {
		if (row < 0 || row >= rowOrder.length) {
			return null;
		}
		int actualRow = rowOrder[row];
		if (column == 0) {
			return actualRow;
		}
		return BankColumns.formattedValue(bank, columnNames[column - 1], actualRow);
	}

	/** @return the schema column name for table column {@code column} (1-based; 0 is the index) */
	public String columnName(int column) {
		return (column >= 1 && column <= columnNames.length) ? columnNames[column - 1] : null;
	}

	/**
	 * Re-sort by the values in {@code column}; a second click on the same
	 * column reverses direction. Sorting a column whose type this class cannot
	 * read as a number (string, group, ...) tracks the click but leaves row
	 * order unchanged, matching legacy CED's own BankTableModel.
	 */
	public void sort(int column) {
		if (rowOrder.length < 2 || column < 0 || column >= getColumnCount()) {
			return;
		}
		if (column == lastSortColumn) {
			ascending = !ascending;
		} else {
			ascending = true;
			lastSortColumn = column;
		}

		if (column == 0) {
			for (int i = 0; i < rowOrder.length; i++) {
				rowOrder[i] = ascending ? i : rowOrder.length - 1 - i;
			}
			fireTableDataChanged();
			return;
		}

		String columnName = columnNames[column - 1];
		if (!isNumeric(BankColumns.type(bank, columnName))) {
			return;
		}
		Integer[] boxed = Arrays.stream(rowOrder).boxed().toArray(Integer[]::new);
		Comparator<Integer> comparator =
				Comparator.comparingDouble(row -> BankColumns.numericValue(bank, columnName, row));
		Arrays.sort(boxed, ascending ? comparator : comparator.reversed());
		for (int i = 0; i < boxed.length; i++) {
			rowOrder[i] = boxed[i];
		}
		fireTableDataChanged();
	}

	private static boolean isNumeric(int type) {
		return type == BankColumns.INT8 || type == BankColumns.INT16 || type == BankColumns.INT32
				|| type == BankColumns.INT64 || type == BankColumns.FLOAT32
				|| type == BankColumns.FLOAT64;
	}
}
