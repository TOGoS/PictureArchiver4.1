package togos.picturearchiver4_1.comframework;

import java.util.ArrayList;
import java.util.Iterator;

import togos.mf.api.Request;
import togos.mf.api.RequestHandler;

public class MultiCommandHandler extends BaseCommandHandler {
	protected ArrayList commandHandlers = new ArrayList();
	
	public boolean _send(Request command) {
		for( Iterator i=commandHandlers.iterator(); i.hasNext(); ) {
			if( ((RequestHandler)i.next()).send(command) ) return true;
		}
		return false;
	}
}
