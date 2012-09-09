/*
 * polymap.org
 * Copyright 2012, Polymap GmbH. All rights reserved.
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
 * API and implementation of the routing service. This class
 * sends request the the server and handles the response.
 */
var RoutingService = Class.extend( new function RoutingProto() {

    /** {@link OpenLayers.Projection} EPSG:4326 */
    this.projection = null;
    
    /** 
     * {@link OpenLayers.Projection} The projection of the Geometry arguments of
     * the methods. 
     */
    this.mapProjection = null;
    
    /**
     * URL: http://<domain>:<port>/<root>/<version>/<profile key>/catch.<output>?json={<parameter>}
     * 
     * @param baseUrl http://<domain>:<port>/<root>/<version>/
     * @param profileKey
     */
    this.init = function( baseUrl, profileKey ) {
       this.baseUrl = baseUrl;
       this.profileKey = profileKey;
       this.projection = new OpenLayers.Projection( "EPSG:4326" );
       this.mapProjection = Atlas.map.getProjectionObject();
       this.attribution = 'Routing <b><a target="_blank" href="http://osm2po.de">osm2po</a></b> by Carsten Möller' ;
    };

    /**
     * @param point {OpenLayers.Geometry.Point}
     * @param distance {Number}
     * @param mode {String} 'length' or 'cost'
     * @param callback {Function} Function with 2 arguments: {boolean} status,
     *            {OpenLayer.Feature.Vector} feature
     */
    this.driveTimePolygon = function( point, distance, mode, callback ) {
        var mapProjection = Atlas.map.getProjectionObject();
        var projection = this.projection;
        
        var transformed = point.clone().transform( mapProjection, projection );

        var maxCost = mode == 'cost'
                ? +(distance) / 60     // minutes -> hours
                : +(distance) / 1000;  // m -> km
        
        var params = 'cmd=fx&format=geojson'
                + '&source=' + transformed.x + ',' + transformed.y 
                + '&maxCost=' + maxCost; 
        
        var url = this.baseUrl + '?' + params;

        var self = this;
        $.ajax( {
            url: url,
            dataType: "html",
            success: function( data ) {
                var featureJson = '{' +
                    '"type": "Feature",' +
                    '"geometry": ' + data + ',' +
                    '"properties": {"name": "DriveTimePolygon"}}';

                var features = new OpenLayers.Format.GeoJSON().read( featureJson );
                var feature = features[0];
                
//                var points = [];
//                for (var i =0; i<feature.geometry.components.length; i++) {
//                    var point = feature.geometry.components[i];
//                    points.push( new OpenLayers.Geometry.Point( point.x, point.y ) );
//                }
                var points = feature.geometry.components;
                var geom = new OpenLayers.Geometry.Polygon( [new OpenLayers.Geometry.LinearRing( points )] );
                //var geom = feature.geometry;
                geom.transform( projection, mapProjection );
                var vector = new OpenLayers.Feature.Vector( geom, {} );

                callback.call( callback, true, vector );
            }
        });
    };
    
    /**
     * Find the shortest path between the given points.
     * 
     * @param fromPoint {OpenLayers.Geometry.Point}
     * @param toPoint {OpenLayers.Geometry.Point}
     * @param callback {function} called when ready.
     */
    this.shortestPath = function( fromPoint, toPoint, callback ) {
        var mapProjection = Atlas.map.getProjectionObject();
        var projection = this.projection;
        
        var fromTransformed = //new OpenLayers.Geometry.Point( fromPoint.x, fromPoint.y )
                fromPoint.clone().transform( mapProjection, projection );
        var toTransformed = //new OpenLayers.Geometry.Point( toPoint.x, toPoint.y )
                toPoint.clone().transform( mapProjection, projection );
        
        var params = 'cmd=fr&format=geojson'
                + '&source=' + fromTransformed.x + ',' + fromTransformed.y 
                + '&target=' + toTransformed.x + ',' + toTransformed.y; 
        
        var url = this.baseUrl + '?' + params;
        
        $.ajax( {
            url: url,
            dataType: "html",
            success: function( data ) {
                var features = new OpenLayers.Format.GeoJSON().read( data );
                
                var vectors = new Array( features.length );
                $.each( features, function( i, feature ) {
                    vectors[i] = new OpenLayers.Feature.Vector( feature.geometry
                            .transform( projection, mapProjection ), feature.data );
                });

                callback.call( callback, true, vectors );
            }
        });
    };
    
});

