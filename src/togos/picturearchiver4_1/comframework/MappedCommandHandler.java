package togos.picturearchiver4_1.comframework;

import java.util.HashMap;

import togos.mf.api.Callable;
import togos.mf.api.Request;
import togos.mf.api.ResponseCodes;

public class MappedCommandHandler extends BaseCommandHandler {
	public MappedCommandHandler(String commandPrefix) {
		super(commandPrefix);
	}

	protected HashMap commandHandlers = new HashMap();
	
	public void putHandler( String commandName, Callable handler ) {
		commandHandlers.put( commandName, handler );
	}
	
	protected boolean _call(Request command) {
		Callable ch = (Callable)commandHandlers.get(command.getResourceName());
		if( ch == null ) return true;
		return ch.call(command).getStatus() != ResponseCodes.UNHANDLED;
	}
}
