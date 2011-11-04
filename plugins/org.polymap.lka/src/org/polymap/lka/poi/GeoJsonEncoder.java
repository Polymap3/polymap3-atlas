/* 
 * polymap.org
 * Copyright 2011, Falko Bräutigam, and individual contributors as indicated
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
 */
package org.polymap.lka.poi;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.NamedIdentifier;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;

import com.vividsolutions.jts.geom.Geometry;

import org.polymap.core.data.pipeline.PipelineProcessor;

/**
 * This stream encodes {@link SimpleFeature} and {@link Document} objects into
 * GeoJSON stream. Later this may also extend to be a {@link PipelineProcessor}
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @since 1.0
 */
public class GeoJsonEncoder
        extends DataOutputStream
        implements ObjectOutput {

    private static final Log log = LogFactory.getLog( GeoJsonEncoder.class );

    private String                      typeName = "Orte";
    
    private boolean                     streamStarted;
    
    private CoordinateReferenceSystem   worldCRS;
    
    /** The accumulator for the encoded features until they are flushed. */
    private FeatureCollection<SimpleFeatureType, SimpleFeature> features;

    
    public GeoJsonEncoder( OutputStream out, CoordinateReferenceSystem worldCRS )
            throws Exception {
        super( out );
        this.worldCRS = worldCRS;
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

        this.features = FeatureCollections.newCollection();
    }
    
    
    private boolean aboutToFlush = false;
    
    public synchronized void flush()
            throws IOException {
        if (aboutToFlush) {
            return;
        }
        aboutToFlush = true;
        log.info( "flush(): ..." );

        try {
            FeatureJSON fjson = new FeatureJSON();
            fjson.setEncodeFeatureBounds( false );
            fjson.setEncodeFeatureCRS( false );
            
            LinkedHashMap obj = new LinkedHashMap();
            obj.put( "type", "FeatureCollection" );
            obj.put( "features", new CollectionEncoder( fjson, null ) );
            obj.put( "crs", new CRSEncoder( fjson, worldCRS ) );
            
            GeoJSONUtil.encode( obj, new OutputStreamWriter( this, "UTF-8" ) );
            
            features.clear();
        }
        finally {
            aboutToFlush = false;
            super.flush();
        }

    }


    protected void encodeFeature( SimpleFeature feature ) 
    throws FactoryException {
        features.add( feature );
        log.debug( "Feature added: " + feature );
    }

    
    protected void encodeSearchResult( SearchResult obj ) 
    throws FactoryException, JSONException, MismatchedDimensionException, TransformException {

        log.debug( "data SRS: " + obj.getSRS() );
        CoordinateReferenceSystem dataCRS = CRS.decode( obj.getSRS() );
        boolean lenient = true; // allow for some error due to different datums
        MathTransform transform = CRS.findMathTransform( dataCRS, worldCRS, lenient );
        //log.debug( "Transform: " + transform );
        
        // type
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName( typeName );
        ftb.add( "geometry", obj.getGeom().getClass(), worldCRS );

        ftb.add( "title", String.class );
        // address
        if (obj.getAddress() != null) {
            ftb.add( "address", String.class );
        }
        // fields
        for (String field : obj.getFieldNames()) {
            String value = obj.getField( field );
            if (value != null && value.length() > 0) {
                ftb.add( field, String.class );
            }
        }
        SimpleFeatureType featuresType = ftb.buildFeatureType();

        // feature
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder( featuresType );

        Geometry geom = obj.getGeom();
        log.debug( "    orig: " + geom );
        geom = transform != null ? JTS.transform( geom, transform ) : geom;
        fb.set( "geometry", geom );
        log.debug( "    transformed: " + geom );
        
        fb.set( "title", /*JSONObject.escape(*/ obj.getTitle() );
        // address
        if (obj.getAddress() != null) {
            //fb.set( "address", JSONObject.escape( obj.getAddress().toJSON() ) );
            fb.set( "address", obj.getAddress().toJSON() );
        }
        // fields
        for (String field : obj.getFieldNames()) {
            String value = obj.getField( field );
            if (value != null && value.length() > 0) {
                // umlauts
                //value = StringEscapeUtils.escapeHtml( value );
                // does not seem to be done by gt-geojson
                //value = JSONObject.escape( value );
                fb.set( field, value );
            }
        }

        encodeFeature( fb.buildFeature( null ) );
    }
    
    
    /**
     * 
     */
    class CRSEncoder
            implements JSONStreamAware {
        
        private FeatureJSON                 fjson;

        private CoordinateReferenceSystem   crs;

    
        public CRSEncoder( FeatureJSON fjson, CoordinateReferenceSystem crs ) {
            super();
            this.fjson = fjson;
            this.crs = crs;
        }
        
        public void writeJSONString( Writer out )
                throws IOException {
            // this is code is from the 'old' JSONServer
            Set<ReferenceIdentifier> ids = worldCRS.getIdentifiers();
            // WKT defined crs might not have identifiers at all
            if (ids != null && ids.size() > 0) {
                NamedIdentifier namedIdent = (NamedIdentifier)ids.iterator().next();
                String csStr = namedIdent.getCodeSpace().toUpperCase();
                
                if (csStr.equals( "EPSG" )) {
                    JSONObject obj = new JSONObject();
                    obj.put( "type", csStr );

                    JSONObject props = new JSONObject();
                    obj.put( "properties", props );
                    props.put( "code", namedIdent.getCode() );
                    
                    obj.writeJSONString( out );
                }
                else {
                    log.warn( "Non-EPSG code not supported: " + csStr );
                }
            }
            else {
                log.warn( "No CRS identifier for CRS: " + worldCRS );
            }

            // this is the original code which throws exception for lookupIdentifier()
//            JSONObject obj = new JSONObject();
//            obj.put("type", "name");
//            
//            Map<String,Object> props = new LinkedHashMap<String, Object>();
//            try {
//                props.put("name", CRS.lookupIdentifier(crs, true));
//            } 
//            catch (FactoryException e) {
//                throw (IOException) new IOException("Error looking up crs identifier").initCause(e);
//            }
//            
//            obj.put("properties", props);
//            return obj;
        }
    }
    

    /**
     * 
     */
    class CollectionEncoder 
            implements JSONStreamAware {

        private FeatureJSON                 fjson;
        
        private MathTransform               transform;
        
        private SimpleFeatureType           transformedSchema;
        
        private CountingOutputStream        byteCounter;
        
        
        public CollectionEncoder( FeatureJSON fjson, CountingOutputStream byteCounter ) {
            this.fjson = fjson;
            this.byteCounter = byteCounter;
        }
        
        public void writeJSONString( Writer out ) 
        throws IOException {
            if (features == null) {
                return;
            }
            
            out.write( "[" );
            
            try {
                int featureCount = 0;
                Iterator<SimpleFeature> it = features.iterator();
                if (it.hasNext()) {
                    fjson.writeFeature( it.next(), out );
                    featureCount++;
                }
                
                while (it.hasNext()) {
//                    // check byte limit
//                    if (byteCounter.getCount() > maxBytes) {
//                        log.warn( "Byte limit reached. Features encoded: " + featureCount );
//                        display.asyncExec( new Runnable() {
//                            public void run() {
//                                MessageDialog.openInformation(
//                                        PolymapWorkbench.getShellToParentOn(),
//                                        "Information",
//                                        "Es können nicht alle Objekte angezeigt werden.\nWenn möglich, dann schränken Sie die Auswahl weiter ein." );
//                            }
//                        } );
//                        break;
//                    }
                    // encode feature
                    out.write( "," );
                    fjson.writeFeature( it.next(), out );
                }
            }
            catch (IOException e) {
                throw e;
            }
            catch (Exception e) {
                throw new IOException( e );
            }

            out.write( "]" );
        }
        
//        /**
//         * Transform the given feature: reproject CRS; (XXX strip properties?)
//         * 
//         * @throws FactoryException 
//         * @throws TransformException 
//         * @throws MismatchedDimensionException 
//         */
//        private SimpleFeature transform( SimpleFeature feature ) 
//        throws FactoryException, MismatchedDimensionException, TransformException {
//            SimpleFeatureType schema = feature.getFeatureType();
//            CoordinateReferenceSystem featureCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
//            
//            if (transform == null && 
//                    featureCRS != null && worldCRS != null &&
//                    !worldCRS.equals( featureCRS )) {
//                
//                transform = CRS.findMathTransform( featureCRS, worldCRS, true );
//            }
//            
////            if (transformedSchema == null) {
////                SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
////                typeBuilder.setCRS( mapCRS );
////                
////                for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
////                    typeBuilder.
////                }
////            }
////            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder();
//            
//            Object[] attributes = feature.getAttributes().toArray();
//            for (int i=0; i < attributes.length; i++) {
//                if (attributes[i] instanceof Geometry) {
//                    attributes[i] = transform != null
//                            ? JTS.transform( (Geometry) attributes[i], transform )
//                            : attributes[i];
//                }
//            }
//
//            return SimpleFeatureBuilder.build( schema, attributes, feature.getID() );
//        }
    }

}
