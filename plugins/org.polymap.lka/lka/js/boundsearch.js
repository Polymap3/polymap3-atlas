/*
 * polymap.org
 * Copyright 2011-2012, Polymap GmbH. All rights reserved.
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
 * 
 * <pre>
 * {
 *     <entry name>: '<Atlas URL>'|<JS file to load and execute>,
 *     <entry name>: '<Atlas URL>'|<JS file to load and execute>,
 *     <branch name>: {...}
 * }
 * </pre>
 */
var BoundSearch = Class.extend( new function BoundSearchProto() {
    
    this.config = null;
    
    /**
     * @param config {Object} ...
     */
    this.init = function( config ) {
        this.config = config;
        $('#search_pane').append( 
                '<a id="bound-search-link" class="ui-widget" href="#" title="{1}">{0}</a>'
                .format( 'bound_search'.i18n(), 'bound_search_tip'.i18n() ) );
        
        var self = this;
        $('#bound-search-link').click( function( ev ) {
            self.createPopup( ev );
        });
    };

    this.close = function() {
        if (this.popup) {
            var self = this;
            this.popup.fadeOut( 1000, function() {
                $(this).remove();
                self.popup = null;

                $(document).unbind( 'keypress', this.keyHandler ); 
                $(document).unbind( 'click', this.clickHandler ); 
            });
        }
    };
    
    /**
     * 
     */
    this.createPopup = function( ev ) {
        var self = this;
        $(document.body).append( 
                '<div id="bound-search-popup" class="ui-widget-content ui-corner-all"' 
                + '   style="z-index:1000; position:absolute; color:#707070;'
                + '   left:300px; top:75px; padding:3px 0px; box-shadow:2px 3px 3px #808080;">'
                //+ '<a href="#" class="close" title="Schliessen" style="float:right;"></a>'
                //+ '<b>' + 'bound_search_heading'.i18n() + '</b><br/>'
                //+ '<hr/>'
                + '<div id="bound-search-content" style="overflow:auto;'
                + '    width:300px; max-height:450px;">'
                + '</div>'
                + '</div>' );
        
        this.popup = $('#bound-search-popup');
        this.popup.find( '>a' ).click( function( ev ) {
            self.close();
        });
        
        // mouseleave
        this.popup.mouseleave( function( ev ) {
            self.close();
        });

        // keyHandler
        $(document).keypress( this.keyHandler = function( ev ) { 
            if (ev.keyCode == 27) {
                self.close();
            }
        });
        
        // clickHandler
        this.clickCount = 0;
        $(document).click( this.clickHandler = function( ev ) {
            if (++self.clickCount == 1) {
                return false;
            }
            var popupClicked = false;
            $(ev.target).parents().each( function( index, parent ) {
                if (parent == self.popup) {
                    popupClicked = true;
                    return false;
                }
            });
            if (!popupClicked) {
                self.close();
            }
        });
 
        this.createEntries( $('#bound-search-content'), this.config, 1 );
    };

    this.pCount = 0;
    
    /**
     * 
     */
    this.createEntries = function( parent, folder, sibling ) {
        var self = this;

        parent.append( '<p id="bound-search-tree-{0}" style="padding:0px 10px; margin:2px"></p>'.format( ++this.pCount ) );
        var p = parent.find( '#bound-search-tree-{0}'.format( this.pCount ) );
        
        var count = 1;
        $.each( folder, function( key, value ) {
            if (typeof value == 'string') {
                p.append( '> <a href="{1}">{0}</a><br/>'.format( key, value ) );
            }
            else {
                p.append( '<b>{0}</b><br/>'.format( key ) );
                self.createEntries( p, value, count++ );
            }
        });
    };
    
});
