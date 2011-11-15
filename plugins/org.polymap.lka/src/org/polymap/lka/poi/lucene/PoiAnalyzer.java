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

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;

/**
 * Used by {@link PoiIndexer} to analyse fields of POIs. 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
final class PoiAnalyzer
        extends Analyzer {

    private final Version       matchVersion;

    
    public PoiAnalyzer( Version matchVersion ) {
        this.matchVersion = matchVersion;
    }


    public TokenStream tokenStream( String fieldName, Reader reader ) {
        Tokenizer tokenStream = new PoiTokenizer( reader );
        //TokenStream result = new StandardFilter( tokenStream );
        TokenStream result = new LowerCaseFilter( tokenStream );
        //result = new StopFilter( enableStopPositionIncrements, result, stopSet );
        return result;
    }

    
    /**
     * A WhitespaceTokenizer is a tokenizer that divides text at whitespace.
     * Adjacent sequences of non-Whitespace characters form tokens.
     */
    final class PoiTokenizer
            extends CharTokenizer {

        public PoiTokenizer( Reader in ) {
            super( in );
        }

        public PoiTokenizer( AttributeSource source, Reader in ) {
            super( source, in );
        }

        public PoiTokenizer( AttributeFactory factory, Reader in ) {
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
                //case ':': return false;
                case '-': return false;
                case '\\': return false;
                case '/': return false;
                case '@': return false;
                case '"': return false;
                case '\'': return false;
                case '{': return false;
                case '}': return false;
                case '[': return false;
                case ']': return false;
                default: return true;
            }
        }
    }

}
