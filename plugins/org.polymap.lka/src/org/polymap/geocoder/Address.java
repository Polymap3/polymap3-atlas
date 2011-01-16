/*
 * polymap.org Copyright 2009, Polymap GmbH, and individual contributors as
 * indicated by the @authors tag.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 * 
 * $Id$
 */
package org.polymap.geocoder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import java.lang.reflect.Field;

import org.json.JSONException;
import org.json.JSONObject;

import com.vividsolutions.jts.geom.Point;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class Address
        implements Cloneable {

    private String          street;

    private String          number;

    private String          postalCode;

    private String          city;

    private String          country;
    
    private Point           point;
    
    private float           score = -1;


    public static Address valueOf( String jsonString ) 
    throws JSONException {
        JSONObject json = new JSONObject( jsonString );
        
        Set<String> keys = new HashSet();
        for (Iterator it=json.keys(); it.hasNext(); ) {
            keys.add( (String)it.next() );
        }
        
        Address result = new Address();
        result.street = keys.contains( "street" ) ? json.optString( "street" ) : null;
        result.country = keys.contains( "country" ) ? json.optString( "country" ) : null;
        result.city = keys.contains( "city" ) ? json.optString( "city" ) : null;
        result.postalCode = keys.contains( "postalCode" ) ? json.optString( "postalCode" ) : null;
        result.number = keys.contains( "number" ) ? json.optString( "number" ) : null;
        return result;
    }
    
    public Address( String street, String number, String postalCode, String city, String country ) {
        this.street = street != null ? street.trim() : null;
        this.number = number != null ? number.trim() : null;
        this.postalCode = postalCode != null ? postalCode.trim() : null;
        this.city = city != null ? city.trim() : null;
        this.country = country != null ? country.trim() : country;
    }

    public Address() {
        // XXX Auto-generated constructor stub
    }

    public Address clone() {
    //throws CloneNotSupportedException {
        try {
            return (Address)super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException( e );
        }
    }
    
    public void set( String fieldName, String value ) 
    throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = getClass().getField( fieldName );
        field.set( this, value );
    }
    
    public String getStreet() {
        return street;
    }
    
    public void setStreet( String street ) {
        this.street = street != null ? street.trim() : null;
    }

    public String getNumber() {
        return number;
    }
    
    public void setNumber( String number ) {
        this.number = number != null ? number.trim() : null;
    }

    public String getPostalCode() {
        return postalCode;
    }
    
    public void setPostalCode( String postalCode ) {
        this.postalCode = postalCode != null ? postalCode.trim() : null;
    }

    public String getCity() {
        return city;
    }

    public void setCity( String city ) {
        this.city = city != null ? city.trim() : null;
    }

    public String getCountry() {
        return country;
    }

    public void setPoint( Point point ) {
        this.point = point;
    }

    public Point getPoint() {
        return point;
    }


    /**
     * The score of the geocoder search. This field is only set if this address
     * is the result of a geocoder search.
     * 
     * @return The score of the search, or -1. 
     */
    public float getScore() {
        return score;
    }

    
    public void setScore( float score ) {
        this.score = score;
    }

    public String toString() {
        return "Address [city=" + city + ", country=" + country + ", number=" + number
                + ", postalCode=" + postalCode + ", street=" + street + ", point=" + point + "]";
    }

    public String toJSON() 
    throws JSONException {
        JSONObject json = new JSONObject();
        json.put( "city", city );
        json.put( "country", country != null ? country : "-" );
        json.put( "postalCode", postalCode );
        json.put( "street", street );
        json.put( "number", number );
        return json.toString();
    }
}
