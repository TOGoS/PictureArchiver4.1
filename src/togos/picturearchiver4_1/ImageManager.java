package togos.picturearchiver4_1;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import contentcouch.app.Linker;
import contentcouch.file.FileUtil;
import contentcouch.path.PathUtil;

public class ImageManager {
	public static final String NS = "http://ns.nuke24.net/PictureArchiver4.1/";
	public static final String DOESNOTEXIST = NS + "doesNotExist";
	public static final String ISARCHIVED = NS + "isArchived";
	public static final String ISDELETED = NS + "isDeleted";
	public static final String ISMODIFIEDFROMORIGINAL = NS + "isModifiedFromOriginal";
	public static final String SUBJECTTAGS = NS + "subjectTags";
		
	static class FoundResource {
		public FoundResource( String uri, Object content ) {
			this.uri = uri;
			this.content = content;
		}
		public Object content;
		public String uri;
	}
	
	public static Image getImage( String url ) {
		try {
			return ImageIO.read(new URL(url));
		} catch( IOException e ) {
			return null;
		}
	}
	
	protected static File getFile( String uri, boolean errorOnNonFileUri ) {
		if( uri == null ) {
			return null;
		} else if( uri.startsWith("file:" ) ) {
			return new File(PathUtil.parseFilePathOrUri(uri).toString());
		} else if( errorOnNonFileUri ) {
			throw new RuntimeException("Not a file: URI: " + uri);
		}
		return null;
	}

	public static String getFileUri( String path ) {
		return PathUtil.maybeNormalizeFileUri(path);
	}

	public static String getFileUri( File f ) {
		return getFileUri(f.getPath());
	}
	
	public static boolean exists( String uri ) {
		if( uri == null ) return false;
		return getFile(uri, true).exists();
	}
	
	public static String munge( String uri, String insert ) {
		int ls = uri.lastIndexOf('/');
		if( ls != -1 ) {
			return uri.substring(0,ls) + "/" + insert + "/" + uri.substring(ls+1);
		}
		return null;
	}
	
	public static String findRealUri( String fakeUri ) {
		if( exists(fakeUri) ) return fakeUri;
		
		String realUri;
			
		realUri = munge( fakeUri, ".deleted" );
		if( exists(realUri) ) return realUri;
			
		realUri = munge( fakeUri, ".originals" );
		if( exists(realUri) ) return realUri;

		return null;
	}
	
	public static FoundResource findImage( String fakeUri ) {
		String realUri = findRealUri(fakeUri);
		if( realUri != null ) {
			return new FoundResource( realUri, getImage(realUri) );
		}
		return null;
	}
	
	protected static void move( File src, File dest ) {
		if( src.getAbsolutePath().equals(dest.getAbsolutePath()) ) return;
		FileUtil.mkParentDirs(dest);
		src.renameTo(dest);
	}

	protected static String join( String[] parts, String sep ) {
		String res = "";
		for( int i=0; i<parts.length; ++i ) {
			res += parts[i];
			if( i<parts.length-1 ) res += sep;
		}
		return res;
	}
	
	protected static void sys( String[] argv ) {
		Process lnProc;
		try {
			lnProc = Runtime.getRuntime().exec(argv);
			int lnProcReturn = lnProc.waitFor();
			if( lnProcReturn != 0 ) {
				throw new RuntimeException("External program returned " + lnProcReturn + ": " + join(argv," "));
			}
		} catch( Exception e) {
			throw new RuntimeException("Exception while running " + join(argv," "), e);
		}

	}
	
	public HashMap archiveDirectoryUriMap = new HashMap();

	protected HashSet resourceUpdateListeners = new HashSet();
	public void addResourceUpdateListener( ResourceUpdateListener l ) {
		resourceUpdateListeners.add(l);
	}
	public void removeResourceUpdateListener( ResourceUpdateListener l ) {
		resourceUpdateListeners.remove(l);
	}
	
	public String getArchiveUri( String fakeUri ) {
		String longestMatch = null;
		for( Iterator i=archiveDirectoryUriMap.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			if( fakeUri.startsWith((String)e.getKey()) ) {
				if( longestMatch == null || ((String)e.getKey()).length() > longestMatch.length() ) {
					longestMatch = (String)e.getKey();
				}
			}
		}
		if( longestMatch != null ) {
			return archiveDirectoryUriMap.get(longestMatch) + fakeUri.substring(longestMatch.length());
		}
		return null;
	}
	
	public boolean isArchived(String fakeUri) {
		return exists(getArchiveUri(fakeUri));
	}

	public boolean isDeleted(String uri) {
		String realUri = findRealUri(uri);
		return realUri != null && realUri.indexOf("/.deleted/") != -1;
	}

	public boolean isModified(String uri) {
		return exists(munge(uri,".originals"));
	}
	
	public Map loadMetadata( String fakeUri ) {
		HashMap metadata = new HashMap();
		metadata.put(ISDELETED, Boolean.valueOf(isDeleted(fakeUri)));
		metadata.put(ISARCHIVED, Boolean.valueOf(isArchived(fakeUri)));
		metadata.put(ISMODIFIEDFROMORIGINAL, Boolean.valueOf(isModified(fakeUri)));
		return metadata;
	}
	
