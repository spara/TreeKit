package org.stem.utilities;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class treepit {
	public final static List<Double> currentStartPoint = new ArrayList<Double>();
	
	public static void main(String[] args) throws NoSuchAuthorityCodeException, FactoryException, TransformException, SchemaException, IOException{
		
		String data;
		int i,j;
		List<String> treePitParams = new ArrayList<String>();
		final SimpleFeatureType TYPE = DataUtilities.createType("TreePit", "polygon:Polygon,dbh:Double,Species:String");
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = FeatureCollections.newCollection();
		File file = getCSVFile(args);
		BufferedReader reader = new BufferedReader( new FileReader( file ));
		
		try {
			data = reader.readLine();
		
			for( data = reader.readLine(); data != null; data = reader.readLine()){
				List<String> record = new ArrayList<String>(Arrays.asList(data.trim().split("\\s+")));

				//get start azimuth form start and end points
				double[] start = new double[] {Double.parseDouble(record.get(0).toString()),Double.parseDouble(record.get(1).toString())};
				double[] end = new double[] {Double.parseDouble(record.get(2).toString()),Double.parseDouble(record.get(3).toString())};
				double[] startLL = toLatLong(start);
				double[] endLL = toLatLong(end);
				currentStartPoint.add(0, startLL[0]);
				currentStartPoint.add(1, startLL[1]);
				double azimuth = getAzimuth(startLL,endLL);
				// remove the start and end point coordinates
				for (i=0; i<4; i++) {
					record.remove(0);
				}

				// process tree pits
				int numberOfTreePits = (int)(record.size()/5);
				for (i=0; i < numberOfTreePits; i++ ) {
					for (j=0;j<5;j++) {
						//System.out.println("while adding to array: "+record.get(j));
						treePitParams.add(record.get(j));
					}
					double dbh = Double.parseDouble(treePitParams.get(3).toString());
					String species = treePitParams.get(4).toString();
					Polygon treePit =  setTreePitGeometry (treePitParams, currentStartPoint, azimuth);
    		
					SimpleFeature feature = SimpleFeatureBuilder.build( TYPE, new Object[]{treePit, dbh,species}, null );
					collection.add( feature );
    		
					for (j=0;j<5;j++) {
						record.remove(0);
						treePitParams.remove(0);
					}
				}
			}
		} 	finally {
			reader.close();
		}
    	
    	File newFile = getNewShapeFile( file );
    	
    	DataStoreFactorySpi factory = new ShapefileDataStoreFactory();

    	Map<String,Serializable> create = new HashMap<String,Serializable>();
    	create.put("url", newFile.toURI().toURL() );
    	create.put("create spatial index",Boolean.TRUE);
    	
    	ShapefileDataStore newDataStore = (ShapefileDataStore) factory.createNewDataStore( create );
    	newDataStore.createSchema( TYPE );
    	newDataStore.forceSchemaCRS( DefaultGeographicCRS.WGS84 );
    	
    	Transaction transaction = new DefaultTransaction("create");
    	
    	String typeName = newDataStore.getTypeNames()[0];
    	FeatureStore<SimpleFeatureType, SimpleFeature> featureStore;
        featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>)
                    newDataStore.getFeatureSource( typeName );
        
    	featureStore.setTransaction(transaction);
    	try {			
    		featureStore.addFeatures(collection);
    		transaction.commit();
    	}
    	catch (Exception problem){
    		problem.printStackTrace();
    		transaction.rollback();
    	}
    	finally {
    		transaction.close();
    	}
		

	}

	public static double getAzimuth(double[] start, double[] end) throws NoSuchAuthorityCodeException, FactoryException {

		System.setProperty("org.geotools.referencing.forceXY", "true");
		CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");

        //System.out.println(start[0]+" , "+start[1]);
		GeodeticCalculator calc = new GeodeticCalculator(wgs84);
		calc.setStartingGeographicPoint(start[0], start[1]);
		calc.setDestinationGeographicPoint(end[0], end[1]);
        //System.out.println("Angle:" + calc.getAzimuth());
		return calc.getAzimuth();
	}
	
	
	public static Polygon setTreePitGeometry(List<String> treePitParams, List<Double> currentStart, double azimuth) throws TransformException, NoSuchAuthorityCodeException, FactoryException {
		
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
		double slope = Math.tan(Math.toRadians(azimuth));
		//System.out.println("slope: "+slope);
		double perpendicularSlope = (1/slope)*-1;
		//System.out.println("perepndicular slope: "+perpendicularSlope);
		double perpendicularAzimuth = Math.toDegrees(Math.atan(perpendicularSlope));
		
	
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

	static SimpleFeatureType createFeatureType(){
	    
	    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
	    builder.setName( "TreePit" );
	    builder.setCRS( DefaultGeographicCRS.WGS84 );
	    
	    //add attributes in order
	    builder.add( "polygon", Polygon.class );
	    builder.add("dbh", Double.class);
	    builder.length(15).add( "Species", String.class );
	    
	    //build the type
	    final SimpleFeatureType TreePit = builder.buildFeatureType();
	    return TreePit;
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
			
			//System.out.println("Opening data file: " + file.getName());
		}
		else {
			file = new File( args[0] );
		}
		if (!file.exists()){
			throw new FileNotFoundException( file.getAbsolutePath() );
		}
		return file;
	}
}
