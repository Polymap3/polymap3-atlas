/*
 * polymap.org
 * Copyright 2009, Falko Bräutigam, and individual contributors as
 * indicated by the @authors tag.
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

/** The configuration of the 'streets' WMS. */
var street_wms_config = {
        name : 'Karte',
        url : '../services/Atlas2',
        layer : 'Ubersicht'
};
/** The configuration of the 'satelite' WMS. */
var satelite_wms_config = {
        name : 'Luftbild',
        url : '../services/Atlas2',
        layer : 'Satelite'
};
/** The configuration of the search field */
var search_field_config = {
        autocomplete_url : 'search',
        search_url : 'search',
        base_url : 'index3.html'
};

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
function MeasureMode( elm ) {
    
    this.elm = elm;
    
    this.active = false;
    
    this.control = null;
    
    this.activate = function() {
        map.addControl( this.control );
        this.control.activate();
        this.active = true;
    }
    
    this.deactivate = function() {
        this.control.deactivate();
        map.removeControl( this.control );
        this.active = false;
    };

    this.onMeasure = function( event ) {
        //var measure = event.measure;
        var geometry = event.geometry;
        var units = event.units;
        var order = event.order;
        var measure = (units == 'km')
                ? event.measure.toFixed(2) + " km"
                : event.measure.toFixed(0) + " m";

        // because of the transformation units no longer matches the outcome 
        // of getArea and getLength, those are in layer units.
//        if (order == 1) {
//            measure = (units == 'km')
//                ? (geom2.getLength()/1000).toFixed(2) + " km"
//                : geom2.getLength().toFixed(2) + " m";
//        } 
//        else {
//            if (units=='km'){
//                out += "Area: " + (geometry.getArea()/1000000).toFixed(0) + " km<sup>2</sup>";
//            } else {
//                out += "Area: " + geometry.getArea().toFixed(0) + " m<sup>2</sup>";
//            }
//        }
        
        $('#dialog').html( "Gesamtlänge: <b>" + measure + "</b>" );
        $('#dialog').dialog({ width:250 , height: 100 , title:"Ergebnis" });

//      var anchor = {
//      size: new OpenLayers.Size(10,10), 
//      offset: new OpenLayers.Pixel(10,10)
//  };
//  map.addPopup(
//          new OpenLayers.Popup.AnchoredBubble(
//              "Ergebnis", 
//              geometry.getCentroid(),  //map.getLonLatFromPixel( event.xy ),
//              new OpenLayers.Size( 300, 300 ),
//              measure.toFixed(3) + " " + units,
//              close_icon,
//              true
//         ));
  
    };

    /**
     * init
     */
    // style the sketch fancy
    var sketchSymbolizers = {
        "Point": {
            pointRadius: 8,
            graphicName: "circle",
            fillColor: "white",
            fillOpacity: .1,
            strokeWidth: 1,
            strokeOpacity: 1,
            strokeColor: "#f55"
        },
        "Line": {
            strokeWidth: 3,
            strokeOpacity: 1,
            strokeColor: "#f55",
            strokeDashstyle: "dash"
        }
    };
    var style = new OpenLayers.Style();
    style.addRules([
        new OpenLayers.Rule({symbolizer: sketchSymbolizers})
    ]);
    var styleMap = new OpenLayers.StyleMap({"default": style});
    this.control = new OpenLayers.Control.Measure(
            OpenLayers.Handler.Path, {
                persist: true,
                geodesic: true,
                handlerOptions: {
                    layerOptions: {styleMap: styleMap}
                }
            });
    this.control.events.on({
        "measure": callback( this.onMeasure, {scope:this, suppressArgs:false} )
        //"measurepartial": callback( this.onMeasure, {scope:this} )
    });
};

function initMeasure() {    
    measure = new MeasureMode( $("#measure_link") );
};

