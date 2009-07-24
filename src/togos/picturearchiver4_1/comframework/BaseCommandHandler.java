package togos.picturearchiver4_1.comframework;

import togos.rra.BaseRequest;
import togos.rra.Request;

public abstract class BaseCommandHandler implements CommandHandler {
	public String commandPrefix;
	
	public BaseCommandHandler( String commandPrefix ) {
		this.commandPrefix = commandPrefix;
	}
	
	protected abstract CommandResponseStream _handleCommand(Request command);
	
	public CommandResponseStream handleCommand(Request command) {
		String uri = command.getUri();
		if( uri.startsWith(commandPrefix) ) uri = uri.substring(commandPrefix.length());
		else return null;
		
		BaseRequest mappedCommand = new BaseRequest(command, uri);
		return _handleCommand(mappedCommand);
	}
}
