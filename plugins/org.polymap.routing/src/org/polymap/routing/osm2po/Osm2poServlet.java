/* 
 * polymap.org
 * Copyright 2012, Polymap GmbH. All rights reserved.
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
package org.polymap.routing.osm2po;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Supplier;

import de.cm.osm2po.Utils;
import de.cm.osm2po.model.Coords;
import de.cm.osm2po.routing.DefaultRouter;
import de.cm.osm2po.routing.Graph;
import de.cm.osm2po.routing.GraphPathFormatter;
import de.cm.osm2po.routing.MultiPathRouter;
import de.cm.osm2po.routing.RoutingResultSegment;
import de.cm.osm2po.routing.SinglePathRouter;

import org.polymap.core.runtime.CachedLazyInit;
import org.polymap.core.runtime.LazyInit;
import org.polymap.core.runtime.Timer;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Osm2poServlet
        extends HttpServlet {

    private static Log log = LogFactory.getLog( Osm2poServlet.class );

    // request params *************************************
    
    public static final RequestParam<String> PARAM_FORMAT = new RequestParam( "format",
            "The format of the result of the request. Possible values are: geojson, cvs. This param is optional.",
            String.class, true, null );
    
    public static final RequestParam<Float> PARAM_MAXCOST = new RequestParam( "maxCost",
            "For cmd=fx only. Possible value: ...",
            Float.class, true, -1f );
    
    public static final RequestParam<String> PARAM_CMD = new RequestParam( "cmd",
            "The command to be executed. Possible values are: fr = find route, fx = find expansion",
            String.class, false, null );
    
    public static final IdOrCoordParam PARAM_SOURCE = new IdOrCoordParam( "source",
            "The source position to start routing from. Possible values are: vertexId or <lon,lat> (EPSG:4326)" );
    
    public static final IdOrCoordParam PARAM_TARGET = new IdOrCoordParam( "target",
            "The target position. Possible values are: vertexId or <lon,lat> (EPSG:4326)" );
    
    
    // instance *******************************************

    private LazyInit<Graph>             graphRef;
    
    
    @Override
    public void init() throws ServletException {
        // get graphFile
        String fileName = getInitParameter( "graphFile" );
        if (fileName == null) {
            fileName = System.getProperty( "org.polymap.routing.osm2po.graphFile" );
        }
        if (fileName == null) {
            throw new IllegalStateException( "No graphFile specified (-Dorg.polymap.routing.osm2po.graphFile). See");
        }
        
        final File graphFile = new File( fileName );
        log.info( "Opening graph file: " + graphFile );

        // graph: lazy, cached, auto closed
        graphRef = new CachedLazyInit( (int)graphFile.length(), new Supplier<Graph>() {
            public Graph get() {
                log.info( "Loading graph into memory..." );
                return new Graph( graphFile ) {
                    protected void finalize() throws Throwable {
                        log.info( "Removing graph from memory..." );
                        close();
                    }
                };
            }
        });
    }


    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp )
    throws ServletException, IOException {
        Timer timer = new Timer();

        OutputStream rout = null, out = null;
        try {
            resp.setContentType( "text/plain; charset=UTF-8" );
            resp.setCharacterEncoding( "UTF-8" );

            // gzip encoding
            rout = resp.getOutputStream();
            out = rout;
            String acceptEncoding = req.getHeader( "Accept-Encoding" );
            if (acceptEncoding != null && acceptEncoding.toLowerCase().contains( "gzip" )) {
                out = new GZIPOutputStream( rout );
                resp.setHeader( "Content-Encoding", "gzip" );
            }

            // params
            String cmd = PARAM_CMD.get( req );
            int sourceId = PARAM_SOURCE.get( req, graphRef.get() );
            
            // command: find route
            if (cmd.equalsIgnoreCase( "fr" )) {
                int targetId = PARAM_TARGET.get( req, graphRef.get() );

                int[] path = findRoute( sourceId, targetId );
                
                if (path != null) {
                    if ("geojson".equalsIgnoreCase( req.getParameter( "format" ) )) {
                        encodeGeoJson( path, out );
                    }
                    else {
                        encodeCoords( path, out );                
                    }
                }
                else {
                    // XXX 422 does not seem appropriate, 
                    // but simplifies error handling on the client
                    resp.sendError( 422, "No route found." );
                }
            }
            // command: find drive-time polygone
            else if (cmd.equalsIgnoreCase( "fx" )) {
                float maxCost = PARAM_MAXCOST.get( req );
                int[] vertices = findExpansion( sourceId, maxCost );
                if (vertices != null) {
                    encodeMultiPoint( vertices, out );
                }
                else {
                    // XXX 422 does not seem appropriate, 
                    // but simplifies error handling on the client
                    resp.sendError( 422, "No route found." );
                }
            }
            else {
                throw new IllegalStateException( "Unknown cmd: " + cmd );
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            log.warn( "", e );
            resp.sendError( 422, e.getLocalizedMessage() );
        }
        finally {
            if (out != null) { out.close(); }
            resp.flushBuffer();
            log.info( "Response time: " + timer.elapsedTime() + "ms" );
        }
    }


    protected int[] findExpansion( int sourceId, float maxCost ) {
        // XXX from request params
        Properties params = new Properties();
        params.setProperty( "findShortestPath", "false" );
        params.setProperty( "ignoreRestrictions", "false" );
        params.setProperty( "ignoreOneWays", "false" );
        params.setProperty( "heuristicFactor", "1.0" ); // 0.0 Dijkstra, 1.0 good A*
    
        MultiPathRouter router = new DefaultRouter();
        router.traverse( graphRef.get(), sourceId, 0, maxCost, params );
        
        return Utils.convexHull( router.getVisited(), graphRef.get().getLats(), graphRef.get().getLons() );
    }


    protected void encodeMultiPoint( int[] vertices, OutputStream out )
    throws Exception {
        assert vertices != null;
        OutputStreamWriter writer = new OutputStreamWriter( out, "ISO-8859-1" );
        writer.append( GraphPathFormatter.createVerticesAsMultiPoint( graphRef.get(), vertices ) );
        writer.flush();
    }


    protected int[] findRoute( int sourceId, int targetId ) {
        // XXX from request params
        Properties params = new Properties();
        params.setProperty( "findShortestPath", "false" );
        params.setProperty( "ignoreRestrictions", "false" );
        params.setProperty( "ignoreOneWays", "false" );
        params.setProperty( "heuristicFactor", "1.0" ); // 0.0 Dijkstra, 1.0 good A*

        // find route
        SinglePathRouter router = new DefaultRouter();
        return router.findPath( graphRef.get(), sourceId, targetId, Float.MAX_VALUE, params );
    }

    
    protected void encodeGeoJson( int[] path, OutputStream out )
    throws Exception {
        assert path != null;
        OutputStreamWriter writer = new OutputStreamWriter( out, "ISO-8859-1" );
        String json = GraphPathFormatter.createPathAsFeatureCollection( graphRef.get(), path );
        writer.write( json );
        writer.flush();
    }


    protected void encodeCoords( int[] path, OutputStream out )
    throws Exception {
        assert path != null;
                
        PrintStream pout = new PrintStream( out, false, "UTF-8" );
        for (int id : path) {
            RoutingResultSegment rrs = graphRef.get().lookupSegment( id );
            Coords coords = rrs.getCoords();
            int segId = rrs.getId();
            int from = rrs.getSourceId();
            int to = rrs.getTargetId();
            String segName = rrs.getName().toString();
            pout.println( from + "-" + to + "  " + segId + "/" + id + " " + segName );
        }
        pout.flush();
    }


    /**
     * Handles 'source' and 'target' params. Values can be vertexId or coord values. 
     */
    static class IdOrCoordParam
            extends RequestParam<Integer> {

        public IdOrCoordParam( String name, String description ) {
            super( name, description, Integer.class, false, null );
        }

        public Integer get( HttpServletRequest req, Graph graph ) {
            String value = req.getParameter( name );
            // missing
            if (value == null) {
                throw new IllegalStateException( "Parameter missing: " + name );
            }
            // lat/lon
            else if (value.contains( "," )) {
                StringTokenizer tokens = new StringTokenizer( value, ",", false );
                float lon = Float.parseFloat( tokens.nextToken() );
                float lat = Float.parseFloat( tokens.nextToken() );
                
                int result = graph.findClosestVertexId( lat, lon );
                
                if (result > -1) {
                    return result;
                }
                else {
                    throw new IllegalStateException( "No vertex found at: " + lon + "/" + lat );
                }
            }
            // id
            else {
                return Integer.parseInt( value );
            }
        }
    }
    
}
