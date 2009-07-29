package togos.picturearchiver4_1.comframework;

import java.util.HashMap;

import togos.mf.MessageIterator;
import togos.mf.RequestHandler;
import togos.rra.Request;

public class MappedCommandHandler extends BaseCommandHandler {
	public MappedCommandHandler(String commandPrefix) {
		super(commandPrefix);
	}

	protected HashMap commandHandlers = new HashMap();
	
	public void putHandler( String commandName, RequestHandler handler ) {
		commandHandlers.put( commandName, handler );
	}
	
	protected MessageIterator _open(Request command) {
		RequestHandler ch = (RequestHandler)commandHandlers.get(command.getUri());
		if( ch == null ) return null;
		return ch.open(command);
	}
}
