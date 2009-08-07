package togos.picturearchiver4_1.comframework;

import java.util.ArrayList;
import java.util.Iterator;

import togos.mf.api.Request;
import togos.mf.api.RequestHandler;
import togos.mf.api.ResponseSession;

public class MultiCommandHandler extends BaseCommandHandler {
	protected ArrayList commandHandlers = new ArrayList();
	
	public ResponseSession _open(Request command) {
		for( Iterator i=commandHandlers.iterator(); i.hasNext(); ) {
			ResponseSession crs = ((RequestHandler)i.next()).open(command);
			if( crs != null ) return crs;
		}
		return null;
	}
}
