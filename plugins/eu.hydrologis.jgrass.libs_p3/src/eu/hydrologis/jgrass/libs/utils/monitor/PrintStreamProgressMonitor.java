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
package eu.hydrologis.jgrass.libs.utils.monitor;

import java.io.PrintStream;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A progress monitor for printstream based applications, i.e. console or commandline.
 * 
 * <p>This implements both {@link IProgressMonitorJGrass} and 
 * {@link IProgressMonitor} in order to be used also in the part of
 * the code that needs to stay clean of rcp code.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PrintStreamProgressMonitor implements IProgressMonitorJGrass {

    protected boolean cancelled = false;
    protected final PrintStream printStream;
    protected String taskName;
    protected int totalWork;
    protected int runningWork;
    protected int lastPercentage = -1;

    public PrintStreamProgressMonitor( PrintStream printStream ) {
        this.printStream = printStream;
    }

    public void beginTask( String name, int totalWork ) {
        this.taskName = name;
        this.totalWork = totalWork;
        runningWork = 0;
        printStream.println(taskName);
    }

    public void done() {
        printStream.println("Finished.");
    }

    public void internalWorked( double work ) {
    }

    public boolean isCanceled() {
        return cancelled;
    }

    public void setCanceled( boolean cancelled ) {
        this.cancelled = cancelled;
    }

    public void setTaskName( String name ) {
        taskName = name;
    }

    public void subTask( String name ) {
    }

    public void worked( int work ) {
        if (totalWork == -1) {
            printStream.print("..."); //$NON-NLS-1$ 
        } else {
            runningWork = runningWork + work;
            // calculate %
            int percentage = 100 * runningWork / totalWork;
            if ((totalWork <= 40 || percentage % 10 == 0) && percentage != lastPercentage) {
                printStream.print(percentage + "%... "); //$NON-NLS-1$ 
                lastPercentage = percentage;
            }
        }
    }

    public PrintStream getPrintStream() {
        return printStream;
    }
}
