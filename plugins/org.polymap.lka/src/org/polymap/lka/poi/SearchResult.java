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

import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

import org.polymap.geocoder.Address;

/**
 * A result record returned by a {@link SearchSPI searcher}.
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class SearchResult {

    /** The score if this result. Range: 0 = no match, 1 = best match */
    private float               score;
    
    private String              title;
   
    private Address             address;
    
    private Geometry            geom;
    
    private String              srs;
    
    private Map<String,String>  fields = new HashMap();
    
    
    /**
     * 
     * @param score The score if this result. Range: 0 = no match, 1 = best match.
     * @param title
     */
    public SearchResult( float score, String title ) {
        //assert score > 0 && score <= 1;
        assert title != null;
        
        this.score = score;
        this.title = title;
    }

    /**
     * The title/name of this record.
     */
    public String getTitle() {
        return title;
    }
    
    public float getScore() {
        return score;
    }

    /**
     * The address of this result, or null if this result has no address.
     */
    public Address getAddress() {
        return address;
    }

    public void setAddress( Address address ) {
        this.address = address;
    }

    /**
     * Returns all field names.
     */
    public Iterable<String> getFieldNames() {
        return fields.keySet();    
    }
    
    /**
     * Returns a field with the given name if any exist in this result, or
     * null.
     */
    public String getField( String name ) {
        return fields.get( name );
    }

    /**
     * 
     * @param name The name of the field.
     * @param value The value of the field.
     * @throws IllegalArgumentException If the given field name already exists.
     */
    public void addField( String name, String value ) {
        assert name != null;
        assert value != null;
        
        String old = fields.put( name, value );
        if (old != null) {
            fields.put( name, old );
            throw new IllegalArgumentException( "The given field name already exists: " + name );
        }
    }

    public void setGeom( Geometry geom ) {
        assert geom != null;
        this.geom = geom;
    }
    
    public Geometry getGeom() {
        return geom;
    }

    public String getSRS() {
        return srs;
    }
    
    public void setSRS( String srs ) {
        this.srs = srs;
    }

}
