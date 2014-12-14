import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class LogReader {

	static class QuitAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3368964938649367920L;

		/**
		 * 
		 */

		public QuitAction(App app) {
			super("Quit");
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
					java.awt.event.KeyEvent.VK_Q,
					java.awt.event.InputEvent.CTRL_DOWN_MASK));
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			System.exit(0);
		}
	}

	static class OpenAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4752327444725892126L;
		private App app;

		public OpenAction(App app) {
			super("Open");
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
					java.awt.event.KeyEvent.VK_O,
					java.awt.event.InputEvent.CTRL_DOWN_MASK));
			this.app = app;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			JFileChooser chooser = new JFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter(
					"Log database", "rdb");
			chooser.setFileFilter(filter);
			int returnVal = chooser.showOpenDialog(app.main);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				if (chooser.getSelectedFile().exists()) {
					Connection c = null;
					try {
						PreparedStatement stmt;
						c = DriverManager.getConnection("jdbc:sqlite:"
								+ chooser.getSelectedFile().getPath());
						stmt = c.prepareStatement("SELECT Time_ms, StateAfter,"
								+ " MsgClass, MsgNumber,"
								+ " Var1, Var2, Var3 FROM logdata;",
								ResultSet.TYPE_FORWARD_ONLY,
								ResultSet.CONCUR_READ_ONLY);
						ResultSet res = stmt.executeQuery();
						app.table_data.setResult(res);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(
								app.main,
								"Error while opening log: "
										+ e.getClass().getName() + ": "
										+ e.getMessage());

					}
				} else {
					JOptionPane.showMessageDialog(app.main,
							"File doesn't exists");
				}
			}
		}
	};

	static class TimeCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = -9001968935061826439L;
		private DateFormat time_format = new SimpleDateFormat("HH:mm:ss.SSS");

		TimeCellRenderer(TimeZone tz) {
			Calendar cal = new GregorianCalendar();
			cal.setTimeZone(tz);
			time_format.setCalendar(cal);
		}

		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {

			if (value instanceof Date) {
				value = time_format.format(value);
			}
			return super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
		}
	};

	static class DateCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = -9001968935061826439L;
		private DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd");

		DateCellRenderer(TimeZone tz) {
			Calendar cal = new GregorianCalendar();
			cal.setTimeZone(tz);
			date_format.setCalendar(cal);
		}

		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {

			if (value instanceof Date) {
				value = date_format.format(value);
			}
			return super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
		}
	};

	static class StateCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = -9001968935061826439L;

		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			final String state_str[] = { "CD", "C", "CAD", "CA", "?", "?",
					"CDA", "?" };
			final Color state_color[] = { new Color(255, 102, 0), Color.red,
					Color.green, Color.yellow, Color.gray, Color.gray,
					Color.green, Color.gray };

			Object v = value;
			v = state_str[((Integer) value).intValue() & 0x7];
			java.awt.Component c = super.getTableCellRendererComponent(table,
					v, isSelected, hasFocus, row, column);
			c.setBackground(state_color[((Integer) value).intValue() & 0x7]);
			return c;
		}
	};

	static class App {
		public LogTableModel table_data;
		public JFrame main;
	};

	private static void createMainWindow(final App app) {
		JFrame win = new JFrame("Log reader");
		win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final TimeZone tz = new SimpleTimeZone(3600 * 1000, "CET");
		TableColumnModel cols = new DefaultTableColumnModel();
		TableColumn col = new TableColumn(0, 10, null, null);
		col.setHeaderValue("ID");
		cols.addColumn(col);
		col = new TableColumn(1, 20, new TimeCellRenderer(tz), null);
		col.setHeaderValue("Time");
		cols.addColumn(col);
		col = new TableColumn(2, 20, new DateCellRenderer(tz), null);
		col.setHeaderValue("Date");
		cols.addColumn(col);
		col = new TableColumn(3, 10, new StateCellRenderer(), null);
		col.setHeaderValue("State");
		cols.addColumn(col);
		col = new TableColumn(4, 40, null, null);
		col.setHeaderValue("Description");
		cols.addColumn(col);

		LogTableModel table_data = new LogTableModel();
		app.table_data = table_data;
		JTable table = new JTable(table_data, cols);
		table.setRowSorter(new TableRowSorter<TableModel>(table_data));
		JScrollPane scrollpane = new JScrollPane(table);

		Container body = win.getContentPane();
		body.setLayout(new BoxLayout(win.getContentPane(), BoxLayout.Y_AXIS));

		Container top_box = new Box(BoxLayout.X_AXIS);
		
		top_box.add(Box.createHorizontalGlue());
		
		JLabel tz_label = new JLabel("Time zone:");
		top_box.add(tz_label);
		
		long default_offset = TimeZone.getDefault().getRawOffset() / (1000*3600);
		final SpinnerModel tz_offset = new SpinnerNumberModel(0,-12,12,1);

		JSpinner tz_spinner = new JSpinner();
		JSpinner.NumberEditor tz_edit = new JSpinner.NumberEditor(tz_spinner,"+##;-##");
		tz_edit.getTextField().setColumns(3);
		System.err.println("Editor preferred: "+tz_edit.getPreferredSize());
		tz_spinner.setModel(tz_offset);
		tz_spinner.setEditor(tz_edit);
		tz_offset.setValue(default_offset); // Make the editor show correct value
		System.err.println("Spinner preferred: "+tz_spinner.getPreferredSize());
		tz_spinner.setMaximumSize(tz_spinner.getPreferredSize());
		
		tz_spinner.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				tz.setRawOffset(((Integer)tz_offset.getValue()).intValue()*3600*1000);
				app.table_data.fireTableDataChanged();
			}
		});
		
		top_box.add(tz_spinner);
		
		body.add(top_box);

		body.add(scrollpane);
		JMenuBar menu_bar = new JMenuBar();
		JMenu file_menu = new JMenu("File");
		file_menu.add(new OpenAction(app));
		file_menu.add(new QuitAction(app));
		menu_bar.add(file_menu);
		win.setJMenuBar(menu_bar);
		win.pack();
		win.setVisible(true);
		app.main = win;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				App app = new App();
				try {
					Class.forName("org.sqlite.JDBC");
				} catch (Exception e) {
					System.err.println(e.getClass().getName() + ": "
							+ e.getMessage());
					System.exit(0);
				}
				createMainWindow(app);

				System.err.println("Running");
			}
		});

	}

}
