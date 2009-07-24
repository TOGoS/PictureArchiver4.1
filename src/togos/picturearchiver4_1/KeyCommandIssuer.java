package togos.picturearchiver4_1;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.HashMap;

import togos.picturearchiver4_1.comframework.CommandHandler;
import togos.picturearchiver4_1.comframework.CommandResponseStream;
import togos.rra.BaseArguments;
import togos.rra.BaseRequest;
import togos.rra.Request;

public class KeyCommandIssuer implements KeyListener {
	public CommandHandler commandHandler;
	public HashMap keyBindings = new HashMap();
	
	public KeyCommandIssuer( CommandHandler commandHandler ) {
		this.commandHandler = commandHandler;
	}
	
	public void addBinding( int keyCode, String uri ) {
		BaseRequest req = new BaseRequest(Request.VERB_POST, uri);
		keyBindings.put( new Integer(keyCode), req );
	}

	public void addBinding( int keyCode, String uri, Object arg ) {
		BaseRequest req = new BaseRequest(Request.VERB_POST, uri, BaseArguments.single(arg), Collections.EMPTY_MAP );
		keyBindings.put( new Integer(keyCode), req );
	}

	public void keyPressed(KeyEvent e) {
		Request command = (Request)keyBindings.get(new Integer(e.getKeyCode()));
		if( command != null ) {
			CommandResponseStream rs = commandHandler.handleCommand(command);
			if( rs == null ) {
				System.err.println("No response to <" + command.getUri() + ">, triggered by key " + e.getKeyCode());
			}
			e.consume();
		}
	}
	public void keyReleased(KeyEvent e) {};
	public void keyTyped(KeyEvent e) {}
}
