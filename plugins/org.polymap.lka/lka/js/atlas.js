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

/** Array of SearchContext instances. */
var contexts = null;

/** The currently active SearchContext: 0->red, 1->green... */
var result_index = 0;

/** The map and the layers. */
map = null;
mapCRS = "EPSG:900913";
dop = null;
mapnik = null;
hillshade = null;

measure = null;  // set by initMeasure();

/**
 * 
 */
function init() {
    contexts = [
        new SearchContext( 0, "images/marker_red.png" ),
        new SearchContext( 1, "images/marker_green.png" ),
        new SearchContext( 2, "images/marker_yellow.png" ),
        new SearchContext( 3, "images/marker_blue.png" ) ];

    
    var $tabs = $('#tabs').tabs(); // first tab selected
    $('#tabs').bind( 'tabsselect', function( event, ui ) {
        //ui.tab     // anchor element of the selected (clicked) tab
        //ui.panel   // element, that contains the selected/clicked tab contents
        
        contexts[result_index].searchStr = $('#search_field').val();
        contexts[result_index].deactivate();     
        
        result_index = ui.index;   // zero-based index of the selected (clicked) tab
        contexts[result_index].activate();     
    });

    $('#search_field').autocomplete({ 
        source: search_field_config.autocomplete_url,
        zIndex: 1000,
        delay: 500
        }
    );
    $('#search_field').keypress( function( event ) {
        //alert(" keypress event" );
        //console.log("  -->" + event.keyCode );
        if (event.keyCode == 13) {
            var searchStr = $("#search_field").val();
            contexts[result_index].search( searchStr );
            
            $('#search_field').autocomplete( "close" );
        }
    } );
    $('#search_field').focus();
    
    // set transformation functions from/to alias projection
    OpenLayers.Projection.addTransform( "EPSG:4326", "EPSG:3857", OpenLayers.Layer.SphericalMercator.projectForward );
    OpenLayers.Projection.addTransform( "EPSG:3857", "EPSG:4326", OpenLayers.Layer.SphericalMercator.projectInverse );

    // avoid pink tiles
    OpenLayers.IMAGE_RELOAD_ATTEMPTS = 3;
    OpenLayers.Util.onImageLoadErrorColor = "transparent";
    OpenLayers.Util.MISSING_TILE_URL = "http://openstreetmap.org/openlayers/img/404.png";
    
    initUrlParams();
}

/**
 * Handle URL params for preset searches. 
 */
function initUrlParams() {
    var search_str = $(document).getUrlParam( "search" );
    if (search_str != null) {
        contexts[0].search( decodeURIComponent( search_str ) );
    }
    search_str = $(document).getUrlParam( "search1" );
    if (search_str != null) {
        contexts[0].search( decodeURIComponent( search_str ) );
    }
    search_str = $(document).getUrlParam( "search2" );
    if (search_str != null) {
        contexts[1].search( decodeURIComponent( search_str ) );
        contexts[1].deactivate();
    }
    search_str = $(document).getUrlParam( "search3" );
    if (search_str != null) {
        contexts[2].search( decodeURIComponent( search_str ) );
        contexts[2].deactivate();
    }
    search_str = $(document).getUrlParam( "search4" );
    if (search_str != null) {
        contexts[3].search( decodeURIComponent( search_str ) );
        contexts[3].deactivate();
    }
    contexts[0].activate();
}
