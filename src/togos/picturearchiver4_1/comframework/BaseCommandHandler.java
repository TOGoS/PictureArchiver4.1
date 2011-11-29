package togos.picturearchiver4_1.comframework;

import togos.mf.api.Callable;
import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;

public abstract class BaseCommandHandler implements Callable {
	public String commandPrefix;
	
	public BaseCommandHandler() {
		this("");
	}

	public BaseCommandHandler( String commandPrefix ) {
		this.commandPrefix = commandPrefix;
	}
	
	protected abstract boolean _call(Request command);
	
	public Response call(Request command) {
		String uri = command.getResourceName();
		if( uri.startsWith(commandPrefix) ) {
			uri = uri.substring(commandPrefix.length());
			BaseRequest mappedCommand = new BaseRequest(command, uri);
			if( _call(mappedCommand) ) return Responses.OK_THANKS; 
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
