package eu.hydrologis.jgrass.libs.utils.monitor;

import java.io.PrintStream;

public class PrintstreamProgress {
	private final PrintStream out;
	private int endvalue;
	private int startvalue;
	private int interval;
	private boolean isOk = true;
	private int prev = -1;

	public PrintstreamProgress(int startvalue, int endvalue, PrintStream out) {
		this.endvalue = endvalue;
		this.startvalue = startvalue;
		interval = endvalue - startvalue;
		if (interval <= 0) {
			out.println("WRONG INTERVAL SUPPLIED FOR PERCENTAGE CALCULUS.");
			isOk = false;
		}
		prev = -1;
		this.out = out;
	}

	public void printPercent(int runningvalue) {

		if (isOk) {
			int x = 100 * runningvalue / interval;
			if (x != prev && x % 10 == 0) {
				out.println(x + "% "); //$NON-NLS-1$
			}
			prev = x;
		}
	}
}
