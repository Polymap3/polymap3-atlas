/*
 * polymap.org
 * Copyright 2011, Falko Bräutigam. All rights reserved.
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
 * Abstract base class for all items in Toolbar or Topbar.
 */
var ToolItem = Class.extend( {
    
    MODE_TOGGLE: "toggle",
    MODE_PUSH: "push",
    
    init: function( id, label, tooltip ) {
        this.id = id;
        this.label = label;
        this.tooltip = tooltip;
        this.mode = this.MODE_PUSH;
        return this;
    },

    /**
     * Called by the toolbar right after the DOM element for us was created.
     */
    elementCreated: function( elm ) {
        this.elm = elm;
    },
    
    getId: function() {
        return this.id;
    },
  
    getLabel: function() {
        return this.label;
    },
  
    getTooltip: function() {
        return this.tooltip;
    },
  
    getMode: function() {
        return this.mode;
    },
    
    onClick: function() {
        alert( "click!" );
    }
  
});


/**
 * 
 */
var LinkToolItem = ToolItem.extend( {
    
    init: function( id, label, tooltip, url ) {
        this._super( id, label, tooltip );
        this.url = url;
        return this;
    },
    
    onClick: function() {
        window.open( this.url );
    }
});


/**
 * 
 */
var HtmlDialogToolItem = ToolItem.extend( new function HtmlDialogToolItemProto() {
    
    this.init = function( id, label, tooltip, div, htmlUrl ){
        this._super( id, label, tooltip );
        this.htmlUrl = htmlUrl;
        this.div = div;
    };
    
    this.onClick = function() {
        var divElm = this.div;
        var title = this.getLabel();
        $.ajax({
            url: this.htmlUrl,
            dataType: "html",
            success: function( data ) {
                divElm.html( data );
                divElm.dialog( {modal:true, width:640, height:560, title:title} );
                //divElm.addClass( 'ui-widget-shadow' );
            }
        });
    };
});


/**
 * 
 */
var ToggleLayerItem = ToolItem.extend( {
    
    init: function( id, label, tooltip, icon, map, layerName ){
        this._super( id, label, tooltip );
        this.mode = "toggle";
        this.icon = icon;
        this.layerName = layerName;
        this.map = map;
        this.enabled = false;
    },
    
    onClick: function() {
        var layer = this.map.getLayersByName( this.layerName )[0];
       
        if (this.enabled) {
            layer.setVisibility( false );
            this.div.remove();
            this.enabled = false;
        }
        else {
            layer.setVisibility( true );
            this.enabled = true;
            
            $('#center_pane').append( '<div id="atlas-opacity-slider"'
                    + ' class="atlas-opacity-slider" style="z-index:5000;"></div>' );
            this.div = $('#center_pane').find('#atlas-opacity-slider');
            this.div.slider( {
                value: 50,
                slide: function(ev, ui) {
                   ev.stopPropagation();
                   var opacity = ui.value / 100;
                   layer.setOpacity( opacity );
                   //ev.preventDefault();
                   return true;
                }
            });
            layer.setOpacity( 0.5 );
        }
    }
});


/**
 *
 */
var GeoRssItem = ToolItem.extend( {
    
    init: function( id ) {
        this._super( id, $.i18n.prop( "tb_georss_label" ), $.i18n.prop( "tb_georss_tip" ) );
        this.icon = $.i18n.prop( "tb_georss_icon" );
        
        Atlas.events.bind( "searchCompleted", 
                callback( this.onSearchCompleted, {scope:this, suppressArgs:false} ) );
    },
    
    elementCreated: function( elm ) {
        this.elm = elm;
        //this.elm.attr( 'disabled', 'disabled' );
    },
    
    onSearchCompleted: function( ev ) {
        this.url = ev.searchURL;
        this.elm.removeAttr( 'disabled' );
        this.elm.attr( "href", this.url );
        this.elm.attr( "target", "_blank" );
    },
    
    onClick: function() {
        if (this.url == null) {
            alert( $.i18n.prop( "tb_georss_disabled" ) );
            return;
        }
        var parts = this.url.split( "?" );
        var rssUrl = parts[0] + "/atlas.kml?" + parts[1] + "&outputType=rss";
        window.open( rssUrl );
    }
});


/**
 *
 */
