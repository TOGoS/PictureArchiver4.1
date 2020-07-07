package togos.picturearchiver4_1;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import togos.picturearchiver4_1.util.*;

public class ImageManager
{
	ImageCompressor imageCompressor = new ImageCompressor(ImageCompressor.STANDARD_COMPRESSION_LEVELS);
	
	enum FlipDirection {
		HORIZONTAL("horizontal"),
		VERTICAL("vertical");

		public final String jpegTranName;
		FlipDirection(String jpegTranName) {
			this.jpegTranName = jpegTranName;
		}
	}
	
	public static final String NS = "http://ns.nuke24.net/PictureArchiver4.1/";
	public static final String DOESNOTEXIST = NS + "doesNotExist";
	public static final String ISARCHIVED = NS + "isArchived";
	public static final String ISDELETED = NS + "isDeleted";
	public static final String ISMODIFIEDFROMORIGINAL = NS + "isModifiedFromOriginal";
	public static final String SUBJECTTAGS = NS + "subjectTags";
	public static final String FILESIZE = "http://bitzi.com/xmlns/2002/01/bz-core#fileLength";
	
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
			StatusLog.log("Reading "+url);
			return ImageIO.read(new URL(url));
		} catch( IOException e ) {
			return null;
		}
	}
	
	protected static String getFilePath( String uri, boolean errorOnNonFileUri) {
		if( uri == null ) {
			return null;
		} else if( uri.startsWith("file:" ) ) {
			return PathUtil.parseFilePathOrUri(uri).toString();
		} else if( errorOnNonFileUri ) {
			throw new RuntimeException("Not a file: URI: " + uri);
		} else {
			return null;
		}
	}
	
	public static List getList( Object o ) {
		if( o == null ) return null;
		if( o instanceof List ) return (List)o;
		if( o instanceof String ) o = ((String)o).split(",\\s*");
		if( o instanceof Object[] ) return Arrays.asList((Object[])o);
		throw new RuntimeException("Don't know how to convert " + o.getClass().getName() + " to List");
	}
	
	public static String getString( Object o ) {
		if( o == null ) return null;
		if( o instanceof String ) return (String)o;
		if( o instanceof List ) o = ((List)o).toArray();
		if( o instanceof Object[] ) {
			String res = "";
			Object[] arr = (Object[])o;
			for( int i=0; i<arr.length; ++i ) {
				res += arr[i].toString();
				if( i<arr.length-1 ) res += ", ";
			}
			return res;
		}
		throw new RuntimeException("Don't know how to convert " + o.getClass().getName() + " to String");
	}
	
	protected static File getFile( String uri, boolean errorOnNonFileUri ) {
		String path = getFilePath(uri, errorOnNonFileUri);
		return path == null ? null : new File(path);
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
	
	/** foo/a.jpg -> foo/subDir/a.jpg */
	public static String munge( String uri, String subDir ) {
		int ls = uri.lastIndexOf('/');
		if( ls != -1 ) {
			return uri.substring(0,ls) + "/" + subDir + "/" + uri.substring(ls+1);
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
		if( fakeUri == null ) return null;
		String realUri = findRealUri(fakeUri);
		if( realUri != null ) {
			return new FoundResource( realUri, getImage(realUri) );
		}
		return null;
	}
	
	protected static void move(File src, File dest ) {
		if( src.getAbsolutePath().equals(dest.getAbsolutePath()) ) return;
		FileUtil.mkParentDirs(dest);
		if (!src.renameTo(dest)) {
			throw new RuntimeException("Failed to move "+src+" to "+dest);
		}
	}
	
	public HashMap archiveDirectoryUriMap = new HashMap();
	public boolean touchingEnabled = true;

	protected HashSet resourceUpdateListeners = new HashSet();
	public void addResourceUpdateListener( ResourceUpdateListener l ) {
		resourceUpdateListeners.add(l);
	}
	public void removeResourceUpdateListener( ResourceUpdateListener l ) {
		resourceUpdateListeners.remove(l);
	}
	
	protected String getDirectoryUri( String uri ) {
		int ls = uri.lastIndexOf('/');
		if( ls != -1 ) {
			return uri.substring(0,ls+1);
		}
		return null;
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
	
	protected File getTagFile( String fakeUri ) {
		String directoryUri = getDirectoryUri(fakeUri);
		String archiveDirUri = getArchiveUri(directoryUri);
		if( archiveDirUri != null ) directoryUri = archiveDirUri;
		return getFile(directoryUri + ".pa4-tags", true);
	}
	
	public TreeMap loadAllTags( File tagFile ) {
		try {
			TreeMap fileTags = new TreeMap();
			if( !tagFile.exists() ) return fileTags;
			BufferedReader r = new BufferedReader(new FileReader(tagFile));
			try {
				String line;
				while( (line = r.readLine()) != null ) {
					line = line.trim();
					if( line.length() == 0 ) continue;
					if( line.startsWith("#") ) continue;
					String[] partz = line.split("\t");
					
					String file = null;
					String tags = null;
					for( int i=0; i<partz.length; ++i ) {
						String uri = partz[i];
						if( uri.indexOf(':') == -1 || uri.startsWith("file:")) {
							file = uri;
						} else if( uri.startsWith("x-metadata:") ) {
							String md = uri.substring(11);
							String[] mdParts = md.split(";");
							for( int j=0; j<mdParts.length; ++j ) {
								String mdPart = mdParts[j];
								if( mdPart.startsWith("tags=") ) {
									tags = (tags == null ? "" : tags + ", ") + UriUtil.uriDecode(mdPart.substring(5));
								}
							}
						}
					}
					if( file != null && tags != null ) {
						String ft = (String)fileTags.get(file);
						fileTags.put(file, ft == null ? tags : ft + ", " + tags );
					}
				}
				return fileTags;
			} finally {
				r.close();
			}
		} catch( IOException e1 ) {
			throw new RuntimeException(e1);
		}
	}
	
	public void saveAllTags( File tagFile, Map tags ) {
		try {
			FileUtil.mkParentDirs(tagFile);
			PrintWriter w = new PrintWriter(new FileWriter(tagFile));
			for( Iterator i=tags.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry e = (Map.Entry)i.next();
				w.println((String)e.getKey() + "\tx-metadata:tags=" + UriUtil.uriEncode((String)e.getValue()));
			}
			w.close();
		} catch( IOException e1 ) {
			throw new RuntimeException(e1);
		}
	}
	
	public String loadTags( String fakeUri ) {
		File tagFile = getTagFile(fakeUri);
		StatusLog.log("Loading tags from "+tagFile);
		Map allTags = loadAllTags(tagFile);
		return (String)allTags.get(fakeUri.substring(fakeUri.lastIndexOf('/')+1));
	}
	
	public Map loadMetadata( String fakeUri ) {
		HashMap metadata = new HashMap();
		metadata.put(ISDELETED, Boolean.valueOf(isDeleted(fakeUri)));
		metadata.put(ISARCHIVED, Boolean.valueOf(isArchived(fakeUri)));
		metadata.put(ISMODIFIEDFROMORIGINAL, Boolean.valueOf(isModified(fakeUri)));
		metadata.put(SUBJECTTAGS, loadTags(fakeUri) );

		File f = getFile(fakeUri, false);
		if( f != null ) metadata.put(FILESIZE, Long.valueOf(f.length()));
		
		return metadata;
	}
	
	//
	
	protected void resourceUpdated( BaseResourceUpdateEvent evt ) {
		for( Iterator i=resourceUpdateListeners.iterator(); i.hasNext(); ) {
			((ResourceUpdateListener)i.next()).resourceUpdated(evt);
		}
	}
	
	protected void resourceUpdated( String fakeUri, boolean originalUpdated, String key, Object value ) {
		HashMap newMetadata = new HashMap();
		newMetadata.put(key,value);
		resourceUpdated(new BaseResourceUpdateEvent( fakeUri, originalUpdated, newMetadata ));
	}

	protected void resourceUpdated( String fakeUri, String key, Object value ) {
		resourceUpdated(fakeUri, false, key, value);
	}
	
	/** Updated in unspecified ways.  Reload the whole thing. */
	protected void resourceUpdated( String fakeUri ) {
		resourceUpdated(new BaseResourceUpdateEvent(fakeUri, true, loadMetadata(fakeUri)));
	}
	
	protected void directoryUpdated( File dir ) {
		if( touchingEnabled ) while( dir != null ) {
			File ccUriFile = new File(dir + "/.ccouch-uri");
			if( ccUriFile.exists() ) ccUriFile.delete();
			dir = dir.getParentFile();
		}
	}
	
	protected void fileUpdated( File f ) {
		File pDir = f.getParentFile();
		if( pDir != null ) directoryUpdated(pDir);
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
		fileUpdated(normalFile);
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
		fileUpdated(deletedFile);
		resourceUpdated(fakeUri, ISDELETED, Boolean.TRUE);
	}
	
	public void unarchive(String fakeUri) {
		File archiveFile = getFile(getArchiveUri(fakeUri), true);
		if( archiveFile != null ) archiveFile.delete();
		fileUpdated(archiveFile);
		resourceUpdated(fakeUri, ISARCHIVED, Boolean.FALSE);
	}
	
	public void archive(String fakeUri) {
		String archiveUri = getArchiveUri(fakeUri);
		File archiveFile = getFile(archiveUri, true);
		if( archiveFile != null ) {
			File srcFile = getFile(findRealUri(fakeUri), true);
			if( srcFile != null ) {
				FileUtil.mkParentDirs(archiveFile);
				Linker.getInstance().link(srcFile, archiveFile);
			}
		}
		if( archiveFile == null ) {
			System.err.println("Couldn't determine archive file for fakeUri="+fakeUri+", archiveUri="+archiveUri);
			return;
		}
		fileUpdated(archiveFile);
		resourceUpdated(fakeUri, ISARCHIVED, Boolean.TRUE);
	}
	
	/**
	 * Ensure the file is backed up to .originals.
	 * @return the current version if it exists, otherwise the original
	 */
	protected File getBackedUpSource(String fakeUri) {
		File normalFile = getFile(findRealUri(fakeUri),true);
		File originalFile = getFile(munge(fakeUri,".originals"),true);
		if( normalFile.exists() && originalFile.exists() ) {
			return normalFile;
		} else if( normalFile.exists() && !originalFile.exists() ) {
			move( normalFile, originalFile );
			return originalFile;
		} else if( !normalFile.exists() && originalFile.exists() ) {
			return originalFile;
		} else {
			throw new RuntimeException("Can't find backed-up original file: " + fakeUri);
		}
	}
	
	/** Return an uncompressed version.  For now this always returns the original.
	 * Which means you need to do rotations, etc *after* compressing. */
	protected File getUncompressedSource(String fakeUri) {
		File normalFile = getFile(findRealUri(fakeUri),true);
		File originalFile = getFile(munge(fakeUri,".originals"),true);
		if( originalFile.exists() ) {
			return originalFile;
		} else if( normalFile.exists() && !originalFile.exists() ) {
			move( normalFile, originalFile );
			return originalFile;
		} else {
			throw new RuntimeException("Can't find backed-up original file: " + fakeUri);
		}
	}

	protected File getOriginal(String fakeUri) {
		File originalFile = getFile(munge(fakeUri,".originals"),true);
		if( originalFile.exists() ) return originalFile;
		File realFile = getFile(findRealUri(fakeUri),true);
		if( realFile.exists() ) return realFile;
		return null;
	}
	
	public void jpegtranRotate(String fakeUri, int degrees) {
		File src = getBackedUpSource(fakeUri);
		File dest = getFile(fakeUri,true);
		try {
			SystemUtil.runCommand(new String[]{"jpegtran","-rotate",String.valueOf(degrees),"-outfile",dest.getAbsolutePath(),src.getAbsolutePath()});
		} catch( SystemUtil.ShellCommandError e ) {
			throw new RuntimeException(e);
		}
		dest.setLastModified(src.lastModified());
		fileUpdated(dest);
		resourceUpdated(fakeUri, true, ISMODIFIEDFROMORIGINAL, Boolean.TRUE);
	}
	
	public void jpegtranFlip(String fakeUri, FlipDirection direction) {
		File src = getBackedUpSource(fakeUri);
		File dest = getFile(fakeUri,true);
		try {
			dest.delete(); // JPEGTran may write a new file rather than overwriting, but just to be sure...
			SystemUtil.runCommand(new String[]{"jpegtran", "-flip", String.valueOf(direction.jpegTranName), "-outfile", dest.getAbsolutePath(), src.getAbsolutePath()});
		} catch( SystemUtil.ShellCommandError e ) {
			throw new RuntimeException(e);
		}
		dest.setLastModified(src.lastModified());
		fileUpdated(dest);
		resourceUpdated(fakeUri, true, ISMODIFIEDFROMORIGINAL, Boolean.TRUE);
	}
	
	public void rotateRight(String fakeUri) {
		jpegtranRotate(fakeUri, 90);
	}
	
	public void rotateLeft(String fakeUri) {
		jpegtranRotate(fakeUri, 270);
	}

	public void flipHorizontal(String fakeUri) {
		jpegtranFlip(fakeUri, FlipDirection.HORIZONTAL);
	}
	
	public void flipVertical(String fakeUri) {
		jpegtranFlip(fakeUri, FlipDirection.VERTICAL);
	}
	
	public void compressAgain(String fakeUri) {
		File src = getUncompressedSource(fakeUri);
		File dest = getFile(fakeUri,true);

		try {
			imageCompressor.compressAgaion(src, dest);
			fileUpdated(dest);
			resourceUpdated(fakeUri);
		} catch( ImageCompressor.CompressionError e ) {
			restoreOriginal(fakeUri);
			throw new RuntimeException(e);
		} catch( ImageCompressor.CouldNotCompressFurther e ) {
			System.err.println("Could not compress further");
		}
	}
	
	public void compressToUnder(String fakeUri, long targetSize) {
		File src = getUncompressedSource(fakeUri);
		File dest = getFile(fakeUri,true);
		
		try {
			imageCompressor.compressToUnder(src, dest, targetSize);
			fileUpdated(dest);
			resourceUpdated(fakeUri);
		} catch( ImageCompressor.CompressionError e ) {
			restoreOriginal(fakeUri);
			throw new RuntimeException(e);
		} catch( ImageCompressor.CouldNotCompressFurther e ) {
			System.err.println("Could not compress further");
		}
	}
	
	
	public void restoreOriginal(String fakeUri) {
		File originalFile = getFile(munge(fakeUri,".originals"),true);
		File normalFile = getFile(fakeUri,true);
		if( originalFile.exists() ) {
			if( normalFile.exists() ) normalFile.delete();
			System.err.println("Mv " + originalFile + " " + normalFile);
			move(originalFile,normalFile);
		}
		fileUpdated(normalFile);
		resourceUpdated(fakeUri, true, ISMODIFIEDFROMORIGINAL, Boolean.FALSE);
	}

	public void saveTags(String fakeUri, String tags) {
		File tagFile = getTagFile(fakeUri);
		Map allTags = loadAllTags(tagFile);
		String tagKey = fakeUri.substring(fakeUri.lastIndexOf('/')+1);
		allTags.put(tagKey, tags);
		saveAllTags(tagFile, allTags);
	}
}
