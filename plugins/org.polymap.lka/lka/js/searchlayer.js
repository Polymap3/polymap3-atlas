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
 * @author falko
 */
SearchLayer = Class.extend( new function SearchLayerProto() {
    
    this.context = null;
    
    this.map = null;
    
    this.config = null;
    
    this.layer = null;
    
    this.features;
    
    this.events = null;

    this.controls;
    
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
        
        this.clustered = new OpenLayers.Strategy.Cluster({
            distance: 25,
            threshold: 2
        });

        //this.initClusterLayer();
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
            if (!ev.feature.cluster) {
                return;
            }
            self.openClusterPopup( ev );
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
        
        var popup = $('#cluster-popup');
        $.each( ev.feature.cluster, function( i, feature ) {
            popup.append( '> <a id="cluster-popup-link-{1}" href="#">{0}</a><br/>'
                    .format( feature.data.title, i ) );
            var link = popup.find( "#cluster-popup-link-{0}".format(i) );
            
            // focus
            link.hover( function( ev ) {
                var div = self.context.findResultDiv( feature );
                self.context.resultDiv.scrollTo( div, 1000 );
            });
            // click
            link.click( function( ev ) {
                var div = self.context.findResultDiv( feature );
                div.find( 'b>a' ).trigger( 'click' );
                self.closeClusterPopup();
                self.selectControl.unselect( clusterFeature );
            });
        });
        popup.mouseleave( function( ev ) {
            self.closeClusterPopup();
            self.selectControl.unselect( clusterFeature );            
        });

        // keyHandler
        $(document).keypress( this.keyHandler = function( ev ) { 
            if (ev.keyCode == 27) {
                self.closeClusterPopup();
                self.selectControl.unselect( clusterFeature );
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
        $(document).unbind( 'keypress', this.keyHandler ); 
        $(document).unbind( 'click', this.clickHandler ); 
        
        $('#cluster-popup').fadeOut( 1700, function() {
            $(this).remove();
        });
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
    this.close = function() {
        this.map.removeLayer( this.layer );
        for (i=0; i<this.controls.length; i++) {
            this.map.removeControl( this.controls[i] );
        }
        this.controls = [];
    };

    /**
     * 
     */
    this.initLayer = function() {
        var self = this;
        
        var noClusterFilter = new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: 'count',
            value: undefined,
        });
        var inClusterFilter = new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.GREATER_THAN,
            property: 'count',
            value: 1
        });
        var clusterRule = new OpenLayers.Rule({
            filter: inClusterFilter,
            symbolizer: {
                'pointRadius': '${radius}',
                'label': '${label}',
                'fontSize': '11px',
                'fontColor': "#ffffff",
                'fontWeight': 'bold'
            }
        });
        var noClusterRule = new OpenLayers.Rule({
            filter: noClusterFilter,
            symbolizer: {
                'externalGraphic': self.config.markerImage
            }
        });
        var defaultStyle = new OpenLayers.Style({
            'graphicTitle': '${tooltip}',
            'graphicHeight': 28,
            'graphicWidth': 36,
            'graphicXOffset': -10.5,
            'graphicYOffset': -12.5,
            'strokeWidth': 2,
            'strokeColor': self.config.color,
            'fillOpacity': '${opacity}',  //0.7,
            'fillColor': self.config.color  //'#a0a0a0'
        }, { 
            'context': {
                'radius': function( feature ) {
                    return feature.cluster ? Math.min(feature.attributes.count, 10) + 6 : 0;
                },
                'graphic': function( feature ) {
                    return feature.cluster ? undefined : self.config.markerImage;
                },
                'tooltip': function( feature ) {
                    return feature.cluster 
                            ? feature.attributes.count 
                            : feature.attributes.title;
                },
                'opacity': function( feature ) {
                    if (feature.geometry instanceof OpenLayers.Geometry.Point) {
                        return feature.cluster ? 0.35 : 1;
                    } else {
                        return 0.35;
                    }
                },
                'label': function( feature ) {
                    return feature.cluster ? feature.attributes.count : '';
                }
            } 
        });
        defaultStyle.addRules( [noClusterRule, clusterRule] );

        var selectStyle = new OpenLayers.Style({
            'strokeColor': '#A000A0',
            'strokeWidth': 3,
            'fillOpacity': 0.8,
            'fillColor': '#a0a0a0',
            'graphicHeight': 36,
            'graphicWidth': 46
        }, { 
            'context': {
                'radius': function( feature ) {
                    return feature.cluster ? Math.min(feature.attributes.count, 10) + 8 : 0;
                },
                'tooltip': function( feature ) {
                    return feature.cluster 
                            ? feature.attributes.count 
                            : feature.attributes.title;
                },
                'label': function( feature ) {
                    return feature.cluster 
                            ? feature.attributes.count 
                            : 'X';
                }
            } 
        });
        selectStyle.addRules([
            new OpenLayers.Rule({
                'filter': noClusterFilter,
                'symbolizer': {
                    'externalGraphic': 'images/marker_selected.png'
                }
            }),
            new OpenLayers.Rule({
                'filter': inClusterFilter,
                'symbolizer': {
                    'pointRadius': '${radius}',
                    'label': '${label}',
                    'fontSize': '12px',
                    'fontColor': "#ffffff",
                    'fontWeight': 'bold'
                }
            })
        ]);

        var styleMap = new OpenLayers.StyleMap({
            'default': defaultStyle,
            'select': selectStyle
        });

        this.layer = new OpenLayers.Layer.GML( "Suchergebnis",
            this.config.url, { 
            'reportError': true,
            'format': OpenLayers.Format.GeoJSON,
            'strategies': [this.clustered],
            'styleMap': styleMap
        });
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
    
    this.onLayerLoaded = function() {
        var self = this;

        // resolve clusters
        self.features = [];
        $.each( this.layer.features, function( i, feature ) {
            if (feature.cluster) {
                self.features = self.features.concat( feature.cluster );
            } else {
                self.features.push( feature );
            }
        });
        
//        // fill clusterLayer
//        if (this.clusterLayer) {
//            var clusterFeatures = [];
//            $.each( this.features, function( i, feature ) {
//                if (feature.geometry instanceof OpenLayers.Geometry.Point) {
//                    var geom = feature.geometry;
//                    clusterFeatures.push( new OpenLayers.Feature.Vector( 
//                        new OpenLayers.Geometry.Point( geom.x, geom.y ), 
//                        feature.attributes ) );
//                }
//            })
//            self.clusterLayer.addFeatures( clusterFeatures );
//            self.map.addLayer( self.clusterLayer );
//            self.clusterLayer.setVisibility( true );
//        }
        // callback
        this.config.onLoaded.call( {} );
    };

    /**
     * 
     */
    this.initClusterLayer = function() {
        var self = this;
        var clusterDefaultStyle = new OpenLayers.Style({
            'pointRadius': '${radius}',
            'graphicTitle': '${tooltip}',
            'label': '${label}',
            'fontSize': '10px',
            'fontColor': "#ffffff",
            'strokeWidth': 2,
            'strokeColor': self.config.color,
            'fillOpacity': 0.7,
            'fillColor': '#a0a0a0'
        }, {
            'context': {
                'radius': function( feature ) {
                    return feature.cluster
                            ? Math.min(feature.attributes.count, 15) + 3
                            : 5;
                },
                'tooltip': function( feature ) {
                    return feature.cluster 
                            ? feature.attributes.count 
                            : '';
                },
                'label': function( feature ) {
                    return feature.cluster 
                            ? feature.attributes.count 
                            : 'X';
                }
            }
        });
        var clusterSelectStyle = new OpenLayers.Style({
            'strokeWidth': 3,
            'fillOpacity': 1
        });

        this.clusterLayer = new OpenLayers.Layer.Vector( 'Clustered', {
            'isBaseLayer': false,
            'visibility': true,
            'reportError': true,
            'styleMap': new OpenLayers.StyleMap({
                'default': clusterDefaultStyle,
                'select': clusterSelectStyle
            }),
            'strategies': [this.clustered],
            'protocol': new OpenLayers.Protocol()
        });
    };

    /**
     * 
     */
    this.getDataExtent = function() {
        return this.layer ? this.layer.getDataExtent() : null;
    }
});