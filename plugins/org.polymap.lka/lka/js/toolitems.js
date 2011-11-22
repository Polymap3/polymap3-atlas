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
    
    /** The id of the created DOM element. */
//    id: null,
//    
//    mode: null,
//    
//    label: "undefined",
//
//    tooltip: "undefined",
    
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
    
//    url: null,
    
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
var HtmlDialogToolItem = ToolItem.extend( {
    
    init: function( id, label, tooltip, div, htmlUrl ){
        this._super( id, label, tooltip );
        this.htmlUrl = htmlUrl;
        this.div = div;
    },
    
    onClick: function() {
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
    }
  
});


/**
 * 
 */
var ToggleLayerItem = ToolItem.extend( {
    
//    icon: null,
//    
//    layerName: null,
    
    init: function( id, label, tooltip, icon, map, layerName ){
        this._super( id, label, tooltip );
        this.mode = "toggle";
        this.icon = icon;
        this.layerName = layerName;
        this.map = map;
        this.enabled = false;
    },
    
    onClick: function() {
        layer = this.map.getLayersByName( this.layerName )[0];
       
        if (this.enabled) {
            layer.setVisibility( false );
            //topo_transparent.setVisibility( false );
            //topo.setVisibility( true );
            this.enabled = false;
        }
        else {
            //topo_transparent.setVisibility( true );
            //topo_transparent.setOpacity( 0.50 );
        
            //topo.setVisibility( false );
            layer.setVisibility( true );
            this.enabled = true;
        }
    }
});


/**
 *
 */
var BookmarkItem = ToolItem.extend( {
    
    init: function( id ) {
        this._super( id, $.i18n.prop( "tb_bookmark_label" ), $.i18n.prop( "tb_bookmark_tip" ) );
        this.icon = $.i18n.prop( "tb_bookmark_icon" );
        
        EventManager.subscribe( "searchCompleted", 
                callback( this.onSearchCompleted, {scope:this, suppressArgs:false} ))
    },
    
    elementCreated: function( elm ) {
        this.elm = elm;
        //this.elm.attr( 'disabled', 'disabled' );
    },
    
    onSearchCompleted: function( ev ) {
        this.url = ev.searchURL;
        this.elm.removeAttr( 'disabled' );
    },
    
    onClick: function() {
        if (this.url == null) {
            alert( $.i18n.prop( "tb_bookmark_disabled" ) );
            return;
        }
        
        var title = $.i18n.prop( "tb_bookmark_title" );
        var comment = $.i18n.prop( "tb_bookmark_comment" );
        var url = this.url;
        
        if (window.sidebar) {
            // firefox
            window.sidebar.addPanel( title, url, comment );
        } 
        else if (window.opera && window.print) {
            // opera
            var elem = document.createElement('a');
            elem.setAttribute('href', url);
            elem.setAttribute('title', title);
            elem.setAttribute('rel', 'sidebar');
            elem.click();
        } 
        else if (document.all) {
            // ie
            window.external.AddFavorite( url, title );
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
        
        EventManager.subscribe( "searchCompleted", 
                callback( this.onSearchCompleted, {scope:this, suppressArgs:false} ))
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
        window.open( this.url );
    }
});


/**
 *
 */
var KmlItem = ToolItem.extend( {
    
    init: function( id ) {
        this._super( id, $.i18n.prop( "tb_kml_label" ), $.i18n.prop( "tb_kml_tip" ) );
        this.icon = $.i18n.prop( "tb_kml_icon" );
        
        EventManager.subscribe( "searchCompleted", 
                callback( this.onSearchCompleted, {scope:this, suppressArgs:false} ))
    },
    
    elementCreated: function( elm ) {
        this.elm = elm;
        //this.elm.attr( 'disabled', 'disabled' );
    },
    
    onSearchCompleted: function( ev ) {
        this.url = ev.searchURL;
        this.elm.removeAttr( 'disabled' );
    },
    
    onClick: function() {
        if (this.url == null) {
            alert( $.i18n.prop( "tb_kml_disabled" ) );
            return;
        }
        // KML
        var parts = this.url.split( "?" );
        var kmlUrl = parts[0] + "/atlas.kml?" + parts[1] + "&outputType=kml";
        window.open( kmlUrl );
    }
});


/**
 *
 */
var LinkItem = ToolItem.extend( {
    
    init: function( id ) {
        this._super( id, $.i18n.prop( "tb_link_label" ), $.i18n.prop( "tb_link_tip" ) );
        this.icon = $.i18n.prop( "tb_link_icon" );
        
        EventManager.subscribe( "searchCompleted", 
                callback( this.onSearchCompleted, {scope:this, suppressArgs:false} ))
    },
    
    elementCreated: function( elm ) {
        this.elm = elm;
        //this.elm.attr( 'disabled', 'disabled' );
    },
    
    onSearchCompleted: function( ev ) {
        this.url = ev.pageURL;
        this.elm.removeAttr( 'disabled' );
    },
    
    onClick: function() {
        if (this.url == null) {
            alert( $.i18n.prop( "tb_link_disabled" ) );
            return;
        }
        var url = this.url;
        var htmlCode = '<iframe width="425" height="350" frameborder="0" scrolling="no" marginheight="0" marginwidth="0" src="'
                + url + '"></iframe>';
        $.ajax({
            url: 'linkdialog.html',
            dataType: "html",
            success: function( data ) {
                var divElm = $( '#dialog' );
                divElm.html( data );
                
                $("#link_input").attr( "value", url );
                $("#html_link_input").attr( "value", htmlCode );
                
                divElm.dialog({ width:350, height:240, title:'Link speichern' });
            }
        });
    }
});


/**
 *
 */
var SubmitNewItem = ToolItem.extend( {
    
    init: function( id ) {
        this._super( id, $.i18n.prop( "tb_new_label" ), $.i18n.prop( "tb_new_tip" ) );
        this.icon = $.i18n.prop( "tb_new_icon" );
        this.url = "test";
    },
    
    onClick: function() {
        var contentURL = "poi_form.html";
        var divElm = $( '#dialog' );
        $.ajax({
            url: contentURL,
            dataType: "html",
            success: function( data ) {
                divElm.html( data );
                divElm.dialog({ width:400, height:430 , title:"Einen neuen Ort anlegen" });
            }
        });
    }
});


