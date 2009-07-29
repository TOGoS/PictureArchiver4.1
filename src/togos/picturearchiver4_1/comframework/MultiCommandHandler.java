package togos.picturearchiver4_1.comframework;

import java.util.ArrayList;
import java.util.Iterator;

import togos.mf.MessageIterator;
import togos.mf.RequestHandler;
import togos.rra.Request;

public class MultiCommandHandler extends BaseCommandHandler {
	protected ArrayList commandHandlers = new ArrayList();
	
	public MessageIterator _open(Request command) {
		for( Iterator i=commandHandlers.iterator(); i.hasNext(); ) {
			MessageIterator crs = ((RequestHandler)i.next()).open(command);
			if( crs != null ) return crs;
		}
		return null;
	}
}
