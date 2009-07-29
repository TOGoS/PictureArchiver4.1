package togos.picturearchiver4_1.comframework;

import togos.mf.MessageIterator;
import togos.mf.RequestHandler;
import togos.rra.BaseRequest;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;

public abstract class BaseCommandHandler implements RequestHandler {
	public String commandPrefix;
	
	public BaseCommandHandler() {
		this("");
	}

	public BaseCommandHandler( String commandPrefix ) {
		this.commandPrefix = commandPrefix;
	}
	
	protected abstract MessageIterator _open(Request command);
	
	public MessageIterator open(Request command) {
		String uri = command.getUri();
		if( uri.startsWith(commandPrefix) ) uri = uri.substring(commandPrefix.length());
		else return null;
		
		BaseRequest mappedCommand = new BaseRequest(command, uri);
		return _open(mappedCommand);
	}
	
	public Response call(Request command) {
		MessageIterator it = open(command);
		if( it == null ) {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
		Response res = null;
		while( it.hasNext() ) {
			Object o = it.next();
			if( o instanceof Response ) res = (Response)o;
		}
		if( res == null ) throw new RuntimeException();
		return res;
	}
	
	public void send(Request command) {
		open(command);
	}
}
