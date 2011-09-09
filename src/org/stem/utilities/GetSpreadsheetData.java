package org.stem.utilities;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.gdata.client.DocumentQuery;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;

public class GetSpreadsheetData {

	@SuppressWarnings("unused")
	private static com.google.gdata.data.spreadsheet.SpreadsheetEntry entry;
	@SuppressWarnings("unused")
	private static WorksheetEntry worksheet;
	
	public static List<String> getSpreadsheetData(SpreadsheetService service, WorksheetEntry worksheet) 
		throws IOException, ServiceException{
		
		String row = "";
		List<String> data= new ArrayList<String>();
		URL listFeedUrl = worksheet.getListFeedUrl();
		ListFeed lfeed =  service.getFeed(listFeedUrl, ListFeed.class);
		for (ListEntry lentry : lfeed.getEntries()) {  
		  for (String tag : lentry.getCustomElements().getTags()) {		  
			  if (lentry.getCustomElements().getValue(tag)==null) {
				  break;
			  } else { 
				  row =row +lentry.getCustomElements().getValue(tag)+ ",";
			  }			
		  }
		  
		  data.add(row);
		  row = "";
		}
		return(data);
		
	}
	
}
