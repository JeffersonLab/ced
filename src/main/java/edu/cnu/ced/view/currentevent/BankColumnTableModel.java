package edu.cnu.ced.view.currentevent;

import java.util.List;

import javax.swing.table.AbstractTableModel;

/** Table model for the Current Event view's central Name/Type/Count table. */
public final class BankColumnTableModel extends AbstractTableModel {

	private static final String[] COLUMN_NAMES = { "Name", "Type", "Count" };

	private List<BankColumnEntry> entries = List.of();

	/** Replace the displayed entries, e.g. with a new event's {@link BankColumnCatalog}. */
	public void setEntries(List<BankColumnEntry> entries) {
		this.entries = (entries == null) ? List.of() : entries;
		fireTableDataChanged();
	}

	@Override
	public int getRowCount() {
		return entries.size();
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override
	public Object getValueAt(int row, int column) {
		BankColumnEntry entry = entryAt(row);
		if (entry == null) {
			return "";
		}
		return switch (column) {
			case 0 -> entry.fullName();
			case 1 -> entry.typeName();
			case 2 -> Integer.toString(entry.rowCount());
			default -> "";
		};
	}

	/** @return the entry backing {@code row}, or {@code null} if out of range */
	public BankColumnEntry entryAt(int row) {
		return (row < 0 || row >= entries.size()) ? null : entries.get(row);
	}

	/** @return the first row whose bank is {@code bankName}, or -1 if none */
	public int rowForBank(String bankName) {
		if (bankName == null) {
			return -1;
		}
		for (int row = 0; row < entries.size(); row++) {
			if (entries.get(row).bankName().equals(bankName)) {
				return row;
			}
		}
		return -1;
	}
}