var KmlItem = ToolItem.extend( new function KmlItemProto() {
    
    this.init = function( id ) {
        this._super( id, $.i18n.prop( "tb_kml_label" ), $.i18n.prop( "tb_kml_tip" ) );
        this.icon = $.i18n.prop( "tb_kml_icon" );
        
        Atlas.events.bind( "searchCompleted", 
                callback( this.onSearchCompleted, {scope:this, suppressArgs:false} ));
    };
    
    this.elementCreated = function( elm ) {
        this.elm = elm;
        //this.elm.attr( 'disabled', 'disabled' );
    };
    
    this.onSearchCompleted = function( ev ) {
        this.url = ev.searchURL;
        this.elm.removeAttr( 'disabled' );
    };
    
    this.onClick = function() {
        if (this.url == null) {
            alert( $.i18n.prop( "tb_kml_disabled" ) );
            return;
        }
        // KML
        var parts = this.url.split( "?" );
        var kmlUrl = parts[0] + "/atlas.kml?" + parts[1] + "&outputType=kml";
        window.open( kmlUrl );
    };
});


/**
 *
 */
var LinkItem = ToolItem.extend( new function LinkItemProto() {
    
    this.init = function( id ) {
        this._super( id, $.i18n.prop( "tb_link_label" ), $.i18n.prop( "tb_link_tip" ) );
        this.icon = $.i18n.prop( "tb_link_icon" );
        
        Atlas.events.bind( "searchCompleted", 
                callback( this.onSearchCompleted, {scope:this, suppressArgs:false} ))
    };
    
    this.onSearchCompleted = function( ev ) {
        // the page URL including search params from all search contexts
        var searchParams = null;
        for (var i=0; i<Atlas.contexts.length; i++) {
            var context = Atlas.contexts[i];
            if (context.searchStr != null && context.searchStr.length > 0) {
                searchParams = searchParams == null 
                        ? "?" : searchParams + "&";
                searchParams += "search" + (i+1) + "=" 
                        + encodeURIComponent( context.searchStr );
            }
        }
        this.url = location.protocol + "//" 
            + location.host + location.pathname + searchParams;

        //this.url = ev.pageURL;  //pageUrl()
        this.elm.removeAttr( 'disabled' );
    };
    
    this.onClick = function() {
        if (this.url == null) {
            alert( $.i18n.prop( "tb_link_disabled" ) );
            return;
        }
        var url = this.url;
        var htmlCode = '<iframe width="425" height="350" frameborder="0" scrolling="no" marginheight="0" marginwidth="0" src="'
                + url + '&north=off&east=off"></iframe>';
        $.ajax({
            url: 'linkdialog.html',
            dataType: "html",
            success: function( data ) {
                var divElm = $( '#dialog' );
                divElm.html( data );
                
                $("#link_input")
                        .focus( function( ev ) { $(this).select(); } )
                        .attr( "value", url );
                $("#html_link_input")
                        .focus( function( ev ) { $(this).select(); } )
                        .attr( "value", htmlCode );
                
                divElm.dialog({ modal:true, width:450, height:200, title:'Link speichern' });
            }
        });
    };
});


/**
 *
 */
var BookmarkItem = LinkItem.extend( {
    
    init: function( id ) {
        this._super( id );
        this.label = $.i18n.prop( "tb_bookmark_label" );
        this.tooltip = $.i18n.prop( "tb_bookmark_tip" );
        this.icon = $.i18n.prop( "tb_bookmark_icon" );
        
//        var self = this;
//        Atlas.events.bind( "searchCompleted", function( ev ) {
//            self.url = ev.pageURL;
//            self.elm.removeAttr( 'disabled' );
//        });
    },
    
    onClick: function() {
        if (!this.url) {
            alert( $.i18n.prop( "tb_bookmark_disabled" ) );
            return;
        }        
        if (window.sidebar) {
            // firefox
            window.sidebar.addPanel( 'tb_bookmark_title'.i18n(), this.url, 'tb_bookmark_comment'.i18n() );
        } 
        else if (window.opera && window.print) {
            // opera
            var elem = document.createElement('a');
            elem.setAttribute('href', this.url);
            elem.setAttribute('title', 'tb_bookmark_title'.i18n());
            elem.setAttribute('rel', 'sidebar');
            elem.click();
        } 
        else if (document.all) {
            // ie
            window.external.AddFavorite( this.url, 'tb_bookmark_title'.i18n() );
        }
        else {
            alert( 'Ihr Browser unterstützt leider nicht das automatische Anlegen eines Bookmarks. :(' );
        }
    }
});


/**
 *
 */
var SubmitNewItem = ToolItem.extend( new function SubmitNewItemProto() {
    
    this.init = function( id ) {
        this._super( id, $.i18n.prop( "tb_new_label" ), $.i18n.prop( "tb_new_tip" ) );
        this.icon = $.i18n.prop( "tb_new_icon" );
        this.url = "test";
    };
    
    this.onClick = function() {
        var contentURL = "poi_form.html";
        var divElm = $( '#dialog' );
        $.ajax({
            url: contentURL,
            dataType: "html",
            success: function( data ) {
                divElm.html( data );
                divElm.dialog({ modal:true, width:400, height:430 , title:"Einen neuen Ort anlegen" });
            }
        });
    };
});
