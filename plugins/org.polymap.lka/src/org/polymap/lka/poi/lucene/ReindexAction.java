/* 
 * polymap.org
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.lka.poi.lucene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.polymap.core.project.ILayer;
import org.polymap.core.project.IMap;
import org.polymap.core.project.ui.util.SelectionAdapter;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class ReindexAction
        implements IObjectActionDelegate {

    private static Log log = LogFactory.getLog( ReindexAction.class );


    @Override
    public void run( IAction action ) {
        PoiIndexer.instance.reindex();
    }


    @Override
    public void selectionChanged( IAction action, ISelection sel ) {
        SelectionAdapter selected = SelectionAdapter.on( sel );
        if (PoiIndexer.instance != null) {
            ILayer layer = selected.first( ILayer.class );
            IMap map = layer != null ? layer.getMap() : selected.first( IMap.class );
            action.setEnabled( map != null && map.getLabel().equalsIgnoreCase( "atlas" ) );
        }
    }


    @Override
    public void setActivePart( IAction action, IWorkbenchPart targetPart ) {
    }

}
