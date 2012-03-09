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

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;

import org.polymap.geocoder.Address;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class AddressQueryParser {
    
    private Analyzer          analyzer;
    

    public AddressQueryParser( Analyzer analyzer ) {
        this.analyzer = analyzer;
    }

    
    public Query parse( Address search ) 
    throws IOException {
        BooleanQuery query = new BooleanQuery();
        if (search.getCity() != null) {
            addFieldQuery( query, AddressIndexer.FIELD_CITY, search.getCity(), 0.75f );
        }
        if (search.getPostalCode() != null) {
            addFieldQuery( query, AddressIndexer.FIELD_POSTALCODE, search.getPostalCode(), 0.6f );
        }
        if (search.getStreet() != null) {
            addFieldQuery( query, AddressIndexer.FIELD_STREET, search.getStreet(), 0.75f );
        }
        if (search.getNumber() != null) {
            addFieldQuery( query, AddressIndexer.FIELD_NUMBER, search.getNumber(), 0.1f );
        }
        return query;
    }
    

    public Query parse( String search ) 
    throws IOException {
        assert search != null;
        BooleanQuery query = new BooleanQuery();
        addFieldQuery( query, AddressIndexer.FIELD_KEYWORDS, search, 0.75f );
        return query;
    }
    
    
    protected void addFieldQuery( BooleanQuery query, String field, String search, float fuzziness )
    throws IOException {
        TokenStream source = analyzer.reusableTokenStream( field, new StringReader( search ) );
        TermAttribute termAtt = null;
        if (source.hasAttribute( TermAttribute.class ) ) {
            termAtt = source.getAttribute( TermAttribute.class );
        }
        while (source.incrementToken()) {
            String term = termAtt.term();
            query.add( new FuzzyQuery( new Term( field, term ), fuzziness ), BooleanClause.Occur.MUST );
        }
    }

}
