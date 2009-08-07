package togos.picturearchiver4_1;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.HashMap;

import togos.mf.api.Request;
import togos.mf.api.RequestHandler;
import togos.mf.api.RequestVerbs;
import togos.mf.api.ResponseSession;
import togos.mf.base.BaseArguments;
import togos.mf.base.BaseRequest;

public class KeyCommandIssuer implements KeyListener {
	public RequestHandler commandHandler;
	public HashMap keyBindings = new HashMap();
	
	public KeyCommandIssuer( RequestHandler commandHandler ) {
		this.commandHandler = commandHandler;
	}
	
	public void addBinding( int keyCode, String uri ) {
		BaseRequest req = new BaseRequest(RequestVerbs.VERB_POST, uri);
		keyBindings.put( new Integer(keyCode), req );
	}

	public void addBinding( int keyCode, String uri, Object arg ) {
		BaseRequest req = new BaseRequest(RequestVerbs.VERB_POST, uri, BaseArguments.single(arg), Collections.EMPTY_MAP );
		keyBindings.put( new Integer(keyCode), req );
	}

	public void keyPressed(KeyEvent e) {
		Request command = (Request)keyBindings.get(new Integer(e.getKeyCode()));
		if( command != null ) {
			ResponseSession rs = commandHandler.open(command);
			if( rs == null ) {
				System.err.println("No response to <" + command.getUri() + ">, triggered by key " + e.getKeyCode());
			}
			e.consume();
		}
	}
	public void keyReleased(KeyEvent e) {};
	public void keyTyped(KeyEvent e) {}
}
