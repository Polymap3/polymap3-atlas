/*
 * polymap.org
 * Copyright 2009, Polymap GmbH, and individual contributors as indicated
 * by the @authors tag.
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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * $Id$
 */

var myLayout;

function init_layout() {

    // this layout could be created with NO OPTIONS - but showing some here just as a sample...
    // myLayout = $('body').layout(); -- syntax with No Options

    myLayout = $('body').layout({

        //  enable showOverflow on west-pane so popups will overlap north pane
        west__showOverflowOnHover: true

        //  reference only - these options are NOT required because are already the 'default'
        ,   closable:               true    // pane can open & close
        ,   resizable:              true    // when open, pane can be resized 
        ,   slidable:               true    // when closed, pane can 'slide' open over other panes - closes on mouse-out
        ,   spacing_open:           3

        //  some resizing/toggling settings
        ,   north__slidable:        false   // OVERRIDE the pane-default of 'slidable=true'
        ,   north__togglerLength_closed: '100%' // toggle-button is full-width of resizer-bar
        ,   north__spacing_closed:  20      // big resizer-bar when open (zero height)
        ,   north__resizable:       false   // OVERRIDE the pane-default of 'resizable=true'
        ,   south__spacing_open:    0       // no resizer-bar when open (zero height)
        ,   south__spacing_closed:  20      // big resizer-bar when open (zero height)
        //  some pane-size settings
        ,   north__minSize:         160
        ,   west__minSize:          100
        ,   east__size:             300
        ,   east__minSize:          200
        ,   east__maxSize:          Math.floor(screen.availWidth / 2) // 1/2 screen width
    });

//    // add event to the 'Close' button in the East pane dynamically...
//    myLayout.addCloseBtn('#btnCloseEast', 'east');
//
//    // add event to the 'Toggle South' buttons in Center AND South panes dynamically...
//    myLayout.addToggleBtn('.south-toggler', 'south');
//
//    // add MULTIPLE events to the 'Open All Panes' button in the Center pane dynamically...
//    myLayout.addOpenBtn('#openAllPanes', 'north');
//    myLayout.addOpenBtn('#openAllPanes', 'south');
//    myLayout.addOpenBtn('#openAllPanes', 'west');
//    myLayout.addOpenBtn('#openAllPanes', 'east');

};
