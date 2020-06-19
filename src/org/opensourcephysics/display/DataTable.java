/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.media.core.NumberField;
import org.opensourcephysics.tools.DataToolTab;

/**
 * DataTable displays multiple TableModels in a table. The first TableModel
 * usually contains the independent variable for the other TableModel so that
 * the visibility of column[0] can be set to false for subsequent TableModels.
 *
 * @author Joshua Gould
 * @author Wolfgang Christian
 * @created February 21, 2002
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DataTable extends JTable {

	/**
	 * A TableModel with no listeners that is by default uneditable.
	 * TableTrackView.TextColumnTableModel allows editing if the track is not locked
	 * as well as editing
	 * 
	 * @author hansonr
	 *
	 */
	public static abstract class OSPTableModel implements TableModel {

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		}

		@Override
		final public void addTableModelListener(TableModelListener l) {
		}

		@Override
		final public void removeTableModelListener(TableModelListener l) {
		}

	}

	/**
	 * Unique refresh modes can be OR'd
	 */
	public static final int MODE_CANCEL = 0x00;

	public static final int MODE_MASK_REBUILD = 0x0F;
	public static final int MODE_CREATE = 0x01;
	public static final int MODE_CLEAR = 0x02;
	public static final int MODE_FRAME = 0x03;
	public static final int MODE_MODEL = 0x04;
	public static final int MODE_TAB = 0x05;

	public static final int MODE_HEADER = 0x10;

	public static final int MODE_MASK_ROWCOL = 0x2F00;
	public static final int MODE_APPEND = 0x2000;
	public static final int MODE_INSERT = 0x2100;
	public static final int MODE_DELETE = 0x2200;
	public static final int MODE_COLUMN = 0x2300;
	public static final int MODE_CELLS = 0x2400;

	public static final int MODE_VALUES = 0x4000;

	public static final int MODE_MASK_STYLE = 0x8F0000;
	public static final int MODE_PATTERN = 0x810000;
	public static final int MODE_FUNCTION = 0x820000;
	public static final int MODE_FORMAT = 0x830000;

	public static final int MODE_SELECT = 0x1000000;

	public static final int MODE_SHOW = 0x2000000;

	static final Color PANEL_BACKGROUND = javax.swing.UIManager.getColor("Panel.background"); //$NON-NLS-1$
	final static Color LIGHT_BLUE = new Color(204, 204, 255);
	static final String NO_PATTERN = DisplayRes.getString("DataTable.FormatDialog.NoFormat"); //$NON-NLS-1$
	public final static String rowName = DisplayRes.getString("DataTable.Header.Row"); //$NON-NLS-1$
	private final static DoubleRenderer defaultDoubleRenderer = new DoubleRenderer();

	protected HashMap<String, PrecisionRenderer> precisionRenderersByColumnName = new HashMap<String, PrecisionRenderer>();
	protected HashMap<String, UnitRenderer> unitRenderersByColumnName = new HashMap<String, UnitRenderer>();
	protected OSPDataTableModel dataTableModel;
	protected RowNumberRenderer rowNumberRenderer;
	int maximumFractionDigits = 3;
	int refreshDelay = 0; // time in ms to delay refresh events
	int mode;

	Timer refreshTimer;
	protected int labelColumnWidth = 40, minimumDataColumnWidth = 24;
	protected NumberFormatDialog formatDialog;
	protected int clickCountToSort = 1;

	/**
	 * Constructs a DataTable with a default data model
	 */
	public DataTable() {
		super();
		init(new OSPDataTableModel());
	}

	/**
	 * Constructs a DatTable with the specified data model
	 *
	 * @param model data model
	 */
	public DataTable(OSPDataTableModel model) {
		super();
		init(model);
	}

	protected void init(OSPDataTableModel model) {
		refreshTimer = new Timer(refreshDelay, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				refreshTableNow(mode);
			}
		});
		refreshTimer.setRepeats(false);
		refreshTimer.setCoalesce(true);
		setModel(model);
		setColumnSelectionAllowed(true);
		setGridColor(Color.blue);
		setSelectionBackground(LIGHT_BLUE);
		JTableHeader header = getTableHeader();
		header.setForeground(Color.blue); // set text color
		TableCellRenderer headerRenderer = new HeaderRenderer(getTableHeader().getDefaultRenderer());
		getTableHeader().setDefaultRenderer(headerRenderer);
		setSelectionForeground(Color.red); // foreground color for selected cells
		setColumnModel(new DataTableColumnModel());
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		setColumnSelectionAllowed(true);
		dataTableModel.decorator.allocate();

		// add column sorting using a SortDecorator
		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!OSPRuntime.isPopupTrigger(e) && !e.isControlDown() && !e.isShiftDown()
						&& e.getClickCount() == clickCountToSort) {
					TableColumnModel tcm = getColumnModel();
					int vc = tcm.getColumnIndexAtX(e.getX());
					int mc = convertColumnIndexToModel(vc);
					if (dataTableModel.getSortedColumn() != mc) {
						sort(mc);
					}
				}
			}

		});
	}

	/**
	 * Sets the maximum number of fraction digits to display in a named column
	 *
	 * @param maximumFractionDigits maximum number of fraction digits to display
	 * @param columnName            name of the column
	 */
	public void setMaximumFractionDigits(String columnName, int maximumFractionDigits) {
		precisionRenderersByColumnName.put(columnName, new PrecisionRenderer(maximumFractionDigits));
	}

	/**
	 * Sets the formatting pattern for a named column
	 *
	 * @param pattern    the pattern
	 * @param columnName name of the column
	 */
	public void setFormatPattern(String columnName, String pattern) {
		if ((pattern == null) || pattern.equals("")) { //$NON-NLS-1$
			precisionRenderersByColumnName.remove(columnName);
		} else {
			precisionRenderersByColumnName.put(columnName, new PrecisionRenderer(pattern));
		}
		firePropertyChange("format", null, columnName); //$NON-NLS-1$
	}

	/**
	 * Sets the units and tooltip for a named column.
	 *
	 * @param columnName name of the column
	 * @param units      the units string (may be null)
	 * @param tootip     the tooltip (may be null)
	 */
	public void setUnits(String columnName, String units, String tooltip) {
		if (units == null) {
			unitRenderersByColumnName.remove(columnName);
		} else {
			TableCellRenderer renderer = precisionRenderersByColumnName.get(columnName);
			if (renderer == null)
				renderer = getDefaultRenderer(Double.class);
//			TableCellRenderer renderer = getDefaultRenderer(Double.class);
//      for (String next: precisionRenderersByColumnName.keySet()) {
//        if(next.equals(columnName)) {
//          renderer = precisionRenderersByColumnName.get(columnName);
//        }
//      }
			UnitRenderer unitRenderer = new UnitRenderer(renderer, units, tooltip);
			unitRenderersByColumnName.put(columnName, unitRenderer);
		}
	}

	/**
	 * Gets the formatting pattern for a named column
	 *
	 * @param columnName name of the column
	 * @return the pattern
	 */
	public String getFormatPattern(String columnName) {
		PrecisionRenderer r = precisionRenderersByColumnName.get(columnName);
		return (r == null) ? "" : r.pattern; //$NON-NLS-1$
	}

	/**
	 * Gets the names of formatted columns Added by D Brown 24 Apr 2011
	 *
	 * @return array of names of columns with non-null formats
	 */
	public String[] getFormattedColumnNames() {
		return precisionRenderersByColumnName.keySet().toArray(new String[0]);
	}

	/**
	 * Gets the formatted value at a given row and column. Added by D Brown 6 Oct
	 * 2010
	 *
	 * @param row the row number
	 * @param col the column number
	 * @return the value formatted as displayed in the table
	 */
	public Object getFormattedValueAt(int row, int col) {
		Object value = getValueAt(row, col);
		if (value == null)
			return null;
		TableCellRenderer renderer = getCellRenderer(row, col);
		Component c = renderer.getTableCellRendererComponent(DataTable.this, value, false, false, 0, 0);
		if (c instanceof JLabel) {
			String s = ((JLabel) c).getText().trim();
			// strip units, if any
			if (renderer instanceof UnitRenderer) {
				String units = ((UnitRenderer) renderer).units;
				if (!"".equals(units)) { //$NON-NLS-1$
					int n = s.lastIndexOf(units);
					if (n > -1)
						s = s.substring(0, n);
				}
			}
			return s;
		}
		return value;
	}

	/**
	 * Gets the format setter dialog.
	 *
	 * @param names    the column name choices
	 * @param selected the initially selected names
	 * @return the format setter dialog
	 */
	public NumberFormatDialog getFormatDialog(String[] names, String[] selected) {
		if (formatDialog == null) {
			formatDialog = new NumberFormatDialog();
			// center on screen
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (dim.width - formatDialog.getBounds().width) / 2;
			int y = (dim.height - formatDialog.getBounds().height) / 2;
			formatDialog.setLocation(x, y);
		}
		formatDialog.setColumns(names, selected);
		return formatDialog;
	}

	/**
	 * Sets the maximum number of fraction digits to display for cells that have
	 * type Double
	 *
	 * @param maximumFractionDigits - maximum number of fraction digits to display
	 */
	public void setMaximumFractionDigits(int maximumFractionDigits) {
		this.maximumFractionDigits = maximumFractionDigits;
		setDefaultRenderer(Double.class, new PrecisionRenderer(maximumFractionDigits));
	}

	/**
	 * Gets the maximum number of digits in the table.
	 * 
	 * @return int
	 */
	public int getMaximumFractionDigits() {
		return maximumFractionDigits;
	}

	/**
	 * Sets the display row number flag. Table displays row number.
	 *
	 * @param _rowNumberVisible <code>true<\code> if table display row number
	 */
	public void setRowNumberVisible(boolean _rowNumberVisible) {
		if (dataTableModel.isRowNumberVisible() != _rowNumberVisible) {
			if (_rowNumberVisible && (rowNumberRenderer == null)) {
				rowNumberRenderer = new RowNumberRenderer(this);
			}
			dataTableModel.setRowNumberVisible(_rowNumberVisible);
		}
	}

	/**
	 * Sets the model for this data table;
	 *
	 * @param _model
	 */
	public void setModel(OSPDataTableModel _model) {
		super.setModel(_model);
		dataTableModel = _model;
	}

	/**
	 * Sets the stride of a TableModel in the DataTable.
	 *
	 * @param tableModel
	 * @param stride
	 */
	public void setStride(TableModel tableModel, int stride) {
		dataTableModel.setStride(tableModel, stride);
	}

	/**
	 * Sets the visibility of a column of a TableModel in the DataTable.
	 *
	 * @param tableModel
	 * @param columnIndex
	 * @param b
	 */
	public void setColumnVisible(TableModel tableModel, int columnIndex, boolean b) {
		dataTableModel.setColumnVisible(tableModel, columnIndex, b);
	}

	/**
	 * Gets the display row number flag.
	 *
	 * @return The rowNumberVisible value
	 */
	public boolean isRowNumberVisible() {
		return dataTableModel.isRowNumberVisible();
	}

	/**
	 * Returns an appropriate renderer for the cell specified by this row and
	 * column. If the <code>TableColumn</code> for this column has a non-null
	 * renderer, returns that. If the <code>TableColumn</code> for this column has
	 * the same name as a name specified in the setMaximumFractionDigits method,
	 * returns the appropriate renderer. If not, finds the class of the data in this
	 * column (using <code>getColumnClass</code>) and returns the default renderer
	 * for this type of data.
	 *
	 * @param row    Description of Parameter
	 * @param column Description of Parameter
	 * @return The cellRenderer value
	 */
	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		int i = convertColumnIndexToModel(column);
		if ((i == 0) && dataTableModel.isRowNumberVisible()) {
			return rowNumberRenderer;
		}
		UnitRenderer unitRenderer = null;
		TableCellRenderer baseRenderer = null;
		try {
			// find units renderer
			// BH 2020.02.14 efficiencies -- needs doublechecking
			TableColumn tableColumn = getColumnModel().getColumn(column);
			unitRenderer = unitRenderersByColumnName.get(tableColumn.getHeaderValue());
//      Iterator<String> keys = unitRenderersByColumnName.keySet().iterator();
//      while(keys.hasNext()) {
//        String columnName = keys.next();
//        if(tableColumn.getHeaderValue().equals(columnName)) {
//          unitRenderer = unitRenderersByColumnName.get(columnName);
//          break;
//        }
//      }
			// find base renderer
			baseRenderer = tableColumn.getCellRenderer();
			if (baseRenderer == null) {
				String key = (String) tableColumn.getHeaderValue();
				baseRenderer = precisionRenderersByColumnName.get(key);
				if (baseRenderer == null && key.endsWith(DataToolTab.SHIFTED)) {
					baseRenderer = precisionRenderersByColumnName.get(key.substring(0, key.length() - 1));
				}
			}
//      if (baseRenderer==null) {
//	      keys = precisionRenderersByColumnName.keySet().iterator();
//	      while(keys.hasNext()) {
//	        String columnName = keys.next();
//	        if(tableColumn.getHeaderValue().equals(columnName)) {
//	        	baseRenderer = precisionRenderersByColumnName.get(columnName);
//	        	break;
//	        }
//	        else if(tableColumn.getHeaderValue().equals(columnName+DataToolTab.SHIFTED)) {
//	        	baseRenderer = precisionRenderersByColumnName.get(columnName);
//	        	break;
//	        }
//	      }
//      }
		} catch (Exception ex) {
		}
		// if no precision base renderer, use default
		if (baseRenderer == null) {
			if (getColumnClass(column).equals(Double.class)) {
				baseRenderer = defaultDoubleRenderer;
			} else {
				baseRenderer = getDefaultRenderer(getColumnClass(column));
			}
		}
		// return unit renderer if defined
		if (unitRenderer != null) {
			unitRenderer.setBaseRenderer(baseRenderer);
			return unitRenderer;
		}
		return baseRenderer;
	}

	/**
	 * Gets the precision renderer, if any, for a given columnn name. Added by D
	 * Brown Dec 2010
	 *
	 * @param columnName the name
	 * @return the PrecisionRenderer, or null if none
	 */
	public TableCellRenderer getPrecisionRenderer(String columnName) {
		return precisionRenderersByColumnName.get(columnName);
	}

	/**
	 * Sets the delay time for table refresh timer. Only called by TableTrackView
	 * dispose
	 *
	 * @param delay the delay in millisecond
	 */
	public void setRefreshDelay(int delay) {
		if (delay > 0) {
			refreshTimer.setDelay(delay);
			refreshTimer.setInitialDelay(delay);
		} else {
			refreshTimer.stop();
		}
		refreshDelay = delay;
	}

	/**
	 * Clear the table and stop refreshing. Only called by TableTrackView dispose.
	 *
	 * @param delay the delay in millisecond
	 */
	public void dispose() {
		clear();
		setRefreshDelay(-1);
	}
	
	/*
	 * @deprecated
	 * Refresh the table by rebuilding everything.
	 * 
	 */
	public void refreshTable() {
		 refreshTable(MODE_MASK_REBUILD);
	}

	
    /*

     * @deprecated

     * Refresh the table by rebuilding everything.

     *

      */

	/**
	 * Refresh the data in the DataTable, as well as other changes to the table,
	 * such as row number visibility. Changes to the TableModels displayed in the
	 * table will not be visible until this method is called.
	 */
	public void refreshTable(int mode) {
		if (refreshDelay > 0) {
			refreshTimer.start();
		} else {
			OSPRuntime.postEvent(new Runnable() {
				@Override
				public synchronized void run() {
					refreshTableNow(mode);
				}

			});
		}
	}

	/**
	 * Performs the action for the refresh timer and refreshTable() method by
	 * refreshing the data in the DataTable.
	 *
	 * @param cause allows more nuanced refresh
	 */

	protected void refreshTableNow(int mode) {
		// BH every sort of refresh goes through here

		OSPLog.debug("DataTable.refreshTableNow " + Integer.toHexString(mode));

		// code added by D Brown to update decimal separator Jan 2018
		try {
			// try block needed to catch occasional ConcurrentModificationException
			for (String key : precisionRenderersByColumnName.keySet()) {
				PrecisionRenderer renderer = precisionRenderersByColumnName.get(key);
				renderer.numberFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
			}
			defaultDoubleRenderer.getFormat().setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
		} catch (Exception e) {
		}

		// code added by D Brown to maintain column order and widths (Mar 2014)
		TableColumnModel model = this.getColumnModel();
		int colCount = model.getColumnCount();
		int[] modelIndexes = new int[colCount];
		int[] columnWidths = new int[colCount];
		ArrayList<Object> columnNames = new ArrayList<Object>();
		// save current order, widths and column names
		for (int i = 0; i < colCount; i++) {
			TableColumn column = model.getColumn(i);
			modelIndexes[i] = column.getModelIndex();
			columnWidths[i] = column.getWidth();
			columnNames.add(column.getHeaderValue());
		}
		// refresh table--this lays out columns in default order and widths
		tableChanged(new TableModelEvent(dataTableModel, TableModelEvent.HEADER_ROW));

		// deal with added and/or removed columns
		int newCount = model.getColumnCount();
		// create list of new column names
		ArrayList<Object> newColumnNames = new ArrayList<Object>();
		for (int i = 0; i < newCount; i++) {
			TableColumn column = model.getColumn(i);
			newColumnNames.add(column.getHeaderValue());
		}
		// determine which column(s) were removed
		TreeSet<Integer> removedIndexes = new TreeSet<Integer>();
		for (int i = 0; i < colCount; i++) {
			if (!newColumnNames.contains(columnNames.get(i))) {
				removedIndexes.add(modelIndexes[i]);
			}
		}
		// determine which column(s) were added
		TreeSet<Integer> addedIndexes = new TreeSet<Integer>();
		for (int i = 0; i < newCount; i++) {
			if (!columnNames.contains(newColumnNames.get(i))) {
				addedIndexes.add(i);
			}
		}
		// rebuild modelIndex and columnWidth arrays
		while (!removedIndexes.isEmpty()) {
			int n = removedIndexes.last();
			removedIndexes.remove(n);
			int[] newModelIndexes = new int[colCount - 1];
			int[] newColumnWidths = new int[colCount - 1];
			int k = 0;
			for (int i = 0; i < colCount; i++) {
				if (modelIndexes[i] == n)
					continue;
				if (modelIndexes[i] > n) {
					newModelIndexes[k] = modelIndexes[i] - 1;
				} else {
					newModelIndexes[k] = modelIndexes[i];
				}
				newColumnWidths[k] = columnWidths[i];
				k++;
			}
			modelIndexes = newModelIndexes;
			columnWidths = newColumnWidths;
			colCount = modelIndexes.length;
		}
		while (!addedIndexes.isEmpty()) {
			int n = addedIndexes.first();
			addedIndexes.remove(n);
			int[] newModelIndexes = new int[colCount + 1];
			int[] newColumnWidths = new int[colCount + 1];
			for (int i = 0; i < colCount; i++) {
				if (modelIndexes[i] >= n) {
					newModelIndexes[i] = modelIndexes[i] + 1;
				} else {
					newModelIndexes[i] = modelIndexes[i];
				}
				newColumnWidths[i] = columnWidths[i];
			}
			// add new columns at end and assign them the default width
			newModelIndexes[colCount] = n;
			newColumnWidths[colCount] = model.getColumn(n).getWidth();
			modelIndexes = newModelIndexes;
			columnWidths = newColumnWidths;
			colCount = modelIndexes.length;
		}
		// restore column order
		outer: for (int targetIndex = 0; targetIndex < colCount; targetIndex++) {
			// find column with modelIndex and move to targetIndex
			for (int i = 0; i < colCount; i++) {
				if (model.getColumn(i).getModelIndex() == modelIndexes[targetIndex]) {
					model.moveColumn(i, targetIndex);
					continue outer;
				}
			}
		}
		// restore column widths
		for (int i = 0; i < columnWidths.length; i++) {
			model.getColumn(i).setPreferredWidth(columnWidths[i]);
			model.getColumn(i).setWidth(columnWidths[i]);
		}
	}

	/**
	 * Add a TableModel object to the table model list.
	 *
	 * @param tableModel
	 */
	//public void add(OSPTableModel tableModel) {
	public void add(TableModel tableModel) {
		dataTableModel.add(tableModel);
	}

	/**
	 * Remove a TableModel object from the table model list.
	 *
	 * @param tableModel
	 */
	public void remove(TableModel tableModel) {
		dataTableModel.remove(tableModel);
	}

	/**
	 * Remove all TableModels from the table model list.
	 */
	public void clear() {
		dataTableModel.clear();
	}

	/**
	 * A wrapper for a TableModel
	 *
	 */
	private static class DataTableElement {
		TableModel tableModel;
		boolean columnVisibilities[]; // boolean values indicating if a column is visible
		int stride = 1; // data stride in the DataTable view

		/**
		 * Constructor DataTableElement
		 *
		 * @param t
		 */
		public DataTableElement(TableModel t) {
			tableModel = t;
		}

		/**
		 * Method setStride
		 *
		 * @param _stride
		 */
		public void setStride(int _stride) {
			stride = _stride;
		}

		/**
		 * Method setColumnVisible
		 *
		 * @param columnIndex
		 * @param visible
		 */
		public void setColumnVisible(int columnIndex, boolean visible) {
			ensureCapacity(columnIndex + 1);
			columnVisibilities[columnIndex] = visible;
		}

		/**
		 * Method getStride
		 *
		 * @return
		 */
		public int getStride() {
			return stride;
		}

		/**
		 * Method getColumnVisibilities
		 *
		 * @return
		 */
		public boolean[] getColumnVisibilities() {
			return columnVisibilities;
		}

		/**
		 * Method getColumnCount
		 *
		 * @return
		 */
		public int getColumnCount() {
			int count = 0;
			int numberOfColumns = tableModel.getColumnCount();
			ensureCapacity(numberOfColumns);
			for (int i = 0; i < numberOfColumns; i++) {
				boolean visible = columnVisibilities[i];
				if (visible) {
					count++;
				}
			}
			return count;
		}

		/**
		 * Method getValueAt
		 *
		 * @param rowIndex
		 * @param columnIndex
		 * @return
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			return tableModel.getValueAt(rowIndex, columnIndex);
		}

		/**
		 * Method getColumnName
		 *
		 * @param columnIndex
		 * @return
		 */
		public String getColumnName(int columnIndex) {
			return tableModel.getColumnName(columnIndex);
		}

		/**
		 * Method getColumnClass
		 *
		 * @param columnIndex
		 * @return
		 */
		public Class<?> getColumnClass(int columnIndex) {
			return tableModel.getColumnClass(columnIndex);
		}

		/**
		 * Method getRowCount
		 *
		 * @return
		 */
		public int getRowCount() {
			return tableModel.getRowCount();
		}

		private void ensureCapacity(int minimumCapacity) {
			if (columnVisibilities == null) {
				columnVisibilities = new boolean[(minimumCapacity * 3) / 2 + 1];
				Arrays.fill(columnVisibilities, true);
			} else if (columnVisibilities.length < minimumCapacity) {
				boolean[] temp = columnVisibilities;
				columnVisibilities = new boolean[(minimumCapacity * 3) / 2 + 1];
				System.arraycopy(temp, 0, columnVisibilities, 0, temp.length);
				Arrays.fill(columnVisibilities, temp.length, columnVisibilities.length, true);
			}
		}

	}

	/*
	 * DefaultDataTableModel acts on behalf of the TableModels that the DataTable
	 * contains. It combines data from these multiple sources and allows the
	 * DataTable to display data is if the data were from a single source.
	 *
	 * @author jgould
	 * 
	 * @created February 21, 2002
	 */
	protected class OSPDataTableModel extends DefaultTableModel {// BH trying this implements DataTableModel {


		ArrayList<DataTableElement> dataTableElements;
		boolean rowNumberVisible;
		private SortDecorator decorator;

		public OSPDataTableModel() {
			dataTableElements = new ArrayList<DataTableElement>();
			decorator = new SortDecorator();
			addTableModelListener(decorator);
		}

		/**
		 * Sorts the table using the given column.
		 * 
		 * @param col int
		 */
		public void sort(int col) {
			decorator.sort(col);
		}

		/**
		 * Gets the sorted column. Added by D Brown 2010-10-24.
		 * 
		 * @return the
		 */
		public int getSortedColumn() {
			return decorator.getSortedColumn();
		}

		public void resetSort() {
			decorator.reset();
		}

		public int getSortedRow(int j) {
			return decorator.getSortedRow(j);
		}

		/**
		 * Method setColumnVisible
		 *
		 * @param tableModel
		 * @param columnIndex
		 * @param b
		 */
//		@Override
		public void setColumnVisible(TableModel tableModel, int columnIndex, boolean b) {
			DataTableElement dte = findElementContaining(tableModel);
			dte.setColumnVisible(columnIndex, b);
		}

		/**
		 * Method setStride
		 *
		 * @param tableModel
		 * @param stride
		 */
//		@Override
		public void setStride(TableModel tableModel, int stride) {
			DataTableElement dte = findElementContaining(tableModel);
			dte.setStride(stride);
		}

		/**
		 * Method setRowNumberVisible
		 *
		 * @param _rowNumberVisible
		 */
//		@Override
		public void setRowNumberVisible(boolean _rowNumberVisible) {
			rowNumberVisible = _rowNumberVisible;
		}

		/**
		 * Method setValueAt modified by Doug Brown 12/19/2013
		 *
		 * @param value
		 * @param rowIndex
		 * @param columnIndex
		 */
		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			decorator.setValueAt(value, rowIndex, columnIndex);
		}

		protected void setElementValue(Object value, int rowIndex, int columnIndex) {
			if (dataTableElements.size() == 0) {
				return;
			}
			if (rowNumberVisible) {
				if (columnIndex == 0) {
					return;
				}
			}
			ModelFilterResult mfr = ModelFilterResult.find(rowNumberVisible, dataTableElements, columnIndex);
			DataTableElement dte = mfr.tableElement;
			int stride = dte.getStride();
			rowIndex = rowIndex * stride;
			if (rowIndex >= dte.getRowCount()) {
				return;
			}
			dte.tableModel.setValueAt(value, rowIndex, mfr.column);
		}

		/**
		 * Method isRowNumberVisible
		 *
		 * @return
		 */
