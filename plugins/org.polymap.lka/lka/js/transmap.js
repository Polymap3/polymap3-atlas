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
 * Tool item that triggers reload of the entire site with
 * the given language code set in the URL param 'lang'.
 */
var LangSwitcherItem = ToolItem.extend( new function LangSwitcherItemProto() {
    
    this.init = function( id, icon, tooltip, lang ) {
        this._super( id, "", tooltip );
        this.lang = lang;
        this.icon = icon;
        return this;
    };
    
    this.onClick = function() {
        location.replace( location.protocol + "//" + location.host + location.pathname + "?lang=" + this.lang );
    };
});


/**
 * Provides content translation based on properties files.
 * <p/>
 * The Atlas object needs to provide a <code>language</code> property that
 * exposes the language code of the language currently used in the browser.
 */
var TransMap = Class.extend( new function TransMapProto() {
    
    this.baseUrl = null;
    
    this.translations = new Array();
    
    /**
     * @param baseUrl
     *            {String} The base Url to load the translation files from. For
     *            example 'bundle/trans_' results in URL
     *            'bundle/trans_de_cz.properties'.
     * @param translations
     *            {Array} Array of Strings in the form "de_cz".
     */
    this.init = function( baseUrl, translationLangCodes ) {
        this.baseUrl = baseUrl;
        this.initTranslations( translationLangCodes );
        
        // listen to 'searchPreparing' -> tweak URL
        var self = this;
        Atlas.events.bind( 'searchPreparing', function( ev ) {
            ev.searchStr = self.enhanceSearch( ev.searchStr );
        });
        
        // listen to 'searchFeatureLoaded' -> translate fields
        Atlas.events.bind( 'searchFeatureLoaded', function( ev ) {
            $.each( ev.feature.data, function( name, value ) {
                ev.feature.data[name] = self.enhanceResult( value );
            });
        });
        return this;
    };
    
    /**
     * Enhance the search string by translated keywords from
     * destination language.
     */
    this.enhanceSearch = function( searchStr ) {
        var currentLang = Atlas.language;        
        for (var i=0; i<this.translations.length; i++) {
            if (this.translations[i].srcMatches( currentLang )) {
                searchStr = this.translations[i].enhanceSearch( searchStr );
            }
        }
        return searchStr;
    };
    
    /**
     * 
     */
    this.enhanceResult = function( line ) {
        var currentLang = Atlas.language;

        for (var i=0; i<this.translations.length; i++) {
            if (this.translations[i].destMatches( currentLang )) {
                line = this.translations[i].enhanceResult( line );
            }
        }
        return line;
    };
    
    /**
     * Load and initialize the translations.
     * 
     * @param translations {Array} Array of Strings in the form "de_cz".
     */
    this.initTranslations = function( translationLangCodes ) {
        for (var i=0; i<translationLangCodes.length; i++) {
            var langCodes = translationLangCodes[i].split( "_" );
            var src_dest = new Translation( langCodes[0], langCodes[1] );
            var dest_src = new Translation( langCodes[1], langCodes[0] );

            this.translations.push( src_dest );
            this.translations.push( dest_src );

            $.ajax({
                url:        this.baseUrl + translationLangCodes + '.properties',
                async:      false,
                contentType:'text/plain;charset=UTF-8',
                dataType:   'text',
                success:    function( data, status ) {
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
    };
});


/**
 *
 */
var Translation = Class.extend( new function TranslationProto() {
    
    this.init = function( src, dest ) {
        this.src = src;
        this.dest = dest;
        this.translators = [];
        return this;
    };

    /** True if the source lang code matches the given code. */
    this.srcMatches = function( langCode ) {
        return langCode.startsWith( this.src );
    };
    
    this.destMatches = function( langCode ) {
        return langCode.startsWith( this.dest );
    };
    
    this.enhanceSearch = function( line ) {
        for (var j=0; j<this.translators.length; j++) {
            line = this.translators[j].enhanceSearch( line );
        }
        return line;
    };
    
    this.enhanceResult = function( line, context ) {
        for (var j=0; j<this.translators.length; j++) {
            line = this.translators[j].enhanceResult( line, context );
        }
        return line;
    };
});


/**
 *
 * <p/>
 * Impl note: Iterating the entire line for each word is more expensive than
 * tokenize outside the loop and then just compare each token but it would not
 * work for multi word phrases.
 */
var WordTranslator = Class.extend( new function WordTranslator() {
    
    this.init = function( from, to ) {
        this.from = from;
        this.to = to;
        return this;
    };

    this.enhanceSearch = function( line, context ) {
        // XXX regex doesn't seem to work with czech phrases!?
        
//        if (this.fromRegex == null) {
//            this.fromRegex = new RegExp( '\\b' + this.from + '\\b', 'gi' );
//        }
        
        // if this.from is a multi word phrase then we need quotes around
        // to prevent Lucene to put an AND in between and to make sure what
        // the OR means; see http://polymap.org/atlas/ticket/48
        return line.replace( this.from, //this.fromRegex, 
                '("' + this.from + '" OR "' + this.to.split( ' ' ).join( '" OR "' ) + '")' );
    };
    
    this.enhanceResult = function( line, context ) {
        if (this.fromRegex == null) {
            this.fromRegex = new RegExp( this.from, 'gi' );
        }
        return line.replace( this.fromRegex, this.to );
    };
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
        
