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
package org.polymap.geocoder.tasks.qi4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.rwt.internal.service.ContextProvider;

import org.polymap.core.operation.OperationSupport;
import org.polymap.core.qi4j.Qi4jPlugin;
import org.polymap.core.qi4j.QiModule;
import org.polymap.core.qi4j.QiModuleAssembler;
import org.polymap.geocoder.tasks.ITask;
import org.polymap.geocoder.tasks.ITaskFilter;
import org.polymap.geocoder.tasks.ITaskRepository;

/**
 * Factory and repository for the domain model artifacts.
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
@SuppressWarnings("restriction")
public class TaskRepository
        extends QiModule
        implements org.polymap.core.model.Module, ITaskRepository {

    private static Log log = LogFactory.getLog( TaskRepository.class );


    /**
     * Get or create the repository for the current user session.
     */
    public static final TaskRepository instance() {
        return Qi4jPlugin.Session.instance().module( TaskRepository.class );
    }


    // instance *******************************************

    private TaskListComposite       taskList;
    
    private OperationSaveListener   operationListener = new OperationSaveListener();
    

    protected TaskRepository( QiModuleAssembler assembler ) {
        super( assembler );
        // for the global instance of the module (Qi4jPlugin.Session.globalInstance()) there
        // is no request context
        if (ContextProvider.hasContext()) {
            OperationSupport.instance().addOperationSaveListener( operationListener );
        }

        taskList = uow.get( TaskListComposite.class, "taskList" );
    }
    
    
    protected void dispose() {
        if (operationListener != null) {
            OperationSupport.instance().removeOperationSaveListener( operationListener );
            operationListener = null;
        }
    }

    
    public List<ITask> findTasks( ITaskFilter filter )
            throws Exception {
        List<ITask> result = new ArrayList();
        for (ITask task : taskList.getTasks()) {
            if (filter.filter( task )) {
                result.add( task );
            }
        }
        return Collections.unmodifiableList( result );
    }


    public void addTask( ITask task ) { 
        taskList.addTask( task );
    }
    
    
    public void removeTask( ITask task ) { 
        taskList.removeTask( task );
    }
    
}