	//

	protected void resourceUpdated( String fakeUri, boolean originalUpdated, String key, Object value ) {
		HashMap newMetadata = new HashMap();
		newMetadata.put(key,value);
		BaseResourceUpdateEvent evt = new BaseResourceUpdateEvent( fakeUri, originalUpdated, newMetadata );
		for( Iterator i=resourceUpdateListeners.iterator(); i.hasNext(); ) {
			((ResourceUpdateListener)i.next()).resourceUdated(evt);
		}
	}

	protected void resourceUpdated( String fakeUri, String key, Object value ) {
		resourceUpdated(fakeUri, false, key, value);
	}
	
	public void undelete(String fakeUri) {
		String deletedUri = findRealUri( fakeUri );
		File normalFile = getFile(fakeUri,true);
		File deletedFile = getFile(deletedUri,true);
		if( deletedFile.equals(fakeUri) ) {
		} else if( normalFile.exists() ) {
			//deletedFile.delete();
		} else {
			deletedFile.renameTo(normalFile);
		}
		resourceUpdated(fakeUri, ISDELETED, Boolean.FALSE);
	}

	public void delete(String fakeUri) {
		String deletedUri = munge(fakeUri,".deleted");
		String realUri = findRealUri(fakeUri);
		File deletedFile = getFile(deletedUri,true);
		File realFile = getFile(realUri,true);
		if( realFile.getAbsolutePath().equals(deletedFile.getAbsolutePath()) ) {
		} else if( realFile.exists() ) {
			 if( deletedFile.exists() ) deletedFile.delete();
			FileUtil.mkParentDirs(deletedFile);
			realFile.renameTo(deletedFile);
		}
		resourceUpdated(fakeUri, ISDELETED, Boolean.TRUE);
	}

	public void unarchive(String fakeUri) {
		File archiveFile = getFile(getArchiveUri(fakeUri), true);
		if( archiveFile != null ) archiveFile.delete();
		resourceUpdated(fakeUri, ISARCHIVED, Boolean.FALSE);
	}

	public void archive(String fakeUri) {
		File archiveFile = getFile(getArchiveUri(fakeUri), true);
		if( archiveFile != null ) {
			File srcFile = getFile(findRealUri(fakeUri), true);
			if( srcFile != null ) {
				FileUtil.mkParentDirs(archiveFile);
				Linker.getInstance().link(srcFile, archiveFile);
			}
		}
		resourceUpdated(fakeUri, ISARCHIVED, Boolean.TRUE);
	}
	
	protected File getBackedUpSource(String fakeUri, boolean errorOnNotFound) {
		File normalFile = getFile(findRealUri(fakeUri),true);
		File originalFile = getFile(munge(fakeUri,".originals"),true);
		if( normalFile.exists() && originalFile.exists() ) {
			return normalFile;
		} else if( normalFile.exists() && !originalFile.exists() ) {
			move( normalFile, originalFile );
			return originalFile;
		} else if( !normalFile.exists() && originalFile.exists() ) {
			return originalFile;
		} else if( errorOnNotFound ) {
			throw new RuntimeException("Can't find backed-up original file: " + fakeUri);
		} else {
			return null;
		}
	}

	protected File getOriginal(String fakeUri) {
		File originalFile = getFile(munge(fakeUri,".originals"),true);
		if( originalFile.exists() ) return originalFile;
		File realFile = getFile(findRealUri(fakeUri),true);
		if( realFile.exists() ) return realFile;
		return null;
	}
	
	public void rotateRight(String fakeUri) {
		File src = getBackedUpSource(fakeUri,true);
		File dest = getFile(fakeUri,true);
		sys(new String[]{"jpegtran","-rotate","90",src.getAbsolutePath(),dest.getAbsolutePath()});
		dest.setLastModified(src.lastModified());
		resourceUpdated(fakeUri, true, ISMODIFIEDFROMORIGINAL, Boolean.TRUE);
	}
	
	public void rotateLeft(String fakeUri) {
		File src = getBackedUpSource(fakeUri,true);
		File dest = getFile(fakeUri,true);
		sys(new String[]{"jpegtran","-rotate","270",src.getAbsolutePath(),dest.getAbsolutePath()});
		dest.setLastModified(src.lastModified());
		resourceUpdated(fakeUri, true, ISMODIFIEDFROMORIGINAL, Boolean.TRUE);
	}
	
	public void restoreOriginal(String fakeUri) {
		File originalFile = getFile(munge(fakeUri,".originals"),true);
		File normalFile = getFile(fakeUri,true);
		if( originalFile.exists() ) {
			if( normalFile.exists() ) normalFile.delete();
			System.err.println("Mv " + originalFile + " " + normalFile);
			move(originalFile,normalFile);
		}
		resourceUpdated(fakeUri, true, ISMODIFIEDFROMORIGINAL, Boolean.FALSE);
	}

	public void saveTags(String fakeUri, String tags) {
		resourceUpdated(fakeUri, SUBJECTTAGS, tags);
	}
}