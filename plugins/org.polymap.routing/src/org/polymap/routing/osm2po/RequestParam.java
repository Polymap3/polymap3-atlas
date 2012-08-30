/* 
 * polymap.org
 * Copyright 2012, Polymap GmbH. All rights reserved.
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
package org.polymap.routing.osm2po;

import javax.servlet.http.HttpServletRequest;

/**
 * Simple Servlet request param parser.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class RequestParam<T> {

    protected String        name;
    
    protected Class<T>      valueType;
    
    protected String        description;
    
    protected boolean       optional;
    
    protected T             defaultValue;

    
    public RequestParam( String name, String description, Class<T> valueType, boolean optional, T defaultValue ) {
        this.name = name;
        this.description = description;
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.valueType = valueType;
    }
    
    
    /**
     *
     * @param req
     * @return The value of this param in the given request.
     * @throws IllegalStateException If the param is missing from request and !optional.
     */
    public T get( HttpServletRequest req ) {
        String value = req.getParameter( name );
        if (value == null && !optional) {
            throw new IllegalStateException( "Parameter missing: " + name + " (" + description + ")");
        }
        else if (value == null && defaultValue != null) {
            return defaultValue;
        }
        else if (value == null) {
            return null;
        }
        // String
        else if (valueType.isAssignableFrom( String.class )) {
            return (T)value;
        }
        // Integer
        else if (valueType.isAssignableFrom( Integer.class )) {
            return (T)Integer.valueOf( value );
        }
        // Float
        else if (valueType.isAssignableFrom( Float.class )) {
            return (T)Float.valueOf( value );
        }
        // unhandled type
        throw new RuntimeException( "Unhandled param type: " + valueType );
    }
    
}
