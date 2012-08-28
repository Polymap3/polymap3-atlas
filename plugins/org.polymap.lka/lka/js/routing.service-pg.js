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
       this.attribution = 'Routing by <b><a href="http://pgrouting.org">PGRouting</a></b>';
    };

    /**
     * @param point {OpenLayers.Geometry.Point}
     * @param distance {Number}
     * @param mode {String} 'length' or 'cost'
     * @param callback {Function}
     */
    this.driveTimePolygon = function( point, distance, mode, callback ) {
        var param = '{'
                + 'point: "POINT(' + point.x + ' ' + point.y + ')",'
                + 'distance: ' + distance + ','
                + 'mode: "' + (mode != null ? mode : 'length') + '",'
                + 'projection: "EPSG:4326"'
                + '}';
        var url = this.baseUrl + "/catch.json?json=" + param + "&key=" + this.profileKey;

        $.ajax( {
            url: url,
            dataType: "html",
            success: function( data ) {
                var json = new OpenLayers.Format.JSON().read( data );
                var temp = new OpenLayers.Format.JSON().write( json.features[0] );
                var feature = new OpenLayers.Format.GeoJSON().read( temp );
                callback.call( callback, json.status, feature[0] );
            }
        });
    };
    
    /**
     * Find the shortest path between the given points.
     * 
     * @param fromPoint {OpenLayers.Geometry.Point}
     * @param toPoint {OpenLayers.Geometry.Point}
     */
    this.shortestPath = function( fromPoint, toPoint, callback ) {
        var param = ('{'
                + 'points:["POINT({0} {1})","POINT({2} {3})"],'
                + 'projection: "EPSG:4326"'
                + '}')
                .format( fromPoint.x, fromPoint.y, toPoint.x, toPoint.y );
        
        var url = this.baseUrl + "/route.json?json=" + param + "&key=" + this.profileKey;
        
        $.ajax( {
            url: url,
            dataType: "html",
            success: function( data ) {
                var json = new OpenLayers.Format.JSON().read( data );
                var temp = new OpenLayers.Format.JSON().write( json.features );
                var features = new OpenLayers.Format.GeoJSON().read( 
                        '{ "type": "FeatureCollection", "features":' + temp + '}' );
                callback.call( callback, json.status, features );
            }
        });
    };
    
});

