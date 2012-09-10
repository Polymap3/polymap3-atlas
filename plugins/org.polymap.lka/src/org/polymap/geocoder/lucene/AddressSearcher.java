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
package org.polymap.geocoder.lucene;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Iterables.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.StringReader;

import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.polymap.geocoder.Address;
import org.polymap.lka.poi.SearchResult;
import org.polymap.lka.poi.SearchSPI;
import org.polymap.lka.poi.lucene.PoiSearcher;

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

    static final GeometryJSON       jsonDecoder = new GeometryJSON();

    public static final Pattern     boundsPattern = PoiSearcher.boundsPattern;

    private LuceneGeocoder          geocoder = LuceneGeocoder.instance();
    

    public AddressSearcher() {
    }
   
    
    public Iterable<String> autocomplete( String term, int maxResults, CoordinateReferenceSystem worldCRS )
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


    public Iterable<SearchResult> search( String term, final int maxResults, final CoordinateReferenceSystem worldCRS )
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

        // search database
        Iterable<Address> addresses = geocoder.find( searchTerm, 10000 /*maxResults*/ );

        GeometryFactory gf = new GeometryFactory();

        // build SearchResults
        Iterable<SearchResult> result = transform( addresses, new Function<Address,SearchResult>() {            
            public SearchResult apply( Address address ) {
                String title = address.getStreet() + ", " + address.getCity();
                SearchResult record = new SearchResult( address.getScore(), title );
                record.setAddress( address );
                
                record.setGeom( address.getPoint() );
                record.setSRS( address.getSRS() );

                record.addField( AddressIndexer.FIELD_CATEGORIES, "Adresse" );
                return record;
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
        // filter equal titles
        result = filter( result, new Predicate<SearchResult>() {

            private Set<String> titles = new HashSet();
            
            public boolean apply( SearchResult input ) {
                return titles.add( input.getTitle() );
            }
        });

        // limit results to maxResults
        return limit( result, maxResults );
    }

}
