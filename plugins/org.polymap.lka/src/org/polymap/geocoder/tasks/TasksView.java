package org.polymap.geocoder.tasks;

import java.util.List;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.core.commands.ExecutionException;

import org.polymap.core.model.event.ModelChangeEvent;
import org.polymap.core.model.event.IModelChangeListener;
import org.polymap.core.operation.OperationSupport;
import org.polymap.core.runtime.Polymap;
import org.polymap.core.workbench.PolymapWorkbench;
import org.polymap.geocoder.tasks.qi4j.TaskRepository;
import org.polymap.geocoder.tasks.qi4j.operations.NewTaskOperation;
import org.polymap.geocoder.tasks.qi4j.operations.RemoveTaskOperation;
import org.polymap.lka.LKAPlugin;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class TasksView
        extends ViewPart {

    private static Log log = LogFactory.getLog( TasksView.class );

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "org.polymap.geocoder.tasks.TasksView";

    private TableViewer         viewer;
    
    private Text                detailsText;

    private Action              addAction, removeAction;

    private Action              doubleClickAction;


    /**
     * 
     */
    static class ViewContentProvider
            implements IStructuredContentProvider, IModelChangeListener {

        private TableViewer         viewer;
        
        public ViewContentProvider() {
            TaskRepository.instance().addEntityListener( this );
        }
        
        public void inputChanged( Viewer v, Object oldInput, Object newInput ) {
            this.viewer = (TableViewer)v;
        }

        public void dispose() {
            TaskRepository.instance().removeEntityListener( this );
        }

        public Object[] getElements( Object parent ) {
            try {
                List<ITask> tasks = TaskRepository.instance().findTasks( ITaskFilter.ALL );
                return tasks.toArray( new ITask[tasks.size()]);
            }
            catch (Exception e) {
                PolymapWorkbench.handleError( LKAPlugin.PLUGIN_ID, this, e.getLocalizedMessage(), e );
                return new Object[] {};
            }
        }

        public void modelChanged( ModelChangeEvent ev ) {
            if (viewer != null) {
                Polymap.getSessionDisplay().asyncExec( new Runnable() {
                    public void run() {
                        viewer.getTable().pack();
                        viewer.refresh();
                    }
                });
            }
        }
        
    }


    /**
     * 
     */
    static class ViewLabelProvider
            extends LabelProvider
            implements ITableLabelProvider {
        
        private static Image        taskImg;
        
        
        public ViewLabelProvider() {
            if (taskImg == null) {
                URL url = BundleUtility.find( LKAPlugin.PLUGIN_ID, "icons/task.gif" );
                assert (url != null) : "No image found.";
                taskImg = ImageDescriptor.createFromURL( url ).createImage();
            }
        }
        
        public String getColumnText( Object obj, int index ) {
            log.debug( "getColumnText(): index=" + index );
            if (index == 0) {
                return ((ITask)obj).getTitle();
            }
            else if (index == 1) {
                return ((ITask)obj).getDescription();
            }
            return getText( obj );
        }

        public Image getColumnImage( Object obj, int index ) {
            log.debug( "getColumnImage(): index=" + index );
            if (index == 0) {
                return taskImg;
            }
            return null;
        }
    }


    /**
     * 
     */
    class NameSorter
            extends ViewerSorter {
    }


    /**
     * The constructor.
     */
    public TasksView() {
    }


    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    public void createPartControl( Composite parent ) {
        viewer = new TableViewer( parent, /*SWT.MULTI |*/ SWT.H_SCROLL | SWT.V_SCROLL );
        
        // setup the table  columns
//        String column1 = "Aufgabe", column2 = "Beschreibung";
//        TableColumn tableColumn1 = new TableColumn( viewer.getTable(), SWT.NONE);
//        tableColumn1.setText( column1 );
//        TableColumn tableColumn2 = new TableColumn( viewer.getTable(), SWT.NONE);
//        tableColumn2.setText( column2 );
//        tableColumn1.pack();
//        tableColumn2.pack();
//        viewer.setColumnProperties( new String[] { column1, column2 } );
        viewer.getTable().setHeaderVisible( true );
//        viewer.getTable().pack();

        viewer.setContentProvider( new ViewContentProvider() );
        viewer.setLabelProvider( new ViewLabelProvider() );
        viewer.setSorter( new NameSorter() );
        viewer.setInput( getViewSite() );

        viewer.addSelectionChangedListener( new ISelectionChangedListener() {
            public void selectionChanged( SelectionChangedEvent ev ) {
                ITask selectedTask = selectedTask();
                detailsText.setText( selectedTask != null
                     ? selectedTask.getDescription() : "" );
            }
        });
        // Create the help context id for the viewer's control
        PlatformUI.getWorkbench().getHelpSystem().setHelp( 
                viewer.getControl(), "org.polymap.lka.viewer" );

        detailsText = new Text( parent, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER | SWT.WRAP );

        // layout
        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = 3;
        formLayout.marginHeight = 3;
        parent.setLayout( formLayout );
        
        FormData viewerData = new FormData();
        viewerData.top = new FormAttachment( 0, 0 );
        viewerData.bottom = new FormAttachment( 50, 0);
        viewerData.left = new FormAttachment( 0, 0);
        viewerData.right = new FormAttachment( 100, 0);
        viewer.getControl().setLayoutData( viewerData );
        
        FormData textData = new FormData();
        textData.top = new FormAttachment( 50, 5 );
        textData.bottom = new FormAttachment( 100, 0);
        textData.left = new FormAttachment( 0, 0);
        textData.right = new FormAttachment( 100, 0);
        detailsText.setLayoutData( textData );
        
        makeActions();
        hookContextMenu();
        hookDoubleClickAction();
        contributeToActionBars();
    }


    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager( "#PopupMenu" );
        menuMgr.setRemoveAllWhenShown( true );
        menuMgr.addMenuListener( new IMenuListener() {
            public void menuAboutToShow( IMenuManager manager ) {
                TasksView.this.fillContextMenu( manager );
            }
        } );
        Menu menu = menuMgr.createContextMenu( viewer.getControl() );
        viewer.getControl().setMenu( menu );
        getSite().registerContextMenu( menuMgr, viewer );
    }


    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
