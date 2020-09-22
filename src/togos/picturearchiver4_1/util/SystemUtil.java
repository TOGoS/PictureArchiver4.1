package togos.picturearchiver4_1.util;

import togos.picturearchiver4_1.StatusLog;

public class SystemUtil
{
	public static class ShellCommandError extends Exception {
		public ShellCommandError(String message) {
			super(message);
		}
		public ShellCommandError(String message, Throwable cause) {
			super(message, cause);
		}
	}
	
	protected static String quoteArg(String arg) {
		if (arg.contains(" ")) return '"'+arg+'"';
		// Bah
		return arg;
	}
	
	/** Quote a command in a manner suitable for debugging; might not be safe to pass to a shell */
	public static String commandString(String[] parts ) {
		String res = "";
		for( int i=0; i<parts.length; ++i ) {
			res += quoteArg(parts[i]);
			if( i<parts.length-1 ) res += " ";
		}
		return res;
	}
	
	public static void runCommand( String[] argv ) throws ShellCommandError {
		Process lnProc;
		try {
			StatusLog.log("Running command: "+commandString(argv));
			lnProc = Runtime.getRuntime().exec(argv);
			int lnProcReturn = lnProc.waitFor();
			if( lnProcReturn != 0 ) {
				throw new ShellCommandError("External program returned " + lnProcReturn + ": " + commandString(argv));
			}
		} catch( Exception e) {
			throw new ShellCommandError("Exception while running " + commandString(argv), e);
		}
	}
}
