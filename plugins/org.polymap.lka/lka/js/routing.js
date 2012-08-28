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
var Routing = Class.extend( new function RoutingProto() {

    /** */ 
    this.service = null;
    
    /** Array of {@link Nearby} objects. */
    this.nearbies;
    
    /** Array of {@link ShortestPath} objects. */
    this.routes;
    
    /**
     * 
     * 
     * @public
     * @param {String} baseUrl http://<domain>:<port>/<root>/<version>/
     * @param {String} profileKey
     */
    this.init = function( baseUrl, profileKey ) {
        this.nearbies = [];
        this.routes = [];
        this.service = new RoutingService( baseUrl, profileKey );
       
        Atlas.events.bind( 'searchResultGenerated', 
                callback( this.onSearchResultGenerated, {scope:this} ) );
    };

    /**
     * Called for every feature that is a result of a search.
     * 
     * @param ev {Event} 
     * @private
     */
    this.onSearchResultGenerated = function( ev ) {
        var space = '&nbsp;&nbsp';
        var html = '<a href="#" title="{1}">{0}</a>{2}';
        ev.div.append( '<p id="routing-'+ev.index + '" class="atlas-result-routing">'
                + html.format( 'routing_to'.i18n(), 'routing_to_tip'.i18n(), space )
                + html.format( 'routing_from'.i18n(), 'routing_from_tip'.i18n(), space )
                + html.format( 'routing_nearby'.i18n(), 'routing_nearby_tip'.i18n(), space ) + '</p>' );
        
        var panel = ev.div.find( '#routing-'+ev.index );
        var self = this;
        
        // nearby search
        var btn3 = panel.find( 'a:nth-child(3)');
        btn3.click( function( ev2 ) {
            $('#tabs').tabs( 'select', '#result_body_routing' );

            //var body = ev.div;
            $('#result_body_routing').empty()
                    .append( '<div id="routing-nearby-'+ev.index+'" class="routing-nearby ui-corner-all" style="display:none;"></div>');
                    
            var elm = $('#routing-nearby-'+ev.index);
            closeButton( elm, function() {
                panel.find( 'a' ).removeAttr( 'disabled' );
                btn3.css( 'font-weight', 'normal' )
                self.nearbies[ev.index].close();
                self.nearbies[ev.index] = null;
            });
            self.nearbies[ev.index] = new Nearby( self.service );
            self.nearbies[ev.index].createControl( elm, ev.index, ev.feature.geometry.getCentroid() );
            elm.fadeIn( 1000 );
        });

        var btn = panel.find( 'a:nth-child(1)');
        btn.click( function( ev2 ) {
            $('#tabs').tabs( 'select', '#result_body_routing' );

            $('#result_body_routing').empty()
                    .append( '<div id="routing-to-'+ev.index+'" class="routing-nearby ui-corner-all" style="display:none;"></div>');
                    
            var elm = $('#routing-to-'+ev.index);
            closeButton( elm, function() {
                panel.find( 'a' ).removeAttr( 'disabled' );
                btn.css( 'font-weight', 'normal' ).removeAttr( 'disabled', null );
                self.routes[ev.index].close();
                self.routes[ev.index] = null;
            });
            self.routes[ev.index] = new ShortestPath( self.service );
            self.routes[ev.index].createControl( elm, ev.index, 
                    null, null, ev.feature.geometry.getCentroid(), ev.feature.data.title );
            elm.fadeIn( 1000 );
        });

        var btn2 = panel.find( 'a:nth-child(2)');
        btn2.click( function( ev2 ) {
            if (btn2.attr( 'disabled' ) === 'disabled') {
                ev2.preventDefault();
            }
            else {
                panel.find( 'a' ).attr( 'disabled', 'disabled' );
                btn2.css( 'font-weight', 'bold' );

                ev.div.append( '<div id="routing-from-'+ev.index+'" class="routing ui-corner-all" style="display:none;"></div>');
                var elm = ev.div.find( '#routing-from-'+ev.index );
                closeButton( elm, function() {
                    panel.find( 'a' ).removeAttr( 'disabled' );
                    btn2.css( 'font-weight', 'normal' ).removeAttr( 'disabled', null );
                    self.routes[ev.index].close();
                    self.routes[ev.index] = null;
                });
                self.routes[ev.index] = new ShortestPath( self.service );
                self.routes[ev.index].createControl( elm, ev.index, 
                        ev.feature.geometry.getCentroid(), ev.feature.data.title, null, null );
                elm.fadeIn( 1000 );
            }
        });
    };

    /**
     * Creates a close button under the given <code>elm</code>.
     * 
     * @param elm {Element}
     * @param callback {Function}
     */
    function closeButton( elm, callback ) {
        elm.append( '<a href="#" class="close" title="Schliessen" style="float:right;"></a>');
        elm.find( 'a' ).click( function( ev ) {
            if (callback != null) {
                callback.call( ev );
            }
        });
    }

});


/**
 * The tool item to be used in a toolbar to test routing functions.
 */
var TestRoutingItem = ToolItem.extend( new function TestRoutingItemProto() {

    /** @type RoutingService */
    this.service = null;
    
    this.init = function( id, service ) {
        this._super( id, $.i18n.prop( "tb_routing_label" ), $.i18n.prop( "tb_routing_tip" ) );
        this.icon = $.i18n.prop( "tb_measure_icon" );
        this.service = service;
    };
    
    this.onClick = function() {
        this.service.driveTimePolygon( 13.33, 50.90, 1000, 'length' );
    };
});

