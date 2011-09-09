package org.stem.utilities;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.postgis.PostgisDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
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
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class loadToPostGIS {
	public final static List<Double> currentStartPoint = new ArrayList<Double>();
	//private static String timeStamp; 
	
	
	public static void makeFeatureCollections(List<String> spreadsheetData) throws NoSuchAuthorityCodeException, FactoryException, TransformException, SchemaException, IOException{
		
		int i,j;
		List<String> treePitParams = new ArrayList<String>();
		final SimpleFeatureType TREEPIT = DataUtilities.createType("TreePit", "geom:Geometry,area:Double,guard:Boolean");
		final SimpleFeatureType TREE = DataUtilities.createType("Tree", "geom:Geometry,lat:Double,lng:Double,species_code:String,dbh:Double,added_by:String,image:String,dateof:Date");
		FeatureCollection<SimpleFeatureType, SimpleFeature> treePitCollection = FeatureCollections.newCollection();
		FeatureCollection<SimpleFeatureType, SimpleFeature> treeCollection = FeatureCollections.newCollection();
		
		
		for (String data : spreadsheetData) {
			if (currentStartPoint.size() > 0) {
				currentStartPoint.remove(0);
				currentStartPoint.remove(0);				
			}

			List<String> record = new ArrayList<String>(Arrays.asList(data.trim().split(",")));
				
			//get start azimuth form start and end points
			String timeStamp = record.get(0);
			double[] start = new double[] {Double.parseDouble(record.get(1).toString()),Double.parseDouble(record.get(2).toString())};
			double[] end = new double[] {Double.parseDouble(record.get(3).toString()),Double.parseDouble(record.get(4).toString())};
			String side = record.get(5);
			double[] startLL = toLatLong(start);
			double[] endLL = toLatLong(end);
			currentStartPoint.add(0, startLL[0]);
			currentStartPoint.add(1, startLL[1]);
			double azimuth = getAzimuth(startLL,endLL);
		
			// remove the start and end point coordinates
			for (i=0; i<6; i++) {
				record.remove(0);
			}

			// process tree pits
			int numberOfTreePits = (int)(record.size()/5);
			for (i=0; i < numberOfTreePits; i++ ) {
				for (j=0;j<5;j++) {
					treePitParams.add(record.get(j));
				}

			Polygon treePit =  setTreePitGeometry (treePitParams, currentStartPoint, azimuth, side);
			Point tree = treePit.getCentroid();
    		Double tpArea = treePit.getArea();
    		Boolean guard = false;
    		if (treePit.isValid()) {
      			SimpleFeature treePitFeature = SimpleFeatureBuilder.build( TREEPIT, new Object[]{treePit,tpArea,guard}, null );
    			treePitCollection.add( treePitFeature );
    			
				double lat = tree.getY();
				double lng = tree.getX();
				double dbh = Double.parseDouble(treePitParams.get(3).toString());
				String species_code = treePitParams.get(4).toString();
				String added_by = null;
				String image = null;
				Date dateof = null;
				SimpleFeature treeFeature = SimpleFeatureBuilder.build( TREE, new Object[]{tree,lat,lng,species_code,dbh,added_by,image,dateof}, null );
				treeCollection.add( treeFeature );
    		} else {
    			System.out.println("Bad Geometry, record skipped");
    		}
					
			for (j=0;j<5;j++) {
				record.remove(0);
				treePitParams.remove(0);
			}
		}
	}
    
	/*
    timeStamp = timeStamp.replace("/", "");
    timeStamp = timeStamp.replace(":", "");
    timeStamp = timeStamp.replace(" ", "");
	
    File treePitFile = getNewShapeFile(new File(timeStamp+"treepit.shp"));
    outputShapeFile(treePitFile, TREEPIT, treePitCollection);
    JOptionPane.showMessageDialog(null, "Tree pit shapefile created");
    
    File treeFile = getNewShapeFile(new File(timeStamp+"tree.shp"));
    outputShapeFile(treeFile, TREE, treeCollection);    
    JOptionPane.showMessageDialog(null, "Tree shapefile created");
	*/
		
	PostgisDataStore pgDs = org.stem.utilities.gui.PostGISLogin.pgLogin();
	FeatureStore<SimpleFeatureType, SimpleFeature> treepitTable = (FeatureStore<SimpleFeatureType, SimpleFeature>) pgDs.getFeatureSource("treepit");
	treepitTable.addFeatures(treePitCollection);
	JOptionPane.showMessageDialog(null, "Tree pit data loaded");
	
	FeatureStore<SimpleFeatureType, SimpleFeature> treeTable = (FeatureStore<SimpleFeatureType, SimpleFeature>) pgDs.getFeatureSource("tree");
	treeTable.addFeatures(treeCollection);
	JOptionPane.showMessageDialog(null, "Tree data loaded");

	
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
	
	
	public static Polygon setTreePitGeometry(List<String> treePitParams, 
			List<Double> currentStart, double azimuth, String side) 
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
		if (side.equals("L")) {
			slope = Math.tan(Math.toRadians(azimuth));
			perpendicularSlope = (1/slope)*-1;
			perpendicularAzimuth = Math.toDegrees(Math.atan(perpendicularSlope));
		} else {
			double perpAzimuth = 360 - (180 - azimuth);
			slope = Math.tan(Math.toRadians(perpAzimuth));
			perpendicularSlope = (1/slope)*-1;
			perpendicularAzimuth = Math.toDegrees(Math.atan(perpendicularSlope));
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
	    builder.add( "geom", Geometry.class );
	    builder.add("area", Double.class);
	    builder.add( "guard", Boolean.class );
	    
	    //build the type
	    final SimpleFeatureType TreePit = builder.buildFeatureType();
	    return TreePit;
	}
	
	static SimpleFeatureType createTreeFeatureType(){
	    
	    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
	    builder.setName( "TreePit" );
	    builder.setCRS( DefaultGeographicCRS.WGS84 );
	    
	    //add attributes in order
	    builder.add( "geom", Geometry.class );
	    builder.add("lat", Double.class);
	    builder.add("lng", Double.class);
	    builder.length(15).add( "species_code", String.class );
	    builder.add("dbh", Double.class);
	    builder.length(15).add("added_by", String.class);
	    builder.length(1000).add("image", String.class);
	    builder.add("dateof", Date.class);
	    
	    
	    //build the type
	    final SimpleFeatureType Tree = builder.buildFeatureType();
	    return Tree;
	}
	
}
