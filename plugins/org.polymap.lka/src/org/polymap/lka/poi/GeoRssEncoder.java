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

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.StringWriter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.json.JSONException;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.module.georss.GeoRSSModule;
import com.sun.syndication.feed.module.georss.SimpleModuleImpl;
import com.sun.syndication.feed.module.georss.geometries.Position;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import org.polymap.core.data.pipeline.PipelineProcessor;
import org.polymap.geocoder.Address;

/**
 * This stream encodes {@link SimpleFeature} and {@link Document} objects into
 * GeoRSS stream. Later this may also extend to be a {@link PipelineProcessor}
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class GeoRssEncoder
        extends DataOutputStream
        //extends ObjectOutputStream
        implements ObjectOutput {

    private static final Log log = LogFactory.getLog( GeoRssEncoder.class );

    private static final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory( null );

    private boolean                     streamStarted;
    
    private CoordinateReferenceSystem   dataCRS, worldCRS;
    
    private MathTransform               transform;

    private List<SyndEntry>             feedEntries;
    
    private String                      baseURL;
    

    public GeoRssEncoder( OutputStream out, CoordinateReferenceSystem worldCRS,
            String baseURL ) throws IOException {
        super( out );
        this.worldCRS = worldCRS;
        this.baseURL = baseURL;
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
                encodeDocument( (SearchResult)obj );
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
        feedEntries = new LinkedList();
    }
    
    
    private boolean aboutToFlush = false;
    
    public synchronized void flush()
            throws IOException {
        if (aboutToFlush) {
            super.flush();
            return;
        }
        aboutToFlush = true;
        log.debug( "flush(): ..." );

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType( "rss_2.0" );

        feed.setTitle( "Mittelsachsen-Atlas" );
        feed.setLink( "http://www.mittelsachsen-atlas.de/" );
        feed.setDescription( "Daten von mittelsachsen-atlas.de" );

        feed.setEntries( feedEntries );
        WireFeed wiredFeed = feed.createWireFeed();
        
        SyndFeedOutput output = new SyndFeedOutput();
        try {
            //output.output( feed, new OutputStreamWriter( out, "ISO-8859-1" ) );

            StringWriter buf = new StringWriter();
            output.output( feed, buf );
            buf.flush();
            write( buf.toString().getBytes( "ISO-8859-1" ) );
        } 
        catch (FeedException e) {
            log.warn( "unhandled: ", e );
        }

        aboutToFlush = false;
        super.flush();
//        feedEntries.clear();
    }


    protected void encodeFeature( SimpleFeature feature ) 
    throws FactoryException {
        //
        CoordinateReferenceSystem featureCRS = feature.getFeatureType().getCoordinateReferenceSystem();
        if (!featureCRS.equals( dataCRS )) {
            boolean lenient = true; // allow for some error due to different datums
            transform = CRS.findMathTransform( dataCRS, worldCRS, lenient );
        }

        // entry
        SyndEntryImpl entry = new SyndEntryImpl();
        entry.setTitle( "Entry" );
        entry.setLink( "http://polymap.org/" );
        entry.setPublishedDate( new Date() );

        // description
        StringBuffer buf = new StringBuffer( 256 );
        Collection<Property> props = feature.getProperties();
        Point point = null;
        for (Property prop : props) {
            //log.info( prop.getName() + ": " + prop.getValue().getClass() );
            if (prop.getValue().getClass().equals( Point.class )) {
                point = (Point)prop.getValue();
            }
            else {
                String value = prop.getValue().toString();
                if (value != null && value.length() > 0) {
                    buf.append( "<b>" ).append( prop.getName() ).append( "</b>" );
                    buf.append( ": " ).append( StringEscapeUtils.escapeHtml( value ) );
                    buf.append( "<br/>" );
                }
            }
        }
        SyndContentImpl description = new SyndContentImpl();
        description.setType( "text/html" );
        description.setValue( buf.toString() );
        entry.setDescription( description );

        GeoRSSModule module = new SimpleModuleImpl();
        //GeometryAttribute geom = feature.getDefaultGeometryProperty();
        //geoRSSModule.setPosition( new Position( 54.2, 12.4 ) );

        // transform
        //point = (Point)JTS.transform( point, transform);

        module.setPosition( new Position( point.getY(), point.getX() ) );
        entry.getModules().add( module );

        feedEntries.add( entry );
        log.debug( "Feed entry added: " + entry );
    }

    
    protected void encodeDocument( SearchResult obj ) 
    throws FactoryException, JSONException, MismatchedDimensionException, TransformException {
        log.debug( "encoding: " + obj.getTitle() );

        // entry
        SyndEntryImpl entry = new SyndEntryImpl();
        entry.setTitle( StringEscapeUtils.escapeHtml( obj.getTitle() ) );
        entry.setLink( StringEscapeUtils.escapeHtml( baseURL + "?search=" + obj.getTitle() ) );
        entry.setPublishedDate( new Date() );

        // address
        Address address = obj.getAddress();
        StringBuffer addressBuf = new StringBuffer( 256 );
        if (address != null && address.getStreet() != null) {
            // postal code
            String postalCode = StringUtils.contains( address.getPostalCode(), "-" )
                    ? StringUtils.substringAfterLast( address.getPostalCode(), "-" )
                    : address.getPostalCode();
            
            addressBuf.append( "<p><em>" ).append( StringEscapeUtils.escapeHtml( address.getStreet() ) )
                    .append( " " ).append( StringEscapeUtils.escapeHtml( address.getNumber() ) )
                    .append( "<br/>" )
                    // remove country code from postal code
                    .append( StringEscapeUtils.escapeHtml( postalCode ) )
                    .append( " " ).append( StringEscapeUtils.escapeHtml( address.getCity() ) )
                    .append( "</em></p>" );
        }

        // position
        CoordinateReferenceSystem featureCRS = CRS.decode( obj.getPosition().srs );
        if (!featureCRS.equals( dataCRS )) {
            transform = CRS.findMathTransform( featureCRS, worldCRS, true );
            dataCRS = featureCRS;
        }
        double x = obj.getPosition().x;
        double y = obj.getPosition().y;
        // FIXME EPSG:900913 offset
        if (obj.getPosition().srs.equals( "EPSG:31468" )) {
            x = obj.getPosition().x - 113;
            y = obj.getPosition().y - 200;
        }
        Point point = gf.createPoint( new Coordinate( x, y ) );
        point = transform != null ? (Point)JTS.transform( point, transform ) : point;
        log.debug( "    transformed: " + point );
        Position pos = new Position( point.getY(), point.getX() );
        
        // description
        StringBuffer buf = new StringBuffer( 256 );
        buf.append( addressBuf );
        for (String field : obj.getFieldNames()) {
            log.debug( "    field: " + field );
            String value = obj.getField( field );
            if (value != null && value.length() > 0) {
                buf.append( "<nobr><b>" ).append( StringEscapeUtils.escapeHtml( StringUtils.capitalize( field ) ) ).append( "</b>" );
                // URL?
                if (value.contains( "www." ) || value.contains( ".de" ) || value.contains( ".com" ) || value.contains( "http://" )) {
                    buf.append( ": " )
                        .append( "<a href=\"" ).append( value ).append( "\" target=\"atlas_content\">" ) 
                        .append( StringEscapeUtils.escapeHtml( value ) )
                        .append( "</a>" ); 
                }
                else {
                    buf.append( ": " ).append( StringEscapeUtils.escapeHtml( value ) );
                }
                buf.append( "</nobr><br/>" );
            }
        }
        SyndContentImpl description = new SyndContentImpl();
        description.setType( "text/html" );
        description.setValue( buf.toString() );
        entry.setDescription(description);

        GeoRSSModule module = new SimpleModuleImpl();
        //GeometryAttribute geom = feature.getDefaultGeometryProperty();
        //geoRSSModule.setPosition( new Position( 54.2, 12.4 ) );

        // transform
        //point = (Point)JTS.transform( point, transform);

        module.setPosition( pos );
        entry.getModules().add( module );

        feedEntries.add( entry );
        //log.info( "Feed entry added: " + entry );
    }
    
}
