/*
 * polymap.org
 * Copyright 2011, Falko Bräutigam. All rights reserved.
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

/**
 *
 */
var TransMap = Class.extend( {
    
    init: function() {
        this.initTranslations();
        
        // tweak context functions
        for (var i=0; i<Atlas.contexts.length; i++) {
            // translate search URL
            var self = this;
            Atlas.contexts[i].createSearchUrl = function( searchStr ) {
                return Atlas.config.searchUrl + 
                    "?search=" + self.enhanceSearch( searchStr ) + 
                    "&outputType=JSON&srs=" + Atlas.map.getProjection();
            }
            // translate result
            Atlas.contexts[i].resultFieldEnhancers.push( function( str ) {
                return self.enhanceResult( str );
            });
        }
        return this;
    },
    
    /**
     * Enhance the search string by translated keywords from
     * destination language.
     */
    enhanceSearch: function( searchStr ) {
        if (this.translations == null) {
            this.initTranslations();
        }
        var trans = this.translations["de"];
        
        var result = "";
        var tokens = searchStr.split( " " );
        for (var i=0; i<tokens.length; i++) {
            result += trans.enhanceSearch( tokens[i] );
            result += i < tokens.length-1 ? " " : "";
        }
        return result;
    },
    
    enhanceResult: function( str ) {
        var trans = this.translations["de"];

        var result = "";
        var tokens = str.split( " " );
        for (var i=0; i<tokens.length; i++) {
            result += trans.enhanceResult( tokens[i] );
            result += i < tokens.length-1 ? " " : "";
        }
        return result;
    },
    
    /**
     * Load and initialize the translations.
     */
    initTranslations: function() {
        // FIXME hard coded de cz
        var src_dest = new Translation( "de", "cz" );
        var dest_src = new Translation( "cz", "de" );

        this.translations = {
            de: src_dest,
            cz: dest_src
        };

        $.ajax({
            url:        "bundle/trans_de_cz.properties",
            async:      false,
            contentType:'text/plain;charset=UTF-8',
            dataType:   'text',
            success:    function(data, status) {
                
                var lines = data.split( /\n/ );
                for (var i=0; i < lines.length; i++) {
                    
                    var pair = lines[i].split( "=" );
                    var key = pair[0];
                    var value = pair[1];
                    
                    var toDest = false;
                    var toSrc = false;
                    if (value.startsWith( '>' )) {
                        toDest = true;
                        value = value.substr(1);
                    }
                    if (key.endsWith( '<' )) {
                        toSrc = true;
                        key = key.substr(0,key.length-1);
                    }
                    if (toDest) {
                        src_dest.translators.push( new WordTranslator( key.trim(), value.trim() ) );
                    }
                    if (toSrc) {
                        dest_src.translators.push( new WordTranslator( value.trim(), key.trim() ) );
                    }
                }
            }
        });
    }
});


/**
 *
 */
var Translation = Class.extend( {
    
    init: function( src, dest ) {
        this.src = src;
        this.dest = dest;
        this.translators = [];
        return this;
    },

    enhanceSearch: function( token ) {
        for (var j=0; j<this.translators.length; j++) {
            var result = this.translators[j].enhanceSearch( token );
            if (result != null && result != token) {
                return result;
            }
        }
        return token;
    },
    
    enhanceResult: function( token, context ) {
        for (var j=0; j<this.translators.length; j++) {
            var result = this.translators[j].enhanceResult( token, context );
            if (result != null && result != token) {
                return result;
            }
        }
        return token;
    }
    
});


/**
 * 
 */
var WordTranslator = Class.extend( {
    
    init: function( from, to ) {
        this.from = from;
        this.to = to;
        return this;
    },

    enhanceSearch: function( token, context ) {
        return token.toLowerCase() == this.from.toLowerCase() 
                ? "(" + token + " OR " + this.to.split( " " ).join( " OR " ) + ")"
                : token;
    },
    
    enhanceResult: function( token, context ) {
        return token.toLowerCase() == this.from.toLowerCase() 
                ? this.to : token;
    }
});

        
//        // benchmark
//        alert( "Start creating..." );
//        var wordPairs = [];
//        for (var i=0; i<1000; i++) {
//            wordPairs.push( new WordPair( ""+i, "cz:"+i ) );
//        }
//        alert( "done. Start searching..." );
//        var count = 0;
//        for (var i=0; i<10000; i++) {
//            for (var j=0; j<wordPairs.length; j++) {
//                var result = wordPairs[j].enhanceSearch( i );
//                if (result != i) {
//                    count++;
//                }
//            }
//        }
//        alert( "done. found: " + count );
        
