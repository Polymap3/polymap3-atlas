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
    this.fromSearchStr = null;
    this.fromInput = null;
    
    this.toPoint = null;
    this.toSearchStr = null;
    this.toInput = null;
    
    /** {RoutingTooltip} */
    this.tooltip;
    
    this.markerImage = null;
    this.pathColor = null;

    /** */
    this.init = function( service ) {
        this.service = service;
        this.markerImage = 'images/marker_b.png';
        this.pathColor = '#4444ff';
        
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
        this.fromSearchStr = fromSearchStr;
        this.toSearchStr = toSearchStr;
        var self = this;
        
        this.elm.append( (
                '<div>'
                + '<b>{0}</b><br/> <input id="{1}" style="width:85%; margin: 1px 0px 1px 0px;"></input>'
                + '    <span id="{7}" style="color:#606060; margin:0px 5px;"></span><br/>'
                + '<b>{2}</b><br/> <input id="{3}" style="width:85%; margin: 1px 50x 1px 0px;"></input>'
                + '    <span id="{8}" style="color:#606060; margin:0px 5px;"></span>'
                + '<center> <button id="{4}" title="{6}" style="margin:3px;">{5}</button> </center>'
                + '</div>')
                .format( 'routing_from_input'.i18n(), 'routing-from-input-'+index,
                        'routing_to_input'.i18n(), 'routing-to-input-'+index,
                        'routing-btn-'+index, 'routing_shortestpath'.i18n(), 'routing_shortestpath_tip'.i18n(),
                        'routing-from-pl-'+index, 'routing-to-pl-'+index) );

        this.elm.find( 'button' ).button()
                //.attr( 'disabled', 'disabled' )
                //.css( 'box-shadow', '0px 1px 1px #090909' )
                .find( 'span').css( 'padding', '1px 7px' );
        
        this.fromInput = this.elm.find( '#routing-from-input-'+index );
        this.toInput = this.elm.find( '#routing-to-input-'+index );

        // set default value
        this.fromInput.val( this.fromSearchStr != null ? this.fromSearchStr : 'frauensteiner 44 freiberg' );
        this.toInput.val( this.toSearchStr != null ? this.toSearchStr : null );
        
        this.createSearchPicklist( 
                this.elm.find( '#routing-from-pl-'+index ), this.fromInput, index,
                function( feature ) { 
                    self.fromPoint = feature.geometry.getCentroid(); 
                });
        this.createSearchPicklist( 
                this.elm.find( '#routing-to-pl-'+index ), this.toInput, index,
                function( feature ) {
                    self.toPoint = feature.geometry.getCentroid(); 
                });
        
        // keydown -> clear current points
        this.elm.find( 'input' ).keydown( function( ev ) {
            //alert( $(this).attr( 'id' ) );
            if ($(this).attr( 'id' ).startsWith( 'routing-from')) {
                self.fromPoint = null;
            } else {
                self.toPoint = null;
            }
        });
        // keyup -> enable/disable, searchStr, doRouteSearch()
        this.elm.find( 'input' ).keyup( function( ev ) {
            var inputElm = $(this);
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
                self.fromSearchStr = $(this).val();
            } else {
                self.toSearchStr = $(this).val();
            }                    
        });
        // autocomplete
        this.elm.find( 'input' ).autocomplete({ source: Atlas.config.autocompleteUrl, zIndex: 1000, delay: 500 });
        
        // click -> checkFromToPoint()
        this.elm.find( '#routing-btn-'+index ).click( function( ev ) {
            self.checkPoints()
        });
    };
    
    /**
     * Check if this.fromPoint and this.toPoint are set. If not call
     * this.createPicklistDialog() and check back.
     */
    this.checkPoints = function() {
        var self = this;
        if (!this.fromPoint) {
            var searchUrl = this.searchService.searchUrl( this.fromInput.val() )
            this.createPicklistDialog( searchUrl, function( feature ) {
                self.fromInput.val( self.fromSearchStr = feature.data.title );
                self.fromPoint = feature.geometry.getCentroid();
                self.checkPoints();
            });
        }
        else if (!this.toPoint) {
            searchUrl = this.searchService.searchUrl( this.toInput.val() )
            this.createPicklistDialog( searchUrl, function( feature ) {
                self.toInput.val( self.toSearchStr = feature.data.title );
                self.toPoint = feature.geometry.getCentroid();
                self.checkPoints();
            });
        }
        else {
            this.doRouteSearch( this.fromPoint, this.toPoint, this.index );
        }
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
            delay = setTimeout( doSetTooltip, 1000 );
        });
        
        function doSetTooltip() {
            var searchUrl = self.searchService.searchUrl( _inputElm.val() );
            self.searchService.search( searchUrl, function( features ) {
                _parentElm.text( features.length );
                _parentElm.attr( 'title', features.length + ' mögliche Orte'  );
                if (features.length == 1 /*&& features[0].data.title == _inputElm.val()*/) {
                    callback.call( self, features[0] );
                }
            });
        }
    };
    
    /**
     * 
     */
    this.createPicklistDialog = function( searchUrl, callback ) {
        var self = this;
        
        $(document.body).append( '<div id="routing-picklist-dialog"></div>' );
        var dialogDiv = $( '#routing-picklist-dialog' );

        dialogDiv.append( '<span style="color:#808080;">Mögliche Orte:</span>'
                + '<select id="routing-result-list" name="list1" size="15"'
                + '    style="width:100%; height:100%;" />' );
                    
        dialogDiv.dialog( {
            title: 'Routing Ziel/Start',
            modal: true,
            //show: 'scale',
            minWidth: 350,
            minHeight: 300,
            close: function( ev, ui ) {
                dialogDiv.remove();
            }
        });

        this.searchService.search( searchUrl, function( features ) {
            dialogDiv.find( '>span' ).text( 'Mögliche Orte (' + features.length + ')' );
            
            var selectElm = dialogDiv.find( '#routing-result-list' );
            selectElm.click( function( ev ) {
                dialogDiv.remove();
                callback.call( self, features[ev.target.index] );
            });

            selectElm.empty();
            for (var i=0; i<features.length; i++) {
                selectElm.append( '<option>' + features[i].data.title + '</option>' );
            }
        });
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
                'isBaseLayer': false,
                'visibility': true,
                'reportError': true,
                'strategies': [new OpenLayers.Strategy.Fixed()],
                'protocol': new OpenLayers.Protocol(),
                'styleMap': self.defaultStyleMap()
            });
            self.layer.attribution = self.service.attribution;

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

            // result panel
            self.tooltip = new RoutingResultPanel( self.elm.parent(), self, status, vectors );            
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
    this.defaultStyleMap = function() {
        var self = this;
        var defaultStyle = new OpenLayers.Style({
            externalGraphic: self.markerImage,  //'images/marker_b.png',
            graphicHeight: 28,
            graphicWidth: 36,
//            graphicXOffset: -10.5,
//            graphicYOffset: -12.5,
            strokeWidth: 5,
            strokeColor: '#4444FF',  //self.pathColor
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
    };
    
});

