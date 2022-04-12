package togos.picturearchiver4_1;

import togos.picturearchiver4_1.util.SystemUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ImageCompressor {
	/** Compression had an error;  Target file may be corrupted or not exist */
	static class CompressionError extends Exception {
		public CompressionError(String message) {
			super(message);
		}
		public CompressionError(String message, Throwable cause) {
			super(message, cause);
		}
	}
	/** Compressed file created, but not under the requested size */
	static class CouldNotCompressFurther extends Exception {
		public CouldNotCompressFurther(String message) {
			super(message);
		}
	}
	
	
	static class CompressionLevel {
		/** Integer from 0-100 indicating image quality in some unspecified way */
		int quality;
		/** Maximum width and height of resulting image, in pixels. */
		int maxWidth, maxHeight;
		
		public CompressionLevel(int quality, int maxWidth, int maxHeight) {
			this.quality = quality;
			this.maxHeight = maxHeight;
			this.maxWidth = maxWidth;
		}
	}
	
	static class CompressionResult {
		File original;
		File target;
		CompressionLevel level;
		long targetSize;
		
		public CompressionResult(File original, File target, CompressionLevel level, long targetSize) {
			this.original = original;
			this.target = target;
			this.level = level;
			this.targetSize = targetSize;
		}
	}
	
	public static List<CompressionLevel> STANDARD_COMPRESSION_LEVELS = Arrays.asList(new CompressionLevel[] {
		new CompressionLevel(85,8192,8192 ),
		new CompressionLevel(75,2048,2048 ),
		new CompressionLevel(75,1536,1536 ),
		new CompressionLevel(75,1024,1024 ),
		new CompressionLevel(65,1024,1024 ),
		new CompressionLevel(55,1024,1024 ),
	});
	
	List<CompressionLevel> compressionLevels;
	/**
	 * UmageMagick-like command to use to convert images.
	 * "gm" or "magick" or whatever, so long as it supports out convert commands.
	 */
	String magickExe = null;
	
	public ImageCompressor(List<CompressionLevel> compressionLevels, String magickExe) {
		this.compressionLevels = compressionLevels;
		this.magickExe = magickExe;
	}
	
	CompressionResult lastCompression;
	
	public CompressionResult compress(File original, File target, CompressionLevel level) throws CompressionError {
		// 195901-20200520_195901.jpg -interlace Plane -gaussian-blur 0.05 -resize "1536x1536>" -quality 85% 195901-20200520_195901.smaller2.jpg
		try {
			if( original.equals(target) ) {
				throw new RuntimeException("original file == target: "+original);
			}
			// Make sure we're writing a new file, not rewriting an existing one!
			// Might be better to do the write-to-temp-file-then-move trick
			// so that if this fails, the file hasn't disappeared
			// (though it *should* be backed up, right?)
			target.delete();
			if( magickExe == null ) {
				throw new RuntimeException("Please set ImageCompressor#magickCommand");
			}
			SystemUtil.runCommand(new String[]{
				magickExe, "convert", original.getPath(),
				"-interlace", "Plane",
				"-gaussian-blur", "0.05",
				"-resize", level.maxWidth + "x" + level.maxHeight + ">",
				"-quality", String.valueOf(level.quality),
				target.getPath()
			});
		} catch( SystemUtil.ShellCommandError e ) {
			throw new CompressionError("Shell command failed", e);
		}
		return lastCompression = new CompressionResult(original, target, level, target.length());
	}
	
	public CompressionResult compressToUnder(File original, File target, long targetSize) throws CompressionError, CouldNotCompressFurther {
		if (targetSize == 0) throw new CouldNotCompressFurther("Refusing to attempt compression to zero bytes!");
		// TODO: Could use lastCompression to skip steps
		for (CompressionLevel l : compressionLevels) {
			CompressionResult r = compress(original, target, l);
			if (r.targetSize <= targetSize) return r;
		}
		throw new CouldNotCompressFurther("Couldn't compress "+original+" to <= "+targetSize);
	}
	
	/** Compress original somewhat to create target.
	 * If target already exists, compresses until size is less than that of target */
	public CompressionResult compressAgaion(File original, File target) throws CompressionError, CouldNotCompressFurther {
		long targetSize = target.exists() ? target.length()-1 : original.length()-1;
		return compressToUnder(original, target, targetSize);
	}
}
