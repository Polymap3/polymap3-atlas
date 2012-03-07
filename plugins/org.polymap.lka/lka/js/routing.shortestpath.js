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
                    fromPoint = feature.geometry.getCentroid(); 
                });
        this.createSearchPicklist( 
                this.elm.find( '#routing-to-pl-'+index ), this.elm.find( '#routing-to-input-'+index ), index,
                function( feature ) {
                    toPoint = feature.geometry.getCentroid(); 
                });
        
        
        // keyup -> enable/disable, searchStr, doRouteSearch()
        var self = this;
        this.elm.find( 'input' ).keyup( function( ev ) {
            var inputElm = $(this);
            //window.setTimeout( function() { inputElm.autocomplete( 'close' ); }, 5000 );
            
            if (ev.keyCode == 13) {
                $(this).autocomplete( 'close' );
                return false;
            }
            if ($(this).val().length != 0) {
                this.elm.find( '#routing-btn-'+index ).removeAttr( 'disabled' );
            } else {
                this.elm.find( '#routing-btn-'+index ).attr( 'disabled', 'disabled' );
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
            if (fromPoint == null || toPoint == null) {
                alert( 'Start und/oder Ziel sind noch nicht eindeutig.' );
            }
            self.doRouteSearch( fromPoint, toPoint, index );
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
        if (this.hoverControl != null) { Atlas.map.removeControl( this.hoverControl ); }
        if (this.layer != null) { Atlas.map.removeLayer( this.layer ); }

        var fromTransformed = new OpenLayers.Geometry.Point( fromPoint.x, fromPoint.y )
                .transform( Atlas.map.getProjectionObject(), this.service.projection );
        var toTransformed = new OpenLayers.Geometry.Point( toPoint.x, toPoint.y )
                .transform( Atlas.map.getProjectionObject(), this.service.projection );

        var self = this;
        self.service.shortestPath( fromTransformed, toTransformed, function( status, features ) {
            if (features.length == 0) {
                alert( 'routing_no_result'.i18n() );
            }

            this.layer = new OpenLayers.Layer.Vector( "ShortestPath", {
                isBaseLayer: false,
                visibility: true,
                reportError: true,
                strategies: [new OpenLayers.Strategy.Fixed()],
                protocol: new OpenLayers.Protocol(),
                styleMap: defaultStyleMap()
            });
            this.layer.attribution = 'Routing by <b><a href="#">PGRouting</a></b>';

            var vectors = new Array( features.length+2 );
            $.each( features, function( i, feature ) {
                vectors[i] = new OpenLayers.Feature.Vector( feature.geometry.transform( 
                        self.service.projection, Atlas.map.getProjectionObject() ), feature.data );
            });
            // flags
            vectors[features.length] = new OpenLayers.Feature.Vector( toPoint, {title:'Ziel'} );
            vectors[features.length+1] = new OpenLayers.Feature.Vector( fromPoint, {title:'Start'} );
            this.layer.addFeatures( vectors );
            Atlas.map.addLayer( this.layer );

            // hover features
            this.selectControl = new OpenLayers.Control.SelectFeature( this.layer, {
                clickout: true, toggle: false,
                multiple: false, hover: false,
                toggleKey: "ctrlKey", // ctrl key removes from selection
                multipleKey: "shiftKey", // shift key adds to selection
                box: false
            });
            Atlas.map.addControl( this.selectControl );
            this.selectControl.activate();

            this.hoverControl = new OpenLayers.Control.SelectFeature( this.layer, {
                hover: true,
                //highlightOnly: true,
                multiple: false
            });
            Atlas.map.addControl( this.hoverControl );
            this.hoverControl.activate();      

            this.layer.events.register( "featureselected", this.layer, function( ev ) {
                //alert( this.selectedFeatures );
            });

            if (this.layer.features.length > 0) {
                Atlas.map.zoomToExtent( this.layer.getDataExtent() );
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