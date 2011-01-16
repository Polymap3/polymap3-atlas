/* 
 * polymap.org
 * Copyright 2009, Polymap GmbH, and individual contributors as indicated
 * by the @authors tag.
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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * $Id$
 */
package org.polymap.lka.poi;

import java.util.ArrayList;
import java.util.List;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.referencing.CRS;
import org.geotools.xml.Encoder;
import org.json.JSONException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import org.polymap.core.data.pipeline.PipelineProcessor;
import org.polymap.lka.poi.SearchResult.Position;

/**
 * This stream encodes {@link SimpleFeature} and {@link Document} objects into
 * KML stream. Later this may also extend to be a {@link PipelineProcessor}
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class KMLEncoder
        extends DataOutputStream
        implements ObjectOutput {

    private static final Log log = LogFactory.getLog( KMLEncoder.class );

    private static final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory( null );

    private String                      typeName = "Orte";
    
    private boolean                     streamStarted;
    
    /** The CRS used for encoding document. */
    private CoordinateReferenceSystem   dataCRS;
    
    private CoordinateReferenceSystem   worldCRS = CRS.decode( "EPSG:4326" );  //DefaultGeographicCRS.WGS84;
    
    private MathTransform               transform;

    private List<SimpleFeature>         features = new ArrayList( 128 );
    
    private SimpleFeatureType           featuresType;


    public KMLEncoder( OutputStream out, CoordinateReferenceSystem dataCRS )
            throws Exception {
        super( out );
        this.dataCRS = dataCRS;
        //worldCRS = CRS.decode( "EPSG:4326" );
    }

    
    public void writeObject( Object obj )
            throws IOException {
        if (!streamStarted) {
            startStream();
        }
        try {
            if (obj instanceof SimpleFeature) {
                encodeFeature( (SimpleFeature)obj );
            }
            else if (obj instanceof SearchResult) {
                encodeSearchResult( (SearchResult)obj );
            }
            else {
                throw new RuntimeException( "Unsupported object type: " + obj );
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException( e.getMessage(), e );
        }
    }

    
    protected void startStream() {
        assert !streamStarted;
        streamStarted = true;
    }
    
    
    private boolean aboutToFlush = false;
    
    public synchronized void flush()
            throws IOException {
        if (aboutToFlush) {
            return;
        }
        aboutToFlush = true;
        log.info( "flush(): ..." );

        //
//        CoordinateReferenceSystem featureCRS = featuresType.getCoordinateReferenceSystem();
//        if (!featureCRS.equals( dataCRS )) {
//            boolean lenient = true; // allow for some error due to different datums
//            transform = CRS.findMathTransform( dataCRS, worldCRS, lenient );
//        }

        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = FeatureCollections.newCollection();
        for (SimpleFeature feature : features) {
            fc.add( feature );
        }

        Encoder encoder = new Encoder( new KMLConfiguration() );
        encoder.setIndenting( true );
        encoder.setEncoding( Charset.forName( "UTF-8" ) );
        encoder.setNamespaceAware( false );
        encoder.encode( fc, KML.kml, this );

        aboutToFlush = false;
        super.flush();
        features = null;
    }


    protected void encodeFeature( SimpleFeature feature ) 
    throws FactoryException {
        if (featuresType == null) {
            featuresType = feature.getFeatureType();
        }
        else {
            assert featuresType.equals( feature.getFeatureType() );
        }
        features.add( feature );
        log.debug( "Feature added: " + feature );
    }

    
    protected void encodeSearchResult( SearchResult obj ) 
    throws FactoryException, JSONException, MismatchedDimensionException, TransformException {

        Position pos = obj.getPosition();
        log.debug( "    src coords: " + pos );
        dataCRS = CRS.decode( pos.srs );
        boolean lenient = true; // allow for some error due to different datums
        transform = CRS.findMathTransform( dataCRS, worldCRS, lenient );
        //log.info( "Transform: " + transform );
        
        // type
        if (featuresType == null) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.setName( typeName );
            tb.add( "geometry", Point.class, worldCRS );
            tb.add( "name", String.class );
            tb.add( "description", String.class );
            featuresType = tb.buildFeatureType();
        }

        // feature
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder( featuresType );
        fb.set( "name", obj.getTitle() );
        fb.set( "description", "Description." );

        Point point = gf.createPoint( new Coordinate( pos.x, pos.y, 0 ) );
        point = (Point)(transform != null ? JTS.transform( point, transform ) : point);
        fb.set( "geometry", point );
        log.debug( "    transformed: " + point );
        
//        for (String name : obj.getFieldNames()) {
//            //log.info( "    field: " + field );
//            fb.set( name, obj.getField( name ) );
//        }

        encodeFeature( fb.buildFeature( null ) );
    }
    
}
