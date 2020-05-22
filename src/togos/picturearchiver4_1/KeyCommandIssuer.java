package togos.picturearchiver4_1;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.HashMap;

import togos.mf.api.Callable;
import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseArguments;
import togos.mf.base.BaseRequest;

public class KeyCommandIssuer implements KeyListener {
	public Callable commandHandler;
	public HashMap<Integer,Request> keyBindings = new HashMap();
	
	public KeyCommandIssuer( Callable commandHandler ) {
		this.commandHandler = commandHandler;
	}
	
	public void addBinding( int keyCode, String uri ) {
		BaseRequest req = new BaseRequest(RequestVerbs.POST, uri);
		keyBindings.put( Integer.valueOf(keyCode), req );
	}

	public void addBinding( int keyCode, String uri, Object arg ) {
		BaseRequest req = new BaseRequest(RequestVerbs.POST, uri, BaseArguments.single(arg), Collections.EMPTY_MAP );
		keyBindings.put( Integer.valueOf(keyCode), req );
	}

	public void keyPressed(KeyEvent e) {
		Request command = (Request)keyBindings.get(new Integer(e.getKeyCode()));
		if( command != null ) {
			if( commandHandler.call(command).getStatus() != ResponseCodes.UNHANDLED ) {
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
