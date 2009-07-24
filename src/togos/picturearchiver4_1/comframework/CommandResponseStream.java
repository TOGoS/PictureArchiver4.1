package togos.picturearchiver4_1.comframework;

import java.util.Iterator;

public interface CommandResponseStream extends Iterator {
	public static CommandResponseStream NORESPONSE = new CommandResponseStream() {
		public void close() {}
		public boolean hasNext() { return false; }
		public Object next() { return null;	}
		public void remove() {}
	};
	
	public void close();
}
