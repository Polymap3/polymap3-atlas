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
 * String standard functions
 */
String.prototype.startsWith = function( str ) {
    return this.match("^"+str) == str;
};

String.prototype.endsWith = function( str ) {
    return this.match(str+"$") == str;
};

String.prototype.trim = function( str ) {
    return this.replace( /^\s\s*/, '' ).replace( /\s\s*$/, '' );
};

String.prototype.afterLast = function( delim ) {
    var tokens = this.split( delim );
    return tokens[tokens.length-1];
};

String.prototype.beforeFirst = function( delim ) {
    var tokens = this.split( delim );
    return tokens[0];
};

/**
 * Allows to convert first char to upper case.
 */
String.prototype.capitalize = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
};

/**
 * Replace all occurences of '{x}' in the String with arguments[x].
 * 
 * @see http://stackoverflow.com/questions/610406/javascript-equivalent-to-printf-string-format/4673436#4673436
 */
String.prototype.format = function() {
    var args = arguments;
    return this.replace( /{(\d+)}/g, function( match, index ) { 
        return typeof args[index] != 'undefined' ? args[index] : match;
    });
};

function show_dialog( div, title, contentURL ) {
    $.ajax({
        url: contentURL,
        dataType: "html",
        success: function( data ) {
          div.html( data );
          div.dialog({ width:640 , height: 560 , title:title });
        }
    });
}

/**
 * 
 */
function createBookmark( title, url, comment ) {
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

/**
 * Generating a callback function bound to an instance.
 * Taken from http://webcache.googleusercontent.com/search?q=cache:boCMVl7cnyEJ:onemarco.com/2008/11/12/callbacks-and-binding-and-callback-arguments-and-references/+javascript+callback+method&cd=4&hl=de&ct=clnk&gl=de&client=firefox-a
 * <p/>
 * Examples:
 * <pre>
 *     callback( this.onMeasure, {scope:this}
 *     callback( this.onMeasure, {scope:this, suppressArgs:false}
 * </pre>
 * @param {Function}
 *            func the callback function
 * @param {Object}
 *            opts an object literal with the following properties (all
 *            optional): scope: the object to bind the function to (what
 *            the "this" keyword will refer to) args: an array of
 *            arguments to pass to the function when it is called, these
 *            will be appended after any arguments passed by the caller
 *            suppressArgs: boolean, whether to supress the arguments passed
 *            by the caller. This default is false.
 */
function callback( func, opts ) {
    var cb = function() {
        var args = opts.args ? opts.args : [];
        var scope = opts.scope ? opts.scope : this;
        var fargs = opts.supressArgs === true ? [] : toArray(arguments);
        func.apply(scope, fargs.concat(args));
    };
    return cb;
}

/** A utility function for callback() */
function toArray(arrayLike) {
    var arr = [];
    for (var i = 0; i < arrayLike.length; i++) {
        arr.push(arrayLike[i]);
    }
    return arr;
}
