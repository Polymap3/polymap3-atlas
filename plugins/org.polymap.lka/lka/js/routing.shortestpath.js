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
 * Atlas UI for shortest path routing.
 * 
 * @author <a href="http://polymap.de">Falko Bräutigam</a>
 */
var ShortestPath = Class.extend( new function ShortestPathProto() {
    
    this.service = null;
    
    this.searchService = null;
    
    /** The document element to create the UI into. */
    this.elm = null;
    
    this.index;
    
    this.layer = null;
    
    this.hoverControl = null; 
    this.selectControl = null;
    
    this.fromPoint = null; 
    this.toPoint = null;
    
    /** {RoutingTooltip} */
    this.tooltip;

    /** */
    this.init = function( service ) {
        this.service = service;
        
        this.searchService = new SearchService( {
            baseUrl: Atlas.config.searchUrl,
            outputType: 'JSON',
            srs: Atlas.map.getProjection() } );
    };
    
    /** */
    this.close = function() {
        if (this.elm != null) { this.elm.remove(); }
        if (this.selectControl != null) { Atlas.map.removeControl( this.selectControl ); }
        if (this.hoverControl != null) { Atlas.map.removeControl( this.hoverControl ); }
        if (this.layer != null) { Atlas.map.removeLayer( this.layer ); }
        if (this.tooltip != null) { this.tooltip.close(); }
    };
    
    /**
     * 
     * @param elm {Element}
     * @param index {Number}
     * @param point {OpenLayers.Geometry.Point}
     * @param fromSearchStr
     * @param toSearchStr 
     */
    this.createControl = function( elm, index, fromPoint, fromSearchStr, toPoint, toSearchStr ) {
        this.elm = elm;
        this.index = index;
        this.fromPoint = fromPoint;
        this.toPoint = toPoint;
        var self = this;
        this.elm.append( ('<div style="background:#f5f5ff; padding:5px; font-size:10px;">'
                + '<b>{0}</b> <input id="{1}" style="width:97%; margin: 1px 0px 1px 0px;"></input>'
                + '    <div id="{7}" style="color:#808080;"></div>'
                + '<b>{2}</b> <input id="{3}" style="width:97%; margin: 1px 50x 1px 0px;"></input>'
                + '    <div id="{8}" style="color:#808080;"></div>'
                + '<center> <button id="{4}" title="{6}" style="margin:3px;">{5}</button> </center>'
                + '</div>')
                .format( 'routing_from_input'.i18n(), 'routing-from-input-'+index,
                        'routing_to_input'.i18n(), 'routing-to-input-'+index,
                        'routing-btn-'+index, 'Route berechnen', 'Route suchen und in der Karte anzeigen',
                        'routing-from-pl-'+index, 'routing-to-pl-'+index) );

        this.elm.find( 'button' ).button()
                //.attr( 'disabled', 'disabled' )
                .css( 'box-shadow', '0px 1px 1px #090909' )
                .find( 'span').css( 'padding', '1px 7px' );
        
        // set default value
        this.elm.find( '#routing-from-input-'+index).val( fromSearchStr != null ? fromSearchStr : null );
        this.elm.find( '#routing-to-input-'+index).val( toSearchStr != null ? toSearchStr : null );
        
        this.createSearchPicklist( 
                this.elm.find( '#routing-from-pl-'+index ), this.elm.find( '#routing-from-input-'+index ), index,
                function( feature ) { 
                    self.fromPoint = feature.geometry.getCentroid(); 
                });
        this.createSearchPicklist( 
                this.elm.find( '#routing-to-pl-'+index ), this.elm.find( '#routing-to-input-'+index ), index,
                function( feature ) {
                    self.toPoint = feature.geometry.getCentroid(); 
                });
        
        
        // keyup -> enable/disable, searchStr, doRouteSearch()
        this.elm.find( 'input' ).keyup( function( ev ) {
            var inputElm = $(this);
            //window.setTimeout( function() { inputElm.autocomplete( 'close' ); }, 5000 );
            
            if (ev.keyCode == 13) {
                $(this).autocomplete( 'close' );
                return false;
            }
            if ($(this).val().length != 0) {
                self.elm.find( '#routing-btn-'+index ).removeAttr( 'disabled' );
            } else {
                self.elm.find( '#routing-btn-'+index ).attr( 'disabled', 'disabled' );
            }

            if ($(this).attr( 'id' ).startsWith( 'routing-from')) {
                fromSearchStr = $(this).val();
            } else {
                toSearchStr = $(this).val();
            }
                    
        });
        // autocomplete
        this.elm.find( 'input' ).autocomplete({ source: Atlas.config.autocompleteUrl, zIndex: 1000, delay: 500 });
         
        this.elm.find( '#routing-btn-'+index ).click( function() {
            if (self.fromPoint == null || self.toPoint == null) {
                alert( 'Start und/oder Ziel sind noch nicht eindeutig.' );
            }
            self.doRouteSearch( self.fromPoint, self.toPoint, self.index );
        });
    };
    
    /**
     * 
     */
    this.createSearchPicklist = function( _parentElm, _inputElm, _index, callback ) {
        var selectElm = null;
        var self = this;
        var delay = null;
        
        _inputElm.keyup( function( ev ) {
            if (delay != null) {
                clearTimeout( delay );
                delay = null;
            }
            delay = setTimeout( doSearchPicklist, 1000 );
        });
        
        function doSearchPicklist() {
            self.searchService.search( self.searchService.searchUrl( _inputElm.val() ), function( features ) {
                if (selectElm == null) {
                    _parentElm.append( ('<span style="font-size:10px;">Suchergebnisse:</span>'
                            + '<select id="routing-result-list-{0}" name="list1" size="5"'
                            + '    style="width:100%; color:#808080; box-shadow: 1px 1px 2px #606060;"/>')
                            .format( _index ) );
                    
                    selectElm = _parentElm.find( '#routing-result-list-'+_index );
                    selectElm.click( function( ev ) {
                        _inputElm.val( $(this).val() );
                        _parentElm.empty();
                        callback.call( self, features[ev.target.index] );
                        selectElm = null;
                    });
                }
                if (features.length == 1 /*&& features[0].data.title == _inputElm.val()*/) {
                    _parentElm.empty();
                    selectElm = null;
                    callback.call( self, features[0] );
                }
                else {
                    selectElm.empty();
                    for (var i=0; i<features.length; i++) {
                        selectElm.append( '<option>' + features[i].data.title + '</option>' );
                    }
                }
            });
        }
    };
    
    /**
     * 
     */
    this.doRouteSearch = function( fromPoint, toPoint, index ) {
        // remove current layer/control
        if (this.selectControl != null) { Atlas.map.removeControl( this.selectControl ); }
        if (this.hoverControl != null) { Atlas.map.removeControl( this.hoverControl ); }
        if (this.layer != null) { Atlas.map.removeLayer( this.layer ); }
        if (this.tooltip != null) { this.tooltip.close(); }

        var fromTransformed = new OpenLayers.Geometry.Point( fromPoint.x, fromPoint.y )
                .transform( Atlas.map.getProjectionObject(), this.service.projection );
        var toTransformed = new OpenLayers.Geometry.Point( toPoint.x, toPoint.y )
                .transform( Atlas.map.getProjectionObject(), this.service.projection );

        // execute search request
        var self = this;
        self.service.shortestPath( fromTransformed, toTransformed, function( status, features ) {
            if (features.length == 0) {
                alert( 'routing_no_result'.i18n() );
            }

            self.layer = new OpenLayers.Layer.Vector( "ShortestPath", {
                isBaseLayer: false,
                visibility: true,
                reportError: true,
                strategies: [new OpenLayers.Strategy.Fixed()],
                protocol: new OpenLayers.Protocol(),
                styleMap: defaultStyleMap()
            });
            self.layer.attribution = 'Routing by <b><a href="#">PGRouting</a></b>';

            var vectors = new Array( features.length+2 );
            $.each( features, function( i, feature ) {
                vectors[i] = new OpenLayers.Feature.Vector( feature.geometry.transform( 
                        self.service.projection, Atlas.map.getProjectionObject() ), feature.data );
            });
            // flags
            vectors[features.length] = new OpenLayers.Feature.Vector( toPoint, {title:'Ziel'} );
            vectors[features.length+1] = new OpenLayers.Feature.Vector( fromPoint, {title:'Start'} );
            self.layer.addFeatures( vectors );
            Atlas.map.addLayer( self.layer );

            // hover features
            self.selectControl = new OpenLayers.Control.SelectFeature( self.layer, {
                clickout: true, toggle: false,
                multiple: false, hover: false,
                toggleKey: "ctrlKey", // ctrl key removes from selection
                multipleKey: "shiftKey", // shift key adds to selection
                box: false
            });
            Atlas.map.addControl( self.selectControl );
            self.selectControl.activate();

            self.hoverControl = new OpenLayers.Control.SelectFeature( self.layer, {
                hover: true,
                //highlightOnly: true,
                multiple: false
            });
            Atlas.map.addControl( self.hoverControl );
            self.hoverControl.activate();      

            // tooltip
            self.tooltip = new RoutingTooltip( status, vectors );            
            self.layer.events.register( "featureselected", self.layer, function( ev ) {
                self.tooltip.highlight( ev.feature );
            });

            if (self.layer.features.length > 0) {
                Atlas.map.zoomToExtent( self.layer.getDataExtent() );
                Atlas.map.zoomOut();
            }
        });
    };

    /** 
     * Create the default style map for the route layer. 
     */
    function defaultStyleMap() {
        var defaultStyle = new OpenLayers.Style({
            externalGraphic: 'images/flag_yellow.png',
            graphicHeight: 20,
            graphicWidth: 28,
//            graphicXOffset: -10.5,
//            graphicYOffset: -12.5,
            strokeWidth: 5,
            strokeColor: '#4444FF',
            strokeOpacity: 0.7,
            fillOpacity: 1
        });
        var selectStyle = new OpenLayers.Style({
            strokeWidth: 5,
            strokeColor: '#FF4444',
            strokeOpacity: 1,
            fillOpacity: 1
        });
        return new OpenLayers.StyleMap({
            'default': defaultStyle,
            'select': selectStyle });
    }
    
});

