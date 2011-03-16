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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.polymap.core.data.pipeline.PipelineProcessor;

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

        Geometry geom = obj.getGeom();
        log.debug( "    src coords: " + geom );
        dataCRS = CRS.decode( obj.getSRS() );
        boolean lenient = true; // allow for some error due to different datums
        transform = CRS.findMathTransform( dataCRS, worldCRS, lenient );
        //log.info( "Transform: " + transform );
        geom = transform != null ? JTS.transform( geom, transform ) : geom;
        
        // type
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName( typeName );
        ftb.add( "geometry", obj.getGeom().getClass(), worldCRS );

        ftb.add( "title", String.class );
        for (String field : obj.getFieldNames()) {
            log.debug( "    field: " + field );
            
            String value = obj.getField( field );
            if (value != null && value.length() > 0) {
                ftb.add( StringUtils.capitalize( field ), String.class );
            }
        }
        featuresType = ftb.buildFeatureType();

        // feature
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder( featuresType );

        fb.set( "geometry", geom );
        
        fb.set( "title", obj.getTitle() );
        for (String field : obj.getFieldNames()) {
            String value = obj.getField( field );
            if (value != null && value.length() > 0) {
                fb.set( StringUtils.capitalize( field ), value );
            }
        }

        encodeFeature( fb.buildFeature( null ) );
    }
    
}
