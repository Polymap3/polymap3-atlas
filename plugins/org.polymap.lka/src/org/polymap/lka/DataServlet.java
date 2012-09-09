/*                                                                                           
 * polymap.org                                                                               
 * Copyright 2010, Polymap GmbH, and individual contributors as indicated                    
 * by the @authors tag.                                                                      
 *                                                                                           
 * This is free software; you can redistribute it and/or modify it                           
 * under the terms of the GNU Lesser General Public License as                               
 * published by the Free Software Foundation; either version 3 of                            
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
 */
package org.polymap.lka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.lf5.util.StreamUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.identity.FeatureId;

import com.google.common.collect.Iterables;
import com.twicom.qdparser.Element;
import com.twicom.qdparser.TaggedElement;
import com.twicom.qdparser.TextElement;
import com.twicom.qdparser.XMLReader;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.polymap.core.data.PipelineFeatureSource;
import org.polymap.core.data.pipeline.PipelineIncubationException;
import org.polymap.core.project.ILayer;
import org.polymap.core.project.IMap;
import org.polymap.core.project.ProjectRepository;
import org.polymap.geocoder.Address;
import org.polymap.geocoder.Geocoder;

/**
 * This servlet receives/provides the data from/o the POI form.
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class DataServlet
        extends HttpServlet {

    private static final Log  log = LogFactory.getLog( DataServlet.class );

    public DataServlet() throws Exception {
        log.info( "Initializing DataServlet ..." );
    }


    /**
     * 
     */
    public void doPost( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException {
        log.info( "POST-Request: pathInfo= " + request.getPathInfo() );
        
        try {
            String content = new String( StreamUtils.getBytes( request.getInputStream() ), "UTF8" );
            log.info( "Content : " + content );

            // adresses
            if (request.getPathInfo().startsWith( "/addresses" )) {
                TaggedElement elm = XMLReader.parse( content );
                printElm( elm, "" );
                
                findAddresses( elm, response );
            }
            // post new feature
            else {
                JSONObject json = request.getPathInfo().endsWith( ".xml" )
                ? convertXML( XMLReader.parse( content ) )
                        : new JSONObject( content );
                log.info( "   json: " + json.toString( 4 ) );

                createFeature( json, new Path( request.getPathInfo() ), findClientIP( request ) );
            }
        }
        catch (Exception e) {
            log.error( e.getLocalizedMessage(), e );
            //throw new ServletException( e.getLocalizedMessage() );
            response.setStatus( 409 );
            response.getWriter().append( e.getLocalizedMessage() ).flush();
            
            //sendError( 409, e.getLocalizedMessage() );
        }
    }

    
    /**
     * 
     */
    protected String findClientIP( HttpServletRequest request ) {
        String forwarded = request.getHeader( "X-Forwarded-For" );
        if (forwarded != null) {
            return forwarded;
        }
        else {
            return request.getRemoteHost();
        }
    }


    public void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException {
	   	log.info( "Request: " + request.getQueryString() );
 	}
   

    protected void printElm( TaggedElement elm, String indent ) {
        System.out.println( indent + "tag = " + elm.getTag() );
        for (Element child : elm.getElements()) {
            if (child instanceof TaggedElement) {
                printElm( (TaggedElement)child, indent + "  " );
            }
            if (child instanceof TextElement) {
                System.out.println( "  " + indent + "cdata = " + ((TextElement)child).toString() );
            }
        }
    }

    
    /**
     * Convert the given XML element into JSON.
     */
    protected JSONObject convertXML( TaggedElement elm ) 
    throws Exception {
        JSONObject result = new JSONObject();
        for (Element child : elm.getElements()) {
            String name = ((TaggedElement)child).getName();
            Element text = ((TaggedElement)child).getChild( 0 );
            if (text != null) {
                result.append( name, text.toString() );
            }
        }
        return result;
    }
    
    
    /**
     * Creates a {@link SimpleFeature} for the given XML element.
     * 
     * @param elm
     * @param path 
     * @param string 
     * @throws IOException If the given path does no exist. 
     * @throws PipelineIncubationException
     * @throws Exception If address search went wrong. 
     */
    protected void createFeature( JSONObject elm, IPath path, String clientIP ) 
    throws Exception {
        log.info( "Creating feature: clientIP=" + clientIP );
        ILayer layer = findLayer( path );
        log.info( "    layer= " + layer );
        if (layer == null) {
            throw new IOException( "No such map/layer: " + path );
        }

        PipelineFeatureSource fs = PipelineFeatureSource.forLayer( layer, true );
        SimpleFeatureType type = fs.getSchema();
        
        // maps lower case field name to type
        Map<String,AttributeType> attrs = new HashMap();
        for (AttributeType attrType : type.getTypes()) {
            attrs.put( attrType.getName().getLocalPart().toLowerCase(), attrType );    
        }
        
        // build feature *** ***
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder( type );
        for (String name : JSONObject.getNames( elm )) {
            AttributeType attrType = attrs.get( name.toLowerCase() );
            if (attrType != null) {
                featureBuilder.set( attrType.getName(), elm.opt( name ) );
            }
        }
        
        // client IP
        AttributeType clientAttrType = attrs.get( "erfasser" );
        if (clientAttrType != null) {
            featureBuilder.set( clientAttrType.getName(), clientIP );
        }
        
        // geocoder
        Address search = new Address();
        for (String name : JSONObject.getNames( elm )) {
            Object value = elm.opt( name );
            if (value != null) {
                if (name.equalsIgnoreCase( "plz" )) {
                    search.setPostalCode( value.toString() );
                }
                else if (name.equalsIgnoreCase( "ort" )) {
                    search.setCity( value.toString() );
                }
                else if (name.equalsIgnoreCase( "strasse" )) {
                    search.setStreet( value.toString() );
                }
                else if (name.equalsIgnoreCase( "hnr" )) {
                    search.setNumber( value.toString() );
                }
            }
        }
        log.info( "    Address: " + search );
        Geocoder geocoder = Geocoder.instance();
        Address address = Iterables.getFirst( geocoder.find( search, 1 ), null );
        
        if (address == null) {
            throw new IOException( "Adresse ist nicht eindeutig." );
        }
        featureBuilder.set( type.getGeometryDescriptor().getLocalName(), address.getPoint() );

        // save feature *** ***
//        Transaction tx = new DefaultTransaction( "create" );
//        fs.setTransaction( tx );
        try {
            FeatureCollection<SimpleFeatureType, SimpleFeature> features = 
                    FeatureCollections.newCollection();
            SimpleFeature feature = featureBuilder.buildFeature( null );
            features.add( feature );
            List<FeatureId> fids = fs.addFeatures( features );
//            tx.commit();
            log.info( "    ### Feature created: " + fids.get( 0 ) );
        } 
        catch (Exception problem) {
            problem.printStackTrace();
//            tx.rollback();
        } 
        finally {
//            tx.close();
        }
    }
    
    
    /**
     *
     * @param path
     * @return The layer or null, if no layer is found for the given path.
     */
    protected ILayer findLayer( IPath path ) {
        IMap rootMap = ProjectRepository.instance().getRootMap();
        
        // crop file.ext
        if (!path.hasTrailingSeparator()) {
            path = path.removeLastSegments( 1 );
        }
        // mapsPath + layerName
        IPath mapsPath = path.removeLastSegments( 1 );
        String layerName = path.lastSegment();
        log.info( "maps=" + mapsPath + ", layer=" + layerName );
        
        // maps path
        IMap map = rootMap;
        for (int i=0; i<mapsPath.segmentCount(); i++) {
            String segment = path.segment( i );
            boolean found = false;
            for (IMap cursor : map.getMaps()) {
                if (cursor.getLabel().equalsIgnoreCase( segment )) {
                    map = cursor;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        // layer
        for (ILayer cursor : map.getLayers()) {
            if (cursor.getLabel().equalsIgnoreCase( layerName )) {
                return cursor;
            }
        }
        return null;
    }

    
    protected void findAddresses( TaggedElement elm, HttpServletResponse response ) 
    throws IOException {
        try {
            // decode search address
            Address search = new Address();
            for (Element child : elm.getElements()) {
                String name = ((TaggedElement)child).getName();
                Element text = ((TaggedElement)child).getChild( 0 );
                if (text != null) {
                    if (name.equalsIgnoreCase( "plz" )) {
                        search.setPostalCode( text.toString() );
                    }
                    else if (name.equalsIgnoreCase( "ort" )) {
                        search.setCity( text.toString() );
                    }
                    else if (name.equalsIgnoreCase( "strasse" )) {
                        search.setStreet( text.toString() );
                    }
                    else if (name.equalsIgnoreCase( "hnr" )) {
                        search.setNumber( text.toString() );
                    }
                }
            }
            // geocode
            log.info( "Search: " + search );
            Geocoder geocoder = Geocoder.instance();
            Iterable<Address> result = geocoder.find( search, 50 );
            
            // encode result addresses
            TaggedElement addressesElm = new TaggedElement( "addresses" );
            for (Address address : result) {
                log.info( "found: " + address );
                TaggedElement addressElm = new TaggedElement( "address" );
                
                TaggedElement fieldElm = new TaggedElement( "Ort" );
                fieldElm.add( address.getCity() );
                addressElm.add( fieldElm );                
                fieldElm = new TaggedElement( "PLZ" );
                fieldElm.add( address.getPostalCode() );
                addressElm.add( fieldElm );
                fieldElm = new TaggedElement( "Strasse" );
                fieldElm.add( address.getStreet() );
                addressElm.add( fieldElm );
                
                addressesElm.add( addressElm );
            }

            // write response
            log.info( "Out: " + addressesElm.toString( true ) );
            OutputStreamWriter out = new OutputStreamWriter( response.getOutputStream(), "UTF-8" );
            out.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
            out.write( addressesElm.toString() );
            out.flush();
        }
        catch (Exception e) {
            log.warn( "", e );
            response.getOutputStream().flush();
        }
    }

}
