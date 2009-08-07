package togos.picturearchiver4_1.comframework;

import togos.mf.api.Request;
import togos.mf.api.RequestHandler;
import togos.mf.api.Response;
import togos.mf.api.ResponseSession;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;

public abstract class BaseCommandHandler implements RequestHandler {
	public String commandPrefix;
	
	public BaseCommandHandler() {
		this("");
	}

	public BaseCommandHandler( String commandPrefix ) {
		this.commandPrefix = commandPrefix;
	}
	
	protected abstract ResponseSession _open(Request command);
	
	public ResponseSession open(Request command) {
		String uri = command.getUri();
		if( uri.startsWith(commandPrefix) ) uri = uri.substring(commandPrefix.length());
		else return null;
		
		BaseRequest mappedCommand = new BaseRequest(command, uri);
		return _open(mappedCommand);
	}
	
	public Response call(Request command) {
		ResponseSession it = open(command);
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
	
	public boolean send(Request command) {
		return open(command) != null;
	}
}
