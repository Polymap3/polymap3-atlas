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
package org.polymap.lka.poi.lucene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.io.StringReader;

import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.ScoreDoc;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import static com.google.common.collect.Iterables.*;
import com.vividsolutions.jts.geom.Geometry;
import org.eclipse.core.runtime.IPath;

import org.polymap.core.runtime.Polymap;
import org.polymap.core.runtime.SessionContext;

import org.polymap.geocoder.Address;
import org.polymap.lka.poi.SearchDispatcher;
import org.polymap.lka.poi.SearchResult;
import org.polymap.lka.poi.SearchSPI;

/**
 * Mediator of this package provisding service via the {@link SearchSPI} interface to
 * the {@link SearchDispatcher}. This also handles the {@link SessionContext}.
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @since 3.0
 */
public class PoiSearcher
        implements SearchSPI {

    private static final Log  log = LogFactory.getLog( PoiSearcher.class );

    static final GeometryJSON   jsonDecoder = new GeometryJSON();

    public static final Pattern boundsPattern = Pattern.compile( "bounds:[^ $]+" );
    
    private PoiIndexer          indexer;
    
    private SessionContext      context;


    public PoiSearcher() 
    throws Exception {
        PoiProvider provider = new PoiProvider();
        IPath workspace = Polymap.getWorkspacePath();
        indexer = new PoiIndexer( provider, new File( workspace.toFile(), "atlas_index" ) );
    }
    
    
    public Iterable<String> autocomplete( String term, int maxResults, CoordinateReferenceSystem worldCRS )
    throws Exception {
        List<String> result = new ArrayList( maxResults );
        for (String s : indexer.searchTerms( term, maxResults )) {
            result.add( s );
        }
        return result;
    }


    public Iterable<SearchResult> search( String term, int maxResults, final CoordinateReferenceSystem worldCRS )
    throws Exception {
        String boundsJson = null;
        String searchTerm = term;

        // extract bounds:{...} param from search term
        Matcher matcher = boundsPattern.matcher( term );
        if (matcher.find()) {
            String boundsParam = term.substring( matcher.start(), matcher.end() );
            boundsJson = StringUtils.substringAfter( boundsParam, "bounds:" );
            searchTerm = StringUtils.remove( searchTerm, boundsParam );
        }
                
        ScoreDoc[] scoreDocs = indexer.search( searchTerm, 10000 /*maxResults*/ );
        
        // create SearchResults
        Iterable<SearchResult> result = transform( Arrays.asList( scoreDocs ), new Function<ScoreDoc,SearchResult>() {
            public SearchResult apply( ScoreDoc scoreDoc ) {
                try {
                    return createSearchResult( scoreDoc );
                }
                catch (Exception e) {
                    log.error( "Error while building SearchResult: ???", e );
                    throw new RuntimeException( e );
                }
            }
        });
        
        // filter bounds
        if (boundsJson != null) {
            final Geometry bounds = jsonDecoder.read( new StringReader( boundsJson ) );
            
            result = filter( result, new Predicate<SearchResult>() {
                public boolean apply( SearchResult record ) {
                    try {
                        CoordinateReferenceSystem recordCRS = CRS.decode( record.getSRS() );
                        MathTransform transform = CRS.findMathTransform( recordCRS, worldCRS, true );
                        Geometry transformed = JTS.transform( record.getGeom(), transform );
                        return bounds.contains( transformed );
                    }
                    catch (Exception e) {
                        throw new RuntimeException( e );
                    }
                }
            });
        }        
        // limit results to maxResults
        return limit( result, maxResults );
    }

    
    protected SearchResult createSearchResult( ScoreDoc scoreDoc )
    throws Exception {
        Document doc = indexer.getDocument( scoreDoc );
        
        String title = doc.get( PoiIndexer.FIELD_TITLE );
        if (title == null) {
            title = doc.get( StringUtils.capitalize( PoiIndexer.FIELD_TITLE ) );
        }
        if (title == null) {
            title = doc.get( PoiIndexer.FIELD_TITLE.toUpperCase() );
        }
        if (title == null) {
            title = "(ohne Namen)";
        }
        SearchResult record = new SearchResult( scoreDoc.score, title );

        for (Fieldable field : doc.getFields()) {
            // geom
            if (field.name().equals( PoiIndexer.FIELD_GEOM )) {
                Geometry geom = jsonDecoder.read( new StringReader( field.stringValue() ) );
                record.setGeom( geom );
            }
            // srs
            else if (field.name().equals( PoiIndexer.FIELD_SRS )) {
                record.setSRS( field.stringValue() );
            }
            // title
            else if (field.name().equalsIgnoreCase( PoiIndexer.FIELD_TITLE )) {
                // already set
            }
            // keywords
            else if (field.name().equalsIgnoreCase( PoiIndexer.FIELD_KEYWORDS )) {
                // ommit keyword
            }
            // address
            else if (field.name().equalsIgnoreCase( PoiIndexer.FIELD_ADDRESS )) {
                record.setAddress( Address.valueOf( field.stringValue() ) );
            }
            // all other fields
            else {
                record.addField( field.name(), field.stringValue() );
            }
        }
        return record;
    }
    
}
