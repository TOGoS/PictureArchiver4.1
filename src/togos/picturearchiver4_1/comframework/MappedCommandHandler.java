package togos.picturearchiver4_1.comframework;

import java.util.HashMap;

import togos.mf.api.Request;
import togos.mf.api.SendHandler;

public class MappedCommandHandler extends BaseCommandHandler {
	public MappedCommandHandler(String commandPrefix) {
		super(commandPrefix);
	}

	protected HashMap commandHandlers = new HashMap();
	
	public void putHandler( String commandName, SendHandler handler ) {
		commandHandlers.put( commandName, handler );
	}
	
	protected boolean _send(Request command) {
		SendHandler ch = (SendHandler)commandHandlers.get(command.getResourceName());
		if( ch == null ) return false;
		return ch.send(command);
	}
}
