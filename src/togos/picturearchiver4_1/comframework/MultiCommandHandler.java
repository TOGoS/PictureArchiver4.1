package togos.picturearchiver4_1.comframework;

import java.util.ArrayList;
import java.util.Iterator;

import togos.rra.Request;

public class MultiCommandHandler implements CommandHandler {
	protected ArrayList commandHandlers = new ArrayList();
	
	public CommandResponseStream handleCommand(Request command) {
		for( Iterator i=commandHandlers.iterator(); i.hasNext(); ) {
			CommandResponseStream crs = ((CommandHandler)i.next()).handleCommand(command);
			if( crs != null ) return crs;
		}
		return null;
	}
}
