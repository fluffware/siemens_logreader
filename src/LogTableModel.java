import javax.swing.table.AbstractTableModel;

import java.sql.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;

/**
 *  Model that maps from a SQLite database containing a Siemens HMI log
 *  
 * @author ksb
 * 
 *        
 */
public class LogTableModel extends AbstractTableModel {

	public class LogRow {
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
	static public final int ColRow = 5;
	static public final int ColRowIndex = 6;
	static public final int ColCount = 7;
	
	static public final int SQLColTime_ms = 1;
	static public final int SQLColStateAfter = 2;
	static public final int SQLColMsgClass = 3;
	static public final int SQLColMsgNumber = 4;
	static public final int SQLColVar1 = 5;
	static public final int SQLColVar2 = 6;
	static public final int SQLColVar3 = 7; 
	static public final int SQLColVar4 = 8; 
	static public final int SQLColVar5 = 9; 
	static public final int SQLColVar6 = 10; 
	static public final int SQLColVar7 = 11; 
	static public final int SQLColVar8 = 12; 
	
	protected long time_base = -2209165200000L; // Difference between 1900 and 1970

	public void setResult(ResultSet r) throws SQLException {
		result = r;
		rows.clear();
		row_count = 0;
		while(r.next()) {
			LogRow row = new LogRow();
			row.Time_msec = Math.round( r.getDouble(SQLColTime_ms)*(3600*24)/1e3) + time_base; // Stored as 1/1e6 days
			row.StateAfter = r.getInt(SQLColStateAfter);
			row.MsgClass = r.getInt(SQLColMsgClass);
			row.MsgNumber = r.getInt(SQLColMsgNumber);
			row.Var1 = r.getString(SQLColVar1);
			row.Var2 = r.getString(SQLColVar2);
			row.Var3 = r.getString(SQLColVar3);
			row.Var4 = r.getString(SQLColVar4);
			row.Var5 = r.getString(SQLColVar5);
			row.Var6 = r.getString(SQLColVar6);
			row.Var7 = r.getString(SQLColVar7);
			row.Var8 = r.getString(SQLColVar8);
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
		// System.err.println("getRowCount");
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
		// System.err.println("row_count = " + row_count);
		return row_count;
	}

	private String append_nonempty(String str) {
		if (str != null && !str.equals("")) {
			return ";"+str;
		}
		return "";
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
			return row.Var1 
					+ append_nonempty(row.Var2)
					+ append_nonempty(row.Var3)
					+ append_nonempty(row.Var4)
					+ append_nonempty(row.Var5)
					+ append_nonempty(row.Var6)
					+ append_nonempty(row.Var7)
					+ append_nonempty(row.Var8)
					+ append_nonempty(row.MsgText);
		case ColRow:
			return row;
		case ColRowIndex:
			return rowIndex;
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
		case ColRow:
			return LogRow.class;
		case ColRowIndex:
			return Integer.class;
		}
		return null;
	}
	
	public SortByIncoming createSortByIncoming() {
		return new SortByIncoming();
	}
	
	public class SortByIncoming implements Comparator<Integer> {
		Integer sorted_to_model[];  // Maps from sorted to  model
		Integer model_to_sorted[];  // Maps from model to sorted
		class DateComparator implements java.util.Comparator<Integer> {
			public int compare(Integer r1, Integer r2) {
				return Long.compare(rows.get(r1).Time_msec,rows.get(r2).Time_msec);
			}
		}	
		
		// to <= from
		private void move_row(int from, int to) {
			//System.err.println("Move "+from+"->"+to);
			int v = sorted_to_model[from];
			for (int i = from; i > to; i--) {
				sorted_to_model[i] = sorted_to_model[i-1];
			}
			sorted_to_model[to] = v;
		}
		
		public void sort() {
			int row_count = getRowCount();
			sorted_to_model = new Integer[row_count];
			
			for (int i = 0; i < row_count; i++) {
				sorted_to_model[i] = i;
			}

			// Sort on date
			Arrays.sort(sorted_to_model, new DateComparator());
			//System.err.println("RowComparator.sort "+row_count);
			
			// Find next incoming event
			
			for (int incoming = 0; incoming < row_count; incoming++) {
				LogRow incoming_row = rows.get(sorted_to_model[incoming]);
				if ((incoming_row.StateAfter & 0x07) == 1) {
					//System.err.println("Incoming at "+incoming+" MsgNumber="+incoming_row.MsgNumber);
					for (int related = incoming + 1; related < row_count; related++) {
						LogRow related_row = rows.get(sorted_to_model[related]);
						if (related_row.MsgNumber == incoming_row.MsgNumber) {
							// Stop looking for related rows if we find another incoming
							if ((related_row.StateAfter & 0x07) == 1) {
								//System.err.println("Stopped looking for related. Found incoming at "+related);
								break;
							}
							//System.err.println("Related at "+related+" MsgNumber="+related_row.MsgNumber);
							incoming++;
							move_row(related,incoming);
						}
					}
				}
			}
			model_to_sorted = new Integer[row_count];
			for (int i = 0; i < row_count; i++) {
				model_to_sorted[sorted_to_model[i]] = i;
			}
			/*
			for (int i = 0; i < row_count; i++) System.err.print(" "+sorted_to_model[i]);
			System.err.println();
			for (int i = 0; i < row_count; i++) System.err.print(" "+model_to_sorted[i]);
			System.err.println();
			*/
		}
		
		public int compare(Integer r1, Integer r2) {
	//		System.err.println("Compare "+r1+" "+r2);
			return model_to_sorted[r1] - model_to_sorted[r2];
		}
	}
	
}
