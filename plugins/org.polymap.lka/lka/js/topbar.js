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
 * The default Topbar used in the Atlas client.
 * <p>
 *  Needs: utils.js 
 */
var Topbar = Class.extend( {
    
//    leftItems: null,
//    
//    rightItems: null,
//    
//    elm: null,
    
    
    init: function( elm ) {
        this.elm = elm;
        this.leftItems = new Array();
        this.rightItems = new Array();
        return this;
    },

    addLeft: function( /*ToolItem*/ item ) {
        this.leftItems.push( item );
        return this;
    },
  
    addRight: function( item ) {
        this.rightItems.push( item );
        return this;
    },
  
    create: function() {
        this.elm.addClass( 'ui-widget' );  // ui-widget-header
        this.elm.addClass( 'ui-widget-content' );
        this.elm.css( "border", "none" );
        this.elm.css( "margin", "3px" );
        this.elm.css( "background", "none" );
        
        for (var i=0; i<this.leftItems.length; i++) {
            this.createItem( this.elm, this.leftItems[i] );
            if (i < this.leftItems.length - 1) {
                this.elm.append( '<span> | </span>' );
            }
        }
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
        
        button.addClass( "ui-widget-content" );
        button.css( "margin", "3px" );
        button.css( "border", "none" );
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
        this.elm.addClass( 'ui-widget' );
        this.elm.addClass( 'ui-widget-content' );
        this.elm.addClass( 'ui-widget-header' );
        this.elm.addClass( 'ui-corner-top' );
        //this.elm.css( "border", "none" );
        this.elm.css( "padding", "2px" );
        //this.elm.css( "border-bottom", "1px solid #808080" );
        
        // left
        for (var i=0; i<this.leftItems.length; i++) {
            var mode = this.leftItems[i].getMode();
            var itemElm = mode == "toggle"
                    ? this.createToggle( this.elm, this.leftItems[i] )
                    : this.createButton( this.elm, this.leftItems[i] );
//            if (i < this.leftItems.length - 1) {
//                this.elm.append( '<span> | </span>' );
//            }
            this.leftItems[i].elementCreated( itemElm );
        }
        // right
        for (var i=0; i<this.rightItems.length; i++) {
            var item = this.rightItems[i];
            var itemElm = item.mode == "toggle"
                    ? this.createToggle( this.elm, item )
                    : this.createButton( this.elm, item );
            itemElm.css( "float", "right" );
            this.rightItems[i].elementCreated( itemElm );
        }
    },
  
    createButton: function( parent, /*ToolItem*/ item ) {
        parent.append( '<button id="' + item.getId() + '" title="' + item.getTooltip() + '">' );
        var button = $( '#' + item.getId() );
        button.button({ label: item.getLabel() });
        button.click( callback( item.onClick, {scope:item, suppressArgs:false} ) );
        if (item.icon != null) {
            button.css( 'display', 'inline' ).css( 'white-space', 'nowrap' ).css( 'width', '90px' );
            button.prepend( '<img src="' + item.icon + '" style="float:left; margin:5px;">' );
        }
        return button;
    },
  
    createToggle: function( parent, item ) {
        parent.append( '<input type="checkbox" id="' + item.getId() + '" title="' + item.getTooltip() + '">'
                + '<label id="' + item.getId() + "_label" + '" for="' + item.getId() + '">Toggle</label>');
        var checkbox = $( '#' + item.getId() );
        var label = $( '#' + item.getId() + "_label" );
        checkbox.button({ 
            label: item.getLabel() 
        });
        checkbox.click( callback( item.onClick, {scope:item, suppressArgs:false} ) );
        if (item.icon != null) {
            label.css( 'width', '90px' );
            label.prepend( '<img src="' + item.icon + '" style="float:left; margin:5px;">' );
        }
        return label;

//    
//    <span id="repeat">
//        <input type="radio" id="repeat0" name="repeat" checked="checked" /><label for="repeat0">No Repeat</label>
//        <input type="radio" id="repeat1" name="repeat" /><label for="repeat1">Once</label>
//        <input type="radio" id="repeatall" name="repeat" /><label for="repeatall">All</label>
//    </span>
//</span>
    }
  
});
