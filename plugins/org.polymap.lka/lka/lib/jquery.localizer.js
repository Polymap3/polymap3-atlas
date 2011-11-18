/*
jQuery Localizer Plugin

Copyright (c) 2011 Sagi Mann. All rights reserved.
Copyright 2011 Falko Bräutigam. All rights reserved.

Redistribution and use in source and binary forms are permitted
provided that the above copyright notice and this paragraph are
duplicated in all such forms and that any documentation,
advertising materials, and other materials related to such
distribution and use acknowledge that the software was developed
by the <organization>.  The name of the
University may not be used to endorse or promote products derived
from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.

-> http://plugins.jquery.com/project/localizer
*/

jQuery.fn.localize = function(defaultStrings) {
	
    var defaults = defaultStrings;
	
	this.find("*").contents().each(function() {
	    if (typeof this.data == 'string') {
	        var s = jQuery.trim(this.data);
	        if (typeof s == 'string' && s.length > 0) {
	            var s2 = localizedString(s, defaults);
	            if (typeof s2 == 'string') {
	                this.data = s2;
	            }
	        }
	    }

	    if (this.nodeName == "IMG") {
	        // use the nodeValue instead of this.src because this.src is resolved
	        // to full path instead of the original value in the html, so it can't be
	        // known at coding time.
	        var s2 = localizedString(this.attributes['src'].nodeValue, defaults);
	        if (typeof s2 == 'string') {
	            this.attributes['src'].nodeValue = s2;
	        }
	    }

	    if (this.nodeName == "A") {
	        // use the nodeValue instead of this.href because this.href is resolved
	        // to full path instead of the original value in the html, so it can't be
	        // known at coding time.
	        var s2 = localizedString(this.attributes['href'].nodeValue, defaults);
	        if (typeof s2 == 'string') {
	            this.href = s2;
	        }
	    }
	    
	    // "title" and "alt" attribute in all nodes
	    if (this.attributes != null) {
	        var elm = this;
	        $.each(['title', 'alt', 'value'], function(index, attrName) {
	            var attr = elm.attributes[attrName];
	            if (attr != null) {
	                var s2 = localizedString(attr.nodeValue, defaults);
	                if (typeof s2 == 'string') {
	                    elm.attributes[attrName].nodeValue = s2;
	                }
	            }
	        });
	    }
	    return this;
	});
	
};


function localizedString( s, defaultStrings ) {
    // XXX check defaultsStrings
//    var s2 = defaultStrings[ s ];
//    if (typeof s2 == 'string') {
//        return s2;
//    }
    return jQuery.i18n.prop( s );
};
