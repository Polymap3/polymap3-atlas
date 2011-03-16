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
package org.polymap.lka.poi.lucene;

import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.StringReader;

import org.geotools.geojson.geom.GeometryJSON;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.ScoreDoc;

import com.vividsolutions.jts.geom.Geometry;

import org.eclipse.core.runtime.IPath;

import org.polymap.core.runtime.Polymap;
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
public class PoiSearcher
        implements SearchSPI {

    private PoiIndexer          indexer;


    public PoiSearcher() 
    throws Exception {
        PoiProvider provider = new PoiProvider();
        IPath workspace = Polymap.getWorkspacePath();
        indexer = new PoiIndexer( provider, new File( workspace.toFile(), "atlas_index" ) );
    }
    
    
    public List<String> autocomplete( String term, int maxResults )
    throws Exception {
        List<String> result = new ArrayList();
        for (String s : indexer.searchTerms( term, maxResults )) {
            result.add( s );
        }
        return result;
    }


    public List<SearchResult> search( String term, int maxResults )
    throws Exception {
        ScoreDoc[] scoreDocs = indexer.search( term, maxResults );
        
        GeometryJSON jsonDecoder = new GeometryJSON();
        
        List<SearchResult> result = new ArrayList( maxResults );
        for (ScoreDoc scoreDoc : scoreDocs) {
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
            result.add( record );
        }
        return result;
    }

}