/**
 * Route and segment display over the map.
 * 
 * @author <a href="http://polymap.de">Falko Bräutigam</a>
 */
var RoutingTooltip = Class.extend( new function RoutingTooltipProto() {
    
    this.tooltip = null;
    
    this.featuresMap;
    
    this.highlighted = null;
    
    this.defaultStyle = null;
    
    this.init = function( status, features ) {
        $(document.body).append( 
                '<div id="routing-tooltip" class="ui-widget-content ui-corner-all"' 
                + '   style="z-index:1000; position:absolute; color:#707070; border:0px solid black;'
                + '   left:50px; top:200px; padding:5px; background:#FFFFCF; box-shadow:2px 3px 3px #808080;">'
                + '<b>' + 'routing_total_length'.i18n() + '</b> ???<br/>'
                + '<b>' + 'routing_total_cost'.i18n() + '</b> ???<br/>'
                + '<hr/>'
                + '<div id="routing-tooltip-list" style="overflow:auto;'
                + '    width:180px; height:300px;">'
                + '</div>'
                + '</div>' );
        this.tooltip = $('#routing-tooltip');
        
        var list = this.tooltip.find('#routing-tooltip-list');
        var totalLength = 0;
        var totalCost = 0;
        var self = this;
        this.featuresMap = {};
        $.each( features, function( i, feature ) {
            if (feature.data.length != undefined && feature.data.cost != undefined) {
                list.append( '<div style="padding:3px; margin-right:5px;">' 
                    + '<b>' + feature.data.name + '</b><br/>'
                    + 'L&auml;nge: ' + feature.data.length.toFixed(0) + ' m <br/>' 
                    + 'Kosten: ' + feature.data.cost.toFixed( 2 ) + ' ? <br/>'
                    + '</div>' );
                self.featuresMap[feature.id] = i;
                totalLength += feature.data.length;
                totalCost += feature.data.cost;
            }
        });
    };
    
    this.close = function() {
        if (this.tooltip != null) { this.tooltip.remove(); }
    };
    
    this.highlight = function( feature ) {
        if (this.highlighted != null) {
            this.highlighted.css( 'background', this.defaultStyle.background );
            this.highlighted.css( 'border', this.defaultStyle.border );
            this.highlighted.css( 'color', this.defaultStyle.color );
        }
        
        var index = this.featuresMap[feature.id];
        var list = this.tooltip.find( '#routing-tooltip-list' );
        this.highlighted = list.find( ':nth-child({0})'.format(index+1) );
        
        this.defaultStyle = {
                background: this.highlighted.css( 'background' ),
                border: this.highlighted.css( 'border' ),
                color: this.highlighted.css( 'color' ) };
        
        this.highlighted.css( 'background', '#FFFFAF' )
                .css( 'color', '#222' )
                .css( 'border', '1px solid #d0d0d0')
                .addClass( 'ui-corner-all' );
        list.scrollTo( this.highlighted, 1000 );
    };
});