/**
 * Generating a callback function bound to an instance.
 * Taken from http://webcache.googleusercontent.com/search?q=cache:boCMVl7cnyEJ:onemarco.com/2008/11/12/callbacks-and-binding-and-callback-arguments-and-references/+javascript+callback+method&cd=4&hl=de&ct=clnk&gl=de&client=firefox-a
 * 
 * @param {Function}
 *            func the callback function
 * @param {Object}
 *            opts an object literal with the following properties (all
 *            optional): scope: the object to bind the function to (what
 *            the "this" keyword will refer to) args: an array of
 *            arguments to pass to the function when it is called, these
 *            will be appended after any arguments passed by the caller
 *            suppressArgs: boolean, whether to supress the arguments passed
 *            by the caller. This default is false.
 */
function callback( func, opts ) {
    var cb = function() {
        var args = opts.args ? opts.args : [];
        var scope = opts.scope ? opts.scope : this;
        var fargs = opts.supressArgs === true ? [] : toArray(arguments);
        func.apply(scope, fargs.concat(args));
    };
    return cb;
}

/** A utility function for callback() */
function toArray(arrayLike) {
    var arr = [];
    for (var i = 0; i < arrayLike.length; i++) {
        arr.push(arrayLike[i]);
    }
    return arr;
}

/**
 * The atlas provides multiple search contexts. The user can switch between
 * them. 
 */
