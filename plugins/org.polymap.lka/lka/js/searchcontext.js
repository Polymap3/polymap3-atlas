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
    
    this.smallMarkerImage = markerImage.split('.')[0] + "_small.png";
    
    this.geomColor = geomColor;
    
    this.searchURL = "#";

    /** Maps category name into JavaScript Function. */
    this.categoryRenderers = {};
    
    var NO_RENDERER = {};
    
    /** Array of Functions to be used to order features. */
    this.resultOrderComparators = [];
    
    /** The feature that was last made visible. */
    this.scrolledFeature = null;
    
    /**
     * Local events for this context. These events are cleared for every renderFeatures()
     * run. So it is primarily for renderers to register listeners.
     * <p/>
     * Supported event types:
     *   - featureselected
     */
    this.events = new Events();
    
    this.active = false;

    
    /**
     * Active this context by updating the GUI elements.
     */
    this.activate = function() {
        this.active = true;
        $('#search_field').val( decodeURIComponent( this.searchStr ) );
        $('#search_img').attr( "src", this.markerImage );
        
//        var tabs = $('#tabs');
//        if (tabs.tabs( "option", "selected" ) != index) {
//            tabs.tabs( 'select', index );
//        }
    
        if (this.layer) {
            this.layer.activate();
        }
        
        // scale/move -> re-render features
        var self = this;
        this.map.events.register( 'moveend', this.map, function( ev ) {
            // skip if not active
            if (!self.active) {
                return;
            }
            if (self.renderTimeout) {
                clearTimeout( self.renderTimeout );
                self.renderCancelled = true;
            }
            
            self.renderTimeout = setTimeout( function() {
                self.renderCancelled = false;
                self.reorderResults();

                if (self.scrolledFeature) {
                    self.scrollToResultDiv( self.scrolledFeature, 100 );
                }                
            }, 2500 );
        });
        this.map.events.register( 'movestart', this.map, function( ev ) {
            if (self.renderTimeout) {
                clearTimeout( self.renderTimeout );
            }
            self.renderCancelled = true;            
        });

        return this;
    };
    
    /**
     * Deactive this context by updating the GUI elements.
     */
    this.deactivate = function() {
        this.active = false;
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
//        try {
            this.resultDiv.empty();
            this.resultDiv.append( 
                    '<div style="width:100px; margin:1em auto; align:center; text-align:center;">' +
                    '<img src="{0}" style="margin:5px;"></img><br/>'.format( 'images/loader_small.gif') +
                    '<em>{0}</em></div>'.format( 'context_loading'.i18n() ) );
            
            // remove old layer
            if (this.layer) {
                this.layer.destroy();
            }
            
            this.searchStr = searchStr;
            
            // GeoJSON
            if (searchStr.startsWith( '{' )) {
                this.searchURL = Atlas.config.searchUrl 
                    + "?search=" + encodeURIComponent( this.searchStr ); 
                var feature = new OpenLayers.Format.GeoJSON().read( searchStr, 'Feature' );

                // new layer / style
                this.layer = new SearchLayer( this, {
                    'feature': feature,
                    'attribution': this.attribution,
                    'markerImage': this.markerImage,
                    'color': this.geomColor
                });
                this.onLayerLoaded();
            }
            // Lucene/server search
            else {
                // trigger Event
                var ev = jQuery.Event( 'searchPreparing' );
                ev.context = this;
                ev.searchStr = searchStr;
                Atlas.events.trigger( ev );

                // encodeURIComponent: see SearchService for more info
                // use ev.searchStr to allow listeners to tweak
                this.searchURL = Atlas.config.searchUrl 
                    + "?search=" + encodeURIComponent( ev.searchStr ) 
                    + "&outputType=JSON&srs=" + this.map.getProjection(); 

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
            
            // general feature click handler
            this.layer.events.register( 'featureselected', this, function( ev ) {
                this.events.trigger( ev );

                if (ev.feature && !ev.feature.cluster) {
                    this.scrollToResultDiv( ev.feature );
                }
            });
//        } 
//        catch( e ) {
//            throw e;
//            //alert( "Problem searching: " + e );
//        }
    };

    /**
     * Callback for async loading of the feature layer. Build the popups
     * and zooms the map to the data extent.
     */
    this.onLayerLoaded = function() {
        this.results = new Array( this.layer.features.length );
        
        var self = this;
        $.each( this.layer.features, function( i, feature ) {
            // trigger Event
            var ev = jQuery.Event( 'searchFeatureLoaded' ); 
            ev.context = this;
            ev.feature = feature; 
            Atlas.events.trigger( ev );
            
            self.results[i] = ev;
        });

        if (this.layer.features.length > 0) {
            var extent = this.map.getExtent();
            var layerExtent = this.layer.layer.getDataExtent();
            if (layerExtent != null) {
                this.map.zoomToExtent( layerExtent );
                if (this.map.getScale() < 20000) {
                    this.map.zoomToScale( 20000, false );
                }
            }
            
            // render in thread so that subsequent move/scale can interrupt
            this.renderFeatures();
            this.renderTimeout = setTimeout( function() {
                self.renderCancelled = false;
                self.reorderResults();
            }, 1000 );

            $('#tab_title_result'+index).text( this.layer.features.length );
        }
        else {
            $('#tab_title_result'+index).text( '<>' );
    
            this.resultDiv.empty();
            this.resultDiv.append( 
                    '<div class="atlas-no-results">{0}</div>'.format( 'context_no_results'.i18n() ) );
            this.resultDiv.find('.atlas-no-results').animate( {'backgroundColor': '#fffff' }, 500, 'swing' );
        }
        this.activate();
        
        // send UI event
        var ev = jQuery.Event( "searchCompleted" ); 
        ev.searchStr = this.searchStr;
        ev.searchURL = Atlas.config.searchUrl + "?search=" + encodeURIComponent( this.searchStr ); 
        Atlas.events.trigger( ev );
    };

    /**
     * 
     */
    this.renderFeatures = function() {
        //this.resultDiv.empty();
        
        // remove all feature listeners that old renderers might have registered
        this.events.unbind( 'featureselected' );
        
        this.featureMap = {};
        
        var self = this;
        $.each( this.layer.features, function( i, feature ) {
//            // check cancel
//            if (self.renderCancelled) {
//                alert( "Rendering cancelled." );
//                return false;
//            }
            
            // no fid -> ommitting
            if (!feature.id || !feature.data) {
                alert( 'Empty feature: ' + feature );
            }
            
            var categories = feature.data.categories != null ? feature.data.categories.split( ',' ) : [];

            // basic HTML
            var displayCats = [];
            for (var i=0; i<categories.length; i++) {
                if (categories[i].charAt(0) == categories[i].charAt(0).toUpperCase()) {
                    displayCats.push( categories[i] );
                }
            }
            var key = feature.id.afterLast( '.' );
            self.resultDiv.append( 
                      '<div class="atlas-result" id="feature-{0}" style="display:none;">'.format( key )  
                    + '  <a style="font-weight:bold;" href="#">{0}</a>'.format( feature.data.title )
                    + '  <div class="atlas-result-inner">'
                    + '    <span class="atlas-result-categories">{0}</span>'.format( displayCats.join( ', ' ) )
                    + '  </div>'
                    + '</div>' );

            // load/execute the renderer
            var categories = feature.data.categories != null ? feature.data.categories.split( ',' ) : [];
            categories.push( 'standard' );
            
            for (var j=0; j<categories.length; j++) {
                var renderer = self.categoryRenderers[categories[j]];
                if (renderer == null) {
                    $.ajax({
                        'url': categories[j].toLowerCase() + '.js',
                        'async': false,
                        'cache': false,
                        'dataType': 'text',
                        'success': function( data ) {
                            renderer = new Function( 'context', 'feature', 'index', 'div', 'title', data );
                            self.categoryRenderers[categories[j]] = renderer;
                        },
                        'error': function() {
                            self.categoryRenderers[categories[j]] = NO_RENDERER;
                        }
                    });
                }
                // execute renderer
                if (renderer != null && renderer != NO_RENDERER) {
                    var div = self.findResultDiv( feature );
                    self.featureMap[div.attr('id')] = feature;
                    
                    // new object/context for every call
                    try {
                        // call the renderer
                        renderer.call( {}, self, feature, i, div.find('.atlas-result-inner'), div.find('>a') );
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
            ev.div = self.findResultDiv( feature );
            Atlas.events.trigger( ev );
        });
    };
    
    /**
     * Order the given features using the priority comparators in {@link Atlas#priority}.
     */
    this.reorderResults = function() {
        var self = this;
        
        var detached = this.resultDiv.find('.atlas-result').detach();
        
        var sorted = detached.sort( function( div1, div2 ) {
//            if (self.renderCancelled) {
//                return 0;
//            }
            var feature1 = self.featureMap[ $(div1).attr('id') ];
            var feature2 = self.featureMap[ $(div2).attr('id') ];
            
            for (var i=0; i<Atlas.priorities.length; i++) {
                var compResult = Atlas.priorities[i].compare( feature1, feature2 );
                if (compResult != 0) {
                    return compResult;
                }
            }
            return 0;
        });

        // remove decorations, deparators, etc.
        this.resultDiv.empty();
        
        // re-insert
        var screenBounds = this.map.getExtent();
        for (var i=0; i<sorted.length; i++) {
            // don't cancel as this would leave resultDiv empty
//            if (self.renderCancelled) {
//                return 0;
//            }
            var div = $(sorted[i]);
            div.css( 'display', 'block' );

            // feature visible on screen?
            var feature = self.featureMap[ div.attr('id') ];
            var featureBounds = feature.geometry.getBounds();
            var onScreen = screenBounds.intersectsBounds( featureBounds );
            div.toggleClass( 'atlas-result-offscreen', !onScreen );
            div.find( '>a' )
                    .css( 'background', onScreen ? 'url({0}) no-repeat scroll 0px 0px'.format( self.smallMarkerImage ) : '')
                    .css( 'padding', '0px 0px 4px 18px' )
                    .css( 'display', 'inline-block' );

            self.resultDiv.append( sorted[i] );
            self.resultDiv.append( '<hr/>');
        }
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
     * @return null
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

        // make this visible after addPopup() -> moveend event -> renderFeatures()
        this.scrolledFeature = feature;

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
    this.scrollToResultDiv = function( feature, duration ) {
        this.scrolledFeature = feature;
        var div = this.findResultDiv( feature );
        if (div) {
            this.resultDiv.scrollTo( div, duration ? duration : 2000 );
        }
    };
    
    /**
     * 
     */
    this.findResultDiv = function( feature ) {
        return feature.id ? this.resultDiv.find( '#feature-' + feature.id.afterLast('.') ) : null;
    };
    
}
