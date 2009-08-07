package togos.picturearchiver4_1.comframework;

import java.util.HashMap;

import togos.mf.api.Request;
import togos.mf.api.RequestHandler;
import togos.mf.api.ResponseSession;

public class MappedCommandHandler extends BaseCommandHandler {
	public MappedCommandHandler(String commandPrefix) {
		super(commandPrefix);
	}

	protected HashMap commandHandlers = new HashMap();
	
	public void putHandler( String commandName, RequestHandler handler ) {
		commandHandlers.put( commandName, handler );
	}
	
	protected ResponseSession _open(Request command) {
		RequestHandler ch = (RequestHandler)commandHandlers.get(command.getUri());
		if( ch == null ) return null;
		return ch.open(command);
	}
}
