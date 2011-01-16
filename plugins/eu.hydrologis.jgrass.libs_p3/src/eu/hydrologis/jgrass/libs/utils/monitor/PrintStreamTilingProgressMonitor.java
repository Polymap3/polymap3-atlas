package eu.hydrologis.jgrass.libs.utils.monitor;

import java.io.PrintStream;

public class PrintStreamTilingProgressMonitor extends PrintStreamProgressMonitor {


    public PrintStreamTilingProgressMonitor( PrintStream printStream) {
        super(printStream);


    }

    
    public void writeTask(String name){
        printStream.println(name);
    }
    
    
    public void beginTask( String name, int totalWork ) {
        this.taskName = name;
        if (totalWork>this.totalWork){
            this.totalWork = totalWork;
            printStream.println(taskName);}
    }
    
    
    public void worked( int work ) {
        if (totalWork == -1) {
            printStream.print("..."); //$NON-NLS-1$ 
        } else {
            runningWork = runningWork + work;
            // calculate %
            int percentage = 100 * runningWork / totalWork;
            if (percentage % 10 == 0 && percentage != lastPercentage) {
                printStream.print(percentage + "%... "); //$NON-NLS-1$ //$NON-NLS-2$
                lastPercentage = percentage;
             if(lastPercentage==100){
               printStream.println("Finished.");
           }}
          
        }
    }

    

    public void done() {
    }
}
