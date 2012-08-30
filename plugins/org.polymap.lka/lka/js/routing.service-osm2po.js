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
       this.attribution = 'Routing <b><a target="_blank" href="http://osm2po.de">osm2po</a></b> by Carsten Möller' ;
    };

    /**
     * @param point {OpenLayers.Geometry.Point}
     * @param distance {Number}
     * @param mode {String} 'length' or 'cost'
     * @param callback {Function}
     */
    this.driveTimePolygon = function( point, distance, mode, callback ) {
        var params = 'cmd=fx&format=geojson'
                + '&source=' + point.x + ',' + point.y 
                + '&maxCost=0.02' /* distance*/; 
        
        var url = this.baseUrl + '?' + params;

        $.ajax( {
            url: url,
            dataType: "html",
            success: function( data ) {
                var featureJson = '{' +
                    '"type": "Feature",' +
                    '"geometry": ' + data + ',' +
                    '"properties": {"name": "DriveTimePolygone"}}';

                var features = new OpenLayers.Format.GeoJSON().read( featureJson );
                var feature = features[0];
                var geom = feature.geometry;
                var ring = new OpenLayers.Geometry.LinearRing( [geom.components[0]]);
                for (var i=0; i<geom.components.length; i++) {
                    var point = geom.components[i];
                    if (!ring.intersects( point )) {
                        ring.addComponent( point );
                    }
                }
                feature.geometry = ring;  //new OpenLayers.Geometry.LinearRing( feature.geometry.components );
                callback.call( callback, true, feature );
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
        var params = 'cmd=fr&format=geojson'
                + '&source=' + fromPoint.x + ',' + fromPoint.y 
                + '&target=' + toPoint.x + ',' + toPoint.y; 
        
        var url = this.baseUrl + '?' + params;
        
        $.ajax( {
            url: url,
            dataType: "html",
            success: function( data ) {
                var features = new OpenLayers.Format.GeoJSON().read( data );
                callback.call( callback, true, features );
            }
        });
    };
    
});

