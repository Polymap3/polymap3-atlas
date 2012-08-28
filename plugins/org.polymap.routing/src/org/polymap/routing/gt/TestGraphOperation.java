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

import java.util.concurrent.atomic.AtomicInteger;

import net.refractions.udig.catalog.memory.ActiveMemoryDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.graph.path.AStarShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.AStarIterator;
import org.geotools.graph.traverse.standard.AStarIterator.AStarFunctions;
import org.geotools.graph.traverse.standard.AStarIterator.AStarNode;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.polymap.core.data.PipelineFeatureSource;
import org.polymap.core.data.operation.DefaultFeatureOperation;
import org.polymap.core.data.util.ProgressListenerAdaptor;
import org.polymap.core.project.ILayer;
import org.polymap.core.project.ProjectRepository;
import org.polymap.core.runtime.Timer;

/**
 * This Operation builds a graph based on the MultiLineString or LineString
 * geometries of the features of the layer using the great {@link org.geotools.graph}
 * package.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@SuppressWarnings("restriction")
public class TestGraphOperation
        extends DefaultFeatureOperation {

    private static Log log = LogFactory.getLog( TestGraphOperation.class );

    
    public Status execute( final IProgressMonitor monitor )
    throws Exception {
        monitor.beginTask( "Building graph", 15 );
        final RoadsGraphGenerator generator = new RoadsGraphGenerator();

        //wrap it in a feature graph generator
        //final FeatureGraphGenerator featureGen = new FeatureGraphGenerator( lineStringGen );

        // generate graph ***
        Timer timer = new Timer();
        final SubProgressMonitor sub = new SubProgressMonitor( monitor, 5, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK );
        sub.beginTask( "RoadsGraphGenerator - ", context.features().size() );

        final AtomicInteger count = new AtomicInteger();
        context.features().accepts( new FeatureVisitor() {
            public void visit( Feature feature ) {
                generator.add( ((SimpleFeature)feature).getDefaultGeometry() );
                if (count.incrementAndGet() % 1000 == 0) {
                    sub.subTask( "Features added: " + count );
                    sub.worked( 1000 );
                }
            }
        }, new ProgressListenerAdaptor( sub ) );
        
        Graph graph = generator.getGraph();
        log.info( "Graph: " + graph.getNodes().size() + " / " + graph.getEdges().size() + " (" + timer.elapsedTime() + "ms)" );
        
        // optimize
        timer.start();
        generator.optimize( new SubProgressMonitor( monitor, 5, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK ) );
        log.info( "Graph: " + graph.getNodes().size() + " / " + graph.getEdges().size() + " (" + timer.elapsedTime() + "ms)" );

        //
        buildTempLayer( graph, monitor );
        
        return Status.OK;
    }
    
    
    protected void buildTempLayer( Graph graph, IProgressMonitor monitor )
    throws Exception {
//        ILayer layer = ProjectRepository.instance().newEntity( ILayer.class, null );
//        layer.setLabel( "Graph" );
//        layer.setOrderKey( 100 );
//        layer.setOpacity( 100 );
//
//        MemoryServiceImpl service = new MemoryServiceImpl( new URL( "http://polymap.org/graph") );
//        ActiveMemoryDataStore ds = service.resolve( ActiveMemoryDataStore.class, null );
//        
//        String graphTypeName = "graph";
//        MemoryGeoResourceImpl geores = new MemoryGeoResourceImpl( graphTypeName, service );
//        layer.setGeoResource( geores );
//        
//        IMap map = context.adapt( ILayer.class ).getMap();
//        map.addLayer( layer );

        ILayer layer = ProjectRepository.instance().newTempLayer( "graph", context.adapt( ILayer.class ).getMap() );

        ActiveMemoryDataStore ds = layer.getGeoResource().resolve( ActiveMemoryDataStore.class, null );
        
        // graph schema ***
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.add( "geom", LineString.class, context.featureSource().getSchema().getCoordinateReferenceSystem() );
        tb.setName( "graph" );
        
        SimpleFeatureType graphSchema = tb.buildFeatureType();
        ds.createSchema( graphSchema );
        
        // add edges to layer *** 
        FeatureCollection features = FeatureCollections.newCollection();

        GeometryFactory geomFactory = new GeometryFactory();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder( graphSchema );
        
        SubProgressMonitor sub2 = new SubProgressMonitor( monitor, 5 );
        sub2.beginTask( "Building graph features - ", graph.getEdges().size() * 2 );

        for (Object edge : graph.getEdges()) {
            Point a = (Point)((Edge)edge).getNodeA().getObject();
            Point b = (Point)((Edge)edge).getNodeB().getObject();
            LineString graphLine = geomFactory.createLineString( new Coordinate[] { 
                    a.getCoordinate(), b.getCoordinate() } );
   
            fb.set( "geom", graphLine );
            SimpleFeature feature = fb.buildFeature( null );
            features.add( feature );
            
            sub2.worked( 1 );
        }
        
        sub2.subTask( "Adding to FeatureStore" );
        PipelineFeatureSource fs = PipelineFeatureSource.forLayer( layer, true );
        fs.addFeatures( features, new ProgressListenerAdaptor( sub2 ));
    }
    
    
    protected void routingTest( Graph graph )
    throws Exception {
        Object[] nodes = graph.getNodes().toArray();
        Node start = (Node)nodes[ 0 ];
        Node end = (Node)nodes[ nodes.length-1 ];
        log.info( "Start: " + start + ", End: " + end );

        Timer timer = new Timer();
        final AtomicInteger count = new AtomicInteger();
        AStarFunctions weighter = new AStarIterator.AStarFunctions( end ) {
            public double h( Node n ) {
                return 0;
            }
            public double cost( AStarNode n1, AStarNode n2 ) {
                count.incrementAndGet();
                return 0;
            }
        };
        AStarShortestPathFinder finder = new AStarShortestPathFinder( graph, start, end, weighter );
        finder.calculate();
        Path path = finder.getPath();
        log.info( "Path size: " + path.size() );
        log.info( "Cost count: " + count.get() );
        log.info( "Time: " + timer.elapsedTime() + "ms" );
    }

}
