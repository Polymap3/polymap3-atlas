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

/***
 * A global event manger used to dispatch events between the components
 * of the UI.
 * <pre>
 *   // Create a new jQuery.Event object with specified event properties.
 *   var e = jQuery.Event("keydown", { keyCode: 64 });
 *
 *   // trigger an artificial keydown event with keyCode 64
 *   jQuery("body").trigger( e );
 * </pre>
 * 
 * -> http://stackoverflow.com/questions/2967332/jquery-plugin-for-event-driven-architecture
 * -> http://api.jquery.com/category/events/event-object/
 */
var EventManager = {
    
    /** */
    subscribe: function( eventType, fn ) {
        $(this).bind( eventType, fn );
    },
    
    unsubscribe: function( eventType, fn ) {
        $(this).unbind( eventType, fn );
    },
    
    fire: function( event ) {
        $(this).trigger( event );
    }
};
