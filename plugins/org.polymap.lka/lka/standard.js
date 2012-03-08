/*
 * polymap.org
 * Copyright 2011-2012, Falko Br�utigam. All rights reserved.
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
    //  click -> popup
    var self = this;
    div.find( 'b>a' ).click( function( ev ) {
        var popupHtml = '<b>' + feature.data.title + '</b><br/>'
                + '_____________________________________'
                + self.createHtml();
        context.openPopup( feature.id, popupHtml );
    });

    /**
     * 
     */
    this.createHtml = function() {
        var resultHtml = '';

        // address
        if (feature.data.address != null) {
            var address = new OpenLayers.Format.JSON().read( feature.data.address );

            if (address != null && address.street != null) {
                resultHtml += ('<p class="atlas-result-address">{0} {1}<br/>{2} {3}</p>'
                        .format( address.street, address.number, address.postalCode, address.city ));
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

    div.append( this.createHtml() );
})