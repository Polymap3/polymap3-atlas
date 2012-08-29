/*
 * polymap.org
 * Copyright 2011, Falko Bräutigam, and individual contributors as
 * indicated by the @authors tag. All rights reserved.
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
 * The tool item to be used in a toolbar to toggle a MeasureMode.
 */
var ToggleMeasureItem = ToolItem.extend( {
    
    init: function( id, measure ){
        this._super( id, $.i18n.prop( "tb_measure_label" ), $.i18n.prop( "tb_measure_tip" ) );
        this.icon = $.i18n.prop( "tb_measure_icon" );
        this.mode = "toggle";
        this.measure = measure;
        this.enabled = false;
    },
    
    onClick: function() {
        if (this.enabled) {
            this.measure.deactivate();
            this.enabled = false;
        }
        else {
            this.measure.activate();
            this.enabled = true;
        }
    }
});


/**
 * This controls a map and a GUI control to provide a measure mode.
 * <p>
 * needs: callback.js
 */
function MeasureMode( map, elm ) {
    
    this.elm = elm;
    
    this.map = map;
    
    this.active = false;
    
    this.control = null;
    
    this.activate = function() {
        this.map.addControl( this.control );
        this.control.activate();
        this.active = true;
    };
    
    this.deactivate = function() {
        this.control.deactivate();
        this.map.removeControl( this.control );
        this.active = false;
    };

    this.onMeasure = function( event ) {
        //var measure = event.measure;
        var geometry = event.geometry;
        var units = event.units;
        var order = event.order;
        var measure = (units == 'km')
                ? event.measure.toFixed(2) + " km"
                : event.measure.toFixed(0) + " m";

        // because of the transformation units no longer matches the outcome 
        // of getArea and getLength, those are in layer units.
//        if (order == 1) {
//            measure = (units == 'km')
//                ? (geom2.getLength()/1000).toFixed(2) + " km"
//                : geom2.getLength().toFixed(2) + " m";
//        } 
//        else {
//            if (units=='km'){
//                out += "Area: " + (geometry.getArea()/1000000).toFixed(0) + " km<sup>2</sup>";
//            } else {
//                out += "Area: " + geometry.getArea().toFixed(0) + " m<sup>2</sup>";
//            }
//        }
        
        $('#dialog').html( "Gesamtlänge: <b>" + measure + "</b>" );
        $('#dialog').dialog({ 
            'width': 250, 
            'height': 100, 
            'title': 'Ergebnis'
//            'close': function( ev, ui ) {
//                // remove everything, including this handler
//                //self.dialog.remove();
//                // deactivate tool
//                self.elm.trigger( 'click' );
//            }
        });

//      var anchor = {
//      size: new OpenLayers.Size(10,10), 
//      offset: new OpenLayers.Pixel(10,10)
//  };
//  map.addPopup(
//          new OpenLayers.Popup.AnchoredBubble(
//              "Ergebnis", 
//              geometry.getCentroid(),  //map.getLonLatFromPixel( event.xy ),
//              new OpenLayers.Size( 300, 300 ),
//              measure.toFixed(3) + " " + units,
//              close_icon,
//              true
//         ));
  
    };

    /**
     * init
     */
    // style the sketch fancy
    var sketchSymbolizers = {
        "Point": {
            pointRadius: 8,
            graphicName: "circle",
            fillColor: "white",
            fillOpacity: .1,
            strokeWidth: 1,
            strokeOpacity: 1,
            strokeColor: "#f55"
        },
        "Line": {
            strokeWidth: 3,
            strokeOpacity: 1,
            strokeColor: "#f55",
            strokeDashstyle: "dash"
        }
    };
    var style = new OpenLayers.Style();
    style.addRules([
        new OpenLayers.Rule({symbolizer: sketchSymbolizers})
    ]);
    var styleMap = new OpenLayers.StyleMap({"default": style});
    this.control = new OpenLayers.Control.Measure(
            OpenLayers.Handler.Path, {
                persist: true,
                geodesic: true,
                handlerOptions: {
                    layerOptions: {styleMap: styleMap}
                }
            });
    this.control.events.on({
        "measure": callback( this.onMeasure, {scope:this, suppressArgs:false} )
        //"measurepartial": callback( this.onMeasure, {scope:this} )
    });
};


