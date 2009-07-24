package togos.picturearchiver4_1.comframework;

import togos.rra.Request;

public interface CommandHandler {
	public CommandResponseStream handleCommand( Request command );
}
