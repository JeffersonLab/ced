package edu.cnu.ced.event;

import java.util.Arrays;

import org.jlab.io.base.DataBank;

/** Defensive helpers for reading optional CLAS12 bank columns. */
public final class BankAccess {

	private BankAccess() { }

	public static boolean hasColumn(DataBank bank, String columnName) {
		return bank != null && columnName != null
				&& Arrays.asList(columns(bank)).contains(columnName);
	}

	public static String[] columns(DataBank bank) {
		if (bank == null || bank.getColumnList() == null) {
			return new String[0];
		}
		String[] copy = bank.getColumnList().clone();
		Arrays.sort(copy);
		return copy;
	}
}
