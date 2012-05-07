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
    
    /** The UI element. */
    this.elm = null;
    
    this.point = null;
    
    this.layer = null;
    
    /** */
    this.init = function( service ) {
        this.service = service;
    };
    
    /** */
    this.close = function() {
        if (this.elm != null) { this.elm.remove(); }
        if (this.layer != null) { Atlas.map.removeLayer( this.layer ); }
    };
    
    /**
     * 
     * @param elm {Element}
     * @param index {Number}
     * @param point {OpenLayers.Geometry.Point}
     */
    this.createControl = function( elm, index, point ) {
        this.elm = elm;
        this.point = point;
        this.elm.append( ('<div class="routing-nearby">'
                + 'routing_cost_input'.i18n() 
                + '    <input id="routing-cost-input-'+index+'" style="width:50px; margin:3px;"></input>'
                + '<span id="routing-cost-type">'
                + '    <input id="cost-length" value="length" type="radio" name="cost-radios" checked="checked"/>'
                + '      <label for="cost-length" title="Angabe ist Wegstrecke in Metern">Meter</label>'
                + '    <input id="cost-time" value="cost" type="radio" name="cost-radios"/>'
                + '      <label for="cost-time" title="Angabe ist Fahrzeit in Minuten">Min.</label>'
                + '</span>'
                + '<center>'
                + '    <button id="routing-nearby-btn-'+index + '" title="{1}">{0}</button>' 
                + '    <button id="routing-nearby-btn2-"'+index +'" title="{3}">{2}</button>' 
                + '</center>'
                + '</div>')
                .format( 'routing_nearby_show'.i18n(), 'routing_nearby_show_tip'.i18n(),
                        'routing_nearby_search'.i18n(), 'routing_nearby_search_tip'.i18n() ) );
        
        $('#routing-cost-type').buttonset();
        $('#routing-cost-type label span').css( 'padding', '1px 7px' );
        
        var buttons = $('#routing-nearby-'+index+' button');
        buttons.button();
//                //.attr( 'disabled', 'disabled' )
//                .css( 'box-shadow', '0px 1px 1px #090909' );        

        $('#routing-nearby-'+index+' button span').css( 'padding', '1px 7px' );
        //$('#routing-nearby-btn').css( 'float', 'right' );

        var self = this;
        $('#routing-cost-input-'+index).val( '1000' ).focus()
            .keyup( function( ev ) {
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
        if (this.layer != null) {
            Atlas.map.removeLayer( this.layer );
        }
        var transformed = new OpenLayers.Geometry.Point( point.x, point.y )
                .transform( Atlas.map.getProjectionObject(), this.service.projection );
        
        var self = this;
        this.service.driveTimePolygon( transformed, cost, mode, function( status, feature ) {
            // XXX handle status
            self.layer = new OpenLayers.Layer.Vector( "Nearby", {
                    isBaseLayer: false,
                    visibility: true,
                    reportError: true,
                    strategies: [new OpenLayers.Strategy.Fixed()],
                    protocol: new OpenLayers.Protocol()
            });
            self.layer.attribution = 'Routing by <b><a href="#">PGRouting</a></b>';
            var vector = new OpenLayers.Feature.Vector( feature.geometry.transform( 
                    self.service.projection, Atlas.map.getProjectionObject() ), {} );
            self.layer.addFeatures( [vector] );
            Atlas.map.addLayer( self.layer );
            
            if (self.layer.features.length > 0) {
                Atlas.map.zoomToExtent( self.layer.getDataExtent() );
                Atlas.map.zoomOut();
            }
        });
    };

});
