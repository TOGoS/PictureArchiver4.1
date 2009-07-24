package togos.picturearchiver4_1;

import java.util.Map;

public interface ResourceUpdateEvent {
	public String getResourceUri();
	public boolean isContentChanged();
	public Map getChangedMetadata();
}
