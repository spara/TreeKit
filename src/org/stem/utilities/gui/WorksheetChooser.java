package org.stem.utilities.gui;

import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;
import org.stem.utilities.GetSpreadsheetData;
import org.stem.utilities.loadToPostGIS;
import org.stem.utilities.makeShapeFile;
import org.stem.utilities.parseCSV;


public class WorksheetChooser extends JFrame {

	private static final long serialVersionUID = -1052316958482747782L;

	/** The Google Spreadsheets GData service. */
	private SpreadsheetService service;
	private FeedURLFactory factory;
	private List<SpreadsheetEntry> spreadsheetEntries;
	private JList spreadsheetListBox;
	private List<WorksheetEntry> worksheetEntries;
	private JList worksheetListBox;
	private JButton loadPostGISButton;
	private JButton makeShapefileButton;
	private JButton cancelButton;
	private Container panel;

	/**Starts the selection off with a spreadsheet feed helper. */
	public WorksheetChooser(SpreadsheetService spreadsheetService) {
		service = spreadsheetService;
		factory = FeedURLFactory.getDefault();
		initializeGui();
	}

	/**
	 * Gets the list of spreadsheets, and fills the list box.
	 */
	private void populateSpreadsheetList() {
		if (retrieveSpreadsheetList()) {
			fillSpreadsheetListBox();
		}
	}

	/**
	 * Asks Google Spreadsheets for a list of all the spreadsheets
	 * the user has access to.
	 * @return true if successful
	 */
	private boolean retrieveSpreadsheetList() {
		SpreadsheetFeed feed;
		try {
			feed = service.getFeed(
					factory.getSpreadsheetsFeedUrl(), SpreadsheetFeed.class);
		} catch (IOException e) {
			showErrorBox(e);
			return false;
		} catch (ServiceException e) {
			showErrorBox(e);
			return false;
		}

		this.spreadsheetEntries = feed.getEntries();
		return true;
	}

	/**
	 * Fills up the list-box of spreadsheets with the already-computed entries.
	 */
	private void fillSpreadsheetListBox() {
		String[] stringsForListbox = new String[spreadsheetEntries.size()];
		for (int i = 0; i < spreadsheetEntries.size(); i++) {
			SpreadsheetEntry entry = spreadsheetEntries.get(i);

			// Title of Spreadsheet (author, updated 2006-6-20 7:30PM)
			stringsForListbox[i] = entry.getTitle().getPlainText()
				+ "        " + entry.getUpdated().toUiString();;
		}
		spreadsheetListBox.setListData(stringsForListbox);
	}

	/**
	 * Gets the list of worksheets in the specified spreadsheet,
	 * and fills the list box.
	 * @param spreadsheet the selected spreadsheet
	 */
	private void populateWorksheetList(SpreadsheetEntry spreadsheet) {
		if (retrieveWorksheetList(spreadsheet)) {
			fillWorksheetListBox(spreadsheet.getTitle().getPlainText());
		}
	}

