package togos.picturearchiver4_1;

import java.util.Map;

public class BaseResourceUpdateEvent implements ResourceUpdateEvent {
	protected String uri;
	protected boolean contentChanged;
	protected Map newMetadata;
	
	public BaseResourceUpdateEvent( String uri, boolean contentChanged, Map newMetadata ) {
		this.uri = uri;
		this.contentChanged = contentChanged;
		this.newMetadata = newMetadata;
	}
	
	public Map getChangedMetadata() {
		return newMetadata;
	}

	public String getResourceUri() {
		return uri;
	}

	public boolean isContentChanged() {
		return contentChanged;
	}

}
