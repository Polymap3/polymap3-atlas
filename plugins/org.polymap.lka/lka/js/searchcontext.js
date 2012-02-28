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
 * <p>
 * Needs: callback.js, utils.js
 */
function SearchContext( map, index, markerImage, resultDiv, geomColor ) {
    
    this.resultDov = resultDiv;
    
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
     * List of functions. Every result field is send through
     * all enhancers to create the proper HTML. 
     */
    this.resultFieldEnhancers = [ enhanceLinkResult ];
    
    
    /**
     * Called by #createFeatureHTML() to allow field enhancer functions.
     */
    this.enhanceResultField = function( str ) {
        for (var i=0; this.resultFieldEnhancers.length; i++) {
            var enhanced = this.resultFieldEnhancers[i].call( this, str );
            if (enhanced != null) {
                return enhanced;
            }
        }
        return str;
    };
    
    /**
     * Active this context by updating the GUI elements.
     */
    this.activate = function() {
        $('#search_field')
            .val( decodeURIComponent( this.searchStr ) );
            //.css( 'box-shadow', '0 0 3px ' + this.geomColor );        
        $('#search_img').attr( "src", this.markerImage );

        if (this.hoverControl != null) {
            this.hoverControl.activate();
        }
        if (this.selectControl != null) {
            this.selectControl.activate();
        }
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
     * Provides a way to enhance search URL before it is send to the server
     */
    this.createSearchUrl = function( searchStr ) {
        return Atlas.config.searchUrl + 
                "?search=" + this.searchStr + 
                "&outputType=JSON&srs=" + this.map.getProjection();
    };
    
    /**
     * 
     */
    this.search = function( searchStr ) {
        try {
            this.searchStr = searchStr;
            // all those functions do not work, at least for me
            //search_str = escape( search_str );
            //search_str = new OpenLayers.Format.JSON().write( search_str, false );
            //search_str = $.URLEncode( search_str );
            //search_str = jQuery.param( search_str, true );
            this.searchStr = encodeURIComponent( this.searchStr );
            this.searchURL = this.createSearchUrl( this.searchStr ); 

            //onSearch( this );
            
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
//                    alert( "Selected feature: " + ev.feature.id );
//                }
            });
            this.map.addControl( this.selectControl );
            this.selectControl.activate();
            var fs_cb = callback( this.onFeatureSelected, {scope:this} );
            this.layer.events.register( "featureselected", this.layer, fs_cb );

            var cb = callback( this.onLoad, {scope:this} );
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
    this.onLoad = function() {
        var close_icon = new OpenLayers.Icon( "images/add_obj.gif" );

        resultDiv.empty();
        var self = this;
        $.each( this.layer.features, function( i, feature ) {
            var feature = self.layer.features[i];
            var resultHtml = '<div class="atlas-result" id="feature-' + i + '" style="margin-bottom:2px;" class="ui-corner-all">'
                    + '<b><a href="#">'
                    + self.enhanceResultField( feature.data.title ) + '</a></b><br/>' 
                    + self.createFeatureHTML( feature )
                    + '</div><hr/>';            
            resultDiv.append( resultHtml );
            
            // click
            resultDiv.find( '#feature-'+i+' a' ).click( function( ev ) {
                self.openPopup( feature.id );
            });
            
            // trigger event
            var ev = jQuery.Event( "searchResultGenerated" );
            ev.context = this;
            ev.feature = feature;
            ev.index = i;
            ev.div = resultDiv.find( '#feature-'+i );
            Atlas.events.trigger( ev );
        });

        if (this.layer.features.length > 0) {
            this.map.zoomToExtent( this.layer.getDataExtent() );
            this.activate();
        }
        
        // send UI event
        var ev = jQuery.Event( "searchCompleted" ); 
        ev.searchStr = this.searchStr;
        ev.searchURL = Atlas.config.searchUrl + "?search=" + this.searchStr; 
        ev.pageURL = pageUrl();
        Atlas.events.trigger( ev );
    };

    /**
     * Creates HTML code for a given feature.
     */
    this.createFeatureHTML = function( feature ) {
        var resultHtml = "";
        // address
        if (feature.data.address != null) {
            var address = new OpenLayers.Format.JSON().read( feature.data.address );
    
            if (address != null && address.street != null) {
                resultHtml += "<p class=\"atlas-result-address\">";
                resultHtml += address.street + " " + address.number + "<br/>";
                resultHtml += address.postalCode + " " + address.city;
                resultHtml += "</p>";
            }
        }

        // fields
        resultHtml += '<p id="feature-field-' + feature.id + '" class="atlas-result-fields">';
        var self = this;
        jQuery.each( feature.data, function(name, value) {
            if (name != "title" && name != "address") {
                resultHtml += "<b>" + name.capitalize() + "</b>";
                resultHtml += ": " + self.enhanceResultField( value ) + "<br/>";
            }
        });
        resultHtml += "</p>";
        return resultHtml;
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
    this.openPopup = function( fid ) {
        // remove old popup
        if (this.popup != null && this.popup.div != null) {
            this.popup.destroy();
        }
        
        var feature = null;
        for (i=0; i<this.layer.features.length; i++) {
            var f = this.layer.features[i];
            if (f.id == fid) {
                feature = f;
                break;
            }
        }
        
        // create popup
        var popupHtml = '<b>' + feature.data.title + '</b><br/>'
                + '_____________________________________'
                + this.createFeatureHTML( feature );
        
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
        
        //$('.resultFields').expandable({title:'...'});
        //$('.olPopup').css( 'height', '300px' ).css( 'width', '300px' );

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

///**
// * This function is called by the generated html/javascript in the result
// * body. It is used by {@link SearchContext#onLoad()} to generate javascript
// * calls inside the generated HTML.
// */
//function onFeatureSelect( contextIndex, fid ) {
//    Atlas.contexts[contextIndex].openPopup( fid );
//}


/**
 *
 */
function enhanceLinkResult( str ) {
    if (str.indexOf( "http://") == 0) {
        valueHtml = "<a href=\"" + str + "\" target=\"_blank\">" + str.substring( 7 ) + "</a>";
    }
    else if (str.indexOf( "www.") == 0) {
        valueHtml = "<a href=\"http://" + str + "\" target=\"_blank\">" + str + "</a>";
    }
}


