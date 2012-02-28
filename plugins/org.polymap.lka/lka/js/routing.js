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
 * Integration of the Routing service into the Atlas UI.
 */
var Routing = Class.extend( {

    /** @private */ 
    service: null,
    
    /** Array of objects representing the nearby search results. */
    nearbies: [],
    
    /**
     * 
     * 
     * @public
     * @param {String} baseUrl http://<domain>:<port>/<root>/<version>/
     * @param {String} profileKey
     */
    init: function( baseUrl, profileKey ) {
       this.service = new RoutingService( baseUrl, profileKey );
       
       Atlas.events.bind( 'searchResultGenerated', 
                callback( this.onSearchResultGenerated, {scope:this} ))
    },

    /**
     * Called for every feature that is a result of a search.
     * 
     * @private
     */
    onSearchResultGenerated: function( ev ) {
        ev.div.append( '<p id="routing-'+ev.index + '" class="atlas-result-routing">'  //#Routing:&nbsp;&nbsp;&nbsp;'
                + '<a href="#" title="' + $.i18n.prop('routing_to_tip') + '">' + $.i18n.prop('routing_to') + '</a>&nbsp;&nbsp'
                + '<a href="#">' + $.i18n.prop('routing_from') + '</a>&nbsp;&nbsp'
                + '<a href="#" title="' + $.i18n.prop('routing_nearby_tip') + '">' + $.i18n.prop('routing_nearby') + '</a>&nbsp;&nbsp'
                + '</p>' );
        
        var self = this;
        var nearby = $('#routing-'+ev.index+' a:nth-child(3)');
        nearby.click( function() {
            nearby.css( 'font-weight', 'bold' );
            
            ev.div.append( '<div id="routing-nearby-'+ev.index+'" class="routing-nearby ui-corner-all" style="display:none;"></div>');
            var elm = $('#routing-nearby-'+ev.index);
            self.closeButton( elm, function() {
                nearby.css( 'font-weight', 'normal' );
                Atlas.map.removeLayer( self.nearbies[ev.index].layer );
                self.nearbies[ev.index] = null;
            });
            self.nearbySearch( elm, ev.index, ev.feature.geometry.getCentroid() );
            elm.fadeIn( 1000 );
        });
    },

    /**
     * 
     */
    closeButton: function( elm, callback ) {
        elm.append( '<a href="#" class="close" title="Schliessen" style="float:right;"></a>');
        elm.find( 'a' ).click( function( ev ) {
            elm.remove();
            if (callback != null) {
                callback.call( ev );
            }
        });
    },

    /**
     * 
     */
    nearbySearch: function( elm, index, point ) {
        elm.append( '<p>' 
                //+ transformed + '</br>'
                + $.i18n.prop('routing_cost_input') 
                + '    <input id="routing-cost-input-'+index+'" style="width:110px; margin:1px;"></input>'
                + '<span id="routing-cost-type">'
                + '    <input id="cost-length" value="length" type="radio" name="cost-radios" checked="checked"/>'
                + '      <label for="cost-length" title="Angabe ist Wegstrecke in Metern">Meter</label>'
                + '    <input id="cost-time" value="cost" type="radio" name="cost-radios"/>'
                + '      <label for="cost-time" title="Angabe ist Fahrzeit in Minuten">Min.</label>'
                + '</span>'
                + '<center>'
                + '    <button id="routing-nearby-btn-'+index + '" title="Erreichbarkeit in der Karte anzeigen">Anzeigen</button>' 
                + '    <button id="routing-nearby-btn2-"'+index +'" title="Orte in der Nähe suchen">Suchen...</button>' 
                + '</center>'
                + '</p>' );
        $('#routing-cost-type').buttonset();
        $('#routing-cost-type label span').css( 'padding', '1px 7px' );
        $('#routing-cost-input-'+index).focus();
        var buttons = $('#routing-nearby-'+index+' button');
        buttons.button()
                .attr( 'disabled', 'disabled' )
                .css( 'box-shadow', '0px 1px 1px #090909' );        

        $('#routing-nearby-'+index+' button span').css( 'padding', '1px 7px' );
        //$('#routing-nearby-btn').css( 'float', 'right' );

        var self = this;
        $('#routing-cost-input-'+index).keyup( function( ev ) {
            if ($(this).val().length != 0) {
                buttons.removeAttr( 'disabled' );
            } else {
                buttons.attr( 'disabled', 'disabled' );
            }
            if (ev.keyCode == 13) {
                var mode = $('input[name="cost-radios"]:checked').val();
                self.doNearbySearch( point, mode, $(this).val(), index );
            }
        });
        
        $('#routing-nearby-btn-'+index).click( function() {
            var mode = $('input[name="cost-radios"]:checked').val();
            var cost = $('#routing-cost-input-'+index).val();
            self.doNearbySearch( point, mode, cost, index );
        });
    },
    
    /**
     * Issues the request the server.
     * <p/>
     * Handles geometry transformation to/from map/routing CRS.
     */
    doNearbySearch: function( point, mode, cost, index ) {
        var transformed = null;
        
        if (this.nearbies[index] != null) {
            Atlas.map.removeLayer( this.nearbies[index].layer );
            transformed = this.nearbies[index].transformed;
            this.nearbies[index] = null;
        }
        else {
            transformed = point.transform( Atlas.map.getProjectionObject(), this.service.projection );
        }
        
        var self = this;
        this.service.driveTimePolygon( transformed.x, transformed.y, cost, mode, function( status, feature ) {
            var nearby = {};
            self.nearbies[index] = nearby;
            nearby.transformed = transformed;
            nearby.layer = new OpenLayers.Layer.Vector( "Nearby", {
                    isBaseLayer: false,
                    visibility: true,
                    reportError: true,
                    strategies: [new OpenLayers.Strategy.Fixed()],
                    protocol: new OpenLayers.Protocol()
            });
            nearby.layer.attribution = 'Routing by <b><a href="#">PGRouting</a></b>';
            nearby.vector = new OpenLayers.Feature.Vector( feature.geometry.transform( 
                    self.service.projection, Atlas.map.getProjectionObject() ), {} );
            nearby.layer.addFeatures( [ nearby.vector ] );
            Atlas.map.addLayer( nearby.layer );
            
            if (nearby.layer.features.length > 0) {
                Atlas.map.zoomToExtent( nearby.layer.getDataExtent() );
                Atlas.map.zoomOut();
            }
        });
    }
});


