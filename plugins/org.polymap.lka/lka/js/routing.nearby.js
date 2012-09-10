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
    
    /** {@link RoutingService} */
    this.service = null;
    
    /** The {@link SearchContext} to be used for nearby searches. */  
    this.searchContext = null;
    
    /** The UI element. */
    this.elm = null;
    
    /** The {@link OpenLayers.Geometry.Point} to start search from. */
    this.point = null;
    
    /** The {@link OpenLayers.Layer.Vector} layers that displays to polygon. */
    this.layer = null;
    
    
    /**
     * 
     * @param searvice
     * @param searchContext The {@link SearchContext} to be used for nearby searches.  
     */
    this.init = function( service, searchContext ) {
        this.service = service;
        this.searchContext = searchContext;
        
        this.searchService = new SearchService( {
            baseUrl: Atlas.config.searchUrl,
            outputType: 'JSON',
            srs: Atlas.map.getProjection() } );
    };
    
    /** 
     * 
     */
    this.close = function() {
        if (this.elm != null) { 
            this.elm.remove(); 
            this.elm.parent().empty();
        }
        if (this.layer != null) { 
            Atlas.map.removeLayer( this.layer ); 
        }
        this.searchContext.resultDiv = this.origResultDiv;
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
        this.elm.append( ('<div id="routing-nearby-'+index+'">'
                + '<b>' + 'routing_cost_input'.i18n() + '</b><br/>' 
                + '<input id="routing-cost-input-'+index+'" style="text-align:right; width:37%; margin:3px;"></input>'
                + '<span id="routing-cost-type">'
                + '    <input id="cost-length" disabled="disabled" value="length" type="radio" name="cost-radios"/>'
                + '      <label for="cost-length" title="Angabe ist Wegstrecke in Metern">Meter</label>'
                + '    <input id="cost-time" disabled="disabled" value="cost" type="radio" name="cost-radios" checked="checked"/>'
                + '      <label for="cost-time" title="Angabe der Wegzeit in Minuten">Min.</label>'
                + '</span>'
                + '    <button id="routing-nearby-btn-'+index + '" title="{1}">{0}</button>' 
                + '<br/><br/><hr/><br/>'
                + '<b>' + 'routing_search_input'.i18n() + '</b><br/>'
                + '<input id="routing-search-input-'+index+'" style="width:70%; margin:3px;"></input>'
                + '    <button id="routing-nearby-btn2-"'+index +'" title="{3}">{2}</button>' 
                + '</div>' )
                .format( 'routing_nearby_show'.i18n(), 'routing_nearby_show_tip'.i18n(),
                        'routing_nearby_search'.i18n(), 'routing_nearby_search_tip'.i18n() ) );

        $('#routing-cost-type').buttonset();
        $('#routing-cost-type label span').css( 'padding', '1px 7px' );
//        $('#routing-cost-type input').attr( 'disabled' , true );


        // tweak target div in SearchContext
        this.elm.parent().append( '<hr/><div id="fake-result-body-'+index+'"></div>' );
        this.origResultDiv = this.searchContext.resultDiv;
        this.searchContext.resultDiv = $('#fake-result-body-'+index);

        // autocomplete
        var searchInput = this.elm.find('#routing-search-input-'+index)
            .autocomplete({ source: Atlas.config.autocompleteUrl, zIndex: 1000, delay: 500 })
            .keyup( function( ev ) {
                if (ev.keyCode == 13) {
                    buttons.eq( 1 ).trigger( 'click' );
                    $(ev.target).autocomplete( 'close' );
                }
            });

        // buttons
        var buttons = $('#routing-nearby-'+index+' button');
        buttons.button();
        $('#routing-nearby-'+index+' button span').css( 'padding', '1px 7px' );

        var self = this;
        $('#routing-cost-input-'+index)
            .val( '10' )
            .focus()
            .keyup( function( ev ) {
                if (ev.keyCode == 13) {
                    buttons.eq( 0 ).trigger( 'click' );
                }
            });
        
        buttons.eq( 0 ).click( function() {
            var mode = $('input[name="cost-radios"]:checked').val();
            var cost = $('#routing-cost-input-'+index).val();
            self.doNearbySearch( point, mode, cost, index );
        });
        
        buttons.eq( 1 ).click( function() {
            var mode = $('input[name="cost-radios"]:checked').val();
            var cost = $('#routing-cost-input-'+index).val();
            var searchText = searchInput.val();
            self.doNearbySearch( point, mode, cost, index, searchText );
        });
        
        // show polygon right after init
        self.doNearbySearch( point, 'cost', '10', index );
        
        return this;
    };
    
    /**
     * Sends the request the server.
     * <p/>
     * Handles geometry transformation to/from map/routing CRS.
     */
    this.doNearbySearch = function( point, mode, cost, index, searchStr ) {
        if (this.layer != null) {
            Atlas.map.removeLayer( this.layer );
        }
        
        var self = this;
        this.service.driveTimePolygon( point, cost, mode, function( status, feature ) {
            // XXX handle status
            
            var defaultStyle = new OpenLayers.Style({
                'strokeWidth': 2,
                'strokeColor': '#808080',
                'strokeOpacity': 0.80,
                'fillOpacity': 0.08,
                'fillColor': '#4444FF'
            });
            var styles = new OpenLayers.StyleMap({
                'default': defaultStyle,
                'select': defaultStyle
            });

            self.layer = new OpenLayers.Layer.Vector( "Nearby", {
                    isBaseLayer: false,
                    visibility: true,
                    reportError: true,
                    strategies: [new OpenLayers.Strategy.Fixed()],
                    protocol: new OpenLayers.Protocol(),
                    styleMap: styles
            });
            self.layer.attribution = self.service.attribution;
            self.layer.addFeatures( [feature] );
            Atlas.map.addLayer( self.layer );
            
            if (self.layer.features.length > 0) {
                Atlas.map.zoomToExtent( self.layer.getDataExtent() );
                Atlas.map.zoomOut();
            }
            
            if (searchStr) {
                var bounds = feature.geometry;
                searchStr += ' bounds:' + new OpenLayers.Format.GeoJSON().write( bounds );
                self.searchContext.search( searchStr )
            }
        });
    };

});
