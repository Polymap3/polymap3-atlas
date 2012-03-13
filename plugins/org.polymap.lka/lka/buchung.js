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
 * BuWA: Angebot mit Buchung.
 *
 * @param this {SearchContext} The context this is called in.
 * @param context {SearchContext}
 * @param feature {OpenLayers.Feature.Vector}
 * @param index {Number} The index of the feature in the result collection.
 */
(function( context, feature, index, div ) {
    var self = this;    

    /**
     * Creates the result list HTML.
     */
    this.createHtml = function( detailsLinkId ) {
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
        resultHtml += '<b>Angebot: </b><span style="display:block; margin:3px 0px 3px 15px;">' + feature.data.Angebot + '';
        resultHtml += ' >>&nbsp;<a id="'+detailsLinkId+'" style="font-weight:bold;" href="#">Details...</a>';
        resultHtml += '</span></p>';
        return resultHtml;
    };    

    // append the HTML
    div.append( this.createHtml( 'feature-details-list-'+index ) );
    div.find( '#feature-details-list-'+index ).click( function( ev ) {
        self.openDetails();
    });

    // popup
    var popupHtml = '<b>' + feature.data.title + '</b><br/>'
            + '_____________________________________'
            + self.createHtml( 'feature-details-popup-'+index );
    
    // result list click -> popup
    div.find( 'b>a' )
        .attr( 'title', 'In der Karte anzeigen' )
        .click( function( ev ) {
            context.openPopup( feature.id, popupHtml );
            $('#feature-details-popup-'+index).click( function( ev ) {
                self.openDetails();
            });
        });

    // feature click -> popup
    context.layer.events.register( "featureselected", context.layer, function( ev ) {
        if (ev.feature == feature) {
            context.openPopup( feature.id, popupHtml );
            $('#feature-details-popup-'+index).click( function( ev ) {
                self.openDetails();
            });
            context.resultDiv.scrollTo( div, 2000 );
        }
    });

    /**
     * 
     */
    this.openDetails = function() {
        var html = '<div id="feature-details-dialog" title="{0}">'.format( feature.data.title );
        
        html += '<table><tr>';
        html += ('<td width="60%" valign="top" style="padding:10px; background:#eaeaea;">'
                + self.createLine( 'Angebot', feature.data.Angebot )
                + self.createLine( 'Anbieter', feature.data.Anbieter )
                + self.createLine( 'Kontakt', feature.data.Ansprech )
                + self.createLine( 'geeignet als', feature.data.Eignung )
                + self.createLine( 'Altersstufe', feature.data.Altereig )
                + self.createLine( 'Lehrplan', feature.data.Lehrplan )
                + self.createLine( 'Anzahl Teilnehmer', feature.data.Perszahl )
                + self.createLine( 'Preis', feature.data.Preis )
                + '</td>');

        html += '<td width="20"></td>';
        
        var address = {
                street: feature.data.StrBu, number: feature.data.HBrBu,
                postalCode: feature.data.PLZBu, city: feature.data.OrtBU
        };
        html += ('<td width="40%" valign="top">'
                + '<img height="200" style="width:100%; float:right; padding-left:20px; padding-bottom:20px;" src="images/bergbau.jpg"></img><br/><br/><br/>'
                + self.createLine( 'Buchung bei', feature.data.BezBU )
                + ('<p class="atlas-result-address">{0} {1}<br/>{2} {3}<br/>' +
                   '<a id="address-link" href="#" title="Ort in der Karte anzeigen" style="font-size:smaller;">Anzeigen...</a></p>')
                        .format( address.street, address.number, address.postalCode, address.city )
                + self.createLine( 'Kontakt', 
                        feature.data.AnspBu + '<br/>' + 
                        'Tel: ' + feature.data.TelBU + '<br/>' + 
                        'EMail: ' + feature.data.MailBu + '<br/>' + 
                        'Web: ' + feature.data.URLBu )
                + '</td>');
        html += '</tr></table>';
            
//        $.each( feature.data, function( name, value ) {
//            if (name != "title" && name != "address" && name != "categories") {
//                html += ('<b>{0}</b>: {1}<br/>'.format( name.capitalize(), value ));
//            }
//        });
        html += '</div>';
        $(document.body).append( html );

        var dialogDiv = $('#feature-details-dialog')
        
        dialogDiv.find('#address-link').click( function( ev ) {
            alert( 'Die Adresse wird als eigene Suchkategorie angezeigt.\n\n'
                    + 'Die aktuelle Suche bleibt aber erhalten. Zwischen den\n'
                    + 'verschiedene Ergebnislisten kann jederzeit mittels der\n'
                    + 'Reiter oberhalb umgeschaltet werden.' );
            
            dialogDiv.dialog( 'close' );
            dialogDiv.remove();

            var searchStr = 'strasse:"' + address.street.replace( /straße/gi, '').toLowerCase() + '"'
                    + ' AND nummer:' + address.number.replace( /[^0-9]+/gi, '')
                    + ' AND ort:' + address.city.toLowerCase() + '*';
            var tabIndex = Atlas.result_index + 1;
            $('#tabs').tabs( 'select', tabIndex );
            Atlas.contexts[tabIndex].search( searchStr );        
        });
        
        dialogDiv.dialog( {
            modal: true,
            //show: 'scale',
            minWidth: 650,
            close: function( ev, ui ) {
                dialogDiv.remove();
            }
        });
    };

    /**
     * 
     */
    this.createLine = function( key, value ) {
        return '<b>{0}</b>:<span style="display:block; margin:3px 0px 3px 15px;">{1}</span>'
                .format( key, value != null ? value : '-' );
    };
    
})