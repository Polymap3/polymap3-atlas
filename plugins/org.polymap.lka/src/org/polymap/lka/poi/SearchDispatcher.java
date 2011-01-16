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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * The dispatcher for the registered {@link SearchSPI service providers}.
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class SearchDispatcher
        implements SearchSPI {

    private static final Log  log = LogFactory.getLog( SearchServlet.class );

    private List<SearchSPI>             searchers = new ArrayList();
    

    public SearchDispatcher()
    throws Exception {
        super();
        log.info( "Initializing SearchDispatcher ..." );
        searchers.add( new org.polymap.lka.poi.lucene.PoiSearcher() );
        searchers.add( new org.polymap.geocoder.lucene.AddressSearcher() );
    }

    
    public List<String> autocomplete( String term, int maxResults )
    throws Exception {
        List<String> result = new ArrayList( maxResults );

        // FIXME score results
        for (SearchSPI searcher : searchers) {
            List<String> records = searcher.autocomplete( term, maxResults );
            for (String record : records) {
                // has the record any result anyway?
                if (StringUtils.containsNone( term, SEPARATOR_CHARS ) || !search( record, 1 ).isEmpty()) {
                    result.add( record );
                }
            }
        }
        return result;
    }


    public List<SearchResult> search( String term, int maxResults )
    throws Exception {
        // sort and accept equal keys
        TreeMap<Float,SearchResult> results = new TreeMap( new Comparator<Float>() {
            public int compare( Float o1, Float o2 ) {
                return o1.equals( o2 ) ? -1 : o1.compareTo( o2 );
            }
        });
        // searchers
        for (SearchSPI searcher : searchers) {
            List<SearchResult> records = searcher.search( term, maxResults );
            for (SearchResult record : records) {
                results.put( record.getScore(), record );
            }
        }
        
        List<SearchResult> result = new ArrayList( maxResults );
        int i = maxResults;
        for (Iterator it=results.values().iterator(); it.hasNext() && i>0; i--) {
            result.add( (SearchResult)it.next() );
        }
        return result;
    }


//    Internal internal() {
//        return new Internal();    
//    }
//    
//    
//    class Internal {
//        
//    }
    
}
