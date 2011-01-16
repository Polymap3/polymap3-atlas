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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.qi4j.api.structure.Application;
import org.qi4j.api.structure.Module;
import org.qi4j.api.unitofwork.NoSuchEntityException;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkFactory;
import org.qi4j.bootstrap.ApplicationAssembly;
import org.qi4j.bootstrap.LayerAssembly;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.spi.uuid.UuidIdentityGeneratorService;

import org.polymap.core.qi4j.QiModule;
import org.polymap.core.qi4j.QiModuleAssembler;
import org.polymap.core.qi4j.entitystore.json.JsonEntityStoreInfo;
import org.polymap.core.qi4j.entitystore.json.JsonEntityStoreService;
import org.polymap.core.runtime.Polymap;
import org.polymap.geocoder.tasks.ITask;
import org.polymap.geocoder.tasks.qi4j.operations.NewTaskOperation;
import org.polymap.geocoder.tasks.qi4j.operations.RemoveTaskOperation;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class TaskRepositoryAssembler
        extends QiModuleAssembler {

    private static Log log = LogFactory.getLog( TaskRepositoryAssembler.class );

    private Application                 app;
    
    private UnitOfWorkFactory           uowf;
    
    private Module                      module;
    
    
    public QiModule newModule() {
        return new TaskRepository( this );
    }


    protected void setApp( Application app ) {
        this.app = app;
        this.module = app.findModule( "adhoc-layer", "tasks-module" );
        this.uowf = module.unitOfWorkFactory();
    }


    public Module getModule() {
        return module;
    }


    public void assemble( ApplicationAssembly _app )
    throws Exception {
        log.info( "Assembling: org.polymap.core.services ..." );
        
        LayerAssembly domainLayer = _app.layerAssembly( "adhoc-layer" );
        ModuleAssembly domainModule = domainLayer.moduleAssembly( "tasks-module" );
        domainModule.addEntities( 
                TaskListComposite.class,
                TaskComposite.class
        );
        domainModule.addTransients( 
                NewTaskOperation.class,
                RemoveTaskOperation.class
        );

//        // persistence
//        Preferences prefRoot = Preferences.userRoot().node( "org/polymap/geocoder/tasks" );
//        domainModule.addServices( PreferencesEntityStoreService.class )
//                .setMetaInfo( new PreferencesEntityStoreInfo( prefRoot ) )
//                .instantiateOnStartup()
//                ;  //.identifiedBy( "rdf-repository" );

        // persistence: workspace/JSON
        File root = new File( Polymap.getWorkspacePath().toFile(), "data" );
        root.mkdir();
        
        File moduleRoot = new File( root, "org.polymap.geocoder.tasks" );
        moduleRoot.mkdir();

        domainModule.addServices( JsonEntityStoreService.class )
                .setMetaInfo( new JsonEntityStoreInfo( moduleRoot ) )
                .instantiateOnStartup()
                ;  //.identifiedBy( "rdf-repository" );
        
        domainModule.addServices( UuidIdentityGeneratorService.class );

        // indexer
//        RdfNativeSesameStoreAssembler rdf = new RdfNativeSesameStoreAssembler();
//        rdf.assemble( domainModule );
    }                

    
    public void createInitData() 
    throws Exception {
        
        // check/init rootMap
        UnitOfWork start_uow = uowf.newUnitOfWork();
        try {
            TaskListComposite taskList = start_uow.get( TaskListComposite.class, "taskList" );
            System.out.println( "TaskList: " + taskList );
            for (ITask task : taskList.getTasks()) {
                System.out.println( "   child: " + task.toString() );
            }
            if (taskList == null) {
                throw new NoSuchEntityException( null );
            }
        }
        catch (Throwable e) {
            try {
                log.info( "No config or error, creating global config. (" + e + ")" );
//                EntityBuilder<IMap> builder = start_uow.newEntityBuilder( IMap.class, "root" );
//                //builder.instance().setLabel( "root" );
//                IMap rootMap = builder.newInstance();
//                rootMap.setLabel( "root" );

                TaskListComposite taskList = start_uow.newEntity( TaskListComposite.class, "taskList" );
                System.out.println( "taskList: " + taskList );
            }
            catch (Exception e1) {
                log.error( e1.getMessage(), e1 );
                throw e1;
            }
        }
        finally {
            start_uow.complete();
            start_uow = null;
        }
    }
    
}
