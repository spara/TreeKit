package org.stem.utilities;

import java.io.IOException;

import javax.swing.JOptionPane;

import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;
import org.stem.utilities.gui.LoginDialog;
import org.stem.utilities.gui.WorksheetChooser;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import java.util.List;


public class FieldDataTool {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ServiceException 
	 */
	public static void main(String[] args) throws IOException, ServiceException {

		/*
		// login
        LoginDialog login = new LoginDialog();
        String userName = login.getUserName();
        String password = login.getPassword();
        SpreadsheetService service = new SpreadsheetService("Test");
        
        try{
        	service.setUserCredentials(userName, password);  
            //JOptionPane.showMessageDialog(login, "Login successful!");
        } catch(AuthenticationException e){
            JOptionPane.showMessageDialog(login, "Unable to login");
        }
		
        @SuppressWarnings("unused")
		WorksheetChooser  wc = new  WorksheetChooser(service);
		*/
		List<String> spreadsheetData;
		spreadsheetData = parseCSV.parse();
		try {
			makeShapeFile.makeShapeFile(spreadsheetData);
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
	}
}