//		@Override
		public boolean isRowNumberVisible() {
			return rowNumberVisible;
		}

		/**
		 * Method getColumnName
		 *
		 * @param columnIndex
		 * @return the name
		 */
		@Override
		public String getColumnName(int columnIndex) {

			if (columnIndex >= getColumnCount()) {
				return "unknown"; //$NON-NLS-1$
			}

			if ((dataTableElements.size() == 0) && !rowNumberVisible) {
				return null;
			}
			if (rowNumberVisible) {
				if (columnIndex == 0) {
					return rowName;
				}
			}
			ModelFilterResult mfr = ModelFilterResult.find(rowNumberVisible, dataTableElements, columnIndex);
			DataTableElement dte = mfr.tableElement;
			String name = dte.getColumnName(mfr.column);
			return name;
		}

		/**
		 * Method getRowCount
		 *
		 * @return
		 */
		@Override
		public int getRowCount() {
			int rowCount = 0;
			if (dataTableElements != null)
				for (int i = 0; i < dataTableElements.size(); i++) {
					DataTableElement dte = dataTableElements.get(i);
					int stride = dte.getStride();
					rowCount = Math.max(rowCount, (dte.getRowCount() + stride - 1) / stride);
				}
			return rowCount;
		}

		/**
		 * Method getColumnCount
		 *
		 * @return
		 */
		@Override
		public int getColumnCount() {
			int columnCount = 0;
			if (dataTableElements != null)
				for (int i = 0; i < dataTableElements.size(); i++) {
					DataTableElement dte = dataTableElements.get(i);
					columnCount += dte.getColumnCount();
				}
			if (rowNumberVisible) {
				columnCount++;
			}
			return columnCount;
		}

		/**
		 * Method getValueAt
		 *
		 * @param rowIndex
		 * @param columnIndex
		 * @return
		 */
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return decorator.getValueAt(rowIndex, columnIndex);
		}

		public Object getElementValue(int rowIndex, int columnIndex) {
			if (dataTableElements.size() == 0) {
				return null;
			}
			if (rowNumberVisible) {
				if (columnIndex == 0) {
					return Integer.valueOf(rowIndex);
				}
			}
			ModelFilterResult mfr = ModelFilterResult.find(rowNumberVisible, dataTableElements, columnIndex);
			DataTableElement dte = mfr.tableElement;
			int stride = dte.getStride();
			rowIndex = rowIndex * stride;
			if (rowIndex >= dte.getRowCount()) {
				return null;
			}
			return dte.getValueAt(rowIndex, mfr.column);
		}

		/**
		 * Method getColumnClass
		 *
		 * @param columnIndex
		 * @return
		 */
		@Override
		public Class<?> getColumnClass(int columnIndex) {

			if (columnIndex >= getColumnCount()) {
				return Object.class;
			}

			if (rowNumberVisible) {
				if (columnIndex == 0) {
					return Integer.class;
				}
			}
			if ((columnIndex == 0) && rowNumberVisible) {
				columnIndex--;
			}
			ModelFilterResult mfr = ModelFilterResult.find(rowNumberVisible, dataTableElements, columnIndex);
			DataTableElement dte = mfr.tableElement;
			return dte.getColumnClass(mfr.column);
		}

		/**
		 * Method isCellEditable
		 *
		 * @param rowIndex
		 * @param columnIndex
		 * @return true if editable
		 */
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return (columnIndex < getColumnCount() && isElementEditable(rowIndex, columnIndex));
		}


		public boolean isElementEditable(int row, int col) {
			return false;
		}

		/**
		 * Method remove
		 *
		 * @param tableModel
		 */
