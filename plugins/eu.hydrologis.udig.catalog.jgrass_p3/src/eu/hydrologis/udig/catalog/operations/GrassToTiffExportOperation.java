/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.udig.catalog.operations;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import net.refractions.udig.ui.PlatformGIS;
import net.refractions.udig.ui.operations.IOp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.libs.iodrivers.JGrassMapEnvironment;
import eu.hydrologis.jgrass.libs.iodrivers.geotools.GrassCoverageReader;
import eu.hydrologis.jgrass.libs.utils.monitor.EclipseProgressMonitorAdapter;
import eu.hydrologis.udig.catalog.internal.jgrass.ChooseCoordinateReferenceSystemDialog;
import eu.hydrologis.udig.catalog.internal.jgrass.JGrassMapGeoResource;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GrassToTiffExportOperation implements IOp {
    private String newTiffPath;
    private CoordinateReferenceSystem crs;
    private int open;
    boolean doExport = false;

    public void op( final Display display, Object target, final IProgressMonitor monitor )
            throws Exception {
        final JGrassMapGeoResource[] mapResourcesArray = (JGrassMapGeoResource[]) target;

        display.syncExec(new Runnable(){

            public void run() {

                Dialog dialog = new Dialog(display.getActiveShell()){
                    private Text newFolderText;
                    private Text crsText;

                    protected Control createDialogArea( Composite maxparent ) {
                        Composite parent = new Composite(maxparent, SWT.None);
                        parent.setLayout(new GridLayout());
                        parent.setLayoutData(new GridData(GridData.FILL_BOTH
                                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

                        // the location name group
                        Group newTiffGroup = new Group(parent, SWT.None);
                        newTiffGroup.setLayout(new GridLayout(2, false));
                        newTiffGroup.setLayoutData(new GridData(GridData.FILL_BOTH
                                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
                        newTiffGroup.setText("folder into which to save the maps");

                        newFolderText = new Text(newTiffGroup, SWT.BORDER);
                        newFolderText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL
                                | GridData.VERTICAL_ALIGN_CENTER));

                        final Button button = new Button(newTiffGroup, SWT.PUSH);
                        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
                        button.setText("...");
                        button.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter(){
                            public void widgetSelected( org.eclipse.swt.events.SelectionEvent e ) {
                                DirectoryDialog fileDialog = new DirectoryDialog(button.getShell(),
                                        SWT.OPEN);
                                String path = fileDialog.open();
                                if (path == null || path.length() < 1) {
                                    newFolderText.setText("");
                                } else {
                                    newFolderText.setText(path);
                                }
                            }
                        });

                        // the crs choice group
                        Group crsGroup = new Group(parent, SWT.None);
                        crsGroup.setLayout(new GridLayout(2, false));
                        crsGroup.setLayoutData(new GridData(GridData.FILL_BOTH
                                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
                        crsGroup.setText("choose the coordinate reference system [optional]");

                        crsText = new Text(crsGroup, SWT.BORDER);
                        crsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL
                                | GridData.VERTICAL_ALIGN_CENTER));
                        crsText.setEditable(false);

                        final Button crsButton = new Button(crsGroup, SWT.BORDER);
                        crsButton.setText(" Choose CRS ");
                        crsButton
                                .addSelectionListener(new org.eclipse.swt.events.SelectionAdapter(){
                                    public void widgetSelected(
                                            org.eclipse.swt.events.SelectionEvent e ) {
                                        final ChooseCoordinateReferenceSystemDialog crsChooser = new ChooseCoordinateReferenceSystemDialog();
                                        crsChooser.open(new Shell(Display.getDefault()));
                                        CoordinateReferenceSystem readCrs = crsChooser.getCrs();
                                        if (readCrs == null)
                                            return;
                                        crsText.setText(readCrs.getName().toString());
                                        crsText.setData(readCrs);
                                    }
                                });

                        return parent;
                    }

                    protected void okPressed() {
                        newTiffPath = newFolderText.getText();
                        Object crsData = crsText.getData();
                        if (crsData instanceof CoordinateReferenceSystem) {
                            crs = (CoordinateReferenceSystem) crsData;
                        }
                        doExport = true;
                        super.okPressed();
                    }

                    protected void cancelPressed() {
                        doExport = false;
                        super.cancelPressed();
                    }
                };
                dialog.setBlockOnOpen(true);
                open = dialog.open();

            }

        });

        if (!doExport) {
            return;
        }
        
        /*
         * run with backgroundable progress monitoring
         */
        IRunnableWithProgress operation = new IRunnableWithProgress(){

            public void run( IProgressMonitor monitor ) throws InvocationTargetException,
                    InterruptedException {
                if (open == SWT.CANCEL) {
                    return;
                }
                if (newTiffPath == null || mapResourcesArray.length < 1) {
                    MessageBox msgBox = new MessageBox(display.getActiveShell(), SWT.ICON_ERROR);
                    msgBox.setMessage("An error occurred in processing the user supplied data.");
                    msgBox.open();
                    return;
                }
                /*
                 * finally do some processing
                 */
                int mapNum = mapResourcesArray.length;
                monitor.beginTask("Exporting maps to geotiff...", mapNum);
                for( int i = 0; i < mapNum; i++ ) {
                    JGrassMapGeoResource tmpMap = mapResourcesArray[i];
                    File mapFile = tmpMap.getMapFile();
                    JGrassMapEnvironment jgMEnv = new JGrassMapEnvironment(mapFile);
                    GrassCoverageReader tmp = new GrassCoverageReader(null, null, false, true,
                            new EclipseProgressMonitorAdapter(monitor));
                    tmp.setInput(jgMEnv.getCELL());
                    try {
                        GridCoverage2D coverage2D = tmp.read(null);
                        if (crs != null) {
                            coverage2D = (GridCoverage2D) Operations.DEFAULT.resample(coverage2D,
                                    crs);
                        }

                        final GeoTiffFormat format = new GeoTiffFormat();
                        final GeoTiffWriteParams wp = new GeoTiffWriteParams();
                        wp.setCompressionMode(GeoTiffWriteParams.MODE_DISABLED);
                        wp.setTilingMode(GeoToolsWriteParams.MODE_DEFAULT);
                        final ParameterValueGroup paramWrite = format.getWriteParameters();
                        paramWrite.parameter(
                                AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                                .setValue(wp);
                        File dumpFile = new File(newTiffPath + File.separator + mapFile.getName()
                                + ".tiff");
                        GeoTiffWriter gtw = (GeoTiffWriter) format.getWriter(dumpFile);
                        gtw.write(coverage2D, (GeneralParameterValue[]) paramWrite.values()
                                .toArray(new GeneralParameterValue[1]));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    monitor.worked(1);
                }
                monitor.done();
            }
        };
        PlatformGIS.runInProgressDialog("Exporting maps to geotiff...", true, operation, true);

    }
}
