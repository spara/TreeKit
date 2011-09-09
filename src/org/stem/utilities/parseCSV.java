package org.stem.utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class parseCSV {
	private static  List<String> data= new ArrayList<String>();
	
	public static List<String> parse() {
		try 
		{
			String fileName = "/Users/sparafina//Terrametrics/TreeKit/Contract_2/treekit_08-28-2011.csv";
			
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			
			String strLine = "";
			while( (strLine = br.readLine()) != null) {
				data.add(strLine);
				strLine = "";
			}
		}
		catch(Exception e){
		}

		
		return(data);
	}
}
