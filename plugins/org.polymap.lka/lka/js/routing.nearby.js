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
 * Atlas UI for nearby routing.
 * 
 * @author <a href="http://polymap.de">Falko Bräutigam</a>
 */
var Nearby = Class.extend( new function NearbyProto() {
    
    this.service = null;
    
    var elm = null;
    
    var point = null;
    
    var layer = null;
    
    /** */
    this.init = function( service ) {
        this.service = service;
    };
    
    /** */
    this.close = function() {
        if (elm != null) { elm.remove(); }
        if (layer != null) { Atlas.map.removeLayer( layer ); }
    };
    
    /**
     * 
     * @param elm {Element}
     * @param index {Number}
     * @param point {OpenLayers.Geometry.Point}
     */
    this.createControl = function( _elm, index, _point ) {
        elm = _elm;
        point = _point;
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
        return this;
    };
    
    /**
     * Sends the request the server.
     * <p/>
     * Handles geometry transformation to/from map/routing CRS.
     */
    this.doNearbySearch = function( point, mode, cost, index ) {
        if (layer != null) {
            Atlas.map.removeLayer( layer );
        }
        var transformed = new OpenLayers.Geometry.Point( point.x, point.y )
                .transform( Atlas.map.getProjectionObject(), this.service.projection );
        
        var self = this;
        this.service.driveTimePolygon( transformed, cost, mode, function( status, feature ) {
            // XXX handle status
            layer = new OpenLayers.Layer.Vector( "Nearby", {
                    isBaseLayer: false,
                    visibility: true,
                    reportError: true,
                    strategies: [new OpenLayers.Strategy.Fixed()],
                    protocol: new OpenLayers.Protocol()
            });
            layer.attribution = 'Routing by <b><a href="#">PGRouting</a></b>';
            var vector = new OpenLayers.Feature.Vector( feature.geometry.transform( 
                    self.service.projection, Atlas.map.getProjectionObject() ), {} );
            layer.addFeatures( [vector] );
            Atlas.map.addLayer( layer );
            
            if (layer.features.length > 0) {
                Atlas.map.zoomToExtent( layer.getDataExtent() );
                Atlas.map.zoomOut();
            }
        });
    };

});