function SearchContext( index, markerImage ) {
    
    /** The index of this context. */
    this.index = index;
    
    /** The string that has been searched for in this context. */
    this.searchStr = "";
    
    this.layer = null;
    
    this.popup = null;
    
    this.selectControl = null;
    
    this.hoverControl = null;
    
    this.tooltipsControl = null;
    
    this.markerImage = markerImage;
    
    this.searchURL = "#";
    
    /**
     * Active this context by updating the GUI elements.
     */
    this.activate = function() {
        $('#search_field').val( decodeURIComponent( this.searchStr ) );        
        $('#search_img').attr( "src", this.markerImage );

        if (this.hoverControl != null) {
            this.hoverControl.activate();
        }
        if (this.selectControl != null) {
            this.selectControl.activate();
        }
        updateLinks();
    };
    
    /**
     * Deactive this context by updating the GUI elements.
     */
    this.deactivate = function() {
        if (this.selectControl != null) {
            this.selectControl.deactivate();
        }
        if (this.hoverControl != null) {
            this.hoverControl.deactivate();
        }
        if (this.popup != null && this.popup.div != null) {
            this.popup.destroy();
            this.popup = null;
        }
    };
    
    /**
     * 
     */
    this.search = function( searchStr ) {
        try {
            this.searchStr = searchStr;
            //search_str = escape( search_str );
            //search_str = new OpenLayers.Format.JSON().write( search_str, false );
            //search_str = $.URLEncode( search_str );
            //search_str = jQuery.param( search_str, true );
            this.searchStr = encodeURIComponent( this.searchStr );
            this.searchURL = search_field_config.search_url + "?search=" + this.searchStr + "&outputType=JSON&srs=" + mapCRS;

            updateLinks();
            
            if (this.layer != null) {
                map.removeLayer( this.layer );
            }
            if (this.selectControl != null) {
                map.removeControl( this.selectControl );
            }
            if (this.hoverControl != null) {
                map.removeControl( this.hoverControl );
            }
            
            // new layer / style
            var style = new OpenLayers.Style({
                'externalGraphic': this.markerImage,
                'graphicHeight': 28,
                'graphicWidth': 36,
                'graphicXOffset': -10.5,
                'graphicYOffset': -12.5
            });
            var styleMap = new OpenLayers.StyleMap({'default':style});

            this.layer = new OpenLayers.Layer.GML( "Suchergebnis",
                this.searchURL, { 
                format: OpenLayers.Format.GeoJSON,
                styleMap: styleMap,
                attribution: '<a href="http://www.landkreis-mittelsachsen.de/">Landkreis Mittelsachsen</a>'
            });
            map.addLayer( this.layer );
 
//          // hover feature
            var fh_cb = callback( this.onFeatureHovered, {scope:this} );
            this.hoverControl = new OpenLayers.Control.SelectFeature( this.layer, {
                hover: true,
                highlightOnly: true,
                clickout: true
                //onSelect: fh_cb
            });
            map.addControl( this.hoverControl );
            this.hoverControl.activate();      
            this.layer.events.register( "featurehighlighted", this.layer, fh_cb );

            // select feature
            this.selectControl = new OpenLayers.Control.SelectFeature( this.layer, {
                clickout: true
//                onSelect: function( ev ) {
//                    alert( "Selected feature: " + ev.feature.id );
//                }
            });
            map.addControl( this.selectControl );
            this.selectControl.activate();
            var fs_cb = callback( this.onFeatureSelected, {scope:this} );
            this.layer.events.register( "featureselected", this.layer, fs_cb );

//            var ico = new OpenLayers.Icon( this.markerImage, new OpenLayers.Size(36, 28) );
//            this.layer = new OpenLayers.Layer.GeoRSS( 'Suchergebnis', this.searchURL, { 
//                'icon' : ico, 
//                styleMap : new OpenLayers.StyleMap( {
//                    // Set the external graphic and background graphic images.
//                    externalGraphic: "images/marker_yellow.png",
//                    backgroundGraphic: "images/marker_shadow.png",
//
//                    // Makes sure the background graphic is placed correctly relative
//                    // to the external graphic.
//                    backgroundXOffset: 0,
//                    backgroundYOffset: -7,
//
//                    // Set the z-indexes of both graphics to make sure the background
//                    // graphics stay in the background (shadows on top of markers looks
//                    // odd; let's not do that).
//                    graphicZIndex: 11,
//                    backgroundGraphicZIndex: 10,
//
//                    pointRadius: 10
//                } ),
//                isBaseLayer: false,
//                rendererOptions: {yOrdering: true}
////                markerClick: function( ev ) {
////                    alert( "ev= " + ev );
////                }
//            } );
//            map.addLayer( this.layer );

//            // select markers
//            map.addControl( new OpenLayers.Control.SelectFeature( search_layer,
//                    {onSelect: onFeatureSelect, onUnselect: onFeatureUnselect} ) );

            var cb = callback( this.onLoad, {scope:this} );
            this.layer.events.register( "loadend", this.layer, cb );
            this.layer.events.register( "loadcancel", this.layer, cb );
            
//            // tooltips
//            this.layer.events.register( "mouseover", this.layer, function( ev ) {
//                tooltips.show( {html:"My first ToolTips"} );
//            });
//            this.layer.events.register( "mouseout", this.layer, function( ev ) {
//                tooltips.hide();
//            });
//
//            var tooltips = new OpenLayers.Control.ToolTips({
//                bgColor: "#ffffd0",
//                //textColor: "black", 
//                bold: true, 
//                opacity: 1 });
//            map.addControl( tooltips );
        } 
        catch( e ) {
            throw e;
            //alert( "Problem searching: " + e );
        }
    };

    /**
     * Callback for async loading of the GeoRSS layer. Build the popups
     * and zooms the map to the data extent.
     */
    this.onLoad = function() {
        var result_element = $("#result_body" + this.index );

        var close_icon = new OpenLayers.Icon( "images/add_obj.gif" );

        var result_html = "";
        for (i=0; i<this.layer.features.length; i++) {
            var feature = this.layer.features[i];
            result_html += "<b><a href=\"javascript:onFeatureSelect(" + this.index + ", '" + feature.id + "');\">" + feature.data.title + "<a></b><br/>"; 
            result_html += "<span>" + feature.data.description + "</span>";
            result_html += "<hr/> ";

        }
        result_element.html( result_html );

        if (this.layer.features.length > 0) {
            map.zoomToExtent( this.layer.getDataExtent() );
            onFeaturesLoaded( this.layer.features );
            this.activate();
        }
    };
    
    /**
     * Callback for 'selectfeature' event.
     * 
     * @param ev
     * @return
     */
    this.onFeatureSelected = function( ev ) {
        this.openPopup( ev.feature.id );
    };

    /**
     * 
     * @param fid The fid of the feature to popup.
     * @return
     */
    this.openPopup = function( fid ) {
        // remove old popup
        if (this.popup != null && this.popup.div != null) {
            this.popup.destroy();
        }
        
        var feature = null;
        for (i=0; i<this.layer.features.length; i++) {
            var f = this.layer.features[i];
            if (f.id == fid) {
                feature = f;
                break;
            }
        }
        
        // create popup
        var popup_html = "<b>" + feature.data.title + "</b><br/>";
        popup_html += feature.data.description;
        var anchor = {
            size: new OpenLayers.Size( 36, 28 ),
            offset: new OpenLayers.Pixel( 0, 0 )
        };
        this.popup = new OpenLayers.Popup.AnchoredBubble( feature.id, 
                feature.geometry.getBounds().getCenterLonLat(),
                new OpenLayers.Size( 400, 400 ),
                popup_html,
                anchor, 
                true   //closeBox
                );
        feature.popup = this.popup;
        this.popup.feature = feature;
        map.addPopup( this.popup );
        return null;
    };

    /**
     * Callback for 'featurehighlighted' event.
     * 
     * @param ev
     * @return
     */
    this.onFeatureHovered = function( ev ) {
        alert( "Hovered: " + ev.feature.id );
        
//        // tooltips
//        this.layer.events.register( "mouseover", this.layer, function( ev ) {
//            tooltips.show( {html:"My first ToolTips"} );
//        });
//        this.layer.events.register( "mouseout", this.layer, function( ev ) {
//            tooltips.hide();
//        });

        if (this.tooltipsControl == null) {
            this.tooltipsControl = new OpenLayers.Control.ToolTips({
                bgColor: "#ffffd0",
                //textColor: "black", 
                bold: true, 
                opacity: 1 });
            map.addControl( tooltips );
        }
        this.tooltipsControl.show( {html:ev.feature.id} );
    };

}

