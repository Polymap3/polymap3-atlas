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
package org.polymap.geocoder.google;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONException;
import org.json.JSONObject;

import com.vividsolutions.jts.geom.Coordinate;

import org.polymap.geocoder.Address;
import org.polymap.geocoder.Geocoder;

public class GoogleGeocoder
        extends Geocoder {
    
    static final org.apache.commons.logging.Log log = 
        org.apache.commons.logging.LogFactory.getLog( GoogleGeocoder.class );

    public static final String      BASE_URL = "http://maps.google.de/maps/geo";

    private HttpClient              httpClient;
    
    private String                  apiKey;


    public GoogleGeocoder( String apiKey ) {
        this.apiKey = apiKey;
        
        this.httpClient = new HttpClient();
        if (System.getProperty( "http.proxySet", "false" ).equalsIgnoreCase( "true" )) {
            String proxyHost = System.getProperty( "http.proxyHost", null );
            int proxyPort = Integer.parseInt( System.getProperty( "http.proxyPort", "3128" ) );
            httpClient.getHostConfiguration().setProxy( proxyHost, proxyPort );
        }
    }


    public List<Address> find( Address fragment, int maxResults )
            throws Exception {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    public List<Address> find( String addressString, int maxResults )
            throws Exception {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    /**
     * Creates a full address string to be used as param for the geocode
     * methods.
     * 
     * @throws IOException
     * @throws HttpException
     */
    public String address( String street, String city, String province, String country )
    throws HttpException, IOException {
        // build address string
        StringBuffer address = new StringBuffer( 128 );
        address.append( street != null ? street : "" );
        address.append( ',' ).append( city != null ? city : "" );
        address.append( ',' ).append( province != null ? province : "" );
        address.append( ',' ).append( country != null ? country : "" );
        return address.toString(); 
    }
        
        
    /**
     * 
     * @param address Complete address string build by
     *        {@link #address(String, String, String, String)}
     * @param maxResult Return up to maxResults addresses.
     * @return
     * @throws HttpException
     * @throws IOException
     * @throws JSONException 
     */
    public Address[] geocode( int maxResults, String address )
    throws HttpException, IOException, JSONException {
        GetMethod m = new GetMethod( BASE_URL );

        HttpMethodParams methodParams = m.getParams();
        methodParams.setParameter( HttpMethodParams.SO_TIMEOUT, new Integer( 10000 ) );
        methodParams.setContentCharset( "ISO-8859-1" );

        // request params
        m.setQueryString( new NameValuePair[] {
                new NameValuePair( "q", address ),    
                new NameValuePair( "output", "json" ),    
                new NameValuePair( "key", apiKey )    
        });

        // request
        long start = System.currentTimeMillis();
        int statusCode = httpClient.executeMethod( m );
        if (statusCode != HttpStatus.SC_OK) {
            throw new HttpException( "Method failed: " + m.getStatusLine() );
        }
        log.info( "Request time: " + (System.currentTimeMillis()-start) + "ms" );

        // parse response
        JSONObject json = new JSONObject( m.getResponseBodyAsString() );
        log.debug( "JSON:: " + json.toString( 4 ) );

        ArrayList result = new ArrayList();
        for (int i=0; i<maxResults; i++) {
            JSONObject placemark;
            try {
                placemark = (JSONObject)query( json, "Placemark[" + i + "]" );
            }
            catch (RuntimeException e) {
                // ArrayIndexOutOfBounds: stop iterating
                break;
            }

            final String commonId = "AddressDetails.Country.AdministrativeArea";

            double lat = Double.parseDouble( query( placemark, "Point.coordinates[1]" ).toString() );
            double lon = Double.parseDouble( query( placemark, "Point.coordinates[0]" ).toString() );
            Coordinate coord = new Coordinate( lon, lat );

//            result.add( new Address(
//                    //query( placemark, "address" ).toString(),
//                    query( placemark,  commonId + ".SubAdministrativeArea.Locality.PostalCode.PostalCodeNumber" ).toString(),
//                    query( placemark,  commonId + ".SubAdministrativeArea.Locality.Thoroughfare.ThoroughfareName" ).toString(),
//            query( placemark,  commonId + ".SubAdministrativeArea.SubAdministrativeAreaName" ).toString() );
//            query( placemark, commonId + ".AdministrativeAreaName" ).toString(),
//            new Point( coord, ) ) );
        }
        return (Address[])result.toArray( new Address[result.size()] );
    }


    /* 
     * Allow query for json nested objects, ie. Placemark[0].address 
     */
    private Object query( JSONObject json, String query ) {
        try {
            String[] keys = query.split( "\\." );
            Object r = queryHelper( json, keys[0] );
            for (int i=1; i<keys.length; i++) {
                //r = queryHelper( json.fromObject( r ), keys[i] );
                throw new RuntimeException( "not yet implemented" );
            }
            return r;
        }
        catch (JSONException e) {
            return "";
        }
    }


    /* 
     * Help in query array objects: Placemark[0] 
     */
    private Object queryHelper( JSONObject json, String query ) 
    throws JSONException {
        int openIndex = query.indexOf( '[' );
        int endIndex = query.indexOf( ']' );
        if (openIndex > 0) {
            String key = query.substring( 0, openIndex );
            int index = Integer.parseInt( query.substring( openIndex + 1, endIndex ) );
            return json.getJSONArray( key ).get( index );
        }
        return json.get( query );
    }

}
