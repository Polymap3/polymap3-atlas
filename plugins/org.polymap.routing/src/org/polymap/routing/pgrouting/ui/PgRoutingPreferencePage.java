/* 
 * polymap.org
 * Copyright 2011, Polymap GmbH. All rights reserved.
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
package org.polymap.routing.pgrouting.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.polymap.core.project.ui.util.SimpleFormData;
import org.polymap.core.runtime.Polymap;
import org.polymap.core.security.SecurityUtils;

import org.polymap.routing.Messages;

/**
 * pgRouting control panel. Allows to configure a remote service identified by an
 * URL.
 * <p/>
 * Later versions may consider to make a full blown Workbench perspective to configure
 * routing.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class PgRoutingPreferencePage
        extends PreferencePage
        implements IWorkbenchPreferencePage {

    public PgRoutingPreferencePage() {
    }

    
    public PgRoutingPreferencePage( String title ) {
        super( title );
        setDescription( Messages.get( "PgRoutingPreferencePage_description" ) );
    }


    public PgRoutingPreferencePage( String title, ImageDescriptor image ) {
        super( title, image );
        setDescription( Messages.get( "PgRoutingPreferencePage_description" ) );
    }


    public void init( IWorkbench workbench ) {
    }


    protected Control createContents( Composite parent ) {
        // admin?
        if (!SecurityUtils.isAdmin( Polymap.instance().getPrincipals() )) {
            Label msg = new Label( parent, SWT.None ); 
            msg.setText( Messages.get( "PgRoutingPreferencePage_noAccess" ) );
            return msg;
        }
        
        // UI components
        Composite contents = new Composite( parent, SWT.NONE );
        contents.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        Label msg = new Label( contents, SWT.WRAP ); 
        msg.setText( "Here we go..." );
        
        // layout
        FormLayout layout = new FormLayout();
        contents.setLayout( layout );

        msg.setLayoutData( new SimpleFormData( 5 ).create() );

        return contents;
    }


    public boolean isValid() {
        // XXX
        return true;
    }


    public boolean okToLeave() {
        // XXX
        return true;
    }


    public boolean performCancel() {
        return super.performCancel();
    }


    public boolean performOk() {
        return super.performOk();
    }


    public void performHelp() {
        // XXX display help page
        throw new RuntimeException( "not yet implemented." );
    }
    
}