/**
 * API and implementation of the routing service. This class
 * sends request the the server and handles the response.
 */
var RoutingService = Class.extend( {

    /**
     * URL: http://<domain>:<port>/<root>/<version>/<profile key>/catch.<output>?json={<parameter>}
     * 
     * @param baseUrl http://<domain>:<port>/<root>/<version>/
     * @param profileKey
     */
    init: function( baseUrl, profileKey ) {
       this.baseUrl = baseUrl;
       this.profileKey = profileKey;
       this.projection = new OpenLayers.Projection( "EPSG:4326" );
    },

    /**
     * @param x {Number}
     * @param y {Number}
     * @param distance {Number}
     * @param mode {String} 'length' or 'cost'
     * @param callback {Function}
     */
    driveTimePolygon: function( x, y, distance, mode, callback ) {
        var param = '{'
                + 'point: "POINT(' + x + ' ' + y + ')",'
                + 'distance: ' + distance + ','
                + 'mode: "' + (mode != null ? mode : 'length') + '",'
                + 'projection: "EPSG:4326"'
                + '}';
        var url = this.baseUrl + "/catch.json?json=" + param + "&key=" + this.profileKey;
        //alert( url );

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
    }
});


/**
 * The tool item to be used in a toolbar to test routing functions.
 */
var TestRoutingItem = ToolItem.extend( {

    /** @private {RoutingService} */
    service: null,
    
    init: function( id, service ) {
        this._super( id, $.i18n.prop( "tb_routing_label" ), $.i18n.prop( "tb_routing_tip" ) );
        this.icon = $.i18n.prop( "tb_measure_icon" );
        this.service = service;
    },
    
    onClick: function() {
        this.service.driveTimePolygon( 13.33, 50.90, 1000, 'length' );
    }
});

