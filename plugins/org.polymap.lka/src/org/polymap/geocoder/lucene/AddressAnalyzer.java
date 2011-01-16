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
import java.io.Reader;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;

/**
 * Used by {@link AddressIndexer} to analyse address fields. 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
class AddressAnalyzer
        extends Analyzer {

    private final Version       matchVersion;

    
    public AddressAnalyzer( Version matchVersion ) {
        this.matchVersion = matchVersion;
    }


    public TokenStream tokenStream( String fieldName, Reader reader ) {
        Tokenizer tokenStream = new AddressTokenizer( reader );
        //TokenStream result = new StandardFilter( tokenStream );
        TokenStream result = new LowerCaseFilter( tokenStream );
        result = new StopFilter( false, result, StopFilter.makeStopSet( "strasse", "straße" ), true );
        result = new StreetSuffixFilter( result );
        return result;
    }

    
    /**
     * A WhitespaceTokenizer is a tokenizer that divides text at whitespace.
     * Adjacent sequences of non-Whitespace characters form tokens.
     */
    class AddressTokenizer
            extends CharTokenizer {

        public AddressTokenizer( Reader in ) {
            super( in );
        }

        public AddressTokenizer( AttributeSource source, Reader in ) {
            super( source, in );
        }

        public AddressTokenizer( AttributeFactory factory, Reader in ) {
            super( factory, in );
        }

        protected boolean isTokenChar( char c ) {
            switch (c) {
                case ' ': return false;
                case '\t': return false;
                case '\r': return false;
                case '\n': return false;
                case '.': return false;
                case ',': return false;
                case ';': return false;
                case ':': return false;
                case '-': return false;
                case '\\': return false;
                case '/': return false;
                case '@': return false;
                case '"': return false;
                case '\'': return false;
                case '(': return false;
                case ')': return false;
                default: return true;
            }
        }
    }
    

    /**
     * 
     */
    class StreetSuffixFilter
            extends TokenFilter {

        private TermAttribute termAtt;

        
        public StreetSuffixFilter( TokenStream in ) {
            super( in );
            termAtt = addAttribute( TermAttribute.class );
        }


        public final boolean incrementToken()
        throws IOException {
            if (input.incrementToken()) {
                
                // XXX use termBuffer() for performance
                String term = termAtt.term();
                if (term.endsWith( "str" )) {
                    String newTerm = StringUtils.substringBefore( term, "str" );
                    termAtt.setTermBuffer( newTerm );
                    //System.out.println( "   term: " + term + " -> " + newTerm );
                }
                else if (term.endsWith( "strasse" )) {
                    String newTerm = StringUtils.substringBefore( term, "strasse" );
                    termAtt.setTermBuffer( newTerm );
                    //System.out.println( "   term: " + term + " -> " + newTerm );
                }
                else if (term.endsWith( "straße" )) {
                    String newTerm = StringUtils.substringBefore( term, "straße" );
                    termAtt.setTermBuffer( newTerm );
                    //System.out.println( "   term: " + term + " -> " + newTerm );
                }

//                final char[] buffer = termAtt.termBuffer();
//                final int length = termAtt.termLength();
//                for (int i = 0; i < length; i++)
//                    buffer[i] = Character.toLowerCase( buffer[i] );

                return true;
            }
            else {
                return false;
            }
        }

    }

}
