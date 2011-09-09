package org.stem.utilities.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisDataStore;
import org.geotools.data.postgis.PostgisDataStoreFactory;
import org.geotools.swing.data.JDataStoreWizard;
import org.geotools.swing.wizard.JWizard;



public class PostGISLogin {

	public static PostgisDataStore pgLogin() throws IOException {
		
		JDataStoreWizard wizard = new JDataStoreWizard(new PostgisDataStoreFactory());
		int result = wizard.showModalDialog();
		if (result != JWizard.FINISH) {
			System.exit(0);
		}
		
		Map<String, Object> connectionParameters = wizard.getConnectionParameters();
			
		PostgisDataStore dataStore = (PostgisDataStore) DataStoreFinder.getDataStore(connectionParameters); 
		
		if (dataStore == null) {
			System.out.println(connectionParameters.size());
			for (Map.Entry entry: connectionParameters.entrySet())
				   System.out.println(entry.getKey()+":"+entry.getValue());
			JOptionPane.showMessageDialog(null, "Could not connect - check parameters");
			System.exit(0);
		}

		return dataStore;
	}
	
}
