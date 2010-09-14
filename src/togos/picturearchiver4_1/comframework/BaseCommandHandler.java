package togos.picturearchiver4_1.comframework;

import togos.mf.api.Request;
import togos.mf.api.SendHandler;
import togos.mf.base.BaseRequest;

public abstract class BaseCommandHandler implements SendHandler {
	public String commandPrefix;
	
	public BaseCommandHandler() {
		this("");
	}

	public BaseCommandHandler( String commandPrefix ) {
		this.commandPrefix = commandPrefix;
	}
	
	protected abstract boolean _send(Request command);
	
	public boolean send(Request command) {
		String uri = command.getResourceName();
		if( uri.startsWith(commandPrefix) ) {
			uri = uri.substring(commandPrefix.length());
			BaseRequest mappedCommand = new BaseRequest(command, uri);
			return _send(mappedCommand);
		}
		return false;
	}
}
