/*
 * polymap.org
 * Copyright 2011, 2012 Falko Br�utigam. All rights reserved.
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
 * Allows to get a coordinate from the map. The coordinate is displayed
 * in a dialog. The dialog allows to re-project coordinate.
 * 
 * @requires jquery.combobox.js
 * @requires proj4js-1.1.0.xxx.js
 */
var CoordinateItem = ToolItem.extend( new function CoordinateItemProto() {
    
    this.init = function( id ) {
        this._super( id, "tb_coord_label".i18n(), "tb_coord_tip".i18n() );
        this.icon = "tb_coord_icon".i18n();
        this.mode = "toggle";
        this.url = "test";
        this.enabled = false;

        var self = this;
        this.control = new OpenLayers.Control.Click( {
            'handlerOptions': { 
                'single': true
            },
            'onClick': function( ev ) {
                self.openDialog( ev );
            }
        });
    };
    
    /**
     * This tool item was clicked.
     */
    this.onClick = function( ev ) {
        if (this.enabled) {
            this.control.deactivate();
            Atlas.map.removeControl( this.control );

            if (this.dialog) {
                this.dialog.remove();
                this.dialog = null;
            }
        }
        else {
            Atlas.map.addControl( this.control );
            this.control.activate();
        }
        this.enabled = !this.enabled;
    };
    
    /**
     * 
     */
    this.openDialog = function( ev ) {
        var self = this;

        if (this.dialog) {
            this.dialog.remove();
            this.dialog = null;
        }

        $(document.body).append( '<div id="coords-dialog"></div>' );
        var dialog = this.dialog = $('#coords-dialog');
        this.searchContext = Atlas.contexts[Atlas.result_index];

        var projOptions = '';
        if (Proj4js.defs) {
            $.each( Proj4js.defs, function( key, value ) {
                var proj = new OpenLayers.Projection( key );
                if (proj.proj.title) {
                    projOptions += '<option value="{0}">{1} ({0})</option>'.format( key, proj.proj.title );
                }
            });
        }
        else {
            projOptions += '<option value="EPSG:900913">Google-Mercator (EPSG:900913)</option>';
            projOptions += '<option value="EPSG:4326">WGS84 (EPSG:4326)</option>';
        }
        
        this.dialog.html( 
                '<p class="atlas-description" style="text-align:justify; padding:10px; margin:0;">{0}</p>'.format( 'coord_msg'.i18n() ) +
                '<div class="ui-corner-all" style="padding:10px 10px; margin-left:auto;">' +
                '    <label>{0}</label><br/>'.format( 'coord_srs_label'.i18n() ) +
                '    <select id="combobox" style="padding:0.2em; margin-right:10px; width:210px;">' + 
                         projOptions +
                '    </select>' +
                '    <button id="calc" title="{0}">{0}</button>'.format( 'coord_calc_label'.i18n() ) +
                '</div><hr/>' +
                '<table class="ui-corner-all" style="width:100%; border:0px solid #e0e0e0; padding:5px 10px;"><tr>' +
                '    <td><label>{0}</label></td>'.format( 'coord_lon_label'.i18n() ) +
                '    <td><input id="lon"></input></td>' +
                '  </tr><tr>' +
                '    <td><label>{0}</label></td>'.format( 'coord_lat_label'.i18n() ) +
                '    <td><input id="lat"></input></td>' +
                '</tr></table><hr/>' +
                '<div class="ui-corner-all" style="border:0px solid #d0d0e0; padding:0px 10px;"><tr>' +
                '    <p class="atlas-description" style="text-align:justify; margin:10px 0;">{0}</p>'.format( 'coord_link_msg'.i18n() ) +
                '    <label>{0}</label><br/>'.format( 'coord_link_title'.i18n() ) +
                '    <input id="url-title" style="width:100%; margin-bottom:10px;" />' +
                '    <label>{0}</label><br/>'.format( 'coord_link_text'.i18n() ) +
                '    <textarea id="url-text" style="width:100%; margin-bottom:20px;" />' +
                '    <label>{0}</label><br/>'.format( 'coord_link_url'.i18n() ) +
                '    <input id="url" style="width:100%; margin-bottom:10px;"></input>' +
                '    <label>{0}</label><br/>'.format( 'coord_link_html'.i18n() ) +
                '    <input id="html" style="width:100%;"></input>' +
                '</div>'
        );
        this.combo = dialog.find( '#combobox' ).blur();//.combobox();
        var comboInput = dialog.find( '.ui-combobox>input' ).blur()
                .css( 'padding', '0.5em' );
                //.css( 'width', '130px' );
        var comboBtn = dialog.find( '.ui-combobox>a' )
                .css( 'height', '1.9em' )
                .css( 'margin-left', '-1px' )
                .css( 'top', '0' );

        comboInput.val( Atlas.map.projection.getCode() );
        comboInput.keypress( function( ev ) {
            if (ev.keyCode == 13) {
                $(this).autocomplete( "close" );
                self.updateUI();
            }
        });

        dialog.find( '#calc' ).button().click( function( ev ) {
            self.updateUI();
        });
        this.dialog.find( '#url-title' )
            .val( '...' )
            .focus( function( ev ) { $(this).select(); } )
            .keyup( function( ev ) { self.deferredUpdateUI(); } )
        this.dialog.find( '#url-text' )
            .val( '...' )
            .focus( function( ev ) { $(this).select(); } )
            .keyup( function( ev ) { self.deferredUpdateUI(); } );

        this.dialog.find( '#lon' ).focus( function( ev ) { $(this).select(); } );
        this.dialog.find( '#lat' ).focus( function( ev ) { $(this).select(); } );
        this.dialog.find( '#url' ).focus( function( ev ) { $(this).select(); } );
        this.dialog.find( '#html' ).focus( function( ev ) { $(this).select(); } ); 
        
        this.coord = Atlas.map.getLonLatFromPixel( ev.xy );
        this.updateUI();

        dialog.dialog({ 
            'width':370, 
            'height': 590,
            'position': [10,10],
            'modal': false,
            'title':'Treffpunkt',
            'close': function( ev, ui ) {
                // deactivate tool
                self.elm.trigger( 'click' );
                // the above does not seem to reach the callback
                if (self.enabled) {
                    self.onClick();
                }
            }
        });
    };
    
    /**
     * 
     */
    this.deferredUpdateUI = function() {
        if (this.updateTimeout) {
            clearTimeout( this.updateTimeout );
        }
        var self = this;
        this.updateTimeout = setTimeout( function() {
            self.updateUI();
        }, 3500 );
    };
    
    /**
     * Re-project coordinates to projection given by the combobox EPSG code
     * and update UI elements. 
     */
    this.updateUI = function() {
        var val = this.combo.val();
        var proj = new OpenLayers.Projection( val );
        var transformed = this.coord.clone().transform( Atlas.map.projection, proj );
        
        this.dialog.find( '#lon' ).val( transformed.lon );
        this.dialog.find( '#lat' ).val( transformed.lat );

        var feature = new OpenLayers.Feature.Vector( 
            new OpenLayers.Geometry.Point( this.coord.lon, this.coord.lat ), 
            {
                'title': this.dialog.find( '#url-title' ).val(),
                'text': this.dialog.find( '#url-text' ).val(),
                'categories': 'Treffpunkt'
            }
        );
        var json = new OpenLayers.Format.GeoJSON().write( feature );
        //alert( json );
        var url = location.protocol + "//" + location.host + location.pathname 
                + '?search1=' + encodeURIComponent( json );
        this.dialog.find( '#url' ).val( url );
        
        this.dialog.find( '#html' ).val( 
                '<iframe width="425" height="350" frameborder="0" scrolling="no" marginheight="0" marginwidth="0" ' +
                'src="' + url + '&north=off&east=off"></iframe>' );
        
        // search context
        this.searchContext.search( json );
    };
});


/**
 * A simple click handler. 
 */
OpenLayers.Control.Click = OpenLayers.Class( OpenLayers.Control, {
    
    defaultHandlerOptions: {
        'single': true,
        'double': false,
        'pixelTolerance': 0,
        'stopSingle': false,
        'stopDouble': false
    },

    initialize: function(options) {
        this.handlerOptions = OpenLayers.Util.extend( {}, this.defaultHandlerOptions );
        
        OpenLayers.Control.prototype.initialize.apply( this, arguments );
        
        this.handler = new OpenLayers.Handler.Click( this, {
            'click': options.onClick,
            'dblclick': options.onDClick 
        }, this.handlerOptions );
    },

    CLASS_NAME: "OpenLayers.Control.Click" 
});
