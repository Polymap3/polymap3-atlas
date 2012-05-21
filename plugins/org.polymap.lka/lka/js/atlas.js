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
 * Instantiate this to create the Atlas environment.
 * <p/>
 * Extend this class and override one of the #initXXX() methods to
 * change/extend default init behaviour. The ctor has a mandatory
 * argument <code>config</code> which defines the configuration of
 * the Atlas environment:
 * 
 * <pre>
 * {
 *     autocompleteUrl: 'search',
 *     searchUrl: 'search',
 *     baseUrl: 'index.html',
 *     mapCRS: 'EPSG:900913'
 * }
 * </pre>
 */
var AtlasClass = Class.extend( new function AtlasClassProto() {
    
    /** @private The conciguration of the Atlas environment. */
    this.config = null;
    
    /** Array of {@link SearchContext} instances. */
    this.contexts = null;
    
    /** Array of {@link SearchPriority} instances. */
    this.priorities = null;

    /** The currently active SearchContext: 0->red, 1->green... */
    this.result_index = 0;

    /** The OpenLayers map. */
    this.map = null;
    
    this.layers = null;
    
    this.events = null;
    
    
    this.init = function( config ) {
        // XXX a bit hacky; allow the init methods to access the global Atlas
        Atlas = this;
        
        this.events = new Events();
        this.config = config;
        this.priorities = [];
        this.initMap();
        this.initContexts();
        this.initUI();
        this.initUrlParams();
    };
    
    /**
     * 
     */
    this.search = function() {
        var searchStr = $("#search_field").val();
        this.contexts[this.result_index].search( searchStr );
    };
    
    /**
     * 
     */
    this.initContexts = function() {        
        // create search contexts
        this.contexts = [
            new SearchContext( this.map, 0, "images/marker_red.png", $("#result_body0"), '#f30000' ),
            new SearchContext( this.map, 1, "images/marker_green.png", $("#result_body1"), '#00AB39' ),
            new SearchContext( this.map, 2, "images/marker_yellow.png", $("#result_body2"), '#B48400' ),
            new SearchContext( this.map, 3, "images/marker_blue.png", $("#result_body3"), '#2243FF' ) 
        ];
        // initialize contexts
        for (var i=0; i < this.contexts.length; i++) {
            this.contexts[i].attribution = 'Daten von <a href="http://polymap.de">Test</a>';
            this.contexts[i].mapCRS = this.config.mapCRS;
        };

        // transform URLs in feature fields into links
        new UrlFieldFeatureResultEnhancer();

        return this;
    };
    
    /**
     * 
     */
    this.initUI = function() {
        var self = this;
        // search context tabs
        $('#tabs').tabs(); // first tab selected
        $('#tabs').bind( 'tabsselect', function( ev, ui ) {
            //ui.tab     // anchor element of the selected (clicked) tab
            //ui.panel   // element, that contains the selected/clicked tab contents
            self.contexts[self.result_index].searchStr = $('#search_field').val();
            self.contexts[self.result_index].deactivate();     

            self.result_index = ui.index;   // zero-based index of the selected (clicked) tab
            self.contexts[self.result_index].activate();
        });

        // search field
        $('#search_field').autocomplete({ 
            source: this.config.autocompleteUrl,
            zIndex: 1000,
            delay: 500
        });
        
        $('#search_field').keypress( function( ev ) {
            if (ev.keyCode == 13) {
                self.search();
                $(this).autocomplete( "close" );
            }
        });

        // search button
        $('#search_btn')
            .button( {label: 'Suchen'} )    
            .click( function( ev ) { self.search(); } );
    };
    
    /**
     * 
     */
    this.initMap = function() {
        // DOP *********
        this.dop = new OpenLayers.Layer.WMS( "DOP", 
            "../services/Atlas-Hintergrund", 
            { layers : 'roads', format: "image/jpeg" },
            { transparent: true, isBaseLayer: false, transitionEffect: 'resize' } );
        this.dop.setVisibility( false );
        this.dop.buffer = 0;
        this.dop.attribution = '<a href="http://www.landesvermessung.sachsen.de/inhalt/geo/basis/basis_dienste.html#geosn">GeoSN</a>';

        // Mapnik *********
        var fiveDays = 7*24*3600;
        this.mapnik = new OpenLayers.Layer.OSM( "OSM Mapnik",
            // "http://tile.openstreetmap.org/${z}/${x}/${y}.png", {
            "osmcache/${z}/${x}/${y}.png?targetBaseURL=http://tile.openstreetmap.org&expires=" + fiveDays, 
            { transitionEffect: 'resize', isBaseLayer: true } );
        var aliasproj = new OpenLayers.Projection( "EPSG:3857" );
        this.mapnik.projection = aliasproj;
        this.mapnik.buffer = 0;

        // Border *********
        this.border = new OpenLayers.Layer.WMS( "Kreisgrenze",
            "../services/Atlas-Hintergrund",
            { layers : 'Kreisgrenze2', transparent: true, isBaseLayer: false } );
        this.border.setVisibility( true );
        this.border.buffer = 0;
        this.border.attribution = 'powered by <b><a target="_blank" href="http://polymap.org/polymap3"> POLYMAP3</a></b>';
        
        // Map ************

        // set transformation functions from/to alias projection
        OpenLayers.Projection.addTransform( "EPSG:4326", "EPSG:3857", OpenLayers.Layer.SphericalMercator.projectForward );
        OpenLayers.Projection.addTransform( "EPSG:3857", "EPSG:4326", OpenLayers.Layer.SphericalMercator.projectInverse );

        // avoid pink tiles
        OpenLayers.IMAGE_RELOAD_ATTEMPTS = 3;
        OpenLayers.Util.onImageLoadErrorColor = "transparent";
        OpenLayers.Util.MISSING_TILE_URL = "http://openstreetmap.org/openlayers/img/404.png";

        var extent = new OpenLayers.Bounds( 11.50, 50.00, 15.25, 52.40 )
            .transform( new OpenLayers.Projection("EPSG:4326"), new OpenLayers.Projection("EPSG:900913") );
        
        var mapOptions = {
            div: "map",
            projection: new OpenLayers.Projection( "EPSG:900913" ),
            displayProjection: new OpenLayers.Projection( "EPSG:4326" ),
            maxExtent: extent,
            restrictedExtent: extent
        };
        this.map = new OpenLayers.Map( mapOptions );

        // border visible if scale >= 150000
        var border = this.border;
        this.map.events.register( 'moveend', this.map, function (e) {
            border.setVisibility( this.getScale() >= 150000 );
        });

        this.map.addLayer( this.mapnik );
        this.map.addLayer( this.dop );
        this.map.addLayer( this.border );
        this.map.zoomToMaxExtent();

        this.map.setCenter( new OpenLayers.LonLat( 13.50, 51.00 )
            .transform( new OpenLayers.Projection( "EPSG:4326" ), new OpenLayers.Projection( "EPSG:900913" ) ),
            9 // Zoom level
        );

        // Controls *********
        var overview = new OpenLayers.Control.OverviewMap( {
            autoActivate: true, 
            size: new OpenLayers.Size( 210, 170 ),
            mapOptions: mapOptions
        });

        if ($(document).getUrlParam( 'north' ) != 'off') {
            this.map.addControl( new OpenLayers.Control.PanZoomBar() );
            this.map.addControl( overview );
            //overview.activate();
            //overview.maximizeControl();
        }
        var scaleLine = new OpenLayers.Control.ScaleLine();
        scaleLine.geodesic = true;
        this.map.addControl( scaleLine );
        this.map.addControl( new OpenLayers.Control.MousePosition() );
    };

    /**
     * Handle URL params for preset searches. 
     */
    this.initUrlParams = function () {
        // wait for the system to setup; especially allow registering
        // listeners that tweak seachStr and result handling
        var self = this;
        setTimeout( function() {
            //
            for (var i=3; i>=0; i--) {
                var search_str = $(document).getUrlParam( 'search'+ (i+1) );
                if (search_str != null) {
                    self.contexts[i].search( decodeURIComponent( search_str ) );
                }
            }

            // wait for all context to load -> activate context[0]
            setTimeout( function() {
                self.contexts[0].activate();

                var bounds = null;
                for (var i=3; i>=0; i--) {
                    var context = self.contexts[i];
                    if (context.layer != null && context.layer.features.length > 0) {
                        if (bounds == null) {
                            bounds = context.layer.getDataExtent()
                        } else {
                            bounds.extend( context.layer.getDataExtent() );
                        }
                    }
                }
                if (bounds != null) {   
                    self.map.zoomToExtent( bounds );
                    if (self.map.getScale() < 20000) {
                        self.map.zoomToScale( 20000, false );
                    }
                }
            }, 2000 );
            
        }, 1000 );        
    };

});


/**
 * 
 * 
 */
var UrlFieldFeatureResultEnhancer = Class.extend( new function() {
   
    this.init = function() {
        Atlas.events.bind( 'searchFeatureLoaded', function( ev ) {
            $.each( ev.feature.data, function( name, value ) {
                if (value == null || typeof value != 'string') {
                    return;
                }
                else if (value.indexOf( "http://") == 0) {
                    ev.feature.data[name] = "<a href=\"" + value + "\" target=\"_blank\">" + value.substring(7) + "</a>";
                }
                else if (value.indexOf( "www.") == 0) {
                    ev.feature.data[name] = "<a href=\"http://" + value + "\" target=\"_blank\">" + value + "</a>";
                }
                else if (value.indexOf( "@") > 0) {
                    ev.feature.data[name] = "<a href=\"mailto:" + value + "\" target=\"_blank\">" + value + "</a>";
                }
            });
        });
    };
    
});

