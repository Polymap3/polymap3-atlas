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
 * Catch 'searchResultGenerated' event and enhance search result
 * with routing UI.
 */
var Routing = Class.extend( new function RoutingProto() {

    /** {@link RoutingService} */
    this.service = null;
    
    /** Array of {@link Nearby} objects. */
    this.nearbies;
    
    /** Array of {@link ShortestPath} objects. */
    this.routes;
    
    /**
     * Constructs a new instance. 
     * 
     * @public
     * @param {String} baseUrl http://<domain>:<port>/<root>/<version>/
     * @param {String} profileKey
     */
    this.init = function( baseUrl, profileKey ) {
        this.nearbies = [];
        this.routes = [];
        this.service = new RoutingService( baseUrl, profileKey );
       
        var self = this;
        Atlas.events.bind( 'searchResultGenerated', function( ev ) {
            self.onSearchResultGenerated( ev )
        });
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
            var index = Atlas.result_index + 1;
            var context = new RoutingSearchContext( index );
            var router = new Nearby( self.service, context.original );
            router.createControl( context.elm, ev.index, ev.feature.geometry.getCentroid() );
            context.createControl( router );
        });

        // 
        var btn = panel.find( 'a:nth-child(1)');
        btn.click( function( ev2 ) {
            var index = Atlas.result_index + 1;
            var context = new RoutingSearchContext( index );
            var router = new ShortestPath( self.service );
            router.createControl( context.elm, index, null, null, ev.feature.geometry.getCentroid(), ev.feature.data.title );
            context.createControl( router );
        });

        //
        var btn2 = panel.find( 'a:nth-child(2)');
        btn2.click( function( ev2 ) {
            var index = Atlas.result_index + 1;
            var context = new RoutingSearchContext( index );
            var router = new ShortestPath( self.service );
            router.createControl( context.elm, index, ev.feature.geometry.getCentroid(), ev.feature.data.title );
            context.createControl( router );
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
 * Special routing search context to replace the original SearchContext in the
 * Atlas#context array.
 */
RoutingSearchContext = Class.extend( new function RoutingSearchContextProto() {
    
    /** The original {@link SearchContext} we are replacing. */
    this.original = null;
    
    /** This is used by LinkItem to generate URL. */
    this.searchStr = null;

    /** The index in the Atlas.contexts array. */
    this.index = -1;
    
    /** {@link ShortestPath} The routing UI. */
    this.router = null;
    
    this.elm = null;
    
    /**
     * Constructs a new object. Retrieves and substitutes the original
     * {@link SearchContext}. Creates a new <div> in the target resultBody and
     * initializes a new {@link ShortestPath} router.
     * 
     * @param contextIndex {int} The index of the SearchContext to replace.
     * @param server {@link RoutingService}
     * @param feature {@link OpenLayers.Feature}
     * @param isStart {boolean} True if the given feature is the starting point for routing.
     */
    this.init = function( contextIndex ) {
        var self = this;
        this.index = contextIndex;
        this.original = Atlas.contexts[this.index];
        this.searchStr = '-';
        $('#tab_title_result'+this.index).text( '<->' );
        
        // check if original is RoutingSearchContext itself
        if (this.original.original) {
            this.original.close();
            this.original = Atlas.contexts[this.index];
        }
        
        // substitute context
        Atlas.contexts[this.index] = this;

        // activate tab (calling #activate())
        var resultBodyId = '#result_body' + this.index;
        $('#tabs').tabs( 'select', resultBodyId );

        // UI
        $(resultBodyId).empty().append( 
                '<div id="routing-'+this.index+'" class="routing-nearby ui-corner-all" style="display:none;">' +
                '<a href="#" class="close" title="Schliessen" style="float:right;"></a>' +
                '</div>' );

        this.elm = $('#routing-'+this.index);
        this.elm.find( 'a' ).click( function( ev ) {
            self.router.close();
            Atlas.contexts[self.index] = self.original;
            self.original.activate();
        });
    };
    
    this.close = function() {
        this.router.close();
        Atlas.contexts[this.original.index] = this.original;
        this.original.activate();
    };
    
    this.createControl = function( router ) {
        this.router = router;
        this.router.markerImage = this.original.markerImage;
        this.router.pathColor = this.original.geomColor;
        this.elm.fadeIn( 1000 );        
    };
    
    this.activate = function() {
        this.active = true;
        $('#search_field').val( this.searchStr );
        $('#search_img').attr( "src", this.original.markerImage );
    };

    this.deactivate = function() {
        this.active = false;        
    };

    this.search = function( searchStr ) {
        this.close();
        this.original.search( searchStr );
    };
    
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

