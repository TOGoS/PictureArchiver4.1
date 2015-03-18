// -*- tab-width:4 -*-
package togos.picturearchiver4_1.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public abstract class Linker
{
	public static class LinkException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public LinkException( String from, String to, String problem ) {
			super( "Failed to create link from " + from + " to " + to + ": " + problem );
		}
		public LinkException( String from, String to, Exception e ) {
			super( "Failed to create link from " + from + " to " + to , e );
		}
		public LinkException( File from, File to, String problem ) {
			this( from.getPath(), to.getPath(), problem );
		}
		public LinkException( File from, File to, Exception e ) {
			this( from.getPath(), to.getPath(), e );
		}
	}
	
	public static class FSUtilLinker extends Linker {
		public void link( File target, File link ) {
			try {
				if( link.exists() ) throw new LinkException(target, link, link.getPath() + " already exists");
				Process lnProc = Runtime.getRuntime().exec(new String[] {"fsutil", "hardlink", "create", link.getPath(), target.getCanonicalPath()});
				int lnProcReturn = lnProc.waitFor();
				if( !link.exists() ) {
					throw new LinkException( link, target, "link does not exist after running 'fsutil hardlink create ...', which returned " + lnProcReturn);
				}
			} catch( InterruptedException e ) {
				throw new LinkException( link, target, e );
			} catch( IOException e ) {
				throw new LinkException( link, target, e );
			}
		}
	}
	
	public static class CopyLinker extends Linker {
		public void link(File source, File dest) {
			try {
				String tmpPrefix = ".tmp-";
				String tmpSuffix = source.getName();
				File destDir = dest.getParentFile();
				if( destDir == null ) destDir = new File("."); // Or something
				File tempFile = File.createTempFile(tmpPrefix, tmpSuffix, destDir);
				long size = source.length();
				FileInputStream fis = new FileInputStream(source);
				try {
					FileOutputStream fos = new FileOutputStream(tempFile);
					long bytesWritten = 0;
					try {
						int r;
						byte[] buffer = new byte[1024*1024];
						while( (r = fis.read(buffer)) > 0 ) {
							fos.write(buffer, 0, r);
							bytesWritten += r;
						}
					} finally {
						fos.close();
					}
					if( bytesWritten != size ) {
						throw new LinkException(dest, source, "Only wrote "+bytesWritten+" of expected "+size+" bytes to "+tempFile);
					}
					long tempSize = tempFile.length();
					if( tempSize != size ) {
						throw new LinkException(dest, source, tempFile+" seems to be the wrogn size: "+tempSize+"/"+size+" bytes");
					}
					// Fix metadata...
					tempFile.setLastModified(source.lastModified());
					tempFile.setReadOnly();
					if( !tempFile.renameTo(dest) ) {
						throw new LinkException(dest, source, "Failed to rename "+tempFile+" to "+dest);
					}
					if( !dest.exists() ) {
						throw new LinkException(dest, source, dest+" does not exist after renaming "+tempFile+" to it");
					}
				} finally {
					fis.close();
					tempFile.delete();
				}
			} catch( IOException e ) {
				throw new LinkException(dest, source, e);
			}
		}
	}
	
	public static class LnLinker extends Linker {
		public void link( File target, File link ) {
			try {
				if( link.exists() ) throw new LinkException(target, link, link.getPath() + " already exists");
				Process lnProc = Runtime.getRuntime().exec(new String[] {"ln", target.getCanonicalPath(), link.getPath()});
				int lnProcReturn = lnProc.waitFor();
				if( !link.exists() ) {
					throw new LinkException( link, target, "link does not exist after running 'ln ...', which returned " + lnProcReturn);
				}
			} catch( InterruptedException e ) {
				throw new LinkException( link, target, e );
			} catch( IOException e ) {
				throw new LinkException( link, target, e );
			}
		}
	}
	
	public static Linker instance;
	public static Linker getInstance() {
		if( instance == null ) {
			String whichOS = System.getProperty("os.name");
			if( whichOS.indexOf("Windows") != -1 ) {
				return new FSUtilLinker();
			} else {
				return new LnLinker();
			}
		}
		return instance;
	}
	
	public abstract void link( File target, File link );
	
	/** Same as -link unless the link already exists, in which case we attempt to replace it through
	 * a series of renames so as not to delete anything if the link cannot be made. */
	public void relink( File target, File link ) {
		if( link.exists() ) {
			String linkParentPath = link.getParent();
			if( linkParentPath == null || linkParentPath.length() == 0 ) {
				linkParentPath = ".";
			} else {
				linkParentPath += "/.";
			}
			File lnTemp = new File(linkParentPath + link.getName() + ".cc-ln-temp");
			if( lnTemp.exists() ) {
				lnTemp.delete();
			}
			
			link( target, lnTemp );
			if( link.exists() && !link.delete() ) {
				lnTemp.delete();
				throw new LinkException(target, link, "Could not delete old file to replace with link");
			}
			if( !lnTemp.renameTo(link) ) {
				throw new LinkException(target, link, "Could not rename temporary link to final location");
			}
		} else {
			link( target, link );
		}
	}

	
	public static void main( String[] args ) {
		String whichOS = System.getProperty("os.name");
		System.out.println("You're running " + whichOS);
	}
}