/**
 * Route and segment display over the map.
 * 
 * @author <a href="http://polymap.de">Falko Bräutigam</a>
 */
var RoutingResultPanel = Class.extend( new function RoutingResultPanelProto() {
    
    this.parent = null;
    
    this.shortestPath = null;
    
    this.highlighted = null;
    
    this.defaultStyle = null;
    
    this.init = function( parent, shortestPath, status, features ) {
        this.parent = parent;
        this.shortestPath = shortestPath;
        this.parent.append( '<hr/><div>'
                + '<div class="routing-result-entry">'
                + '<b>' + 'routing_total_length'.i18n() + '</b> <span id="routing-total-length" /> km<br/>'
                + '<b>' + 'routing_total_cost'.i18n() + '</b> <span id="routing-total-cost" /> min.<br/>'
                + '</div>'
                + '<div id="routing-tooltip-list" class="routing-result-list"></div>'
                + '</div>' );
        
        var list = this.parent.find('#routing-tooltip-list');
        var totalLength = 0;
        var totalTime = 0;
        var self = this;
        
        var street = {'name': 'Start', 'length':0, 'time':0, 'features':[], 'index':0};
        this.streets = [street];
        $.each( features, function( i, feature ) {
            
            if (feature.data.length && feature.data.time) {
                if (street.name != feature.data.name) {
                    list.append( '<div class="routing-result-entry">'
                            + '<b>' + (street.name.length > 0 ? street.name : '-') + '</b><br/>'
                            + '<span style="float:right;">'
                            + (street.length).toFixed( 2 ) + ' km <br/>' 
                            //+ (street.time/60).toFixed( 2 ) + ' min. <br/>'
                            + '</span></div>' );

                    street = {'name': feature.data.name, 'length':0, 'time':0, 'features':[feature], 'index':self.streets.length};
                    self.streets.push( street );
                }
                else {
                    street.features.push( feature );
                    street.length += feature.data.length;
                    street.time += feature.data.time;
                }
                
                totalLength += feature.data.length;
                totalTime += feature.data.time;
            }
        });
        this.parent.find( '#routing-total-length' ).text( (totalLength).toFixed(2) );
        this.parent.find( '#routing-total-cost' ).text( (totalTime/60).toFixed(2) );
    };
    
    this.close = function() {
    };
    
    this.highlight = function( feature ) {
        if (this.highlighted != null) {
            this.highlighted.css( 'background', this.defaultStyle.background );
            this.highlighted.css( 'border', this.defaultStyle.border );
            this.highlighted.css( 'color', this.defaultStyle.color );
        }

        var found;
        for (var i=0; i<this.streets.length && !found; i++) {
            var street = this.streets[i];
            for (var j=0; j<street.features.length; j++) {
                if (street.features[j] == feature) {
                    found = street;
                    break;
                }
            }
        }
        if (found) {
            var list = this.parent.find( '#routing-tooltip-list' );
            this.highlighted = list.find( ':nth-child({0})'.format( found.index+1 ) );

            this.defaultStyle = {
                    background: this.highlighted.css( 'background' ),
                    border: this.highlighted.css( 'border' ),
                    color: this.highlighted.css( 'color' ) };

            this.highlighted.css( 'background', '#FFFFC0' );
//            .css( 'color', '#222' )
//            .css( 'border', '1px solid #d0d0d0')
//            .addClass( 'ui-corner-all' );
            list.scrollTo( this.highlighted, 1000 );
        }
    };
});
