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
package org.polymap.geocoder.tasks.qi4j.operations;

import org.qi4j.api.composite.TransientComposite;
import org.qi4j.api.concern.Concerns;
import org.qi4j.api.mixin.Mixins;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.polymap.core.qi4j.event.AbstractOperation;
import org.polymap.core.qi4j.event.OperationBoundsConcern;
import org.polymap.geocoder.tasks.ITask;
import org.polymap.geocoder.tasks.qi4j.TaskRepository;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
@Concerns( 
        OperationBoundsConcern.class )
@Mixins( 
        RemoveTaskOperation.Mixin.class 
)
public interface RemoveTaskOperation
        extends IUndoableOperation, TransientComposite {

    public void init( ITask task );
    
    /** Implementation is provided bei {@link AbstractOperation} */ 
    public boolean equals( Object obj );
    
    public int hashCode();

    /**
     * Implementation. 
     */
    public static abstract class Mixin
            extends AbstractOperation
            implements RemoveTaskOperation {
        
        private ITask               task;


        public Mixin() {
            super( "[undefined]" );
        }


        public void init( ITask _task ) {
            this.task = _task;
            setLabel( "Aufgabe löschen" );
        }


        public IStatus execute( IProgressMonitor monitor, IAdaptable info )
        throws ExecutionException {
            try {
                TaskRepository repo = TaskRepository.instance();
                repo.removeTask( task );
                repo.removeEntity( task );
            }
            catch (Throwable e) {
                throw new ExecutionException( e.getMessage(), e );
            }
            return Status.OK_STATUS;
        }


        public IStatus redo( IProgressMonitor monitor, IAdaptable info )
        throws ExecutionException {
            // XXX Auto-generated method stub
            throw new RuntimeException( "not yet implemented." );
        }


        public IStatus undo( IProgressMonitor monitor, IAdaptable info )
        throws ExecutionException {
            // XXX Auto-generated method stub
            throw new RuntimeException( "not yet implemented." );
        }

    }

}
