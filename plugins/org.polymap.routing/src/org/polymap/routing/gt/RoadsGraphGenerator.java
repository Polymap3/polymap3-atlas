/* 
 * polymap.org
 * Copyright 2011, Polymap GmbH. All rights reserved.
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
package org.polymap.routing.gt;

import java.util.List;

import org.geotools.graph.build.GraphBuilder;
import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graphable;
import org.geotools.graph.structure.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * 
 * @see LineStringGraphGenerator
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class RoadsGraphGenerator
        extends BasicLineGraphGenerator {

    private static Log log = LogFactory.getLog( RoadsGraphGenerator.class );
    
    private static GeometryFactory gf = new GeometryFactory();

    
    public Graphable add( Object obj ) {
        LineString ls = obj instanceof MultiLineString
                ? (LineString)((MultiLineString)obj).getGeometryN( 0 )
                : (LineString)obj;

        for (int i=0; i < ls.getNumPoints()-1; i++) {
            Edge e = (Edge)super.add( new LineSegment(
                    ls.getCoordinateN( i ), 
                    ls.getCoordinateN( i+1 ) ) );
        }
        return null;
    }

    
    public void optimize( IProgressMonitor monitor ) {
        monitor.beginTask( "Optimizing graph - ", getNodeMap().size() );
        
        GraphBuilder builder = getGraphBuilder();
        int count = 0, found = 0;
        for (Object obj : getNodeMap().values()) {
            Node node = (Node)obj;
            List<Edge> edges = node.getEdges();
            
            if (edges.size() == 2) {
                Node a = (edges.get( 0 )).getOtherNode( node );
                Node b = (edges.get( 1 )).getOtherNode( node );
                
                builder.removeNode( node );
            
                builder.addEdge( builder.buildEdge( a, b ) );
                found++;
            }
            if (++count % 1000 == 0) {
                monitor.subTask( "Nodes: " + count + ", Simple: " + found );
                monitor.worked( 1000 );
            }
        }
    }

    
    public Graphable remove( Object obj ) {
        throw new RuntimeException( "not yet implemented." );
    }


    public Graphable get( Object obj ) {
        throw new RuntimeException( "not yet implemented." );
    }


    protected void setObject( Node n, Object obj ) {
        //set underlying object to be point instead of coordinate
        Coordinate c = (Coordinate)obj;
        n.setObject( gf.createPoint( c ) );
    }

}
