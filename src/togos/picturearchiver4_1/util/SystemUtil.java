package togos.picturearchiver4_1.util;

public class SystemUtil
{
	public static class ShellCommandError extends Exception {
		ShellCommandError(String message) {
			super(message);
		}
		ShellCommandError(String message, Throwable cause) {
			super(message, cause);
		}
	}
	
	protected static String join( String[] parts, String sep ) {
		String res = "";
		for( int i=0; i<parts.length; ++i ) {
			res += parts[i];
			if( i<parts.length-1 ) res += sep;
		}
		return res;
	}
	
	public static void runCommand( String[] argv ) throws ShellCommandError {
		Process lnProc;
		try {
			lnProc = Runtime.getRuntime().exec(argv);
			int lnProcReturn = lnProc.waitFor();
			if( lnProcReturn != 0 ) {
				throw new ShellCommandError("External program returned " + lnProcReturn + ": " + join(argv," "));
			}
		} catch( Exception e) {
			throw new ShellCommandError("Exception while running " + join(argv," "), e);
		}
	}
}
