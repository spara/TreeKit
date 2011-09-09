package org.stem.utilities;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.stem.utilities.gui.LoginDialog;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Lineal;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class makeShapeFile {
	public final static List<Double> currentStartPoint = new ArrayList<Double>();
	private static String timeStamp; 
	private static TreeMap genusmap = genusMap.makeMap();
	private static TreeMap posmap = positionMap();
	
	public static void makeShapeFile(List<String> spreadsheetData) throws NoSuchAuthorityCodeException, FactoryException, TransformException, SchemaException, IOException{
		
		int i,j;
		List<String> treePitParams = new ArrayList<String>();
		// treePit,blockID,bedLength,bedWidth,addressNumber,streetName,treeNumber,treePosition}, null );
		final SimpleFeatureType TREEPIT = DataUtilities.createType("TreePit", "polygon:Polygon,geohash:String,blockID:Integer,bedLength:Double,bedWidth:Double,addressNumber:String,streetName:String,treeNumber:Integer,treePosition:String");
		//tree,treebedID,blockID,genus,genusConf,species,speciesConf,dbh}
		final SimpleFeatureType TREE = DataUtilities.createType("Tree", "point:Point,geohash:String,blockID:Integer,status:String,genus:String,species:String,fastigate:Integer,SpeciesConf:Integer,dbh:Double,addressNumber:String,streetName:String,treeNumber:Integer,treePosition:String");
		// begin and end lines for each block
		final SimpleFeatureType SURVEYBLOCK = DataUtilities.createType("SurveyBlock","surveyLine:LineString,blockId:Integer,direction:String,numTrees:Integer,numBeds:Integer,sumLength:Double,lineLength:Double,diffStatus:String");
		FeatureCollection<SimpleFeatureType, SimpleFeature> treePitCollection = FeatureCollections.newCollection();
		FeatureCollection<SimpleFeatureType, SimpleFeature> treeCollection = FeatureCollections.newCollection();
		FeatureCollection<SimpleFeatureType, SimpleFeature> surveyBlockCollection = FeatureCollections.newCollection();
		java.util.Date date= new java.util.Date();
		Long ts = date.getTime();
		timeStamp = ts.toString();
		int lineNumber = 0;
		int numTrees = 0;
		int numBeds = 0;
		double sumLength = 0;
		double lineLength = 0;
		
		for (String data : spreadsheetData) {
			
			if (currentStartPoint.size() > 0) {
				currentStartPoint.remove(0);
				currentStartPoint.remove(0);				
			}
            
			
			List<String> record = new ArrayList<String>(Arrays.asList(data.split("\\|")));
			// get distance from last treebed to end
			double distToEnd = Double.parseDouble(record.get(record.size()-1));
			
			int blockID = Integer.parseInt(record.get(0).toString());
			double[] start = new double[] {Double.parseDouble(record.get(8).toString()),Double.parseDouble(record.get(7).toString())};
			double[] end = new double[] {Double.parseDouble(record.get(10).toString()),Double.parseDouble(record.get(9).toString())};
			String side = record.get(11).trim();
			double[] startLL = start;
			double[] endLL = end;
			currentStartPoint.add(0, startLL[0]);
			currentStartPoint.add(1, startLL[1]);
			double azimuth = getAzimuth(startLL,endLL);
			String dir = getDirection(start,end);
			System.out.println("rec size w/ start: " + (record.size()-1) + " num recs "); 
			// remove the start and end point coordinates
			for (i=0; i<13; i++) {
				record.remove(0);
			}

			// process tree pits
			// 8-28-2011 there are now 12 parameters
			System.out.println("rec size: " + (record.size()) + " num recs " + (record.size()-1)/13 );
			
			int numberOfTreePits = (int)((record.size()-1)/13);
			for (i=0; i < numberOfTreePits; i++ ) {
				System.out.println("tree " + i);
				for (j=0;j<13;j++) {
					treePitParams.add(record.get(j));
				}
			
				if (!treePitParams.get(0).equals("NULL")) {				
					
					double distToTreebed = Double.parseDouble(treePitParams.get(0).toString());
					Polygon treePit =  setTreePitGeometry (treePitParams, currentStartPoint, azimuth, side, dir);
					double bedLength	= Double.parseDouble(treePitParams.get(1).toString());
					double bedWidth	= Double.parseDouble(treePitParams.get(2).toString());	
					String addressNumber = treePitParams.get(4);
					String streetName = standardizeStreet(treePitParams.get(5).trim());
					int treeNumber = 0;
					if (!treePitParams.get(6).equals("NULL")) {
						treeNumber = Integer.parseInt(treePitParams.get(6).trim());
					}
					String treePosition = getPosition(treePitParams.get(7));
					sumLength = sumLength + bedLength + distToTreebed;
					// 	set up tree feature
					Point tree = treePit.getCentroid();
					String geohash = GeoHash.getGeoHash(tree.getY(),tree.getX());

					// create tree bed feature
					SimpleFeature treePitFeature = SimpleFeatureBuilder.build( TREEPIT, new Object[]{treePit,geohash,blockID,bedLength,bedWidth,addressNumber,streetName,treeNumber,treePosition}, null );
					if (treePit.getArea() > 0 ) {
						treePitCollection.add( treePitFeature );
						numBeds++;
					}
			
					// get genus and species
					//String genus =null;
					int speciesConf = 0;
					int fastigate = 0;
					String status = treePitParams.get(8).toString();
					//System.out.println(status);
					String genus = treePitParams.get(9).toString();
					//System.out.println(genus);
					String species = treePitParams.get(10).toString();
					//System.out.println(species);
					//genus = getGenus(species);
					if (!treePitParams.get(11).equals("NULL")) {
						fastigate = Integer.parseInt(treePitParams.get(11));
						//System.out.println(fastigate);
					}
					//System.out.println(fastigate);
					if (!treePitParams.get(12).equals("NULL")) {
						speciesConf = Integer.parseInt(treePitParams.get(12).trim().toString());
						//System.out.println(speciesConf);
					}
					//System.out.println(speciesConf);
			
					// calc dbh
					double dbh = 0.0;
					if (!treePitParams.get(3).equals("NULL")) {
						dbh = Double.parseDouble(treePitParams.get(3).toString())/Math.PI;
					}
			
					// create tree feature
					SimpleFeature treeFeature = SimpleFeatureBuilder.build( TREE, new Object[]{tree,geohash,blockID,status,genus,species,fastigate,speciesConf,dbh,addressNumber,streetName,treeNumber,treePosition}, null );
					if (treePit.getArea() > 0) {
						treeCollection.add( treeFeature );
						numTrees++;
					}
					
			
				}

				// clean up
				for (j=0;j<13;j++) {
					record.remove(0);
					treePitParams.remove(0);
				}			
			}
			
			// create surveyBlock attributes
			LineString surveyBlock = getSurveyBlock(start,end);
			sumLength = sumLength + distToEnd;
			lineLength =getSBDistance(start, end) * 3.28083989501312 ;
			double sbLength = surveyBlock.getLength();
			String slStatus = null;
			if (sumLength == lineLength) {
				slStatus = "equals";
			} else if (sumLength > lineLength) {
				slStatus = "greater";
			} else if (sumLength < lineLength) {
				slStatus = "less";
			}
			
			// create surveyblock feature
			SimpleFeature surveyBlockFeature = SimpleFeatureBuilder.build( SURVEYBLOCK, new Object[]{surveyBlock,blockID,dir,numTrees,numBeds,sumLength,lineLength,slStatus}, null );
			surveyBlockCollection.add( surveyBlockFeature );
		
			// reset counters
			numTrees = 0;
			numBeds = 0;
			sumLength = 0;
			lineLength = 0;
			slStatus = null;
		}
    
		// write shape files	
		timeStamp = timeStamp.replace("/", "");
		timeStamp = timeStamp.replace(":", "");
		timeStamp = timeStamp.replace(" ", "");
		String path = "/Users/sparafina/Terrametrics/TreeKit/test/";
		File treePitFile = getNewShapeFile(new File(timeStamp+"treepit.shp"));
		outputShapeFile(treePitFile, TREEPIT, treePitCollection);
		//JOptionPane.showMessageDialog(null, "Tree bed shapefile created");
    
		File treeFile = getNewShapeFile(new File(timeStamp+"tree.shp"));
		outputShapeFile(treeFile, TREE, treeCollection);    
		//JOptionPane.showMessageDialog(null, "Tree shapefile created");
    
		File sbFile = getNewShapeFile(new File(timeStamp+"surveyBlock.shp"));
		outputShapeFile(sbFile, SURVEYBLOCK, surveyBlockCollection);  
    
	}
	
	public static String getGenus(String species) {
		String genus=null;
		if (genusmap.containsKey(species)) {
			genus = genusmap.get(species).toString();
		}
		return genus;
	}
	
	public static void outputShapeFile(File file, SimpleFeatureType ft, FeatureCollection<SimpleFeatureType, SimpleFeature> fc) throws IOException {
	    DataStoreFactorySpi factory = new ShapefileDataStoreFactory();
	    Map<String,Serializable> create = new HashMap<String,Serializable>();
	    create.put("url", file.toURI().toURL() );
	    create.put("create spatial index",Boolean.TRUE);
	    	
	    ShapefileDataStore newDataStore = (ShapefileDataStore) factory.createNewDataStore( create );
	    newDataStore.createSchema( ft );
	    newDataStore.forceSchemaCRS( DefaultGeographicCRS.WGS84 );	
	    Transaction transaction = new DefaultTransaction("create");
	    String typeName = newDataStore.getTypeNames()[0];
	    FeatureStore<SimpleFeatureType, SimpleFeature> featureStore;
	    featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>)
	                    newDataStore.getFeatureSource( typeName );    
	    featureStore.setTransaction(transaction);
	    try {			
	    	featureStore.addFeatures(fc);
	    	transaction.commit();
	    } catch (Exception problem){
	    	problem.printStackTrace();
	    	transaction.rollback();
	    } finally {
	    	transaction.close();
	    }
	}

	public static double getAzimuth(double[] start, double[] end) 
		throws NoSuchAuthorityCodeException, FactoryException {
		System.setProperty("org.geotools.referencing.forceXY", "true");
		CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
		GeodeticCalculator calc = new GeodeticCalculator(wgs84);
		calc.setStartingGeographicPoint(start[0], start[1]);
		calc.setDestinationGeographicPoint(end[0], end[1]);
		return calc.getAzimuth();
	}
	
	
	public static String getDirection(double[] start, double[] end) {
		String direction = null;
		double startx = start[0];
		double endx = end[0];
		if (startx > endx) {
			direction = "W";
		} else {
			direction = "E";
		}
		return direction;	
	}
	
	public static Polygon setTreePitGeometry(List<String> treePitParams, 
			List<Double> currentStart, double azimuth, String side, String dir) 
			throws TransformException, NoSuchAuthorityCodeException, 
			FactoryException {
		
		Coordinate[] polygonCoordinates = new Coordinate[5];
		CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
		double D_ft	= Double.parseDouble(treePitParams.get(0).toString());
		double T_ft	= Double.parseDouble(treePitParams.get(1).toString());
		double W_ft	= Double.parseDouble(treePitParams.get(2).toString());	
		
		// initialize calculator
		GeodeticCalculator calc = new GeodeticCalculator(wgs84);
		
		// get first point of tree pit
		double currentStartX = Double.parseDouble(currentStart.get(0).toString());
		double currentStartY = Double.parseDouble(currentStart.get(1).toString());
		calc.setStartingGeographicPoint(currentStartX, currentStartY);
		calc.setDirection(azimuth, D_ft*0.3048);
		Point2D startTreePitPoint = calc.getDestinationGeographicPoint();
		polygonCoordinates[0] = new Coordinate(startTreePitPoint.getX(),startTreePitPoint.getY());
		
		// get second point of tree pit
		calc.setStartingGeographicPoint(startTreePitPoint.getX(),startTreePitPoint.getY());
		calc.setDirection(azimuth, T_ft*0.3048);
		Point2D endTreePitPoint = calc.getDestinationGeographicPoint();
		polygonCoordinates[1] = new Coordinate(endTreePitPoint.getX(),endTreePitPoint.getY());
		
		// calc the azimuth of the perpendicular line
		double slope = 0.0;
		double perpendicularSlope = 0.0;
		double perpendicularAzimuth =0.0;

		// handle cases based on direction and side of street based on starting point
		if (side.equals("R") && dir.equals("W")) {
		    double perpAzimuth = 0.0;
			slope = Math.tan(Math.toRadians(azimuth));
			perpendicularSlope = (1/slope)*-1;
			perpAzimuth = Math.toDegrees(Math.atan(perpendicularSlope));
			if (perpAzimuth > 180 && perpAzimuth < 360) {
				perpendicularAzimuth = (180 - perpAzimuth);
			} else {			
				perpendicularAzimuth =  perpAzimuth;
			}	
		}
		if (side.equals("R") && dir.equals("E")) {			
			double perpAzimuth = 0.0;
			slope = Math.tan(Math.toRadians(azimuth));
			perpendicularSlope = (1/slope)*-1;
			perpAzimuth = Math.toDegrees(Math.atan(perpendicularSlope));
			if (perpAzimuth > -180 && perpAzimuth < 0) {
				perpendicularAzimuth = (180 + perpAzimuth);
			} else {
				perpendicularAzimuth =  (180 - perpAzimuth)/-1;
			}
		}
		if (side.equals("L") && dir.equals("W")) {
		    double perpAzimuth = 0.0;
			slope = Math.tan(Math.toRadians(azimuth));
			perpendicularSlope = (1/slope)*-1;
			perpAzimuth = Math.toDegrees(Math.atan(perpendicularSlope));
			if (perpAzimuth > -180 && perpAzimuth < 0) {
				perpendicularAzimuth = (180 + perpAzimuth);
			} else {
				perpendicularAzimuth = (180 - perpAzimuth)/-1;
			}	
		}
		if (side.equals("L") && dir.equals("E")) {			
			double perpAzimuth = 0.0;
			slope = Math.tan(Math.toRadians(azimuth));
			perpendicularSlope = (1/slope)*-1;
			perpAzimuth = Math.toDegrees(Math.atan(perpendicularSlope));			
			perpendicularAzimuth = perpAzimuth;
		}
			
	
		// calc the position of parallel destination point
		calc.setStartingGeographicPoint(endTreePitPoint.getX(),endTreePitPoint.getY());
		calc.setDirection(perpendicularAzimuth, W_ft*0.3048);
		Point2D endTreePitPointPerpendicular = calc.getDestinationGeographicPoint();
		polygonCoordinates[2] = new Coordinate(endTreePitPointPerpendicular.getX(),endTreePitPointPerpendicular.getY());
		
		// calc the position of parallel start point
		calc.setStartingGeographicPoint(startTreePitPoint.getX(),startTreePitPoint.getY());
		calc.setDirection(perpendicularAzimuth, W_ft*0.3048);
		Point2D startTreePitPointPerpendicular = calc.getDestinationGeographicPoint();
		polygonCoordinates[3] = new Coordinate(startTreePitPointPerpendicular.getX(),startTreePitPointPerpendicular.getY());
		polygonCoordinates[4] = new Coordinate(startTreePitPoint.getX(),startTreePitPoint.getY());

		// create a polygon
		GeometryFactory geomFac = new GeometryFactory();
		LinearRing rgG = geomFac.createLinearRing(polygonCoordinates);
		Polygon    pgG = geomFac.createPolygon(rgG,null);

		//reset starting point		
		currentStartPoint.remove(0);
		currentStartPoint.remove(0);
		currentStartPoint.add(0, endTreePitPoint.getX());
		currentStartPoint.add(1, endTreePitPoint.getY());
        return pgG;
	}
	
	public static double[] toLatLong(double[] point) throws NoSuchAuthorityCodeException, FactoryException, TransformException {
	    System.setProperty("org.geotools.referencing.forceXY", "true");
	    CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
	    CoordinateReferenceSystem LongIslandFt = CRS.decode("EPSG:2263");
	    double[] proj = new double[2];

	    // reproject forward
	    MathTransform mt = CRS.findMathTransform(LongIslandFt, wgs84);
	    mt.transform(point, 0, proj, 0, 1);
		return proj;
	}

	static SimpleFeatureType createTreePitFeatureType(){
	    
	    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
	    builder.setName( "TreePit" );
	    builder.setCRS( DefaultGeographicCRS.WGS84 );
	    
	    //add attributes in order	    
	    //blockID,bedLength,bedWidth,addressNumber,streetName,treeNumber,treePosition}, null );
	    builder.add( "polygon", Polygon.class );
	    builder.add( "geohash", String.class );
	    builder.add("blockID", Integer.class);
	    builder.add("bedLength", Double.class);
	    builder.add("bedWidth", Double.class);
	    builder.length(10).add("addressNumber", String.class);
	    builder.length(25).add("streetName", String.class);
	    builder.add("treeNumber", Integer.class);
	    builder.length(10).add("treePosition",String.class);
	    
	    //build the type
	    final SimpleFeatureType TreePit = builder.buildFeatureType();
	    return TreePit;
	}
	
	static SimpleFeatureType createTreeFeatureType(){
	    
	    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
	    builder.setName( "Tree" );
	    builder.setCRS( DefaultGeographicCRS.WGS84 );
	    
	    //add attributes in order
	    // tree,treebedID,blockID,genus,genusConf,species,speciesConf,dbh}, null );
	    // 08-28-2001 update:
	    // {tree,geohash,blockID,status,genus,species,fastigate,speciesConf,dbh,addressNumber,streetName,treeNumber,treePosition}, null );
	    builder.add( "point", Point.class );
	    builder.add( "geohash", String.class );
	    builder.add("blockID", Integer.class);
	    builder.length(12).add("Status", String.class);
	    builder.length(50).add( "Genus", String.class );
	    builder.length(50).add( "Species", String.class );
	    builder.add("Fastigate", Integer.class);
	    builder.add("speciesConf", Integer.class);
	    builder.add("dbh", Double.class);
	    builder.length(10).add("addressNumber", String.class);
	    builder.length(25).add("streetName", String.class);
	    builder.add("treeNumber", Integer.class);
	    builder.length(10).add("treePosition",String.class);	    
	    
	    //build the type
	    final SimpleFeatureType Tree = builder.buildFeatureType();
	    return Tree;
	}
	
	private static File getNewShapeFile(File file) {
		String path = file.getAbsolutePath();
		String newPath = path.substring(0,path.length()-4)+".shp";
		
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Save shapefile");
		chooser.setSelectedFile( new File( newPath ));		
		chooser.setFileFilter( new FileFilter(){
	        public boolean accept( File f ) {
	            return f.isDirectory() || f.getPath().endsWith("shp") || f.getPath().endsWith("SHP");
	        }
	        public String getDescription() {
	            return "Shapefiles";
	        }
		});
		int returnVal = chooser.showSaveDialog(null);
		
		if(returnVal != JFileChooser.APPROVE_OPTION) {
			System.exit(0);
		}
		File newFile = chooser.getSelectedFile();
		if( newFile.equals( file )){
			System.out.println("Cannot replace "+file);
			System.exit(0);
		}
		return newFile;
	}

	private static File getCSVFile(String[] args) throws FileNotFoundException {
		File file;
		if (args.length == 0){
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Open data file");
			chooser.setFileFilter( new FileFilter(){
	            public boolean accept( File f ) {
	                return f.isDirectory() || f.getPath().endsWith("csv") || f.getPath().endsWith("CSV")  || f.getPath().endsWith("txt");
	            }
	            public String getDescription() {
	                return "S+em Data File";
	            }
			});
			int returnVal = chooser.showOpenDialog( null );
			
			if(returnVal != JFileChooser.APPROVE_OPTION) {
				System.exit(0);
			}
			file = chooser.getSelectedFile();
		}
		else {
			file = new File( args[0] );
		}
		if (!file.exists()){
			throw new FileNotFoundException( file.getAbsolutePath() );
		}
		return file;
	}
	
	public static String getPosition(String pos) {
		String position=null;
		if (posmap.containsKey(pos)) {
			position = posmap.get(pos).toString();
		}
		return position;
	}
	
	public static TreeMap<String, String> positionMap() {
		
		Map<String, String> pmap = new HashMap<String, String>();    
		pmap = new TreeMap<String, String>();        
		
		pmap.put("Front","O"); 
		pmap.put("Side","S");
	    pmap.put("Rear","R");
	    pmap.put("Across","X"); 
	    pmap.put("Adjacent","A"); 
	    pmap.put("Median","M"); 
	    pmap.put("Median-Greenstreets","MG"); 
	    pmap.put("Side / Across","SA"); 
	    pmap.put("Side / Median","SM");
	    pmap.put("Rear / Across","RX");
	    pmap.put("Rear / Median","RM");
	    pmap.put("Assigned","Z");
	    pmap.put("undefine","U");
	    
	    return (TreeMap<String, String>) pmap;
	}
	
	public static String standardizeStreet(String street ) {
		String standardStreet= "HOLDER";
		Pattern thePattern = Pattern.compile("Street");
		Matcher m = thePattern.matcher("");
		m.reset(street);
		if (m.find()) {
			Pattern pp = Pattern.compile("Street");
			standardStreet = street.replaceFirst(pp.toString(), "ST").toUpperCase();
		}
		
		thePattern = Pattern.compile("Avenue");
		m = thePattern.matcher("");
		m.reset(street);
		if (m.find()) {
			Pattern pp = Pattern.compile("Avenue");
			standardStreet = street.replaceFirst(pp.toString(), "AV").toUpperCase();
			pp = Pattern.compile("SAINT");
			m = pp.matcher("");
			m.reset(standardStreet);
			if (m.find()) {
				//
				standardStreet = standardStreet.replace(pp.toString(), "ST").toUpperCase();
			}
		}
		
		thePattern = Pattern.compile("Place");
		m = thePattern.matcher("");
		m.reset(street);
		if (m.find()) {
			Pattern pp = Pattern.compile("Place");
			standardStreet = street.replaceFirst(pp.toString(), "PL").toUpperCase();
		}
		  
		return standardStreet;
	}
	
	static SimpleFeatureType createSurveyBlockFeatureType(){
	    
	    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
	    builder.setName( "surveyBlock" );
	    builder.setCRS( DefaultGeographicCRS.WGS84 );
	    
	    //add attributes in order
	    builder.add( "line", LineString.class );
	    builder.add("blockID", Integer.class);
	    builder.add("direction", String.class);
	    builder.add("numTrees", Integer.class);
	    builder.add("numBeds", Integer.class);
	    builder.add("sumLength", Double.class);
	    builder.add("lineLength", Double.class);
	    builder.add("lenStatus", String.class);
	    
	    //build the type
	    final SimpleFeatureType surveyBlock = builder.buildFeatureType();
	    return surveyBlock;
	}
	
	static LineString getSurveyBlock(double[] start, double[] end) {
		
		Coordinate[] coords  = new Coordinate[] {new Coordinate(start[0],start[1]), new Coordinate(end[0],end[1])};
		GeometryFactory geomFac = new GeometryFactory();
		LineString sb = geomFac.createLineString(coords);
		return sb;
	}

	static double getSBDistance(double[] start, double[] end) {
		double dis=0;
		try {
			dis =JTS.orthodromicDistance(new Coordinate(start[0],start[1]), new Coordinate(end[0],end[1]), DefaultGeographicCRS.WGS84);
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dis;
	}
}
