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
 * Provides the API of the extendable search result priority of
 * {@link SearchContext}.
 * 
 * @requires js/inheritance.js
 * @author falko
 */
SearchPriority = Class.extend( new function SearchPriorityProto() {
    
    this.init = function() {
    };
    
    this.compare = function( feature1, feature2 ) {
        alert( 'Implement the compare() method!' );
    };
    
});


/**
 * Ordering via visibility (on/off screen) of the feature.
 */
OnScreenPriority = SearchPriority.extend( new function OnScreenPriorityProto() {
    
    this.init = function() {
        this._super();
    };
    
    this.compare = function( feature1, feature2 ) {
        // no geometry -> offScreen
        if (!feature1.geometry) {
            return -1;
        }
        if (!feature2.geometry) {
            return 1;
        }
        var screenBounds = Atlas.map.getExtent();
        var onScreen1 = screenBounds.intersectsBounds( feature1.geometry.getBounds() );
        var onScreen2 = screenBounds.intersectsBounds( feature2.geometry.getBounds() );
        if (onScreen1 == onScreen2) {
            return 0;
        } else {
            return onScreen1 ? -1 : 1
        }
    };
    
});


/**
 * Lexical ordering using the feature title. 
 */
LexicalPriority = SearchPriority.extend( new function LexicalPriorityProto() {
    
    this.init = function() {
        this._super();
    };
    
    this.compare = function( feature1, feature2 ) {
        if (feature1.data.title < feature2.data.title) {
            return -1;
        } else if (feature1.data.title > feature2.data.title) {
            return 1;
        }
        return 0;
    };
    
});

/**
 * Lexical ordering using the feature title. 
 */
ExclusiveCategoriesPriority = SearchPriority.extend( new function ExclusiveCategoriesPriorityProto() {
    
    this.categories;
    
    /**
     * @param categories Array of Strings representing the priority order;
     */
    this.init = function( categories ) {
        this._super();
        this.categories = categories;
        
        // listen to 'searchPreparing' -> tweak URL
        var self = this;
        Atlas.events.bind( 'searchPreparing', function( ev ) {
            var clause = '';
            for (var i=0; i<self.categories.length; i++) {
                clause += clause.length != 0 ? ' OR ' : '';
                clause += 'categories:' + self.categories[i];
            }
            ev.searchStr = ev.searchStr + ' AND (' + clause + ')';
            //alert( ev.searchStr );
        });

    };
    
    this.compare = function( feature1, feature2 ) {
        // no ordering here
        return 0;
    };
    
});

/**
 * Lexical ordering using the feature title. 
 */
CategoriesPriority = SearchPriority.extend( new function CategoriesPriorityProto() {
    
    this.categories;
    
    /**
     * @param categories Array of Strings representing the priority order;
     *      high index means low priority.
     */
    this.init = function( categories ) {
        this._super();
    };
    
    this.compare = function( feature1, feature2 ) {
        if (feature1.data.title < feature2.data.title) {
            return -1;
        } else if (feature1.data.title > feature2.data.title) {
            return 1;
        }
        return 0;
    };
    
});
