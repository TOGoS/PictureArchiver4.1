package togos.picturearchiver4_1;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;

import togos.picturearchiver4_1.comframework.CommandHandler;
import togos.picturearchiver4_1.comframework.CommandResponseStream;
import togos.rra.BaseRequest;
import togos.rra.Request;

public class KeyCommandIssuer implements KeyListener {
	public CommandHandler commandHandler;
	public HashMap keyBindings = new HashMap();
	
	public KeyCommandIssuer( CommandHandler commandHandler ) {
		this.commandHandler = commandHandler;
	}
	
	public void addBinding( int keyCode, String uri ) {
		keyBindings.put( new Integer(keyCode), uri );
	}

	public void keyPressed(KeyEvent e) {
		String commandUri = (String)keyBindings.get(new Integer(e.getKeyCode()));
		if( commandUri != null ) {
			System.err.println("Issuing command " + commandUri);
			CommandResponseStream rs = commandHandler.handleCommand(new BaseRequest(Request.VERB_POST, commandUri));
			if( rs == null ) {
				System.err.println("No response to " + commandUri);
			}
			e.consume();
		}
	}
	public void keyReleased(KeyEvent e) {};
	public void keyTyped(KeyEvent e) {}
}