/**
 * Updates the GeoRSS, KML, Bookmark, Link links. 
 */
function updateLinks() {
//  $("#result_header" + this.index).html( "" + this.searchStr );

    // GeoRSS
    var searchStr = contexts[result_index].searchStr;
    var searchURL = search_field_config.search_url + "?search=" + searchStr;
    if (searchStr.length > 0) {
        $("#georss_link").attr( "href", searchURL);
        $("#georss_link").attr( "target", "_blank" );
    } else {
        $("#georss_link").attr( "href", "javascript:alert('Dieser Link ermöglicht das Abonieren einer Suchanfrage. Geben Sie zuerst eine Suchabfrage ein.')" );
    }

    // KML
    var parts = searchURL.split( "?" );
    var kmlURL = parts[0] + "/atlas.kml?" + parts[1] + "&outputType=kml";
    if (searchStr.length > 0) {
        $("#kml_link").attr( "href", kmlURL );
    } else {
        $("#kml_link").attr( "href", "javascript:alert('Dieser Link ermöglicht die Anzeige in GoogleEarth. Geben Sie zuerst eine Suchabfrage ein.')" );
    }
    
    // ?search=XY&search2=AB
    var allSearchParams = null;
    for (i=0; i<contexts.length; i++) {
        var context = contexts[i];
        if (context.searchStr != null && context.searchStr.length > 0) {
            allSearchParams = allSearchParams == null 
                    ? "?" : allSearchParams + "&";
            allSearchParams += "search" + (i+1) + "=" + context.searchStr;
        }
    }    
    var pageURL = location.protocol + "//" 
            + location.host + location.pathname + allSearchParams;
    
    if (searchStr.length > 0) {
        $("#bookmark_link").attr( "onClick", "createBookmark('Mittelsachsen Atlas','" + pageURL + "', '')" );
    } else {
        $("#bookmark_link").attr( "onClick", "alert('Dieser Link ermöglicht das Setzen eines Lesezeichens für eine Suchanfrage. Geben Sie zuerst eine Suchabfrage ein.')" );
    }

    if (searchStr.length > 0) {
        $("#link_input").attr( "value", pageURL );
        $("#html_link_input").attr( "value", "<iframe width=\"425\" height=\"350\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\" marginwidth=\"0\" src=\"" + pageURL + "\"></iframe>" );
        $("#link_link").attr( "onClick", "showLinkDialog()" );
    } else {
        $("#link_link").attr( "onClick", "alert(\"Dieser Link ermöglicht das Versenden und Speichern von Suchanfragen. Geben Sie zuerst eine Suchabfrage ein.\")" );
    }
}

