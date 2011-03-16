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
package org.polymap.geocoder.lucene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.GeometryFactory;

import org.polymap.geocoder.Address;
import org.polymap.lka.poi.SearchResult;
import org.polymap.lka.poi.SearchSPI;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class AddressSearcher
        implements SearchSPI {

    private static final Log  log = LogFactory.getLog( AddressIndexer.class );
    
    private LuceneGeocoder          geocoder = LuceneGeocoder.instance();
    

    public AddressSearcher() {
    }
   
    
    public List<String> autocomplete( String term, int maxResults )
    throws Exception {
        // search for the last term in the search
        String addressStr = term.toLowerCase();
        String prefix = "";
        if (StringUtils.contains( addressStr, " " )) { 
            prefix = StringUtils.substringBeforeLast( term, " " ) + " ";
            addressStr = StringUtils.substringAfterLast( term, " " ).toLowerCase();
        }
        
        List<String> result = new ArrayList();
        
        // streets
        String[] terms = geocoder.indexer.searchTerms( 
                AddressIndexer.FIELD_STREET, addressStr, 5000 );
        for (int i=0; i<maxResults && i<terms.length; ) {
            log.info( "   Street: " + terms[i] );

            // find possible streets
            List<Address> addrs = geocoder.findStreets( new Address( terms[i], null, null, null, null ), maxResults );
            for (Iterator it=addrs.iterator(); it.hasNext() && i<maxResults; i++) {
                Address address = (Address)it.next();
                result.add( prefix + address.getStreet() + ", " + address.getCity() );
            }
        }

//        // cities
//        terms = geocoder.indexer.searchTerms( 
//                AddressIndexer.FIELD_CITY, addressStr, 5000 );
//        for (int i=0; i<maxResults && i<terms.length; ) {
//            log.info( "   City: " + terms[i] );
//
//            // find possible streets
//            List<Address> addrs = geocoder.findCities( new Address( null, null, null, terms[i], null ), maxResults );
//            for (Iterator it=addrs.iterator(); it.hasNext() && i<maxResults; i++) {
//                Address address = (Address)it.next();
//                result.add( address.getCity() );
//            }
//        }
        return result;
    }


    public List<SearchResult> search( String term, int maxResults )
    throws Exception {
        List<Address> addresses = geocoder.find( term, maxResults );

        GeometryFactory gf = new GeometryFactory();
        
        // avoid record with same city/street
        Map<String,SearchResult> result = new HashMap( maxResults );
        for (Address address : addresses) {
            String title = address.getStreet() + ", " + address.getCity();
            SearchResult record = new SearchResult( address.getScore(), title );
            record.setAddress( address );
            
            record.setGeom( address.getPoint() );
            record.setSRS( address.getSRS() );
            
            result.put( title, record );
        }
        return new ArrayList( result.values() );
    }

}
