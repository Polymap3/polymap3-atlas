/*
 * polymap.org
 * Copyright 2011-2012, Falko Bräutigam. All rights reserved.
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
 * Handles several layers, controls and styles for the result
 * features of a search request.
 * 
 * @requires js/inheritance.js
 */
SearchLayer = Class.extend( new function SearchLayerProto() {
    
    this.context = null;
    
    this.map = null;
    
    this.config = null;
    
    this.layer = null;
    
    /** Array of Feature instances. */
    this.features;
    
    this.events = null;

    this.controls;
    
    this.clusterPopup;
    
    /**
     * @param config {
     *     url: The URL the load the layer features from.
     *     markerImage: The markerImage in the map.
     *     attribution: Attribution of the layer.
     *     onFeatureHovered: Function to be called when feature is hovered.
     *     onLoaded: Function callback
     * }
     */
    this.init = function( context, config ) {
        this.context = context;
        this.map = Atlas.map;
        this.config = config;
        this.controls = [];
        this.features = [];
        
        this.clustered = new OpenLayers.Strategy.AtlasCluster({
            'distance': 22,
            'threshold': 2
        });

        this.initLayer();
        
        // hover feature
        this.hoverControl = new OpenLayers.Control.SelectFeature( 
            this.layer, {
            'hover': true,
            'highlightOnly': true
//            'eventListeners': {
//                'featureunhighlighted': function( feature ) {
//                    self.selectControl.unselect( feature );
//                }
//            }
        });
        this.controls.push( this.hoverControl );
        this.map.addControl( this.hoverControl );
        this.hoverControl.activate();
        this.layer.events.register( 'featurehighlighted', this.layer, this.config.onFeatureHovered );

        // select feature
        this.selectControl = new OpenLayers.Control.SelectFeature( 
            this.layer, {
            clickout: true
        });
        this.controls.push( this.selectControl );
        this.map.addControl( this.selectControl );
        this.selectControl.activate();

        var self = this;
        this.layer.events.register( 'featureselected', this.layer, function( ev ) {
            if (ev.feature.cluster && !self.clusterPopup) {
                self.openClusterPopup( ev );
            }
        });
    };
    
    /**
     * 
     */
    this.openClusterPopup = function( ev ) {
        var self = this;
        var clusterFeature = ev.feature;
        this.closeClusterPopup();
        
        var lonlat = ev.feature.geometry.getBounds().getCenterLonLat();
        var xy = this.map.getViewPortPxFromLonLat( lonlat );
        var mapOffset = $('#map').offset();
            
        $(document.body).append( 
                '<div id="cluster-popup" class="ui-widget-content ui-corner-all"' 
                + '   style="z-index:1000; position:absolute;'
                + '   left:{0}px; top:{1}px; padding:5px; box-shadow:2px 3px 3px #808080;">'
                        .format( xy.x + mapOffset.left - 10, xy.y + mapOffset.top - 10 )
                + '</div>' );
        
        this.clusterPopup = $('#cluster-popup');
 
        var scrollTimeout = null;

        $.each( ev.feature.cluster, function( i, feature ) {
            self.clusterPopup.append( '> <a id="cluster-popup-link-{1}" href="#">{0}</a><br/>'
                    .format( feature.data.title, i ) );
            var link = self.clusterPopup.find( "#cluster-popup-link-{0}".format(i) );
            
            // focus
            link.hover( function( ev ) {
                if (scrollTimeout) {
                    clearTimeout( scrollTimeout );
                }
                scrollTimeout = setTimeout( function() {
                    var div = self.context.findResultDiv( feature );
                    self.context.resultDiv.scrollTo( div, 1000 );
                }, 1750 );
            });
            // click
            link.click( function( ev ) {
                var div = self.context.findResultDiv( feature );
                div.find( '>a' ).trigger( 'click' );
                self.closeClusterPopup();
                self.selectControl.unselect( clusterFeature );
            });
        });
        
        // mouseleave
        this.clusterPopup.mouseleave( function( ev ) {
            self.closeClusterPopup();
            self.selectControl.unselect( clusterFeature );
            clearTimeout( scrollTimeout );
        });
        // keyHandler
        $(document).keypress( this.keyHandler = function( ev ) { 
            if (ev.keyCode == 27) {
                self.closeClusterPopup();
                self.selectControl.unselect( clusterFeature );
                clearTimeout( scrollTimeout );
            }
        });
        // clickHandler
        $(document).click( this.clickHandler = function( ev ) {
//            alert( ev );
//            this.closeClusterPopup();
        });
    };
    
    /**
     * 
     */
    this.closeClusterPopup = function() {
        if (this.clusterPopup) {
            var self = this;
            this.clusterPopup.fadeOut( 1000, function() {
                $(this).remove();
                self.clusterPopup = null;

                $(document).unbind( 'keypress', this.keyHandler ); 
                $(document).unbind( 'click', this.clickHandler ); 
            });
        }
    };
    
    this.activate = function() {
        for (i=0; i<this.controls.length; i++) {
            this.controls[i].activate();
        }        
    };

    this.deactivate = function() {
        for (i=0; i<this.controls.length; i++) {
            this.controls[i].deactivate();
        }        
    };
    
    /**
     * 
     */
    this.destroy = function() {
        for (i=0; i<this.controls.length; i++) {
            this.map.removeControl( this.controls[i] );
            this.controls[i].destroy();
        }

        if (this.layer) {
            this.map.removeLayer( this.layer );
            //this.layer.events.destroy();
            this.layer.destroy();
            this.layer = null;
        }
        
        this.controls = [];
    };

    /**
     * 
     */
    this.initLayer = function() {
        var self = this;

        // StyleMap
        var styleMap = this.newStyleMap();

        // search: URL
        if (this.config.url) {
            this.layer = new OpenLayers.Layer.GML( "Suchergebnis",
                this.config.url, { 
                'reportError': true,
                'format': OpenLayers.Format.GeoJSON,
                'strategies': [this.clustered],
                'styleMap': styleMap
            });
//            this.layer = new OpenLayers.Layer.Vector( "Suchergebnis", {
//                'reportError': true,
//                //'format': OpenLayers.Format.GeoJSON,
//                'strategies': [this.clustered],
//                'styleMap': styleMap,
//                'protocol': new OpenLayers.Protocol.HTTP({
//                    'url': this.config.url,
//                    'format': new OpenLayers.Format.GeoJSON()
//                })
//            });
        }        
        // search: GeoJSON feature
        else if (this.config.feature) {
            this.layer = new OpenLayers.Layer.Vector( "Suchergebnis", {
                'reportError': true,
                'strategies': [this.clustered],
                'protocol': new OpenLayers.Protocol(),
                'styleMap': styleMap
            });
            this.features = [this.config.feature];
            this.layer.addFeatures( this.features );
        }
        else {
            alert( 'Config does neither contain "url" nor "feature"!' );
        }
        
        this.layer.attribution = this.config.attribution;
        this.map.addLayer( this.layer );
        this.events = this.layer.events;

        this.layer.events.register( 'loadend', this.layer, function() {
            self.onLayerLoaded();    
        });
        this.layer.events.register( 'loadcancel', this.layer, function() {
            self.onLayerLoaded();            
        });
    };
    
    /**
     * Creates a new StyleMap for this layer.
     * 
     * @returns {OpenLayers.StyleMap} The StyleMap to be used for our layer. 
     */
    this.newStyleMap = function() {
        return new SearchLayerStyle( this ).newStyleMap();
    }
    
    /**
     * 
     */
    this.onLayerLoaded = function() {
        var self = this;

        // resolve clusters into single features
        self.features = [];
        $.each( this.layer.features, function( i, feature ) {
            if (feature.cluster) {
                self.features = self.features.concat( feature.cluster );
            } else {
                self.features.push( feature );
            }
        });
        
        // callback
        this.config.onLoaded.call( {} );
    };

    /**
     * 
     */
    this.getDataExtent = function() {
        return this.layer ? this.layer.getDataExtent() : null;
    }
});
