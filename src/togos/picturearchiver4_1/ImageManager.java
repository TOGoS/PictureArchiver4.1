package togos.picturearchiver4_1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class ImageManager {
	public static final String NS = "http://ns.nuke24.net/PictureArchiver4.1/";
	public static final String DOESNOTEXIST = NS + "doesNotExist";
	public static final String ISARCHIVED = NS + "isArchived";
	public static final String ISDELETED = NS + "isDeleted";
	public static final String ISMODIFIEDFROMORIGINAL = NS + "isModifiedFromOriginal";
	public static final String SUBJECTTAGS = NS + "subjectTags";
		
	public HashMap archiveDirectories = new HashMap();

	protected HashSet resourceUpdateListeners = new HashSet();
	public void addResourceUpdateListener( ResourceUpdateListener l ) {
		resourceUpdateListeners.add(l);
	}
	public void removeResourceUpdateListener( ResourceUpdateListener l ) {
		resourceUpdateListeners.remove(l);
	}
	
	public boolean isArchived(String uri) {
		return Math.random() > 0.5;
	}

	public boolean isDeleted(String uri) {
		return Math.random() > 0.5;
	}

	public boolean isModified(String uri) {
		return Math.random() > 0.5;
	}
	
	public Map loadMetadata( String fakeUri ) {
		HashMap metadata = new HashMap();
		metadata.put(ISDELETED, Boolean.valueOf(isDeleted(fakeUri)));
		metadata.put(ISARCHIVED, Boolean.valueOf(isArchived(fakeUri)));
		metadata.put(ISMODIFIEDFROMORIGINAL, Boolean.valueOf(isModified(fakeUri)));
		return metadata;
	}
	
	//

	protected void resourceUpdated( String fakeUri, String key, Object value ) {
		HashMap newMetadata = new HashMap();
		newMetadata.put(key,value);
		BaseResourceUpdateEvent evt = new BaseResourceUpdateEvent( fakeUri, false, newMetadata );
		for( Iterator i=resourceUpdateListeners.iterator(); i.hasNext(); ) {
			((ResourceUpdateListener)i.next()).resourceUdated(evt);
		}
	}
	
	public void undelete(String fakeUri) {
		resourceUpdated(fakeUri, ISDELETED, Boolean.FALSE);
	}

	public void delete(String fakeUri) {
		resourceUpdated(fakeUri, ISDELETED, Boolean.TRUE);
	}

	public void unarchive(String fakeUri) {
		System.err.println("Unarchive " + fakeUri);
		resourceUpdated(fakeUri, ISARCHIVED, Boolean.FALSE);
	}

	public void archive(String fakeUri) {
		System.err.println("Archive " + fakeUri);
		resourceUpdated(fakeUri, ISARCHIVED, Boolean.TRUE);
	}

	public void rotateRight(String fakeUri) {
		resourceUpdated(fakeUri, ISMODIFIEDFROMORIGINAL, Boolean.TRUE);
	}

	public void rotateLeft(String fakeUri) {
		resourceUpdated(fakeUri, ISMODIFIEDFROMORIGINAL, Boolean.TRUE);
	}
	
	public void restoreOriginal(String fakeUri) {
		resourceUpdated(fakeUri, ISMODIFIEDFROMORIGINAL, Boolean.FALSE);
	}

	public void saveTags(String fakeUri, String tags) {
		resourceUpdated(fakeUri, SUBJECTTAGS, tags);
	}
}