/**
 * 
 */
function createBookmark( title, url, comment ) {
    if (window.sidebar) {
        // firefox
        window.sidebar.addPanel( title, url, comment );
    } 
    else if (window.opera && window.print) {
        // opera
        var elem = document.createElement('a');
        elem.setAttribute('href', url);
        elem.setAttribute('title', title);
        elem.setAttribute('rel', 'sidebar');
        elem.click();
    } 
    else if (document.all) {
        // ie
        window.external.AddFavorite( url, title );
    }
}

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
    
//    map = new OpenLayers.Map(
//            {  div:"map", projection: "EPSG:4326" ,displayProjection: "EPSG:31468"                                         
//			//, maxExtent:      new OpenLayers.Bounds( 4490874.464054939 , 4717726.787148177 , 5559115.960922469,5731895.072413327 )
//            //, maxExtent: new OpenLayers.Bounds(4483170.793914523 , 5543510.242838027  , 4724642.258884646, 5751672.1422677105)  
//			//, maxExtent: new OpenLayers.Bounds( 4543969, 5614262, 4614589, 5677937 )
//			 , maxExtent: new OpenLayers.Bounds(10.673217773438,49.432983398437 , 16.034545898438,52.113647460937) 
//			, maxResolution: 500000
//			, maxScale: 2000
//			, minScale: 300000
//			//, numZoomLevels: 19,        //16
//			/*, units:'m'*/ });
//    //map.moveTo( new OpenLayers.Bounds( 4543969, 5614262, 4614589, 5677937 ).getCenterLonLat(), 15 );
    
    
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
 * Init the map component and the layers. 
 */