	/**
	 * Gets the list of worksheets from Google Spreadsheets.
	 * @param spreadsheet the spreadsheet to get a list of worksheets for
	 * @return true if successful
	 */
	private boolean retrieveWorksheetList(SpreadsheetEntry spreadsheet) {
		WorksheetFeed feed;
		try {
			feed = service.getFeed(
					spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
		} catch (IOException e) {
			showErrorBox(e);
			return false;
		} catch (ServiceException e) {
			showErrorBox(e);
			return false;
		}
		this.worksheetEntries = feed.getEntries();
		return true;
	}


	/**
	 * Fills up the list-box of worksheets with the already computed entries.
	 */
	private void fillWorksheetListBox(String spreadsheetTitle) {
		String[] stringsForListbox = new String[worksheetEntries.size()];

		for (int i = 0; i < worksheetEntries.size(); i++) {
			WorksheetEntry entry = worksheetEntries.get(i);
			stringsForListbox[i] = entry.getTitle().getPlainText();
		}
		worksheetListBox.setListData(stringsForListbox);
	}

	/**
	 * Handles when a user presses the "View Worksheets" button.
	 * @throws SchemaException 
	 * @throws TransformException 
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 *
	 */
	private void handleLoadPostGISButton() throws NoSuchAuthorityCodeException, FactoryException, TransformException, SchemaException {
		List<String> spreadsheetData;
		int selected = worksheetListBox.getSelectedIndex();
		if (selected < 0) {
			JFrame frame = new JFrame();
			JOptionPane.showMessageDialog(frame, "Please select a Worksheet");
		}
		if (worksheetEntries != null && selected >= 0) {
			try {
				spreadsheetData = GetSpreadsheetData.getSpreadsheetData(service, worksheetEntries.get(selected));
				loadToPostGIS.makeFeatureCollections(spreadsheetData);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ServiceException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Handles when a user presses a "View Cells Demo" button.
	 * @throws SchemaException 
	 * @throws TransformException 
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 */
	private void handleMakeShapefileButton() 
		throws NoSuchAuthorityCodeException, FactoryException, 
			TransformException, SchemaException {
 
		List<String> spreadsheetData;
		int selected = worksheetListBox.getSelectedIndex();
		/*
		if (selected < 0) {
			JFrame frame = new JFrame();
			JOptionPane.showMessageDialog(frame, "Please select a Worksheet");
		}
		*/
		//if (worksheetEntries != null && selected >= 0) {
			try {
				//spreadsheetData = GetSpreadsheetData.getSpreadsheetData(service, worksheetEntries.get(selected));
				spreadsheetData = parseCSV.parse();
				makeShapeFile.makeShapeFile(spreadsheetData);
			} catch (IOException e) {
				e.printStackTrace();
			}/* catch (ServiceException e) {
				e.printStackTrace();
			}*/
		//}
	}


	/**
	 * Shows the feed URL's as you select spreadsheets.
	 */
	private void handleSpreadsheetSelection() {
		int selected = spreadsheetListBox.getSelectedIndex();
     
		if (spreadsheetEntries != null && selected >= 0) {
			populateWorksheetList(spreadsheetEntries.get(selected));
		}
	}

	/**
	 * Shows the feed URL's as you select worksheets within a spreadsheets.
	 */
	private void handleWorksheetSelection() {
		int selected = worksheetListBox.getSelectedIndex();

		if (worksheetEntries != null && selected >= 0) {
			WorksheetEntry entry = worksheetEntries.get(selected);
		}
	}

	/**
	 * Shows a pop-up dialog box alerting the user of an error.
	 *
	 * @param e the exception
	 */
	public void showErrorBox(Exception e) {
		if (e instanceof IOException) {
			JOptionPane.showMessageDialog(null,
					"There was an error contacting Google: " + e.getMessage(),
					"Error contacting Google",
					JOptionPane.ERROR_MESSAGE);
		} else if (e instanceof AuthenticationException) {
			JOptionPane.showMessageDialog(null,
					"Your username and/or password were rejected: " + e.getMessage(),
					"Authentication Error",
					JOptionPane.ERROR_MESSAGE);
		} else if (e instanceof ServiceException) {
			JOptionPane.showMessageDialog(null,
					"Google returned the error: " + e.getMessage(),
					"Google had an error processing the request",
					JOptionPane.ERROR_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(null,
					"There was an unexpected error: " + e.getMessage(),
					"Unexpected error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

 // ---- GUI code from here on down ----------------------------------------

	private void initializeGui() {
		setTitle("Choose your Worksheet");

		panel = getContentPane();
		panel.setLayout(new FlowLayout());

		// Top part - choose a spreadsheet
		JPanel spreadsheetChooserPanel = new JPanel();
		spreadsheetChooserPanel.setLayout(new BorderLayout());

		spreadsheetListBox = new JList();
		Dimension dListBox = new Dimension(350,500);
		spreadsheetChooserPanel.setSize(dListBox);
		spreadsheetChooserPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(4,4,4,4),
				"Spreadsheets"));
		spreadsheetChooserPanel.add(new JScrollPane(spreadsheetListBox));
		spreadsheetListBox.addListSelectionListener(new ActionHandler());

		// choose a worksheet
		JPanel worksheetChooserPanel = new JPanel();
		worksheetChooserPanel.setLayout(new BorderLayout());
		worksheetListBox = new JList(
				new String[] { "[Please click on a Spreadsheet for a list]" });
		worksheetChooserPanel.setSize(dListBox);
		worksheetChooserPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(4,4,4,4), "Worksheets"));
		worksheetChooserPanel.add(new JScrollPane(worksheetListBox));
		worksheetListBox.addListSelectionListener(new ActionHandler());

		// load PostGIS file button
		JPanel buttonPanel = new JPanel();
		buttonPanel.setSize(100,30);
		loadPostGISButton = new JButton("Load to PostGIS");
		loadPostGISButton.addActionListener(new ActionHandler());
		buttonPanel.add(loadPostGISButton);
		
		// make shape file button
		buttonPanel.setSize(100,30);
		makeShapefileButton = new JButton("Make Shapefile");
		makeShapefileButton.addActionListener(new ActionHandler());
		buttonPanel.add(makeShapefileButton);
   
		// cancel button
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionHandler());   
		buttonPanel.add(cancelButton);

		// setup dialog box
		JSplitPane jSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				spreadsheetChooserPanel, worksheetChooserPanel);
		jSplitPane.setContinuousLayout(false);
		jSplitPane.setSize(600, 300);
		panel.add(jSplitPane);
		panel.add(buttonPanel);
     
		populateSpreadsheetList();

		pack();
		setSize(575, 250);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	private class ActionHandler
		implements ActionListener, ListSelectionListener {
		boolean alreadyDisposed = false;

		public void actionPerformed(ActionEvent ae) {
			if (ae.getSource() == loadPostGISButton) {
				try {
					handleLoadPostGISButton();
				} catch (NoSuchAuthorityCodeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FactoryException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TransformException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SchemaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (ae.getSource() == makeShapefileButton) {
				try {
					handleMakeShapefileButton();
				} catch (NoSuchAuthorityCodeException e) {
					e.printStackTrace();
				} catch (FactoryException e) {
					e.printStackTrace();
				} catch (TransformException e) {
					e.printStackTrace();
				} catch (SchemaException e) {
					e.printStackTrace();
				}
			} else if (ae.getSource() == cancelButton) {
				if (!alreadyDisposed) {
					alreadyDisposed = true;
					dispose();
				} else { //make sure the program exits
					setDefaultCloseOperation(EXIT_ON_CLOSE);
					setVisible(false);
					dispose();
					System.exit(0);
				}
			}
		}

		public void valueChanged(ListSelectionEvent e) {
			if (e.getSource() == spreadsheetListBox) {
				handleSpreadsheetSelection();
			} else if (e.getSource() == worksheetListBox) {
				handleWorksheetSelection();
			}
		}  
	}
}