//		@Override
		public void remove(TableModel tableModel) {
			DataTableElement dte = findElementContaining(tableModel);
			dataTableElements.remove(dte);
		}

		/**
		 * Method clear
		 */
//		@Override
		public void clear() {
			dataTableElements.clear();
		}

		/**
		 * Method add
		 *
		 * @param tableModel
		 */
//		@Override
		public void add(TableModel tableModel) {
			dataTableElements.add(new DataTableElement(tableModel));
		}

//		/**
//		 * Method addTableModelListener
//		 *
//		 * @param l
//		 */
//		@Override
//		public void addTableModelListener(TableModelListener l) {
//		}
//
//		/**
//		 * Method removeTableModelListener
//		 *
//		 * @param l
//		 */
//		@Override
//		public void removeTableModelListener(TableModelListener l) {
//		}

		/**
		 * returns the DataTableElement that contains the specified TableModel
		 *
		 * @param tableModel
		 * @return Description of the Returned Value
		 */
		private DataTableElement findElementContaining(TableModel tableModel) {
			for (int i = 0; i < dataTableElements.size(); i++) {
				DataTableElement dte = dataTableElements.get(i);
				if (dte.tableModel == tableModel) {
					return dte;
				}
			}
			return null;
		}

		protected class SortDecorator implements TableModelListener {
			private int indexes[];
			private int sortedColumn; // added by D Brown 2010-10-24

			/**
			 * Constructor SortDecorator
			 * 
			 * @param model
			 */
			public SortDecorator() {
			}

			/**
			 * Gets the sorted row number for a given real model row Added by D Brown Dec
			 * 2017
			 * 
			 * @param realModelRow the unsorted row number
			 * @return the sorted row number
			 */
			public int getSortedRow(int realModelRow) {
				for (int i = 0; i < indexes.length; i++) {
					if (indexes[i] == realModelRow)
						return i;
				}
				return -1;
			}

			protected Object getValueAt(int row, int column) {
				if (column >= getColumnCount()) {
					return null;
				}
				if (indexes.length <= row) {
					allocate();
				}
				return getElementValue(indexes[row], column);
			}

			public void setValueAt(Object aValue, int row, int column) {
				if (indexes.length <= row) {
					allocate();
				}
				setElementValue(aValue, indexes[row], column);
			}

			@Override
			public void tableChanged(TableModelEvent e) {
				allocate();
			}

			public void sort(int column) {
				sortedColumn = column;
				int rowCount = getRowCount();
				if (indexes.length <= rowCount) {
					allocate();
				}

				// new faster sort method added by D Brown 2015-05-16
				try {
					if (getColumnClass(column) == Double.class || getColumnClass(column) == Integer.class) {
						Double[][] sortArray = new Double[rowCount][2];
						if (getColumnClass(column) == Double.class) {
							for (int i = 0; i < rowCount; i++) {
								sortArray[i][0] = (Double) getElementValue(i, column);
								sortArray[i][1] = 1.0 * indexes[i];
							}
						} else {
							for (int i = 0; i < rowCount; i++) {
								sortArray[i][0] = ((Integer) getElementValue(i, column)).doubleValue();
								sortArray[i][1] = 1.0 * indexes[i];
							}
						}
						Arrays.sort(sortArray, new Comparator<Double[]>() {
							@Override
							public int compare(Double[] a, Double[] b) {
								if (a[0] == null || b[0] == null) {
									return b[0] == a[0] ? 0 : b[0] == null ? -1 : 1;
								}
								return (b[0] < a[0]) ? 1 : ((b[0] > a[0]) ? -1 : 0);
							}
						});
						for (int i = 0; i < rowCount; i++) {
							indexes[i] = sortArray[i][1].intValue();
						}
					} else { // use older sort method for String data
						for (int i = 0; i < rowCount; i++) {
							for (int j = i + 1; j < rowCount; j++) {
								if (compare(indexes[i], indexes[j], column) < 0) {
									swap(i, j);
								}
							}
						}
					}
				} catch (Exception e) {
				}
			}

			// added by D Brown 2010-10-24
			public int getSortedColumn() {
				return sortedColumn;
			}

			public void swap(int i, int j) {
				int tmp = indexes[i];
				indexes[i] = indexes[j];
				indexes[j] = tmp;
			}

			public int compare(int i, int j, int column) {
				Object io = getElementValue(i, column);
				Object jo = getElementValue(j, column);
				if ((io != null) && (jo == null)) {
					return 1;
				}
				if ((io == null) && (jo != null)) {
					return -1;
				}
				if ((io == null) && (jo == null)) {
					return 0;
				}
				if ((io instanceof Integer) && (jo instanceof Integer)) {
					int a = ((Integer) io).intValue();
					int b = ((Integer) jo).intValue();
					return (b < a) ? -1 : ((b > a) ? 1 : 0);
				} else if ((io instanceof Double) && (jo instanceof Double)) {
					double a = ((Double) io).doubleValue();
					double b = ((Double) jo).doubleValue();
					return (b < a) ? -1 : ((b > a) ? 1 : 0);
				} else if ((io instanceof Integer) && (jo instanceof Double)) {
					int a = ((Integer) io).intValue();
					double b = ((Double) jo).doubleValue();
					return (b < a) ? -1 : ((b > a) ? 1 : 0);
				} else if ((io instanceof Double) && (jo instanceof Integer)) {
					double a = ((Double) io).doubleValue();
					int b = ((Integer) jo).intValue();
					return (b < a) ? -1 : ((b > a) ? 1 : 0);
				}
				int c = jo.toString().compareTo(io.toString());
				return (c < 0) ? -1 : ((c > 0) ? 1 : 0);
			}

			private void allocate() {
				indexes = new int[getRowCount()];
				for (int i = 0; i < indexes.length; ++i) {
					indexes[i] = i;
				}
			}

			public void reset() {
				allocate();
				sortedColumn = -1;
			}

		}

	}

	private static class ModelFilterResult {
		DataTableElement tableElement;
		int column;

		/**
		 * Constructor ModelFilterResult
		 *
		 * @param _tableElement
		 * @param _column
		 */
		public ModelFilterResult(DataTableElement _tableElement, int _column) {
			tableElement = _tableElement;
			column = _column;
		}

		/**
		 * Method find
		 *
		 * @param rowNumberVisible
		 * @param dataTableElements
		 * @param tableColumnIndex
		 * @return
		 */
		public static ModelFilterResult find(boolean rowNumberVisible, ArrayList<DataTableElement> dataTableElements,
				int tableColumnIndex) {
			if (rowNumberVisible) {
				tableColumnIndex--;
			}
			int totalColumns = 0;
			for (int i = 0; i < dataTableElements.size(); i++) {
				DataTableElement dte = dataTableElements.get(i);
				dte.ensureCapacity(tableColumnIndex);
				int columnCount = dte.getColumnCount();
				totalColumns += columnCount;
				if (totalColumns > tableColumnIndex) {
					// int columnIndex = Math.abs(totalColumns - columnCount - tableColumnIndex);
					int columnIndex = (columnCount + tableColumnIndex) - totalColumns;
					boolean visible[] = dte.getColumnVisibilities();
					for (int j = 0; j < tableColumnIndex; j++) {
						if (!visible[j]) {
							columnIndex++;
						}
					}
					return new ModelFilterResult(dte, columnIndex);
				}
			}
			return null; // this shouldn't happen
		}

	}

	private class DataTableColumnModel extends DefaultTableColumnModel {
		/**
		 * Method getColumn
		 *
		 * @param columnIndex
		 * @return
		 */
		@Override
		public TableColumn getColumn(int columnIndex) {
			TableColumn tableColumn;
			try {
				tableColumn = super.getColumn(columnIndex);
			} catch (Exception ex) { // return an empty column if the columnIndex is not valid.
				return new TableColumn();
			}
			String headerValue = (String) tableColumn.getHeaderValue();
			if (headerValue == null) {
				return tableColumn;
			} else if (headerValue.equals(rowName) && (tableColumn.getModelIndex() == 0)) {
				tableColumn.setMaxWidth(labelColumnWidth);
				tableColumn.setMinWidth(labelColumnWidth);
				tableColumn.setResizable(false);
			} else {
				tableColumn.setMinWidth(minimumDataColumnWidth);
			}
			return tableColumn;
		}

	}

	/**
	 * A default double renderer for the table
	 */
	protected static class DoubleRenderer extends DefaultTableCellRenderer {
		NumberField numberField;

		/**
		 * Constructor
		 */
		public DoubleRenderer() {
			super();
			numberField = new NumberField(0);
			setHorizontalAlignment(SwingConstants.RIGHT);
			setBackground(Color.WHITE);
		}

		@Override
		public void setValue(Object value) {
			if (value == null) {
				setText(""); //$NON-NLS-1$
				return;
			}
			numberField.setValue((Double) value);
			setText(numberField.getText());
		}

		/**
		 * Gets the number format
		 */
		DecimalFormat getFormat() {
			return numberField.getFormat();
		}

	}

	/**
	 * A settable precision double renderer for the table
	 */
	protected static class PrecisionRenderer extends DefaultTableCellRenderer {
		DecimalFormat numberFormat;
		String pattern;

		/**
		 * PrecisionRenderer constructor
		 *
		 * @param precision - maximum number of fraction digits to display
		 */
		public PrecisionRenderer(int precision) {
			super();
			numberFormat = (DecimalFormat) NumberFormat.getInstance();
			numberFormat.setMaximumFractionDigits(precision);
			setHorizontalAlignment(SwingConstants.RIGHT);
			setBackground(Color.WHITE);
		}

		/**
		 * PrecisionRenderer constructor
		 *
		 * @param pattern a formatting pattern
		 */
		public PrecisionRenderer(String pattern) {
			super();
			numberFormat = (DecimalFormat) NumberFormat.getInstance();
			numberFormat.applyPattern(pattern);
			this.pattern = pattern;
			setHorizontalAlignment(SwingConstants.RIGHT);
		}

		/**
		 * Sets the maximum number of fraction digits to display
		 *
		 * @param precision - maximum number of fraction digits to display
		 */
		public void setPrecision(int precision) {
			numberFormat.setMaximumFractionDigits(precision);
		}

		@Override
		public void setValue(Object value) {
			setText((value == null) ? "" : numberFormat.format(value)); //$NON-NLS-1$
		}

	}

	protected static class RowNumberRenderer extends JLabel implements TableCellRenderer {
		JTable table;

		/**
		 * RowNumberRenderer constructor
		 *
		 * @param _table Description of Parameter
		 */
		public RowNumberRenderer(JTable _table) {
			super();
			table = _table;
			setHorizontalAlignment(SwingConstants.RIGHT);
			setOpaque(true); // make background visible.
			setForeground(Color.black);
			setBackground(PANEL_BACKGROUND);
		}

		/**
		 * returns a JLabel that is highlighted if the row is selected.
		 *
		 * @param table
		 * @param value
		 * @param isSelected
		 * @param hasFocus
		 * @param row
		 * @param column
		 * @return
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (table.isRowSelected(row)) {
				int[] i = table.getSelectedColumns();
				if ((i.length == 1) && (table.convertColumnIndexToModel(i[0]) == 0)) {
					setBackground(PANEL_BACKGROUND);
				} else {
					setBackground(Color.gray);
				}
			} else {
				setBackground(PANEL_BACKGROUND);
			}
			setText(value.toString());
			return this;
		}

	}

	/**
	 * A cell renderer that adds units to displayed values. Added by D Brown Dec
	 * 2010
	 */
	protected static class UnitRenderer implements TableCellRenderer {
		TableCellRenderer baseRenderer;
		String units;
		String tooltip;

		/**
		 * UnitRenderer constructor
		 *
		 * @param renderer a TableCellRenderer
		 * @param factor   a conversion factor
		 */
		public UnitRenderer(TableCellRenderer renderer, String units, String tooltip) {
			super();
			this.units = units;
			this.tooltip = tooltip;
			setBaseRenderer(renderer);
		}

		/**
		 * Sets the base renderer.
		 * 
		 * @param renderer the base renderer
		 */
		public void setBaseRenderer(TableCellRenderer renderer) {
			this.baseRenderer = renderer;
		}

		/**
		 * Returns the rendered component.
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = baseRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (c instanceof JLabel && units != null) {
				JLabel label = (JLabel) c;
				if (label.getText() != null && !label.getText().equals("")) //$NON-NLS-1$
					label.setText(label.getText() + units);
				label.setToolTipText(tooltip);
			}
			return c;
		}

	}

	public class NumberFormatDialog extends JDialog {
		JButton closeButton, cancelButton, helpButton, applyButton;
		JLabel patternLabel, sampleLabel;
		JTextField patternField, sampleField;
		java.text.DecimalFormat sampleFormat;
		String[] displayedNames;
		Map<String, String> realNames = new HashMap<String, String>();
		Map<String, String> prevPatterns = new HashMap<String, String>();
		JList<String> columnList;
		JScrollPane columnScroller;

		protected NumberFormatDialog() {
			super(JOptionPane.getFrameForComponent(DataTable.this), true);
			setLayout(new BorderLayout());
			setTitle(DisplayRes.getString("DataTable.NumberFormat.Dialog.Title")); //$NON-NLS-1$
			// create sample format
			sampleFormat = (java.text.DecimalFormat) java.text.NumberFormat.getNumberInstance();
			// create buttons
			closeButton = new JButton(DisplayRes.getString("Dialog.Button.Close.Text")); //$NON-NLS-1$
			closeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
			applyButton = new JButton(DisplayRes.getString("Dialog.Button.Apply.Text")); //$NON-NLS-1$
			applyButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					patternField.getAction().actionPerformed(e);
				}
			});
			cancelButton = new JButton(DisplayRes.getString("GUIUtils.Cancel")); //$NON-NLS-1$
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for (String displayedName : displayedNames) {
						String name = realNames.get(displayedName);
						setFormatPattern(name, prevPatterns.get(name));
					}
					refreshTableNow(MODE_CANCEL);
					setVisible(false);
				}
			});
			helpButton = new JButton(DisplayRes.getString("GUIUtils.Help")); //$NON-NLS-1$
			helpButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String tab = "      "; //$NON-NLS-1$
					String nl = System.getProperty("line.separator", "/n"); //$NON-NLS-1$ //$NON-NLS-2$
					JOptionPane.showMessageDialog(formatDialog,
							DisplayRes.getString("DataTable.NumberFormat.Help.Message1") + nl + nl + //$NON-NLS-1$
					tab + DisplayRes.getString("DataTable.NumberFormat.Help.Message2") + nl + //$NON-NLS-1$
					tab + DisplayRes.getString("DataTable.NumberFormat.Help.Message3") + nl + //$NON-NLS-1$
					tab + DisplayRes.getString("DataTable.NumberFormat.Help.Message4") + nl + //$NON-NLS-1$
					tab + DisplayRes.getString("DataTable.NumberFormat.Help.Message5") + nl + nl + //$NON-NLS-1$
					DisplayRes.getString("DataTable.NumberFormat.Help.Message6") + " PI.", //$NON-NLS-1$ //$NON-NLS-2$
							DisplayRes.getString("DataTable.NumberFormat.Help.Title"), //$NON-NLS-1$
							JOptionPane.INFORMATION_MESSAGE);
				}

			});
			// create labels and text fields
			patternLabel = new JLabel(DisplayRes.getString("DataTable.NumberFormat.Dialog.Label.Format")); //$NON-NLS-1$
			sampleLabel = new JLabel(DisplayRes.getString("DataTable.NumberFormat.Dialog.Label.Sample")); //$NON-NLS-1$
			patternField = new JTextField(6);
			patternField.setAction(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String pattern = patternField.getText();
					if (pattern.indexOf(NO_PATTERN) > -1)
						pattern = ""; //$NON-NLS-1$
					// substitute 0 for other digits
					for (int i = 1; i < 10; i++) {
						pattern = pattern.replaceAll(String.valueOf(i), "0"); //$NON-NLS-1$
					}
					int i = pattern.indexOf("0e0"); //$NON-NLS-1$
					if (i > -1) {
						pattern = pattern.substring(0, i) + "0E0" + pattern.substring(i + 3); //$NON-NLS-1$
					}
					try {
						showNumberFormatAndSample(pattern);
						// apply pattern to all selected columns
						java.util.List<String> selectedColumns = columnList.getSelectedValuesList();
						for (Object displayedName : selectedColumns) {
							String name = realNames.get(displayedName.toString());
							setFormatPattern(name, pattern);
						}
						refreshTableNow(MODE_PATTERN);
					} catch (RuntimeException ex) {
						patternField.setBackground(new Color(255, 153, 153));
						patternField.setText(pattern);
						return;
					}
				}

			});
			patternField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						patternField.setBackground(Color.white);
					} else {
						patternField.setBackground(Color.yellow);
						// refresh sample format after text changes
						Runnable runner = new Runnable() {
							@Override
							public void run() {
								String pattern = patternField.getText();
								if (pattern.indexOf(NO_PATTERN) > -1)
									pattern = ""; //$NON-NLS-1$
								// substitute 0 for other digits
								for (int i = 1; i < 10; i++) {
									pattern = pattern.replaceAll(String.valueOf(i), "0"); //$NON-NLS-1$
								}
								int i = pattern.indexOf("0e0"); //$NON-NLS-1$
								if (i > -1) {
									pattern = pattern.substring(0, i) + "0E0" + pattern.substring(i + 3); //$NON-NLS-1$
								}
								if (pattern.equals("") || pattern.equals(NO_PATTERN)) { //$NON-NLS-1$
									TableCellRenderer renderer = DataTable.this.getDefaultRenderer(Double.class);
									Component c = renderer.getTableCellRendererComponent(DataTable.this, Math.PI, false,
											false, 0, 0);
									if (c instanceof JLabel) {
										String text = ((JLabel) c).getText();
										sampleField.setText(text);
									}
								} else {
									try {
										sampleFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
										sampleFormat.applyPattern(pattern);
										sampleField.setText(sampleFormat.format(Math.PI));
									} catch (Exception e) {
									}
								}
							}
						};
						SwingUtilities.invokeLater(runner);
					}
				}

			});
			patternField.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					patternField.setBackground(Color.white);
					patternField.getAction().actionPerformed(null);
				}

			});
			sampleField = new JTextField(6);
			sampleField.setEditable(false);
			// column scroller (list is instantiated in setColumns() method)
			columnScroller = new JScrollPane();
			columnScroller.setPreferredSize(new Dimension(160, 120));
			// assemble dialog
			JPanel formatPanel = new JPanel(new GridLayout());
			JPanel patternPanel = new JPanel();
			patternPanel.add(patternLabel);
			patternPanel.add(patternField);
			formatPanel.add(patternPanel);
			JPanel samplePanel = new JPanel();
			samplePanel.add(sampleLabel);
			samplePanel.add(sampleField);
			formatPanel.add(samplePanel);
			add(formatPanel, BorderLayout.NORTH);
			JPanel columnPanel = new JPanel(new BorderLayout());
			columnPanel.setBorder(
					BorderFactory.createTitledBorder(DisplayRes.getString("DataTable.FormatDialog.ApplyTo.Title"))); //$NON-NLS-1$
			columnPanel.add(columnScroller, BorderLayout.CENTER);
			add(columnPanel, BorderLayout.CENTER);
			JPanel buttonPanel = new JPanel();
			buttonPanel.add(helpButton);
			buttonPanel.add(applyButton);
			buttonPanel.add(closeButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel, BorderLayout.SOUTH);
			pack();
		}

		private void showNumberFormatAndSample(int[] selectedIndices) {
			if (selectedIndices == null || selectedIndices.length == 0) {
				showNumberFormatAndSample(""); //$NON-NLS-1$
			} else if (selectedIndices.length == 1) {
				String name = realNames.get(displayedNames[selectedIndices[0]]);
				String pattern = getFormatPattern(name);
				showNumberFormatAndSample(pattern);
			} else {
				// do all selected indices have same pattern?
				String name = realNames.get(displayedNames[selectedIndices[0]]);
				String pattern = getFormatPattern(name);
				for (int i = 1; i < selectedIndices.length; i++) {
					name = realNames.get(displayedNames[selectedIndices[i]]);
					if (!pattern.equals(getFormatPattern(name))) {
						pattern = null;
						break;
					}
				}
				showNumberFormatAndSample(pattern);
			}

		}

		private void showNumberFormatAndSample(String pattern) {
			if (pattern == null) {
				sampleField.setText(""); //$NON-NLS-1$
				patternField.setText(""); //$NON-NLS-1$
				return;
			}
			if (pattern.equals("") || pattern.equals(NO_PATTERN)) { //$NON-NLS-1$
				TableCellRenderer renderer = DataTable.this.getDefaultRenderer(Double.class);
				Component c = renderer.getTableCellRendererComponent(DataTable.this, Math.PI, false, false, 0, 0);
				if (c instanceof JLabel) {
					String text = ((JLabel) c).getText();
					sampleField.setText(text);
				}
				patternField.setText(NO_PATTERN);
			} else {
				sampleFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
				sampleFormat.applyPattern(pattern);
				sampleField.setText(sampleFormat.format(Math.PI));
				patternField.setText(pattern);
			}
		}

		void setColumns(String[] names, String[] selected) {
			displayedNames = new String[names.length];
			realNames.clear();
			for (int i = 0; i < names.length; i++) {
				String s = TeXParser.removeSubscripting(names[i]);
				// add white space for better look
				displayedNames[i] = "   " + s + " "; //$NON-NLS-1$ //$NON-NLS-2$
				realNames.put(displayedNames[i], names[i]);
				if (selected != null) {
					for (int j = 0; j < selected.length; j++) {
						if (selected[j] != null && selected[j].equals(names[i])) {
							selected[j] = displayedNames[i];
						}
					}
				}
			}
			prevPatterns.clear();
			for (String name : names) {
				prevPatterns.put(name, getFormatPattern(name));
			}
			// create column list and add to scroller
			columnList = new JList<String>(displayedNames);
			columnList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
			columnList.setVisibleRowCount(-1);
			columnList.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					showNumberFormatAndSample(columnList.getSelectedIndices());
				}
			});
			columnScroller.setViewportView(columnList);
			pack();
			int[] indices = null;
			if (selected != null) {
				// select requested names
				indices = new int[selected.length];
				for (int j = 0; j < indices.length; j++) {
					inner: for (int i = 0; i < displayedNames.length; i++) {
						if (displayedNames[i].equals(selected[j])) {
							indices[j] = i;
							break inner;
						}
					}
				}
				columnList.setSelectedIndices(indices);
			} else
				showNumberFormatAndSample(indices);
		}

	}

	/**
	 * A header cell renderer that identifies sorted columns. Added by D Brown
	 * 2010-10-24
	 */
	public class HeaderRenderer implements TableCellRenderer {
		DrawingPanel panel = new DrawingPanel();
		TableCellRenderer renderer;
		protected DrawableTextLine textLine = new DrawableTextLine("", 0, -6); //$NON-NLS-1$

		/**
		 * Constructor HeaderRenderer
		 * 
		 * @param renderer
		 */
		public HeaderRenderer(TableCellRenderer renderer) {
			this.renderer = renderer;
			textLine.setJustification(TextLine.CENTER);
			panel.addDrawable(textLine);
		}

		public TableCellRenderer getBaseRenderer() {
			return renderer;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			// value is column name
			String name = (value == null) ? "" : value.toString(); //$NON-NLS-1$
			textLine.setText(name);
			if (OSPRuntime.isMac()) {
				name = TeXParser.removeSubscripting(name);
			}
			Component c = renderer.getTableCellRendererComponent(table, name, isSelected, hasFocus, row, col);
			if (!(c instanceof JComponent)) {
				return c;
			}
			JComponent comp = (JComponent) c;
			int sortCol = dataTableModel.getSortedColumn();
			Font font = comp.getFont();
			if (OSPRuntime.isMac()) {
				// textline doesn't work on OSX
				comp.setFont((sortCol != convertColumnIndexToModel(col)) ? font.deriveFont(Font.PLAIN)
						: font.deriveFont(Font.BOLD));
				if (comp instanceof JLabel) {
					((JLabel) comp).setHorizontalAlignment(SwingConstants.CENTER);
				}
				return comp;
			}
			java.awt.Dimension dim = comp.getPreferredSize();
			dim.height += 1;
			panel.setPreferredSize(dim);
			javax.swing.border.Border border = comp.getBorder();
			if (border instanceof javax.swing.border.EmptyBorder) {
				border = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
			}
			panel.setBorder(border);
			// set font: bold if sorted column
			textLine.setFont((sortCol != convertColumnIndexToModel(col)) ? font : font.deriveFont(Font.BOLD));
			textLine.setColor(comp.getForeground());
			textLine.setBackground(comp.getBackground());
			panel.setBackground(comp.getBackground());
			return panel;
		}

	}

	public void sort(int col) {
		dataTableModel.sort(col);
	}

	public int getSortedRow(int i) {
		return dataTableModel.getSortedRow(i);
	}

	public void resetSort() {
		dataTableModel.resetSort();
	}

}

/*
 * Open Source Physics software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 *
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be
 * released under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston MA 02111-1307 USA or view the license online at
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
