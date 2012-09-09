/* 
 * polymap.org
 * Copyright 2009-2012, Polymap GmbH. All rights reserved.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Function;
import static com.google.common.collect.Iterables.*;

import org.polymap.core.runtime.Polymap;

/**
 * The dispatcher for the registered {@link SearchSPI service providers}.
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @since 3.0
 */
public class SearchDispatcher
        implements SearchSPI {

    private static final Log  log = LogFactory.getLog( SearchServlet.class );

    private ExecutorService             executorService = Polymap.executorService();
    
    private List<SearchSPI>             searchers = new ArrayList();
    

    public SearchDispatcher()
    throws Exception {
        super();
        log.info( "Initializing SearchDispatcher ..." );
        searchers.add( new org.polymap.lka.poi.lucene.PoiSearcher() );
        searchers.add( new org.polymap.geocoder.lucene.AddressSearcher() );
    }

    
    public Iterable<String> autocomplete( final String term, final int maxResults, final CoordinateReferenceSystem worldCRS )
    throws Exception {
        // call searches in separate threads
        // XXX score results
        List<Future<List<String>>> results = new ArrayList();
        for (final SearchSPI searcher : searchers) {
            results.add( executorService.submit( new Callable<List<String>>() {
            
                public List<String> call() throws Exception {
                    log.info( "Searcher started: " + searcher.getClass().getSimpleName() );
                    Iterable<String> records = searcher.autocomplete( term, maxResults, null );
                    
                    // use real list (not Iterables) in order to make sure
                    // this processing is done inside the thread
                    List<String> result = new ArrayList( maxResults );
                    Iterator<String> it = records.iterator();
                    while (it.hasNext() && result.size() <= maxResults) {
                        String record = it.next();
                        // has the record any result anyway?
                        if (StringUtils.containsNone( term, SEPARATOR_CHARS ) 
                                || !search( record, 1, worldCRS ).iterator().hasNext()) {
                            result.add( record );
                        }
                    }
                    return result;
                }                
            }));
        }
        
        // wait for threads; concat all records
        return concat( transform( results, new Function<Future<List<String>>,List<String>>() {
            public List<String> apply( Future<List<String>> future ) {
                try {
                    return future.get();
                }
                catch (Exception e) {
                    log.warn( "", e );
                    return new ArrayList();
                }
            }
        }));
    }


    public Iterable<SearchResult> search( final String term, final int maxResults, final CoordinateReferenceSystem worldCRS )
    throws Exception {
        // call searches in separate threads
        // XXX score results
        List<Future<Iterable<SearchResult>>> results = new ArrayList();
        for (final SearchSPI searcher : searchers) {
            results.add( executorService.submit( new Callable<Iterable<SearchResult>>() {
            
                public Iterable<SearchResult> call() throws Exception {
                    log.info( "Searcher started: " + searcher.getClass().getSimpleName() );
                    return searcher.search( term, maxResults, worldCRS );
                }                
            }));
        }
        
        // wait for threads; concat all SearchResults, limit to maxResults
        return limit( concat( transform( results, new Function<Future<Iterable<SearchResult>>,Iterable<SearchResult>>() {
            public Iterable<SearchResult> apply( Future<Iterable<SearchResult>> future ) {
                try {
                    return future.get();
                }
                catch (Exception e) {
                    log.warn( "", e );
                    return new ArrayList();
                }
            }
        })), maxResults );


//        // searchers
//        for (SearchSPI searcher : searchers) {
//            try {
//                for (SearchResult record : searcher.search( term, maxResults, worldCRS )) {
//                    results.put( record.getScore(), record );
//                }
//            }
//            catch (Exception e) {
//                log.warn( "", e );
//            }            
//        }
//
//        return Iterables.limit( results.values(), maxResults );
    }

}
