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
package org.polymap.geocoder;

import java.util.Arrays;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Point;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.polymap.core.data.ui.csvimport.CsvImporter;
import org.polymap.core.operation.OperationSupport;
import org.polymap.core.runtime.Polymap;
import org.polymap.geocoder.lucene.AddressIndexer;
import org.polymap.geocoder.tasks.TasksView;
import org.polymap.geocoder.tasks.qi4j.TaskRepository;
import org.polymap.geocoder.tasks.qi4j.operations.NewTaskOperation;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class CsvOperation
        implements org.polymap.core.data.ui.csvimport.CsvOperation {

    private static final Log  log = LogFactory.getLog( CsvOperation.class );

    public static final String[] PROP_Y = { "hw", "hoch", "hochwert", "y" };
    public static final String[] PROP_X = { "rw", "rechts", "rechtswert", "x" };
    

    public CsvOperation() {
    }

    public void perform( CsvImporter importer )
            throws Exception {
        log.info( "perform(): ..." );
        ByteArrayOutputStream logmsg = new ByteArrayOutputStream();
        PrintStream logOut = new PrintStream( logmsg );

        // find address fields
        String[] header = importer.getHeader();
        if (header == null) {
            throw new Exception( "Es sind noch keine Feldnamen festgelegt. Benutzen Sie die automatische Zuordnung aus dem CSV-Header oder setzen sie diese von Hand." );
        }
        logOut.println( "Kopfzeile" );
        logOut.println( "----------------------------------------" );
        logOut.println( "Kopfzeile: " + Arrays.asList( header ) );

        int cityField = -1;
        int streetField = -1;
        int numberField = -1;
        int codeField = -1;
        int xField = -1;
        int yField = -1;
        
        for (int i=0; i<header.length; i++) {
            if (ArrayUtils.contains( AddressIndexer.PROP_CITY, header[i].toLowerCase() )) {
                cityField = i;
                logOut.println( "    Feld: " + header[i] + ", Position: " + i );
            }
            else if (ArrayUtils.contains( AddressIndexer.PROP_STREET, header[i].toLowerCase() )) {
                streetField = i;
                logOut.println( "    Feld: " + header[i] + ", Position: " + i );
            }
            else if (ArrayUtils.contains( AddressIndexer.PROP_NUMBER, header[i].toLowerCase() )) {
                numberField = i;
                logOut.println( "    Feld: " + header[i] + ", Position: " + i );
            }
            else if (ArrayUtils.contains( AddressIndexer.PROP_POSTALCODE, header[i].toLowerCase() )) {
                codeField = i;
                logOut.println( "    Feld: " + header[i] + ", Position: " + i );
            }
            else if (ArrayUtils.contains( PROP_X, header[i].toLowerCase() )) {
                xField = i;
                logOut.println( "    Feld: " + header[i] + ", Position: " + i );
            }
            else if (ArrayUtils.contains( PROP_Y, header[i].toLowerCase() )) {
                yField = i;
                logOut.println( "    Feld: " + header[i] + ", Position: " + i );
            }
        }
        
        // geocode addresses
        logOut.println( "Daten" );
        logOut.println( "----------------------------------------" );
        Geocoder geocoder = Geocoder.instance();
        for (String[] values : importer.getLines()) {
            logOut.println( "Zeile: " + Arrays.asList( values ) );
            Address address = new Address( values[streetField], values[numberField], 
                    null /*values.get( codeField )*/, values[cityField], null );
            logOut.println( "    -> " + address );
            List<Address> found = geocoder.find( address, 3 );
            logOut.println( "    => " + found );
            
            if (found != null && found.size() > 0) {
                Point point = found.get( 0 ).getPoint();
                values[xField] = importer.getNumberFormat().format( point.getX() );
                values[yField] = importer.getNumberFormat().format( point.getY() );
            }
            else {
                NewTaskOperation op = TaskRepository.instance().newOperation( NewTaskOperation.class );
                StringBuffer desc = new StringBuffer( 256 );
                for (String value : values) {
                    desc.append( value ).append( " | " );
                }
                op.init( "Import: " + values[streetField], desc.toString() );
                OperationSupport.instance().execute( op, false, false );
                
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                TasksView view = (TasksView)page.showView( TasksView.ID );
            }
        }
        logOut.flush();
        
        // result dialog
        Display display = Polymap.getSessionDisplay();
        ResultDialog dialog = new ResultDialog( display.getActiveShell(), logmsg.toString() );
        dialog.open();
    }
    
    
    /**
     * 
     *
     * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
     * @version POLYMAP3 ($Revision$)
     * @since 3.0
     */
    class ResultDialog
            extends Dialog {

        private Text            text;

        private String          msg;
        
        
        protected ResultDialog( Shell parentShell, String msg ) {
            super( parentShell );
            this.msg = msg;
        }

        protected boolean isResizable() {
            return true;
        }

        protected void configureShell( Shell shell ) {
            super.configureShell( shell );
            shell.setText( "Ergebnisse der Addresssuche" );
            shell.setSize( 800, 600 );

            Rectangle parentSize = getParentShell().getBounds();
            Rectangle mySize = shell.getBounds();
            int locationX, locationY;
            locationX = (parentSize.width - mySize.width)/2+parentSize.x;
            locationY = (parentSize.height - mySize.height)/2+parentSize.y;
            shell.setLocation( new org.eclipse.swt.graphics.Point( locationX, locationY ) );
        }

        protected Control createDialogArea( Composite parent ) {
            Composite composite = (Composite)super.createDialogArea( parent );
            
            Text l = new Text( composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP );
            l.setText( "Nachfolgend sehen sie das Protokoll der Adresssuche. Jeweils 3 Zeilen bezeichnen einen Datensatz. " +
                    "Leere Zeilen ([]) konnten nicht zugeordnet werden. Nach verlassen des Dialoges sind die Koordinaten für die einzelnen Datensätze gesetzt und werden beim Import verwendet. " +
                    "Datensätze ohne Adresszuordnung werden ignoriert und nicht importiert." );
            l.setLayoutData( new GridData( GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
            
//            ScrolledComposite scroll = new ScrolledComposite( composite, SWT.V_SCROLL | SWT.BORDER );
//            scroll.setLayoutData( new GridData( GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL));
            
            text = new Text( composite, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL );
            text.setText( msg );
            text.setLayoutData( new GridData( GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

//            applyDialogFont( composite );
            return composite;
        }
        
        protected void createButtonsForButtonBar( Composite parent ) {
            createButton( parent, IDialogConstants.OK_ID,
                    IDialogConstants.get().OK_LABEL, false );
//            //do this here because setting the text will set enablement on the ok
//            // button
//            text.setFocus();
//            if (value != null) {
//                text.setText(value);
//                text.selectAll();
//            }
        }
    }

}
