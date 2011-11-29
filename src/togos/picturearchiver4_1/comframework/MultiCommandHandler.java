package togos.picturearchiver4_1.comframework;

import java.util.ArrayList;
import java.util.Iterator;

import togos.mf.api.Callable;
import togos.mf.api.Request;
import togos.mf.api.ResponseCodes;

public class MultiCommandHandler extends BaseCommandHandler {
	protected ArrayList commandHandlers = new ArrayList();
	
	public boolean _call(Request command) {
		for( Iterator i=commandHandlers.iterator(); i.hasNext(); ) {
			if( ((Callable)i.next()).call(command).getStatus() != ResponseCodes.UNHANDLED ) return true;
		}
		return false;
	}
}
