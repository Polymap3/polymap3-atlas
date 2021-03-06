/*
 * polymap.org
 * Copyright 2011, Falko Br�utigam. All rights reserved.
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
 * The default Topbar used in the Atlas client.
 * <p>
 *  Needs: utils.js 
 */
var Topbar = Class.extend( {
    
    init: function( elm ) {
        this.elm = elm;
        this.leftItems = new Array();
        this.rightItems = new Array();
        return this;
    },

    addLeft: function( item ) {
        this.leftItems.push( item );
        return this;
    },
  
    addRight: function( item ) {
        this.rightItems.push( item );
        return this;
    },
  
    create: function() {
        this.elm.addClass( 'ui-widget' )  // ui-widget-header
                .addClass( 'ui-widget-content' );
        
        // left items
        for (var i=0; i<this.leftItems.length; i++) {
            var itemElm = this.createItem( this.elm, this.leftItems[i] );
            itemElm.css( 'float', 'left' );
            if (i < this.leftItems.length - 1) {
                this.elm.append( '<span style="float:left;"> | </span>' );
            }
        }
        // right items
        for (var i=this.rightItems.length-1; i >= 0; i--) {
            var itemElm = this.createItem( this.elm, this.rightItems[i] );
            itemElm.css( 'float', 'right' );
            if (i < this.rightItems.length - 1) {
                itemElm.append( '<span> | </span>' );
            }
        }
    },
  
    createItem: function( parent, /*ToolItem*/ item ) {
        parent.append( '<span id="' + item.getId() + '_outer' + '">' +
                '<a id="' + item.getId() + '" href="#" title="' + item.getTooltip() + '">' + item.getLabel() + '</a></span>' );

        var button = $( '#' + item.getId() );

        if (item.icon != null) {
            button.prepend( '<img src="' + item.icon + '" style="float:left; margin:0px;">' );
        }
        
        button.addClass( "ui-widget-content" );
        button.click( callback( item.onClick, {scope:item, suppressArgs:false} ) );

        item.elementCreated( button );
        return $( '#' + item.getId() + '_outer' );
    }
});


/**
 * The default Toolbar used in the Atlas client.
 * <p>
 *  Needs: Topbar, utils.js
 */
var Toolbar = Topbar.extend( {
    
    init: function( elm ) {
        this._super( elm );
        return this;
    },

    create: function() {
        this.elm.addClass( 'ui-widget' )
                .addClass( 'ui-widget-content' )
                .addClass( 'ui-widget-header' )
                .addClass( 'ui-corner-top' );
        
        // left
        for (var i=0; i<this.leftItems.length; i++) {
            var mode = this.leftItems[i].getMode();
            var itemElm = mode == "toggle"
                    ? this.createToggle( this.elm, this.leftItems[i] )
                    : this.createButton( this.elm, this.leftItems[i] );
            itemElm.css( "float", "left" );
            this.leftItems[i].elementCreated( itemElm );
        }
        // right
        for (var i=0; i<this.rightItems.length; i++) {
            var item = this.rightItems[i];
            var itemElm = item.mode == "toggle"
                    ? this.createToggle( this.elm, item )
                    : this.createButton( this.elm, item );
            itemElm.css( "float", "right" );
                //.css( 'margin-right', 'auto' );
                //.css( 'margin-left', 'auto' );
            this.rightItems[i].elementCreated( itemElm );
        }
    },
  
    createButton: function( parent, item ) {
        parent.append( '<button id="' + item.getId() + '" title="' + item.getTooltip() + '">' );
        
        var button = $( '#'+item.getId() );
        button.button({ label: item.getLabel() });
        button.click( callback( item.onClick, {scope:item, suppressArgs:false} ) );
        
        if (item.icon != null) {
            button.css( 'display', 'inline' );
            $('#'+item.getId()+'>span')
                    .css( 'background', 'url(' + item.icon + ') no-repeat scroll 5px 50%' );
        }
        return button;
    },
  
    createToggle: function( parent, item ) {
        parent.append( '<input type="checkbox" id="' + item.getId() + '" title="' + item.getTooltip() + '">'
                + '<label " for="' + item.getId() + '"></label>');
        var checkbox = $('#'+item.getId());
        checkbox.button({ label: item.getLabel() });
        checkbox.click( callback( item.onClick, {scope:item, suppressArgs:false} ) );

        var label = $( '#'+item.getId()+"~label" );
        
        if (item.icon != null) {
            // space for the icon (span is the label text)
            $('#'+item.getId()+"~label>span")
                    .css( 'display', 'block' )
                    .css( 'background', 'url(' + item.icon + ') no-repeat scroll 5px 50%' );
        }
        return label;
    }
  
});