function initMap() {
    // DOP *********
    dop = new OpenLayers.Layer.WMS( "DOP.Sachsen", 
//          "http://www.landesvermessung.sachsen.de/ias/basiskarte4/service/SRV4ADV_P_DOPRGB/WMSFREE_TK/wmsservice?",
//          "http://www.landesvermessung.sachsen.de/ias/basiskarte4/service/SRV4DOPFREE/WMSFREE_TK/WMSFREE_TK/wmsservice?",  // layer: MS
        "../services/Atlas-Hintergrund",
        { layers : 'DOP-RGB',
          format: 'image/jpeg' },
        {
          projection: new OpenLayers.Projection( "EPSG:3857" ),
          transparent: true,
          isBaseLayer: false,
          buffer: 0
    });
    //dop.setTileSize( new OpenLayers.Size( 400, 400 ) );
    dop.setVisibility( false );
    dop.buffer = 0;

    // Mapnik **********
    var fiveDays = 5*24*60*60;
    mapnik = new OpenLayers.Layer.OSM( "OSM Mapnik TileServer", 
        "osmcache/${z}/${x}/${y}.png?transparent=F1EEE8&targetBaseURL=http://tile.openstreetmap.org&expires=" + fiveDays, {  // "http://tile.openstreetmap.org/${z}/${x}/${y}.png", {
        buffer: 0,
        transitionEffect: 'resize'
    });
    mapnik.buffer = 0;
    // override default EPSG code
    aliasproj = new OpenLayers.Projection( "EPSG:3857" );
    mapnik.projection = aliasproj;
    
    // Hillshade **********
    hillshade = new OpenLayers.Layer.OSM( "Hillshade", 
        "osmcache/${z}/${x}/${y}.png?targetBaseURL=http://toolserver.org/~cmarqu/hill&expires=" + fiveDays, {  // "http://toolserver.org/~cmarqu/hill/${z}/${x}/${y}.png", {
        isBaseLayer: false,
        visibility: false,
        attribution: '<a href="http://nasa.gov/">NASA SRTM</a>',
        buffer: 0,
        transitionEffect: 'resize'
    });
    hillshade.setZIndex( 1000 );
    hillshade.projection = aliasproj;
    
    var extent = new OpenLayers.Bounds( 12.50, 50.70, 13.50, 51.27 )
        .transform( 
            new OpenLayers.Projection("EPSG:4326"),
            new OpenLayers.Projection("EPSG:900913"));

    // Map ************
    map = new OpenLayers.Map( {
        div: "map",
        projection: new OpenLayers.Projection( "EPSG:900913" ),
        displayProjection: new OpenLayers.Projection( "EPSG:4326" ),
        maxExtent: extent,
        restrictedExtent: extent
    });

    map.addLayer( mapnik );
    map.addLayer( dop );
    map.addLayer( hillshade );
    map.zoomToMaxExtent();

    map.setCenter( new OpenLayers.LonLat( 13.186, 51.002 )
        .transform(
            new OpenLayers.Projection( "EPSG:4326" ), // transform from WGS 1984
            new OpenLayers.Projection( "EPSG:900913" ) // to Spherical Mercator Projection
        ), 10 // Zoom level
    );

    // Controls *********
    var overview = new OpenLayers.Control.OverviewMap( {
        'autoActivate':true, 
        'size':new OpenLayers.Size( 210, 170 )
    });
    map.addControl( overview );
    overview.activate();
    overview.maximizeControl();
    
    map.addControl( new OpenLayers.Control.PanZoomBar() );
    var scaleLine = new OpenLayers.Control.ScaleLine();
    scaleLine.geodesic = true;
    map.addControl( scaleLine );
    map.addControl( new OpenLayers.Control.MousePosition() );
    map.addControl( new OpenLayers.Control.LayerSwitcher() );
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

/**
 * 
 * @param feature
 * @return
 */
function onFeatureSelect( contextIndex, fid ) {
    contexts[contextIndex].openPopup( fid );
}

///**
// * 
// * @param feature
// * @return
// */
//function onFeatureUnselect( feature ) {
//    alert( feature );
////    map.removePopup( feature.popup );
////    feature.popup.destroy();
////    feature.popup = null;
//}    

/**
 * Callback function to be notified when features are loaded into a
 * SearchContext.
 * 
 * @param SearchContext The SearchContext the features were loaded into.
 * @param features The features.
 * @return
 */
onFeaturesLoaded = function( context, features ) {
    // do nothing here;
};

function show_dialog( title, contentURL ) {
    $.ajax({
        url: contentURL,
        dataType: "html",
        success: function( data ) {
          $('#dialog').html( data );
          $('#dialog').dialog({ width:640 , height: 560 , title:title });
        }
    });
}

function showLinkDialog() {
    $('#link_dialog').css( "visibility", "visible" );
    $('#link_dialog').dialog({ width:350 , height: 240 , title:'Link speichern' });
}

function showPoiDialog() {
    var contentURL = "poi_form.html";
    $.ajax({
        url: contentURL,
        dataType: "html",
        success: function( data ) {
          $('#dialog').html( data );
          $('#dialog').dialog({ width:400 , height: 430 , title:"Einen neuen Ort anlegen" });
        }
    });
}


function edit_poi_start(data) {
    if (window.location.hostname == 'localhost') {        
        // local test
        var iframe_url = 'http://localhost:8080/orbeon/xforms-sandbox/run?';
        var form_url = 'http://localhost:10080/lka/poi_form.xhtml';
    }
    else {
        // connected from internet
        var iframe_url = xforms_config.iframe_url;
        var form_url = xforms_config.form_url;
    }
    window.alert( 'IFrame URL: ' + iframe_url + '\nForm base URL: ' + form_url );
    
    $("#edit_poi_div").css( "height", "300px" );
    $("#edit_poi_div").html( '<iframe '
            + 'src="' + iframe_url + 'url=' + form_url + '" '
            + 'frameborder="0" align="center" width="97%" height="330" name="edit" />' );
    
}
