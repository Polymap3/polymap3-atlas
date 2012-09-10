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
 * Creates StyleMap used by a SearchLayer. 
 */
SearchLayerStyle = Class.extend( new function SearchLayerStyleProto() {
  
    /** {SearchLayer} */
    this.searchLayer = null;
    
    this.config = null;
    
    /**
     * 
     */
    this.init = function( searchLayer  ) {
        this.searchLayer = searchLayer;
        this.config = searchLayer.config;
    };

    /**
     * 
     */
    this.newStyleMap = function() {
        var self = this;
        var noClusterFilter = new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: 'count',
            value: undefined
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
            'graphicYOffset': -20,
            'strokeWidth': 3,
            'strokeColor': self.config.color,
            'fillOpacity': '${opacity}',  //0.7,
            'fillColor': self.config.color  //'#a0a0a0'
        }, { 
            'context': {
                'radius': function( feature ) {
                    return feature.cluster ? Math.min(feature.attributes.count, 15) + 9 : 0;
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
                    if (feature.cluster) {
                        return 0.6;
                    } else {
                        return feature.geometry instanceof OpenLayers.Geometry.Point ? 0.80 : 0.35;
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
            'strokeWidth': 4,
            'fillOpacity': 0.8,
            'fillColor': '#a0a0a0',
            'graphicHeight': 36,
            'graphicWidth': 46
        }, { 
            'context': {
                'radius': function( feature ) {
                    return feature.cluster ? Math.min(feature.attributes.count, 10) + 12 : 0;
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

        return new OpenLayers.StyleMap({
            'default': defaultStyle,
            'select': selectStyle
        });    
    };
    
//    this.newStyleMap = function() {
//        var defaultStyle = new OpenLayers.Style({
//            'graphicTitle': 'overriden',
//            'graphicHeight': 28,
//            'graphicWidth': 36,
//            'graphicXOffset': -10.5,
//            'graphicYOffset': -28,
//            'strokeWidth': 3,
//            'strokeColor': '#f00000',
//            'fillOpacity': 0.7,
//            'fillColor': '#a0a0a0'
//        });
//        return new OpenLayers.StyleMap({
//            'default': defaultStyle,
//            'select': defaultStyle
//        });
//    }; 
});

/**
 * 
 */
OpenLayers.Strategy.AtlasCluster = OpenLayers.Class( OpenLayers.Strategy.Cluster, {
    
    /**
     * Method: shouldCluster
     * Determine whether to include a feature in a given cluster.
     * 
     * Only Point geometries are clustered. All other geometry types are never
     * clustered.
     *
     * Parameters:
     * cluster - {<OpenLayers.Feature.Vector>} A cluster.
     * feature - {<OpenLayers.Feature.Vector>} A feature.
     *
     * Returns:
     * {Boolean} The feature should be included in the cluster.
     */
    shouldCluster: function( cluster, feature ) {
        if (!(feature.geometry instanceof OpenLayers.Geometry.Point)) {
            return false;
        }
        else {
            return OpenLayers.Strategy.Cluster.prototype.shouldCluster.apply( this, arguments );
        }
    },
    
    CLASS_NAME: "OpenLayers.Strategy.AtlasCluster" 
});
