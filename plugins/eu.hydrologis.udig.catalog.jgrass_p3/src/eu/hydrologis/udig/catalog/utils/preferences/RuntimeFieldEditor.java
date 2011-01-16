package eu.hydrologis.udig.catalog.utils.preferences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import net.refractions.udig.internal.ui.UiPlugin;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public final class RuntimeFieldEditor extends FieldEditor {
    private static final String RunntimeFieldEditor_workspace_path = "Workspace path";
    private static final String RunntimeFieldEditor_locale = "Locale for user interface";
    private static final String RunntimeFieldEditor_maxheap = "Maximum heap memory in megabytes";
    private static final String RunntimeFieldEditor_restart = "Restart with the above settings.";
    private static final String RunntimeFieldEditor_error = "Error";
    private static final String RunntimeFieldEditor_path_not_existing = "The path to the workspace has to be an existing folder.";
    private static final String RunntimeFieldEditor_memory_positive = "The memory value has to be a positive number bigger than 64.";

    private final String WORKSPACE_PATH = "WORKSPACE_PATH"; //$NON-NLS-1$
    private final String LANGUAGE = "LANGUAGE"; //$NON-NLS-1$
    private final String MEMORY = "MEMORY"; //$NON-NLS-1$

    private Text wkspaceText;
    private Text langText;
    private Text memoryText;
    private String workspacePath;

    public RuntimeFieldEditor( String name, String labelText, Composite parent ) {
        super(name, labelText, parent);

    }

    public int getNumberOfControls() {
        return 3;
    }
    protected void doStore() {
        saveValues();
    }

    protected void doLoadDefault() {
        wkspaceText.setText(workspacePath);
    }
    protected void doLoad() {
        wkspaceText.setText(getPreferenceStore().getString(WORKSPACE_PATH));
        langText.setText(getPreferenceStore().getString(LANGUAGE));
        memoryText.setText(getPreferenceStore().getString(MEMORY));
    }

    protected void doFillIntoGrid( final Composite parent, int numColumns ) {
        setPreferenceStore(UiPlugin.getDefault().getPreferenceStore());
        URL instanceUrl = Platform.getInstanceLocation().getURL();
        workspacePath = new File(instanceUrl.getFile()).toString();

        // workspace
        Label wkspaceLabel = new Label(parent, SWT.NONE);
        wkspaceLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        wkspaceLabel.setText(RunntimeFieldEditor_workspace_path);

        wkspaceText = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
        wkspaceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        wkspaceText.setText(workspacePath);

        Button wkspaceButton = new Button(parent, SWT.PUSH);
        wkspaceButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        wkspaceButton.setText("..."); //$NON-NLS-1$
        wkspaceButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter(){
            public void widgetSelected( org.eclipse.swt.events.SelectionEvent e ) {
                DirectoryDialog fileDialog = new DirectoryDialog(parent.getShell(), SWT.OPEN);
                String path = fileDialog.open();
                if (path == null || path.length() < 1) {
                    wkspaceText.setText(""); //$NON-NLS-1$
                } else {
                    wkspaceText.setText(path);
                }
            }
        });

        // language
        Label langLabel = new Label(parent, SWT.NONE);
        langLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        langLabel.setText(RunntimeFieldEditor_locale);

        langText = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
        GridData gD = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gD.horizontalSpan = 2;
        langText.setLayoutData(gD);
        langText.setText(""); //$NON-NLS-1$

        // memory
        Label memoryLabel = new Label(parent, SWT.NONE);
        memoryLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        memoryLabel.setText(RunntimeFieldEditor_maxheap);

        memoryText = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
        GridData gD2 = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gD2.horizontalSpan = 2;
        memoryText.setLayoutData(gD2);
        memoryText.setText(""); //$NON-NLS-1$

        // restart
        Button restartButton = new Button(parent, SWT.PUSH);
        restartButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        restartButton.setText(RunntimeFieldEditor_restart);
        restartButton.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected( SelectionEvent e ) {
                if (saveValues())
                    restart();
            }
        });

        // put some defaults in
        getPreferenceStore().setValue(WORKSPACE_PATH, wkspaceText.getText());
        getPreferenceStore().setValue(LANGUAGE, langText.getText());
        getPreferenceStore().setValue(MEMORY, memoryText.getText());

    }

    private boolean checkValues() {
        String wksPath = wkspaceText.getText();
        File f = new File(wksPath);
        if (!f.exists()) {
            MessageDialog.openError(wkspaceText.getShell(), RunntimeFieldEditor_error,
                    RunntimeFieldEditor_path_not_existing);
            return false;
        }

        String memory = memoryText.getText();
        int mem = 0;
        try {
            mem = Integer.parseInt(memory);
        } catch (Exception e) {
        }
        if (mem < 64) {
            MessageDialog.openError(wkspaceText.getShell(), RunntimeFieldEditor_error,
                    RunntimeFieldEditor_memory_positive);
            return false;
        }
        return true;
    }

    private boolean saveValues() {
        if (checkValues()) {
            getPreferenceStore().setValue(WORKSPACE_PATH, wkspaceText.getText());
            getPreferenceStore().setValue(LANGUAGE, langText.getText());
            getPreferenceStore().setValue(MEMORY, memoryText.getText());
            return true;
        } else {
            return false;
        }
    }

    private void restart() {
        try {

            URL configUrlURL = Platform.getConfigurationLocation().getURL();

            String configFilePath = configUrlURL.getFile() + File.separator + "config.ini"; //$NON-NLS-1$
            File configFile = new File(configFilePath);
            // System.out.println("config.ini changed:" + configFile);

            // vmargs go in the udig.ini file
            File appFolder = configFile.getParentFile().getParentFile();
            String[] list = appFolder.list();
            String iniName = null;
            for( String l : list ) {
                if (l.endsWith(".ini")) { //$NON-NLS-1$
                    iniName = l;
                }
            }
            if (iniName == null) {
                MessageDialog.openError(wkspaceText.getShell(), RunntimeFieldEditor_error,
                        "The configuration file (*.ini) doesn't exist in: "
                                + appFolder.getAbsolutePath());
                return;
            }

            File iniFile = new File(appFolder, iniName);
            // System.out.println("udig.ini changed:" + iniFile.getAbsolutePath());
            if (iniFile.exists()) {
                BufferedReader bR = new BufferedReader(new FileReader(iniFile));
                List<String> opts = new ArrayList<String>();
                String line = null;
                while( (line = bR.readLine()) != null ) {
                    if (line.matches(".*Xmx.*")) { //$NON-NLS-1$
                        line = line.replaceFirst("Xmx[0-9]+", "Xmx" + memoryText.getText()); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    opts.add(line);
                }
                bR.close();
                BufferedWriter bW = new BufferedWriter(new FileWriter(iniFile));
                for( String lineStr : opts ) {
                    bW.write(lineStr);
                    bW.write("\n"); //$NON-NLS-1$
                }
                bW.close();
            }

            // language and path go in the config.ini file
            Properties properties = new Properties();
            properties.load(new FileInputStream(configFile));

            String path = wkspaceText.getText();
            path = new File(path).toURI().toURL().toExternalForm();
            path = path.replaceAll("%20", " "); //$NON-NLS-1$ //$NON-NLS-2$
            properties.setProperty("osgi.instance.area.default", path); //$NON-NLS-1$
            properties.setProperty("osgi.nl", langText.getText()); //$NON-NLS-1$

            Set<Object> keySet = properties.keySet();
            BufferedWriter bW = new BufferedWriter(new FileWriter(configFile));

            for( Object key : keySet ) {
                String keyStr = (String) key;
                if (!keyStr.equals("eof")) { //$NON-NLS-1$
                    bW.write(keyStr);
                    bW.write("="); //$NON-NLS-1$
                    bW.write(properties.getProperty(keyStr));
                    bW.write("\n"); //$NON-NLS-1$
                }
            }
            bW.write("eof=eof"); //$NON-NLS-1$
            bW.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        PlatformUI.getWorkbench().restart();
    }
    @Override
    protected void adjustForNumColumns( int numColumns ) {
    }
}
