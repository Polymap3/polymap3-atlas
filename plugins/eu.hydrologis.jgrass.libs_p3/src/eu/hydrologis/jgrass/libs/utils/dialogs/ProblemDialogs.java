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
package eu.hydrologis.jgrass.libs.utils.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import eu.hydrologis.jgrass.libs.JGrassLibsPlugin;
import eu.hydrologis.jgrass.libs.messages.Messages;

/**
 * Dialogs that get executed in the display thread.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ProblemDialogs {
    /**
     * Opens an error dialog with the given message.
     * 
     * @param shell a parent shell, can be null.
     * @param errorMessage the message.
     * @param doAsync if true, it is executed in async mode.
     */
    public static void errorDialog( final Shell shell, final String errorMessage, boolean doAsync ) {
        if (doAsync) {
            Display.getDefault().asyncExec(new Runnable(){
                public void run() {
                    error(errorMessage, shell);
                }
            });
        } else {
            Display.getDefault().syncExec(new Runnable(){
                public void run() {
                    error(errorMessage, shell);
                }
            });
        }
    }

    /**
     * Opens a warning dialog with the given message.
     * 
     * @param shell a parent shell, can be null.
     * @param warningMessage the message.
     * @param doAsync if true, it is executed in async mode.
     */
    public static void warningDialog( final Shell shell, final String warningMessage,
            boolean doAsync ) {
        if (doAsync) {
            Display.getDefault().asyncExec(new Runnable(){
                public void run() {
                    warning(warningMessage, shell);
                }
            });
        } else {
            Display.getDefault().syncExec(new Runnable(){
                public void run() {
                    warning(warningMessage, shell);
                }
            });
        }
    }

    /**
     * Opens an info dialog with the given message.
     * 
     * @param shell a parent shell, can be null.
     * @param infoMessage the message.
     * @param doAsync if true, it is executed in async mode.
     */
    public static void infoDialog( final Shell shell, final String infoMessage, boolean doAsync ) {
        if (doAsync) {
            Display.getDefault().asyncExec(new Runnable(){
                public void run() {
                    info(infoMessage, shell);
                }
            });
        } else {
            Display.getDefault().syncExec(new Runnable(){
                public void run() {
                    info(infoMessage, shell);
                }
            });
        }
    }
    /**
     * @param errorMessage
     * @param shell
     */
    private static void error( final String errorMessage, Shell shell ) {
        Shell sh = shell != null ? shell : PlatformUI.getWorkbench().getDisplay().getActiveShell();
        MessageDialog dialog = new MessageDialog(sh,
                Messages.getString("ProblemDialogs.error"), null, errorMessage, //$NON-NLS-1$
                MessageDialog.ERROR, new String[]{Messages.getString("ProblemDialogs.ok")}, 0); //$NON-NLS-1$
        dialog.setBlockOnOpen(true);
        dialog.open();
    }

    /**
     * @param warningMessage
     * @param shell
     */
    private static void warning( final String warningMessage, Shell shell ) {
        Shell sh = shell != null ? shell : PlatformUI.getWorkbench().getDisplay().getActiveShell();
        MessageDialog dialog = new MessageDialog(sh,
                Messages.getString("ProblemDialogs.warning"), null, warningMessage, //$NON-NLS-1$
                MessageDialog.WARNING, new String[]{Messages.getString("ProblemDialogs.ok")}, 0); //$NON-NLS-1$
        dialog.setBlockOnOpen(true);
        dialog.open();
    }

    /**
     * @param infoMessage
     * @param shell
     */
    private static void info( final String infoMessage, final Shell shell ) {
        Shell sh = shell != null ? shell : PlatformUI.getWorkbench().getDisplay().getActiveShell();
        MessageDialog dialog = new MessageDialog(sh,
                Messages.getString("ProblemDialogs.Info"), null, infoMessage, //$NON-NLS-1$
                MessageDialog.INFORMATION, new String[]{Messages.getString("ProblemDialogs.ok")}, 0); //$NON-NLS-1$
        dialog.setBlockOnOpen(true);
        dialog.open();
    }

}
