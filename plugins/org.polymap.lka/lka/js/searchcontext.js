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
    
    /** {SearchLayer} */
    this.layer = null;
    
    this.popup = null;
        
    this.markerImage = markerImage;
    
    this.geomColor = geomColor;
    
    this.searchURL = "#";
    
    
    /**
     * Active this context by updating the GUI elements.
     */
    this.activate = function() {
        $('#search_field').val( decodeURIComponent( this.searchStr ) );
        $('#search_img').attr( "src", this.markerImage );
        
//        var tabs = $('#tabs');
//        if (tabs.tabs( "option", "selected" ) != index) {
//            tabs.tabs( 'select', index );
//        }

        if (this.layer) {
            this.layer.activate();
        }
        return this;
    };
    
    /**
     * Deactive this context by updating the GUI elements.
     */
    this.deactivate = function() {
        if (this.layer) {
            this.layer.deactivate();
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
            this.resultDiv.empty();
            this.resultDiv.css( 'bottom', '0px' ).css( 'height', '100%' );
            this.resultDiv.append( 
                    '<div style="width:100px; margin:0 auto; align:center; text-align:center;">' +
                    '<img src="{0}" style="margin:5px;"></img><br/>'.format( 'images/loader_small.gif') +
                    '<em>{0}</em></dev>'.format( 'context_loading'.i18n() ) );
            
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

            // remove old layer
            if (this.layer) {
                this.layer.close();
            }
            
            // new layer / style
            this.layer = new SearchLayer( this, {
                'url': this.searchURL,
                'attribution': this.attribution,
                'markerImage': this.markerImage,
                'color': this.geomColor,
                //'onFeatureHovered': callback( this.onFeatureHovered, {scope:this} ),
                'onLoaded': callback( this.onLayerLoaded, {scope:this} ) 
            });
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
        
        /* maps category name into JavaScript Function. */
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
            var key = feature.id.afterLast( '.' );
            self.resultDiv.append( '<div class="atlas-result" id="feature-' + key  
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
                            renderer = new Function( 'context', 'feature', 'index', 'div', data );
                            categoryRenderers[categories[j]] = renderer;
                        },
                        'error': function() {
                            categoryRenderers[categories[j]] = NO_RENDERER;
                        }
                    });
                }
                // execute renderer
                if (renderer != null && renderer != NO_RENDERER) {
                    var div = self.resultDiv.find( '#feature-'+key );
                    // new object/context for every call
                    try {
                        renderer.call( {}, self, feature, i, div );
                    }
                    catch ( e ) {
                        alert( 'Renderer: ' + e );
                    }
                    break;
                }
            }
            
            // trigger event
            var ev = jQuery.Event( "searchResultGenerated" );
            ev.context = this;
            ev.feature = feature;
            ev.index = i;
            ev.div = self.resultDiv.find( '#feature-'+key );
            Atlas.events.trigger( ev );
            
            self.results[i] = ev;
        });

        if (this.layer.features.length > 0) {
            this.map.zoomToExtent( this.layer.layer.getDataExtent() );
            if (this.map.getScale() < 20000) {
                this.map.zoomToScale( 20000, false );
            }
            $('#tab_title_result'+index).text( this.layer.features.length );
        }
        else {
            $('#tab_title_result'+index).text( '<>' );
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
    };

    /**
     * 
     * @param fid The fid of the feature to popup.
     * @return
     */
    this.openPopup = function( featureOrFid, popupHtml ) {
        // remove old popup
        if (this.popup != null && this.popup.div != null) {
            this.popup.destroy();
        }
        
        if (popupHtml == null) {
            popupHtml = "funktioniert noch nicht wieder..."
        }
        
        var feature = null;
        if (typeof featureOrFid == 'string') {
            for (var i=0; i<this.layer.features.length; i++) {
                var f = this.layer.features[i];
                if (f.id == featureOrFid) {
                    feature = f;
                    break;
                }
            }
        }
        else {
            feature = featureOrFid;
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
     * 
     */
    this.findResultDiv = function( feature ) {
        return this.resultDiv.find( '#feature-' + feature.id.afterLast('.') );
    };
    
}
