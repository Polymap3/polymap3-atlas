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

package org.polymap.geocoder.tasks;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.polymap.core.project.ui.DefaultPartListener;
import org.polymap.core.workbench.PolymapWorkbench;
import org.polymap.lka.LKAPlugin;


/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class ShowTasksViewAction
        extends DefaultPartListener
        implements IWorkbenchWindowActionDelegate {

    private IAction             action;
    
    private IWorkbenchPage      page;

    
    public void init( IWorkbenchWindow window ) {
        page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        page.addPartListener( this );
    }


    public void dispose() {
    }


    public void run( IAction _action ) {
        this.action = _action;

        try {
            if (action.isChecked()) {
                TasksView view = (TasksView)page.showView( TasksView.ID );
                view.dispose();
            }
            else {
                TasksView view = (TasksView)page.findView( TasksView.ID );
                page.hideView( view );
            }
        }
        catch (PartInitException e) {
            PolymapWorkbench.handleError( LKAPlugin.PLUGIN_ID, this, e.getLocalizedMessage(), e );
        }
    }


    public void selectionChanged( IAction _action, ISelection _selection ) {
        this.action = _action;
    }

    
    // DefaultPartListener ********************************

    public void partClosed( IWorkbenchPart part ) {
        if (part.getSite().getId().equals( TasksView.ID )) {
            action.setChecked( false );
        }
    }

    
    public void partOpened( IWorkbenchPart part ) {
        if (part.getSite().getId().equals( TasksView.ID )) {
            action.setChecked( true );
        }
    }

}
