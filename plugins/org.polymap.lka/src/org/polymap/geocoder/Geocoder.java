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
package org.polymap.geocoder;

import org.polymap.geocoder.lucene.LuceneGeocoder;

/**
 * The geocoding API. 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public abstract class Geocoder {
    
    public static Geocoder instance() {
        return LuceneGeocoder.instance();
    }

    /**
     *
     * @param addressString
     * @throws Exception
     */
    public abstract Iterable<Address> find( String addressString, int maxResults )
    throws Exception;
    
    /**
     *
     * @param fragment
     * @param maxResults 
     * @throws Exception
     */
    public abstract Iterable<Address> find( Address fragment, int maxResults )
    throws Exception;
    
    
}
