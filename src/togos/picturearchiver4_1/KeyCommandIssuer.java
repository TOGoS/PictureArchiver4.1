package togos.picturearchiver4_1;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.HashMap;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.SendHandler;
import togos.mf.base.BaseArguments;
import togos.mf.base.BaseRequest;

public class KeyCommandIssuer implements KeyListener {
	public SendHandler commandHandler;
	public HashMap keyBindings = new HashMap();
	
	public KeyCommandIssuer( SendHandler commandHandler ) {
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
			if( commandHandler.send(command) ) {
				e.consume(); 
			} else {
				StatusLog.log("<" + command.getResourceName() + "> unhandled; triggered by key " + e.getKeyCode());
			}
			e.consume();
		}
	}
	
	public void keyReleased(KeyEvent e) {};
	public void keyTyped(KeyEvent e) {}
}