//        fillLocalPullDown( bars.getMenuManager() );
        fillLocalToolBar( bars.getToolBarManager() );
    }


//    private void fillLocalPullDown( IMenuManager manager ) {
//        manager.add( addAction );
//        manager.add( new Separator() );
//        manager.add( action2 );
//    }


    private void fillContextMenu( IMenuManager manager ) {
        manager.add( removeAction );
        // Other plug-ins can contribute there actions here
        manager.add( new Separator( IWorkbenchActionConstants.MB_ADDITIONS ) );
    }


    private void fillLocalToolBar( IToolBarManager manager ) {
        manager.add( removeAction );
        // Other plug-ins can contribute there actions here
        manager.add( new Separator( IWorkbenchActionConstants.MB_ADDITIONS ) );
    }


    private void makeActions() {
        // removeAction
        removeAction = new Action() {
            public void run() {
                try {
                    RemoveTaskOperation op = TaskRepository.instance().newOperation( RemoveTaskOperation.class );
                    op.init( selectedTask() );
                    OperationSupport.instance().execute( op, false, false );
                }
                catch (ExecutionException e) {
                    PolymapWorkbench.handleError( LKAPlugin.PLUGIN_ID, this, e.getLocalizedMessage(), e );
                }
            }
        };
        removeAction.setText( "Aufgabe löschen" );
        removeAction.setToolTipText( "Aufgabe löschen" );

        URL url = BundleUtility.find( LKAPlugin.PLUGIN_ID, "icons/elcl16/remove_task.gif" );
        assert (url != null) : "No image found.";
        removeAction.setImageDescriptor( ImageDescriptor.createFromURL( url ) );
        
        // addAction
        addAction = new Action() {
            public void run() {
                try {
                    NewTaskOperation op = TaskRepository.instance().newOperation( NewTaskOperation.class );
                    op.init( "Test", "Ist nur zum Testen..." );
                    OperationSupport.instance().execute( op, false, false );
                }
                catch (ExecutionException e) {
                    PolymapWorkbench.handleError( LKAPlugin.PLUGIN_ID, this, e.getLocalizedMessage(), e );
                }
            }
        };
        addAction.setText( "Testaufgabe anlegen" );
        addAction.setToolTipText( "Testaufgabe anlegen" );

        url = BundleUtility.find( LKAPlugin.PLUGIN_ID, "icons/elcl16/add_task.gif" );
        assert (url != null) : "No image found.";
        addAction.setImageDescriptor( ImageDescriptor.createFromURL( url ) );
        
        // doubleClickAction
        doubleClickAction = new Action() {
            public void run() {
                try {
                    ITask selectedTask = selectedTask();
                    if (selectedTask != null) {
                        detailsText.setText( selectedTask.getDescription() );
                    }
                }
                catch (Exception e) {
                    PolymapWorkbench.handleError( LKAPlugin.PLUGIN_ID, this, e.getLocalizedMessage(), e );
                }
            }
        };

    }


    private void hookDoubleClickAction() {
        viewer.addDoubleClickListener( new IDoubleClickListener() {
            public void doubleClick( DoubleClickEvent event ) {
                doubleClickAction.run();
            }
        } );
    }


    private void showMessage( String message ) {
        MessageDialog.openInformation( viewer.getControl().getShell(), "Tasks View", message );
    }

    
    private ITask selectedTask() {
        ISelection sel = viewer.getSelection();
        if (sel instanceof IStructuredSelection) {
            Object elm = ((IStructuredSelection)sel).getFirstElement();
            if (elm instanceof ITask) {
                return (ITask)elm;
            }
            else {
                log.info( "No ITask: " + elm );
            }
        }
        return null;
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

}
