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
     * @param searchStr
     * @param outputType
     * @param srs
     * @param bounds {OpenLayers.Geometry}
     */
    this.searchUrl = function( searchStr, outputType, srs, bounds ) {
        var template = '{0}?search={1}&outputType={2}&srs={3}';

        if (bounds) {
            var json = new OpenLayers.Format.GeoJSON().write( bounds );
//            var obj = {
//                'type': 'LinearRing',
//                'coordinates': []
//            }
//         "coordinates": [
//           [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0],
//             [100.0, 1.0], [100.0, 0.0] ]
//           ]
//       },
            searchStr += ' bounds:' + json;
        }
        // all those functions do not work, at least for me
        //search_str = escape( search_str );
        //search_str = new OpenLayers.Format.JSON().write( search_str, false );
        //search_str = $.URLEncode( search_str );
        //search_str = jQuery.param( search_str, true );
        searchStr = encodeURIComponent( searchStr );
        outputType = outputType ? outputType : this.config.outputType;
        srs = srs ? srs : this.config.srs;

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