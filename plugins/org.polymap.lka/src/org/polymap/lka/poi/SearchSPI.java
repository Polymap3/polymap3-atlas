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

import java.util.List;

import org.apache.lucene.queryParser.QueryParser;

/**
 * POI searcher interface.
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @since 3.0
 */
public interface SearchSPI {

    public static final char[] SEPARATOR_CHARS = { ' ', ',', ';' };

    public static final String FIELD_CATEGORIES = "categories";
    
//    /**
//     * 
//     * @param site
//     */
//    public void init( SearchSite site );

    /**
     * 
     * @param term A search term following the rules defined by the Lucene
     *        {@link QueryParser}.
     * @return List of search results.
     */
    public List<SearchResult> search( String term, int maxResults )
    throws Exception;


    /**
     * 
     * @param term A search term following the rules defined by the Lucene
     *        {@link QueryParser}.
     * @return List of possible completions of the given search term. The
     *         completions should include the complete prfix if the search uses
     *         the last word for completion only.
     */
    public List<String> autocomplete( String term, int maxResults )
    throws Exception;

    
//    /**
//     * 
//     */
//    public interface SearchSite {
//        
//        void forceReindex();
//        
//    }
    
}
