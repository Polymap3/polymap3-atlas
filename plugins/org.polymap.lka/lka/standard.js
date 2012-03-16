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
 * The standard feature result renderer.
 *
 * @param this {SearchContext} The context this is called in.
 * @param context {SearchContext}
 * @param feature {OpenLayers.Feature.Vector}
 * @param index {Number} The index of the feature in the result collection.
 */
(function( context, feature, index, div ) {
    var self = this;

    /**
     * Creates the HTML for result list entry and popup.
     */
    this.createHtml = function() {
        var resultHtml = '';

        // address
        if (feature.data.address != null) {
            var address = new OpenLayers.Format.JSON().read( feature.data.address );

            if (address != null && address.street != null) {
                resultHtml += '<p class="atlas-result-address">{0} {1}<br/>{2} {3}</p>'
                        .format( address.street, address.number, address.postalCode, address.city );
            }
        }

        // fields
        resultHtml += '<p id="feature-field-' + feature.id + '" class="atlas-result-fields">';
        $.each( feature.data, function( name, value ) {
            if (name != "title" && name != "address" && name != "categories") {
                resultHtml += ('<b>{0}</b>: {1}<br/>'.format( name.capitalize(), value ));
            }
        });
        resultHtml += '</p>';
        return resultHtml;
    };    

    /**
     * 
     */
    this.createPopupHtml = function() {
        return '<b>' + feature.data.title + '</b><br/>'
                + '_____________________________________'
                + self.createHtml();
    };
    
    // result list entry
    div.append( this.createHtml() );
    
    // result list click -> popup
    div.find( 'b>a' )
        .attr( 'title', 'result_list_entry_link'.i18n() )
        .click( function( ev ) {
            context.openPopup( feature.id, self.createPopupHtml() ); 
        });
    
    // feature click -> popup
    context.layer.events.register( "featureselected", context.layer, function( ev ) {
        if (ev.feature == feature) {
            context.openPopup( feature.id, self.createPopupHtml() );
            context.resultDiv.scrollTo( div, 2000 );
        }
    });

})