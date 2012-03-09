/*
 * polymap.org
 * Copyright 2011-2012, Falko Bräutigam. All rights reserved.
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
 * The atlas provides multiple search contexts. The user can switch between
 * them.
 *
 * @param map The map we are working for. 
 * @requires callback.js, utils.js
 */
function SearchContext( map, index, markerImage, resultDiv, geomColor ) {
    
    this.resultDiv = resultDiv;
    
    this.results = [];
    
    /** The OpenLayers.Map we are working for. */
    this.map = map;
    
    this.attribution = 'Daten von <a href=\"http://polymap.org\">Test</a>';

    /** The index of this context. */
    this.index = index;
    
    /** The string that has been searched for in this context. */
    this.searchStr = "";
    
    this.layer = null;
    
    this.popup = null;
    
    this.selectControl = null;
    
    this.hoverControl = null;
    
    this.tooltipsControl = null;
    
    this.markerImage = markerImage;
    
    this.geomColor = geomColor;
    
    this.searchURL = "#";
    
    
    /**
     * Active this context by updating the GUI elements.
     */
    this.activate = function() {
        $('#search_field')
            .val( decodeURIComponent( this.searchStr ) );
            //.css( 'box-shadow', '0 0 3px ' + this.geomColor );        
        $('#search_img').attr( "src", this.markerImage );
        
//        var tabs = $('#tabs');
//        if (tabs.tabs( "option", "selected" ) != index) {
//            tabs.tabs( 'select', index );
//        }

        if (this.hoverControl != null) {
            this.hoverControl.activate();
        }
        if (this.selectControl != null) {
            this.selectControl.activate();
        }
        return this;
    };
    
    /**
     * Deactive this context by updating the GUI elements.
     */
    this.deactivate = function() {
        if (this.selectControl != null) {
            this.selectControl.deactivate();
        }
        if (this.hoverControl != null) {
            this.hoverControl.deactivate();
        }
        if (this.popup != null && this.popup.div != null) {
            this.popup.destroy();
            this.popup = null;
        }
    };
    
    /**
     * 
     */
    this.search = function( searchStr ) {
        try {
            this.searchStr = searchStr;
            // see SearchService for more info
            this.searchStr = encodeURIComponent( this.searchStr );
            this.searchURL = Atlas.config.searchUrl 
                    + "?search=" + this.searchStr 
                    + "&outputType=JSON&srs=" + this.map.getProjection(); 

            // trigger Event
            var ev = jQuery.Event( 'searchPreparing' );
            ev.context = this;
            ev.searchStr = searchStr;
            Atlas.events.trigger( ev );

            if (this.layer != null) {
                this.map.removeLayer( this.layer );
            }
            if (this.selectControl != null) {
                this.map.removeControl( this.selectControl );
            }
            if (this.hoverControl != null) {
                this.map.removeControl( this.hoverControl );
            }
            
            // new layer / style
            var defaultStyle = new OpenLayers.Style({
                'externalGraphic': this.markerImage,
                'graphicHeight': 28,
                'graphicWidth': 36,
                'graphicXOffset': -10.5,
                'graphicYOffset': -12.5,
                'strokeWidth': 2,
                'strokeColor': this.geomColor,
                fillOpacity: 0.7,
                fillColor: '#a0a0a0'
            });
            var selectStyle = new OpenLayers.Style({
                'strokeWidth': 3,
                fillOpacity: 1
            });
            var styleMap = new OpenLayers.StyleMap({
                'default': defaultStyle,
                'select': selectStyle });

            this.layer = new OpenLayers.Layer.GML( "Suchergebnis",
                this.searchURL, { 
                format: OpenLayers.Format.GeoJSON,
                styleMap: styleMap
            });
            this.layer.attribution = this.attribution;
            this.map.addLayer( this.layer );
 
//          // hover feature
            var fh_cb = callback( this.onFeatureHovered, {scope:this} );
            this.hoverControl = new OpenLayers.Control.SelectFeature( this.layer, {
                hover: true,
                highlightOnly: true,
                clickout: true
                //onSelect: fh_cb
            });
            this.map.addControl( this.hoverControl );
            this.hoverControl.activate();
            this.layer.events.register( "featurehighlighted", this.layer, fh_cb );

            // select feature
            this.selectControl = new OpenLayers.Control.SelectFeature( this.layer, {
                clickout: true
//                onSelect: function( ev ) {
//                    alert( "Selected feature: " + ev );
//                }
            });
            this.map.addControl( this.selectControl );
            this.selectControl.activate();
            var fs_cb = callback( this.onFeatureSelected, {scope:this} );
            this.layer.events.register( "featureselected", this.layer, fs_cb );

            var cb = callback( this.onLayerLoaded, {scope:this} );
            this.layer.events.register( "loadend", this.layer, cb );
            this.layer.events.register( "loadcancel", this.layer, cb );
            
//            // tooltips
//            this.layer.events.register( "mouseover", this.layer, function( ev ) {
//                tooltips.show( {html:"My first ToolTips"} );
//            });
//            this.layer.events.register( "mouseout", this.layer, function( ev ) {
//                tooltips.hide();
//            });
//
//            var tooltips = new OpenLayers.Control.ToolTips({
//                bgColor: "#ffffd0",
//                //textColor: "black", 
//                bold: true, 
//                opacity: 1 });
//            map.addControl( tooltips );
        } 
        catch( e ) {
            throw e;
            //alert( "Problem searching: " + e );
        }
    };

    /**
     * Callback for async loading of the feature layer. Build the popups
     * and zooms the map to the data extent.
     */
    this.onLayerLoaded = function() {
        this.resultDiv.empty();
        this.resultDiv.css( 'bottom', '0px' ).css( 'height', '100%' );
        this.results = new Array( this.layer.features.length );
        
        /* Maps category name into JavaScript Function. */
        var categoryRenderers = {};
        var NO_RENDERER = {};
        
        var self = this;
        $.each( this.layer.features, function( i, feature ) {
            
            // trigger Event
            var ev = jQuery.Event( 'searchFeatureLoaded' ); 
            ev.context = this;
            ev.feature = feature; 
            Atlas.events.trigger( ev );

            // basic HTML
            self.resultDiv.append( '<div class="atlas-result" id="feature-' + i 
                    + '"    style="margin-bottom:2px;" class="ui-corner-all">'
                    + '<b><a href="#">' + feature.data.title + '</a></b><br/>' 
                    + '</div><hr/>' );
            
            // load/execute the renderer
            var categories = feature.data.categories != null ? feature.data.categories.split( ',' ) : [];
            categories.push( 'standard' );
            
            for (var j=0; j<categories.length; j++) {
                var renderer = categoryRenderers[categories[j]];
                if (renderer == null) {
                    $.ajax({
                        'url': categories[j].toLowerCase() + '.js',
                        'async': false,
                        'cache': false,
                        'dataType': 'text',
                        'success': function( data ) {
                            categoryRenderers[categories[j]] = renderer = eval( data );
                        },
                        'error': function() {
                            categoryRenderers[categories[j]] = NO_RENDERER;
                        }
                    });
                }
                // execute renderer
                if (renderer != null && renderer != NO_RENDERER) {
                    var div = self.resultDiv.find( '#feature-'+i );
                    // new object/context for every call
                    renderer.call( {}, self, feature, i, div );
                    break;
                }
            }
            
            // trigger event
            var ev = jQuery.Event( "searchResultGenerated" );
            ev.context = this;
            ev.feature = feature;
            ev.index = i;
            ev.div = self.resultDiv.find( '#feature-'+i );
            Atlas.events.trigger( ev );
            
            self.results[i] = ev;
        });

        if (this.layer.features.length > 0) {
            this.map.zoomToExtent( this.layer.getDataExtent() );
            if (this.map.getScale() < 20000) {
                this.map.zoomToScale( 20000, false );
            }
        }
        this.activate();
        
        // send UI event
        var ev = jQuery.Event( "searchCompleted" ); 
        ev.searchStr = this.searchStr;
        ev.searchURL = Atlas.config.searchUrl + "?search=" + this.searchStr; 
        ev.pageURL = pageUrl();
        Atlas.events.trigger( ev );
    };

    /**
     * Callback for 'selectfeature' event.
     * 
     * @param ev
     * @return
     */
    this.onFeatureSelected = function( ev ) {
        this.openPopup( ev.feature.id );
        
//        for (var i=0; i<this.results.length; i++) {
//            var result = this.results[i];
//            if (result.feature == ev.feature) {
//                alert( result.div.position().top );
//                this.resultDiv.scrollTop( result.div.position().top );
//                return false;
//            }
//        }
    };

    /**
     * 
     * @param fid The fid of the feature to popup.
     * @return
     */
    this.openPopup = function( fid, popupHtml ) {
        // remove old popup
        if (this.popup != null && this.popup.div != null) {
            this.popup.destroy();
        }
        
        if (popupHtml == null) {
            popupHtml = "funktioniert noch nicht wieder..."
        }
        
        var feature = null;
        for (var i=0; i<this.layer.features.length; i++) {
            var f = this.layer.features[i];
            if (f.id == fid) {
                feature = f;
                break;
            }
        }
        
        var anchor = {
            size: new OpenLayers.Size( 36, 28 ),
            offset: new OpenLayers.Pixel( 0, 0 )
        };
        this.popup = new OpenLayers.Popup.AnchoredBubble( feature.id, 
                feature.geometry.getBounds().getCenterLonLat(),
                new OpenLayers.Size( 400, 400 ),
                popupHtml,
                anchor, 
                true   //closeBox
                );
        feature.popup = this.popup;
        this.popup.feature = feature;
        map.addPopup( this.popup );
        return null;
    };

    /**
     * Callback for 'featurehighlighted' event.
     * 
     * @param ev
     * @return
     */
    this.onFeatureHovered = function( ev ) {
        alert( "Hovered: " + ev.feature.id );
        
//        // tooltips
//        this.layer.events.register( "mouseover", this.layer, function( ev ) {
//            tooltips.show( {html:"My first ToolTips"} );
//        });
//        this.layer.events.register( "mouseout", this.layer, function( ev ) {
//            tooltips.hide();
//        });

        if (this.tooltipsControl == null) {
            this.tooltipsControl = new OpenLayers.Control.ToolTips({
                bgColor: "#ffffd0",
                //textColor: "black", 
                bold: true, 
                opacity: 1 });
            map.addControl( tooltips );
        }
        this.tooltipsControl.show( {html:ev.feature.id} );
    };

}
