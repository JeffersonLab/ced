package edu.cnu.ced.view.tracks;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.cnu.ced.data.TrackRow;
import edu.cnu.mdi.format.DoubleFormat;

/**
 * Table model shared by the Monte Carlo Tracks and Reconstructed Tracks
 * views -- the same 15 columns, in the same order, as legacy CED's own
 * shared {@code cnuphys.lund.TrajectoryTableModel}.
 */
public final class TrackTableModel extends AbstractTableModel {

	private static final String[] COLUMN_NAMES = { "Id", "PID", "name", "m (MeV)", "q", "x₀ (cm)",
			"y₀ (cm)", "z₀ (cm)", "p (MeV)", "θ (deg)", "φ (deg)", "KE (MeV)", "Et (MeV)",
			"status", "source" };

	/** Preferred column widths, in the same order as {@link #COLUMN_NAMES}. */
	public static final int[] COLUMN_WIDTHS =
			{ 40, 55, 65, 90, 35, 90, 90, 90, 95, 90, 90, 95, 95, 55, 180 };

	private List<TrackRow> rows = List.of();

	/** Replace the displayed rows, e.g. with a new event's extracted tracks. */
	public void setRows(List<TrackRow> rows) {
		this.rows = (rows == null) ? List.of() : rows;
		fireTableDataChanged();
	}

	@Override
	public int getRowCount() {
		return rows.size();
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
	public Object getValueAt(int rowIndex, int column) {
		TrackRow row = rowAt(rowIndex);
		if (row == null) {
			return "";
		}
		return switch (column) {
			case 0 -> Integer.toString(row.trackId());
			case 1 -> row.isSyntheticPid() ? "---" : Integer.toString(row.pid());
			case 2 -> row.name();
			case 3 -> DoubleFormat.doubleFormat(row.massMeV(), 3);
			case 4 -> Integer.toString(row.charge());
			case 5 -> DoubleFormat.doubleFormat(row.x0(), 3);
			case 6 -> DoubleFormat.doubleFormat(row.y0(), 3);
			case 7 -> DoubleFormat.doubleFormat(row.z0(), 3);
			case 8 -> DoubleFormat.doubleFormat(row.momentumMeV(), 3);
			case 9 -> DoubleFormat.doubleFormat(row.thetaDeg(), 3);
			case 10 -> DoubleFormat.doubleFormat(row.phiDeg(), 3);
			case 11 -> DoubleFormat.doubleFormat(row.kineticEnergyMeV(), 3);
			case 12 -> DoubleFormat.doubleFormat(row.totalEnergyMeV(), 3);
			case 13 -> Integer.toString(row.status());
			case 14 -> row.source();
			default -> "";
		};
	}

	/** @return the row backing table row {@code rowIndex}, or {@code null} if out of range */
	public TrackRow rowAt(int rowIndex) {
		return (rowIndex < 0 || rowIndex >= rows.size()) ? null : rows.get(rowIndex);
	}
}
