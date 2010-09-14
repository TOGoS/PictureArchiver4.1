package togos.picturearchiver4_1.util;

import java.io.File;

public class FileUtil
{
	public static void rmdir(File f) {
		if( !f.exists() ) return; 
		if( f.isDirectory() ) {
			File[] fs = f.listFiles();
			for( int i=fs.length-1; i>=0; --i ) {
				rmdir(fs[i]);
			}
		}
		f.delete();
	}
	
	public static boolean mkdirs(File d) {
		if( d != null && !d.exists() ) {
			if( !d.mkdirs() ) {
				throw new RuntimeException("Couldn't create dir " + d);
			}
			return true;
		}
		return false;
	}
	
	public static boolean mkParentDirs(File f) {
		File d = f.getParentFile();
		return mkdirs(d);
	}
}
