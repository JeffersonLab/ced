package edu.cnu.ced.view.currentevent;

/**
 * One row of the Current Event view's central table: a single bank/column
 * pair present in an event, plus everything the table needs to render it.
 *
 * @param bankName the owning bank's name, e.g. {@code "CND::adc"}
 * @param columnName the column name within that bank, e.g. {@code "sector"}
 * @param bankIndex 0-based position of {@code bankName} among the event's
 *        sorted bank names -- used only to alternate the table's row
 *        background by bank, not by individual row
 * @param typeName the column's schema type name, e.g. {@code "int"}
 * @param rowCount the bank's row count (shared by every column in the bank)
 */
public record BankColumnEntry(String bankName, String columnName, int bankIndex,
		String typeName, int rowCount) {

	/** @return {@code "bankName.columnName"}, as shown in the table's Name column */
	public String fullName() {
		return bankName + "." + columnName;
	}
}
