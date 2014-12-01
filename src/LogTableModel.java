import javax.swing.table.AbstractTableModel;
import java.sql.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 *  Model that maps from a SQLite datbase containing a Siemens HMI log
 *  
 * @author ksb
 * 
 *        
 */
public class LogTableModel extends AbstractTableModel {

	protected class LogRow {
		int index;
		long Time_msec;
		int MsgProc;
		int StateAfter;
		int MsgClass;
		int MsgNumber;
		String Var1;
		String Var2;
		String Var3;
		String Var4;
		String Var5;
		String Var6;
		String Var7;
		String Var8;
		String TimeString;
		String MsgText;
		String PLC;
	}

	ArrayList<LogRow> rows = new ArrayList<LogRow>();
	ResultSet result = null;
	int row_count = -1;
	
	Calendar cal = new GregorianCalendar();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static public final int ColID = 0;
	static public final int ColTime = 1;
	static public final int ColDate = 2;
	static public final int ColState = 3;
	static public final int ColDescription = 4;
	static public final int ColCount = 5;
	
	static public final int SQLColTime_ms = 1;
	static public final int SQLColStateAfter = 2;
	static public final int SQLColMsgClass = 3;
	static public final int SQLColMsgNumber = 4;
	static public final int SQLColVar1 = 5;
	static public final int SQLColVar2 = 6;
	static public final int SQLColVar3 = 7; 

	
	protected long time_base = -2209165200000L; // Difference between 1900 and 1970

	public void setResult(ResultSet r) throws SQLException {
		result = r;
		rows.clear();
		row_count = 0;
		System.err.println("time_base="+time_base);
		while(r.next()) {
			LogRow row = new LogRow();
			row.Time_msec = Math.round( r.getDouble(SQLColTime_ms)*(3600*24)/1e3) + time_base; // Stored as 1/1e6 days
			row.StateAfter = r.getInt(SQLColStateAfter);
			row.MsgClass = r.getInt(SQLColMsgClass);
			row.MsgNumber = r.getInt(SQLColMsgNumber);
			row.Var1 = r.getString(SQLColVar1);
			row.Var2 = r.getString(SQLColVar2);
			row.Var3 = r.getString(SQLColVar3);
			/*
			row.Var4 = r.getString(SQLColVar4);
			row.Var5 = r.getString(SQLColVar5);
			row.Var6 = r.getString(SQLColVar6);
			row.Var7 = r.getString(SQLColVar7);
			row.Var8 = r.getString(SQLColVar8);*/
			rows.add(row);
			row_count++;
		}
		fireTableDataChanged();
	}

	@Override
	public int getColumnCount() {
		return ColCount;
	}

	@Override
	public int getRowCount() {
		System.err.println("getRowCount");
		if (result == null)
			return 0;
		try {
			if (row_count < 0) {
				result.last();
				row_count = result.getRow();
			}
		} catch (SQLException e) {
			System.err.println("Failed to get number of rows: "
					+ e.getMessage());
			return 0;
		}
		System.err.println("row_count = " + row_count);
		return row_count;
	}

	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		LogRow row = rows.get(rowIndex);
		switch (columnIndex) {
		case ColID:
			return row.MsgNumber;
		case ColTime:
			return new Date(row.Time_msec);
		case ColDate:
			return new Date(row.Time_msec);
		case ColState:
			return row.StateAfter;
		case ColDescription:
			return row.Var1 + ";" + row.Var2 + ";" + row.Var3
					+ ";" + row.Var4 + ";" + row.Var5 + ";"
					+ row.Var6 + ";" + row.Var7 + ";"
					+ row.Var8 + ";" + row.MsgText;
		}
		return null;
	}
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case ColID:
			return Integer.class; 
		case ColTime:
			return Date.class;
		case ColDate:
			return Date.class;

		case ColState:
			return Integer.class;
		case ColDescription:
			return String.class;
		}
		return null;
	}
}
