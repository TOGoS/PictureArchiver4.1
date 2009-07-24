package togos.picturearchiver4_1.comframework;

import java.util.HashMap;

import togos.rra.Request;

public class MappedCommandHandler extends BaseCommandHandler {
	public MappedCommandHandler(String commandPrefix) {
		super(commandPrefix);
	}

	protected HashMap commandHandlers = new HashMap();
	
	public void putHandler( String commandName, CommandHandler handler ) {
		commandHandlers.put( commandName, handler );
	}
	
	protected CommandResponseStream _handleCommand(Request command) {
		CommandHandler ch = (CommandHandler)commandHandlers.get(command.getUri());
		System.err.println("Got " + command.getUri());
		if( ch == null ) return null;
		return ch.handleCommand(command);
	}
}
