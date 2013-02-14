/* 
 * polymap.org
 * Copyright 2009-2013, Falko Bräutigam. All rights reserved.
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
package org.polymap.geocoder.tasks.qi4j.operations;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.polymap.core.qi4j.event.AbstractModelChangeOperation;
import org.polymap.geocoder.tasks.ITask;
import org.polymap.geocoder.tasks.qi4j.TaskRepository;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @since 3.0
 */
public class RemoveTaskOperation
        extends AbstractModelChangeOperation {

    private ITask task;


    public RemoveTaskOperation() {
        super( "[undefined]" );
    }


    public void init( ITask _task ) {
        this.task = _task;
        setLabel( "Aufgabe löschen" );
    }


    public IStatus doExecute( IProgressMonitor monitor, IAdaptable info )
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

}
