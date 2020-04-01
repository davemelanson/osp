package test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTable;

import org.opensourcephysics.display.OSPFrame;
import org.opensourcephysics.tools.FontSizer;

public class FontResizeTest {

	Object[][] data = { { "Kathy", "Smith", "Snowboarding", new Integer(5), Boolean.FALSE },
			{ "John", "Doe", "Rowing", new Integer(3), Boolean.TRUE },
			{ "Sue", "Black", "Knitting", new Integer(2), Boolean.FALSE },
			{ "Jane", "White", "Speed reading", new Integer(20), Boolean.TRUE },
			{ "Joe", "Brown", "Pool", new Integer(10), Boolean.FALSE } };

	String[] columnNames = { "First Name", "Last Name", "Sport", "# of Years", "Vegetarian" };

	OSPFrame frame = new OSPFrame("Font Test");
	JMenuItem sizeUpItem;
	JMenuItem sizeDownItem;
	JMenuBar menuBar;
	JTable table;

	@SuppressWarnings("deprecation")
	FontResizeTest() {
	    menuBar = new JMenuBar();
		frame.getRootPane().setMenuBar(menuBar);
		// font menu
		JMenu fontMenu = new JMenu("Font Test"); //$NON-NLS-1$
		menuBar.add(fontMenu);
		JMenuItem sizeUpItem = new JMenuItem("Increase"); //$NON-NLS-1$
		sizeUpItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FontSizer.levelUp();
			}

		});
		fontMenu.add(sizeUpItem);

		table = new JTable(data, columnNames);

		FontSizer.setFonts(table, FontSizer.getLevel());
		FontSizer.addPropertyChangeListener("level", new PropertyChangeListener() { //$NON-NLS-1$
			public void propertyChange(PropertyChangeEvent e) {
				int level = ((Integer) e.getNewValue()).intValue();
				FontSizer.setFonts(menuBar, level);
				FontSizer.setFonts(table, level);
			}

		});

		frame.setContentPane(table);
		frame.setSize(500, 500);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		new FontResizeTest();

	}

}
