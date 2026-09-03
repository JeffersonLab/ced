package edu.cnu.ced.data;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataDescriptor;

import edu.cnu.mdi.format.DoubleFormat;

/**
 * Generic, schema-driven access to a single column of a {@link DataBank},
 * for any bank/column pair -- no hardcoded knowledge of particular banks.
 * <p>
 * Column types are read through {@link DataDescriptor#getProperty(String, String)}
 * with the property name {@code "type"}, which every {@code DataBank}
 * implementation (HIPO, EVIO, ...) is required to answer. The returned codes
 * match {@code org.jlab.jnp.hipo4.data.DataType}'s ordinals (BYTE=1 through
 * BRANCH=12); this class never depends on that hipo4 class directly, only on
 * the plain {@code org.jlab.io.base} interfaces.
 */
public final class BankColumns {

	private BankColumns() { }

	/** Type is unknown, or the column does not exist in the bank's schema. */
	public static final int UNKNOWN = 0;
	public static final int INT8 = 1;
	public static final int INT16 = 2;
	public static final int INT32 = 3;
	public static final int FLOAT32 = 4;
	public static final int FLOAT64 = 5;
	public static final int STRING = 6;
	public static final int GROUP = 7;
	public static final int INT64 = 8;
	public static final int VECTOR3F = 9;
	public static final int COMPOSITE = 10;
	public static final int TABLE = 11;
	public static final int BRANCH = 12;

	private static final String[] TYPE_NAMES = { "unknown", "byte", "short", "int", "float",
			"double", "string", "group", "long", "vector3f", "composite", "table", "branch" };

	/** @return the schema type code for {@code column} in {@code bank}, or {@link #UNKNOWN} */
	public static int type(DataBank bank, String column) {
		if (bank == null || column == null) {
			return UNKNOWN;
		}
		DataDescriptor descriptor = bank.getDescriptor();
		if (descriptor == null || !descriptor.hasEntry(column)) {
			return UNKNOWN;
		}
		int type = descriptor.getProperty("type", column);
		return (type < 0 || type >= TYPE_NAMES.length) ? UNKNOWN : type;
	}

	/** @return the human-readable name for a type code from {@link #type(DataBank, String)} */
	public static String typeName(int type) {
		return (type < 0 || type >= TYPE_NAMES.length) ? TYPE_NAMES[UNKNOWN] : TYPE_NAMES[type];
	}

	/** @return the human-readable type name of {@code column} in {@code bank} */
	public static String typeName(DataBank bank, String column) {
		return typeName(type(bank, column));
	}

	/**
	 * Format every row of one column as a display string, in row order.
	 * Byte/short/int/long values are formatted as plain integers; float/double
	 * values use {@link DoubleFormat}. Types this class cannot generically read
	 * (string, group, and the composite/table/branch container types) yield
	 * {@code "n/a"} for every row, matching what those columns show in legacy
	 * CED's own bank browser.
	 *
	 * @return one formatted string per row, or an empty list if the bank or
	 *         column is unavailable
	 */
	public static List<String> formattedValues(DataBank bank, String column) {
		int rows = (bank == null) ? 0 : bank.rows();
		List<String> values = new ArrayList<>(rows);
		if (rows == 0) {
			return values;
		}

		switch (type(bank, column)) {
			case INT8 -> { for (byte v : bank.getByte(column)) values.add(Byte.toString(v)); }
			case INT16 -> { for (short v : bank.getShort(column)) values.add(Short.toString(v)); }
			case INT32 -> { for (int v : bank.getInt(column)) values.add(Integer.toString(v)); }
			case INT64 -> { for (long v : bank.getLong(column)) values.add(Long.toString(v)); }
			case FLOAT32 -> {
				for (float v : bank.getFloat(column)) values.add(DoubleFormat.doubleFormat(v, 6, 4));
			}
			case FLOAT64 -> {
				for (double v : bank.getDouble(column)) values.add(DoubleFormat.doubleFormat(v, 6, 4));
			}
			default -> { for (int i = 0; i < rows; i++) values.add("n/a"); }
		}
		return values;
	}

	/** @return the formatted value of {@code column} at {@code row}, or {@code ""} if unavailable */
	public static String formattedValue(DataBank bank, String column, int row) {
		if (bank == null || row < 0 || row >= bank.rows()) {
			return "";
		}
		switch (type(bank, column)) {
			case INT8 -> { return Byte.toString(bank.getByte(column, row)); }
			case INT16 -> { return Short.toString(bank.getShort(column, row)); }
			case INT32 -> { return Integer.toString(bank.getInt(column, row)); }
			case INT64 -> { return Long.toString(bank.getLong(column, row)); }
			case FLOAT32 -> { return DoubleFormat.doubleFormat(bank.getFloat(column, row), 6, 4); }
			case FLOAT64 -> { return DoubleFormat.doubleFormat(bank.getDouble(column, row), 6, 4); }
			default -> { return "n/a"; }
		}
	}

	/**
	 * A numeric value at one row, for sorting a table by column -- {@link Double#NaN}
	 * for row/column combinations this class cannot read as a number.
	 */
	public static double numericValue(DataBank bank, String column, int row) {
		if (bank == null || row < 0 || row >= bank.rows()) {
			return Double.NaN;
		}
		return switch (type(bank, column)) {
			case INT8 -> bank.getByte(column, row);
			case INT16 -> bank.getShort(column, row);
			case INT32 -> bank.getInt(column, row);
			case INT64 -> bank.getLong(column, row);
			case FLOAT32 -> bank.getFloat(column, row);
			case FLOAT64 -> bank.getDouble(column, row);
			default -> Double.NaN;
		};
	}
}
