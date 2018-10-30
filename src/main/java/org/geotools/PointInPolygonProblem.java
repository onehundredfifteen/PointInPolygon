package org.geotools.PointInPolygon;

import java.io.File;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;

import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureCollection;

import org.opengis.filter.Filter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.GeometryAttribute;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.quadtree.Quadtree;



public class PointInPolygonProblem {

    /**
     * PointInPolygonProblem
     * Simple app presenting PiP matching algorithm
	 *
	 * 1. Populate and index random points in the quadtree
	 * 2. Query the quadtree with a rectangular extent of country's shape
	 * 3. With the Visitor object, discard all points that don't belong to the country's area
	 * 4. Print the result
     */
    public static void main(String[] args) throws Exception {

		if(args.length == 1) {
			Quadtree quadtree = new Quadtree();
			
			populateQuadTree(quadtree, 1000000);
			
			//this is my answer to the challenge
			Map<String, ArrayList<Object> > result = groupPoints(quadtree, getCountriesFromShapefile(args[0]));
			//-----------------------------------
			
			printMap(result);
		}
		else {
			System.out.println("PointInPolygonProblem\nUsage: pass the shp file path as argument");
		}
    }
	
	/**
     * Populate the given quadtree with set of N random points
     */
	public static void populateQuadTree(Quadtree qtree, int N) {
		
		Random rand = new Random();
		GeometryFactory gf = new GeometryFactory();
		
		//coordinate constraints 
		final double long_min = -180F;
		final double long_max = 180F;
		final double lat_min = -90F;
		final double lat_max = 90F;
		
		//populate quad tree
        for(int i = 0; i < N; ++i) {
            Point pt = gf.createPoint(new Coordinate(rand.nextDouble() * (long_max - long_min) + long_min,  
													 rand.nextDouble() * (lat_max - lat_min) + lat_min
													 ));
													 
            qtree.insert(pt.getEnvelopeInternal(), pt);
        }
    }
	
	/**
     * Group points indexed in the quadtree by country in the collection.
	 * Assume that each shape is an instance of MultiPolygon
     */
	public static Map<String, ArrayList<Object>> groupPoints(Quadtree qtree, FeatureCollection<SimpleFeatureType, SimpleFeature> country_collection) {
		
		final int attr_countryname_index = 4; //index of country name feature attribute
		
		Map<String, ArrayList<Object> > pointsByCountry = new HashMap<>();

        try (FeatureIterator<SimpleFeature> features = country_collection.features()) {
            while (features.hasNext()) {
                
				ArrayList<Object> result = new ArrayList<Object>();
				
				//get next shape
				SimpleFeature feature = features.next();
				GeometryAttribute ga = feature.getDefaultGeometryProperty();
				MultiPolygon mp = (MultiPolygon) ga.getValue(); 
				
				//query quadtree (with visitor)
				qtree.query(mp.getEnvelopeInternal(), new CountryVisitor(mp, result)); 
				
				//add new country
				pointsByCountry.put((String)feature.getAttribute(attr_countryname_index), result); 
            }
        }
 
		return pointsByCountry;
	}
	
	/**
     * Open a shapefile and get the shape collection
     */
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> getCountriesFromShapefile(String shapefile_path) throws java.net.MalformedURLException, java.io.IOException {
		
        File file = new File(shapefile_path);
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; 
		
        return source.getFeatures(filter);
	}
	
	/**
     * Print the points-by-country map as Country->Number of points
     */
	public static void printMap(Map<String, ArrayList<Object> > points) {
		
        Iterator iterator = points.keySet().iterator();
  
		while (iterator.hasNext()) {
		   String key = iterator.next().toString();
		   ArrayList<Object> value = points.get(key);
		  
		   System.out.println(key + " " + value.size());
		}
	}
}