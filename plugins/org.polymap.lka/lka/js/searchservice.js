/*
 * polymap.org
 * Copyright 2011-2012, Polymap GmbH. All rights reserved.
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
 * Config: 
 * <pre>
 * {
 *     baseUrl: //,
 *     outputType: // the output type send to server: 'JSON', 'RSS',
 *     srs: // the SRS to be requested from the server 
 * }
 */
var SearchService = Class.extend( new function SearchServiceProto() {

    this.config = null;

    /**
     * 
     */
    this.init = function( config ) {
        this.config = config;
    };

    /**
     * 
     */
    this.searchUrl = function( searchStr, outputType, srs ) {
        var template = '{0}?search={1}&outputType={2}&srs={3}';
        if (outputType == null) {
            outputType = this.config.outputType;
        }
        if (srs == null) {
            srs = this.config.srs;
        }
        // all those functions do not work, at least for me
        //search_str = escape( search_str );
        //search_str = new OpenLayers.Format.JSON().write( search_str, false );
        //search_str = $.URLEncode( search_str );
        //search_str = jQuery.param( search_str, true );
        searchStr = encodeURIComponent( searchStr );
        return template.format( this.config.baseUrl, searchStr, outputType, srs );
    };

    /**
     * 
     */
    this.search = function( url, callback ) {
        $.ajax( {
            url: url,
            dataType: "html",
            success: function( data ) {
                var features = new OpenLayers.Format.GeoJSON().read( data );
                callback.call( callback, features != null ? features : [] );
            }
        });
    };

});