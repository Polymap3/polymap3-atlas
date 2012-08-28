/* 
 * polymap.org
 * Copyright 2012, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.routing.osm2po.test;

import java.util.Properties;

import java.io.File;
import java.io.PrintStream;

import junit.framework.TestCase;

import org.polymap.core.runtime.Timer;

import de.cm.osm2po.model.LatLon;
import de.cm.osm2po.routing.DefaultRouter;
import de.cm.osm2po.routing.Graph;
import de.cm.osm2po.routing.GraphPathFormatter;
import de.cm.osm2po.routing.RoutingResultSegment;
import de.cm.osm2po.routing.SinglePathRouter;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Osm2poTest
        extends TestCase {

    private static final PrintStream  log = System.out;
    
    private Graph               graph;
    
    
    protected void setUp() throws Exception {
        super.setUp();
        Timer timer = new Timer();
        File graphFile = new File( "/home/falko/Data/osm2po/sachsen/sachsen_2po.gph" );
        graph = new Graph( graphFile );
        log.println( "Graph loading done: " + timer.elapsedTime() + "ms" );
    }


    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    
    public void testAStar() throws Exception {
        Timer timer = new Timer();
        SinglePathRouter router = new DefaultRouter();

        // Possible additional params for DefaultRouter
        Properties params = new Properties();
        params.setProperty( "findShortestPath", "false" );
        params.setProperty( "ignoreRestrictions", "false" );
        params.setProperty( "ignoreOneWays", "false" );
        params.setProperty( "heuristicFactor", "1.0" ); // 0.0 Dijkstra, 1.0 good A*

        // Leipzig / Freiberg :)
        int sourceId = graph.findClosestVertexId( 51.3f, 12.4f );
        int targetId = graph.findClosestVertexId( 50.9f, 13.3f );
        
        int[] path = router.findPath( graph, sourceId, targetId, Float.MAX_VALUE, params );

        if (path != null) {
            for (int id : path) {
                RoutingResultSegment rrs = graph.lookupSegment( id );
                int segId = rrs.getId();
                int from = rrs.getSourceId();
                int to = rrs.getTargetId();
                String segName = rrs.getName().toString();
                log.println( from + "-" + to + "  " + segId + "/" + id + " " + segName );
                
                log.print( "LatLons: " );
                for (LatLon latlon : rrs.getLatLons()) {
                    log.print( latlon.getLat() + "," + latlon.getLon() );
                }
                log.println( ".");
            }
        }
        log.println( "Routing done: " + timer.elapsedTime() + "ms" );    
    }
    
    
    public void testGeoJSON() throws Exception {
        SinglePathRouter router = new DefaultRouter();

        // Possible additional params for DefaultRouter
        Properties params = new Properties();
        params.setProperty( "findShortestPath", "false" );
        params.setProperty( "ignoreRestrictions", "false" );
        params.setProperty( "ignoreOneWays", "false" );
        params.setProperty( "heuristicFactor", "1.0" ); // 0.0 Dijkstra, 1.0 good A*

        // Leipzig / Freiberg :)
        int sourceId = graph.findClosestVertexId( 51.3f, 12.4f );
        int targetId = graph.findClosestVertexId( 50.9f, 13.3f );
        
        int[] path = router.findPath( graph, sourceId, targetId, Float.MAX_VALUE, params );
        
        Timer timer = new Timer();
        String json = GraphPathFormatter.createPathAsFeatureCollection( graph, path );
        log.println( json );
        
        log.println( "GeoJSON encoding done: " + timer.elapsedTime() + "ms" );        
    }
}
