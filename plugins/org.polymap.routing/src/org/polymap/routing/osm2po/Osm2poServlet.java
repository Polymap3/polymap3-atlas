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

import de.cm.osm2po.model.Coords;
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
public class Osm2poServlet
        extends HttpServlet {

    private static Log log = LogFactory.getLog( Osm2poServlet.class );

    private Graph           graph;
    
    
    public Osm2poServlet() {
        super();
        File graphFile = new File( "/home/falko/Data/osm2po/sachsen/sachsen_2po.gph" );
        graph = new Graph( graphFile );
    }


    @Override
    public void destroy() {
        super.destroy();
        graph.close();
    }


    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp )
    throws ServletException, IOException {
        resp.setContentType( "text/plain; charset=UTF-8" );
        resp.setCharacterEncoding( "UTF-8" );
        
        // param: source
        int sourceId = parseIdOrCoords( "source", req );
        if (sourceId == -1) {
            resp.getWriter().println( "No vertex found for source." );
            return;
        }
        // param: target
        int targetId = parseIdOrCoords( "target", req );
        if (targetId == -1) {
            resp.getWriter().println( "No vertex found for target." );
            return;
        }

        OutputStream rout = null, out = null;
        try {
            // gzip encoding
            rout = resp.getOutputStream();
            out = rout;
            String acceptEncoding = req.getHeader( "Accept-Encoding" );
            if (acceptEncoding != null && acceptEncoding.toLowerCase().contains( "gzip" )) {
                out = new GZIPOutputStream( rout, 512, true );
                resp.setHeader( "Content-Encoding", "gzip" );
            }
            
            // param: cmd
            String cmd = parseCmd( "cmd", req );
            
            // command: find route
            if (cmd.equalsIgnoreCase( "fr" )) {
                // XXX from request params
                Properties params = new Properties();
                params.setProperty( "findShortestPath", "false" );
                params.setProperty( "ignoreRestrictions", "false" );
                params.setProperty( "ignoreOneWays", "false" );
                params.setProperty( "heuristicFactor", "1.0" ); // 0.0 Dijkstra, 1.0 good A*

                // find route
                SinglePathRouter router = new DefaultRouter();
                int[] path = router.findPath( graph, sourceId, targetId, Float.MAX_VALUE, params );
                
                if (path == null) {
                    out.write( "No route found.".getBytes( "UTF-8" ) );
                }
                else {
                    // format: geojson
                    if ("geojson".equalsIgnoreCase( req.getParameter( "format" ) )) {
                        encodeGeoJson( path, out );
                    }
                    // format: ???
                    else {
                        encodeCoords( path, out );                
                    }
                }
            }
            else {
                throw new ServletException( "Unknown cmd: " + cmd );
            }
        }
        catch (ServletException e) {
            throw e;
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServletException( e );
        }
        finally {
            if (out != null) { out.flush(); }
            resp.flushBuffer();
        }
    }

    
    protected void encodeGeoJson( int[] path, OutputStream out )
    throws Exception {
        assert path != null;
        OutputStreamWriter writer = new OutputStreamWriter( out, "ISO-8859-1" );
        writer.append( GraphPathFormatter.createPathAsFeatureCollection( graph, path ) );
        writer.flush();
    }


    protected void encodeCoords( int[] path, OutputStream out )
    throws Exception {
        assert path != null;
                
        PrintStream pout = new PrintStream( out, false, "UTF-8" );
        for (int id : path) {
            RoutingResultSegment rrs = graph.lookupSegment( id );
            Coords coords = rrs.getCoords();
            int segId = rrs.getId();
            int from = rrs.getSourceId();
            int to = rrs.getTargetId();
            String segName = rrs.getName().toString();
            pout.println( from + "-" + to + "  " + segId + "/" + id + " " + segName );
        }
        pout.flush();
    }


    protected String parseCmd( String name, HttpServletRequest req )
    throws ServletException {
        String value = req.getParameter( name );
        // missing
        if (value == null) {
            throw new ServletException( "Parameter missing: " + name );
        }
        return value;
    }


    protected int parseIdOrCoords( String name, HttpServletRequest req )
    throws ServletException {
        String value = req.getParameter( name );
        // missing
        if (value == null) {
            throw new ServletException( "Parameter missing: " + name );
        }
        // lat/lon
        else if (value.contains( "," )) {
            StringTokenizer tokens = new StringTokenizer( value, ",", false );
            float lat = Float.parseFloat( tokens.nextToken() );
            float lon = Float.parseFloat( tokens.nextToken() );
            return graph.findClosestVertexId( lat, lon );
        }
        else {
            return Integer.parseInt( value );
        }
    }
    
}
