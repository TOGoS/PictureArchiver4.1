package togos.picturearchiver4_1;

public class StatusLog
{
	interface SimpleLogger {
		public void log( String text );
	}
	
	public static class SingleLineStderr implements SimpleLogger {
		public void log( String text ) {
			if( text.length() > 79 ) {
				text = text.substring(0,38) + "..." + text.substring(text.length()-38);
			}
			
			String s10 = "          ";
			String s70 = s10+s10+s10+s10+s10+s10+s10;
			System.err.print( "\r"+s70+"         \r" );
			System.err.print( text+"\r" );
		}
	}
	
	public static class VerboseStderrLogger implements SimpleLogger {
		public void log( String text ) {
			System.err.println( text );
		}
	}
	
	protected static SimpleLogger instance = new SingleLineStderr();
	public static SimpleLogger getInstance() {  return instance;  }
	public static void setInstance(SimpleLogger l) {  instance = l;  }
	
	public static void log( String text ) {
		getInstance().log(text);
	}
}
