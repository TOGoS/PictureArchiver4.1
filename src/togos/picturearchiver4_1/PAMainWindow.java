package togos.picturearchiver4_1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.value.Arguments;
import togos.picturearchiver4_1.ImageManager.FoundResource;
import togos.picturearchiver4_1.comframework.BaseCommandHandler;
import togos.picturearchiver4_1.comframework.MappedCommandHandler;
import togos.picturearchiver4_1.util.Linker;
import togos.picturearchiver4_1.util.PathUtil;
import togos.picturearchiver4_1.util.UriUtil;

public class PAMainWindow extends JFrame implements ResourceUpdateListener
{
	private static final long serialVersionUID = 1L;

	class ImagePanel extends DraggableScrollPane {
		private static final long serialVersionUID = 1L;
		
		InnerImagePanel innerPanel;
		float scale = 1.0f;
		
		public ImagePanel() {
			super();
			innerPanel = new InnerImagePanel();
			this.setViewportView(innerPanel);
		}
		
		public void validate() {
			innerPanel.setSize(innerPanel.getPreferredSize());
			super.validate();
		}
		
		public void setImage(Image i, float scale) {
			this.scale = scale;
			innerPanel.setImage(i, scale);
			validate();
		}
		
		public void setImageAutoscale(Image i) {
			float scaleX, scaleY;
			if( i != null ) {
				scaleX = (float)getViewport().getWidth() / i.getWidth(null);
				scaleY = (float)getViewport().getHeight() / i.getHeight(null);
			} else {
				scaleX = scaleY = 1.0f;
			}
			float scaleQ = (scaleX < scaleY ? scaleX : scaleY);
			setImage( i, scaleQ < 1.0 ? scaleQ : 1.0f );
		}
	}
	
	class State {
		int listIndex;
		String fakeUri;
		String realUri;
		Image image;
		Map<String,?> metadata;
		float scale;
	}
	
	ImageManager imageManager = new ImageManager();
	MappedCommandHandler requestSender = new MappedCommandHandler("/pa4/ui/");
	KeyCommandIssuer kci = new KeyCommandIssuer(requestSender);

	ImagePanel ip;
	JPanel textPanel;
	JPanel   titlePanel;
	JLabel     titleLabel;
	JPanel     pzPanel;
	JLabel       positionLabel;
	JLabel       dimensionsLabel;
	JLabel       zoomLabel;
	JLabel       fileSizeLabel;
	JPanel   tagsPanel;
	JLabel     tagsLabelLabel;
	JTextField tagsInput;
	JPanel   statusPanel;
	JLabel     statusLabelLabel;
	JPanel     statusSubPanel;
	JLabel       archivedLabel;
	JLabel       deletedLabel;
	JLabel       modifiedLabel;
	JLabel       doesNotExistLabel;

	List rootImageUriList;
	List imageUriList;
	State state = new State();
	float zoomUnit = 1.5f;
	
	protected void doCommand(String name) {
		Request req = new BaseRequest(RequestVerbs.POST, name);
		if( requestSender.call(req).getStatus() == ResponseCodes.UNHANDLED ) {
			StatusLog.log("Unhandled command <" + name + ">");
		}
	}
	
	public PAMainWindow() {
		super();
		
		imageManager.addResourceUpdateListener(this);
		
		requestSender.putHandler("goToDelta", new BaseCommandHandler() {
			public boolean _call(Request command) {
				goToModIndex(state.listIndex + ((Integer)((Arguments)command.getContent()).getPositionalArguments().get(0)).intValue());
				return true;
			}
		});
		requestSender.putHandler("goToNext", new BaseCommandHandler() {
			public boolean _call(Request command) {
				goToNext(); return true;
			}
		});
		requestSender.putHandler("goToPrevious", new BaseCommandHandler() {
			public boolean _call(Request command) {
				goToPrevious(); return true;
			}
		});
		requestSender.putHandler("goToFirst", new BaseCommandHandler() {
			public boolean _call(Request command) {
				goToIndex(0); return true;
			}
		});
		requestSender.putHandler("goToLast", new BaseCommandHandler() {
			public boolean _call(Request command) {
				goToIndex(imageUriList.size()-1); return true;
			}
		});
		requestSender.putHandler("editTags", new BaseCommandHandler() {
			public boolean _call(Request command) {
				tagsInput.requestFocus(); return true;
			}
		});
		requestSender.putHandler("toggleCurrentDeleted", new BaseCommandHandler() {
			public boolean _call(Request command) {
				toggleCurrentDeleted(); return true;
			}
		});
		requestSender.putHandler("toggleCurrentArchived", new BaseCommandHandler() {
			public boolean _call(Request command) {
				toggleCurrentArchived(); return true;
			}
		});
		requestSender.putHandler("rotateCurrentRight", new BaseCommandHandler() {
			public boolean _call(Request command) {
				rotateCurrentRight(); return true;
			}
		});
		requestSender.putHandler("rotateCurrentLeft", new BaseCommandHandler() {
			public boolean _call(Request command) {
				rotateCurrentLeft(); return true;
			}
		});
		requestSender.putHandler("flipHorizontal", new BaseCommandHandler() {
			public boolean _call(Request command) {
				flipCurrentHorizontal(); return true;
			}
		});
		requestSender.putHandler("flipVertical", new BaseCommandHandler() {
			public boolean _call(Request command) {
				flipCurrentVertical(); return true;
			}
		});
		requestSender.putHandler("quickCompress", new BaseCommandHandler() {
			public boolean _call(Request command) {
				quickCompress(); return true;
			}
		});
		requestSender.putHandler("compress", new BaseCommandHandler() {
			public boolean _call(Request command) {
				compress(); return true;
			}
		});
		requestSender.putHandler("restoreCurrentOriginal", new BaseCommandHandler() {
			public boolean _call(Request command) {
				restoreCurrentOriginal(); return true;
			}
		});
		requestSender.putHandler("toggleFullscreen", new BaseCommandHandler() {
			public boolean _call(Request command) {
				toggleFullscreen(); return true;
			}
		});
		requestSender.putHandler("zoomIn", new BaseCommandHandler() {
			public boolean _call(Request command) {
				changeScale(zoomUnit); return true;
			}
		});
		requestSender.putHandler("zoomOut", new BaseCommandHandler() {
			public boolean _call(Request command) {
				changeScale(1/zoomUnit); return true;
			}
		});
		requestSender.putHandler("reloadImageList", new BaseCommandHandler() {
			protected boolean _call(Request command) {
				reloadImageList(); return true;
			}
		});
		
		setTitle("PictureArchiver4");
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(Color.BLACK);

		ip = new ImagePanel();
		ip.setBorder(null);
		ip.setPreferredSize(new Dimension(512,384));
		mainPanel.add(ip, BorderLayout.CENTER);
		
		titleLabel = new JLabel("");
		titleLabel.setForeground(Color.WHITE);
		
		positionLabel = new JLabel("");
		positionLabel.setForeground(Color.GRAY);

		dimensionsLabel = new JLabel("");
		dimensionsLabel.setForeground(Color.LIGHT_GRAY);

		zoomLabel = new JLabel("");
		zoomLabel.setForeground(Color.GRAY);

		fileSizeLabel = new JLabel("");
		fileSizeLabel.setForeground(Color.LIGHT_GRAY);
		
		statusLabelLabel = new JLabel(" Status: ");
		statusLabelLabel.setVisible(false);
		statusLabelLabel.setOpaque(true);
		statusLabelLabel.setBackground(Color.DARK_GRAY);
		statusLabelLabel.setForeground(Color.WHITE);
		
		archivedLabel = new JLabel("Archived");
		archivedLabel.setVisible(false);
		archivedLabel.setForeground(Color.YELLOW);
		
		deletedLabel = new JLabel("Deleted");
		deletedLabel.setVisible(false);
		deletedLabel.setForeground(Color.RED);
		
		modifiedLabel = new JLabel("Modified");
		modifiedLabel.setVisible(false);
		modifiedLabel.setForeground(Color.GREEN);
		
		doesNotExistLabel = new JLabel("Not Found");
		doesNotExistLabel.setVisible(false);
		doesNotExistLabel.setForeground(Color.ORANGE);
		
		tagsInput = new JTextField("");
		tagsInput.setBackground(Color.BLACK);
		tagsInput.setForeground(new Color(0x00FF00));
		tagsInput.setCaretColor(new Color(0x88FF88));
		tagsInput.setBorder(new CompoundBorder(new LineBorder(Color.WHITE), BorderFactory.createEmptyBorder(0, 8, 0, 8)));
		tagsInput.setVisible(false);

		tagsLabelLabel = new JLabel(" Tags: ");
		//tagsLabelLabel.setDisplayedMnemonic('t');
		//tagsLabelLabel.setLabelFor(tagsLabel);
		tagsLabelLabel.setOpaque(true);
		tagsLabelLabel.setVisible(false);
		tagsLabelLabel.setBackground(Color.DARK_GRAY);
		tagsLabelLabel.setForeground(Color.WHITE);
		
		pzPanel = new JPanel(new FlowLayout());
		pzPanel.setOpaque(false);

		titlePanel = new JPanel(new BorderLayout());
		titlePanel.setOpaque(false);
		
		statusSubPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		statusSubPanel.setOpaque(false);
		
		statusPanel = new JPanel(new BorderLayout());
		statusPanel.setOpaque(false);
		
		tagsPanel = new JPanel(new BorderLayout());
		tagsPanel.setOpaque(false);
		
		textPanel = new JPanel(new GridLayout(3,1));
		textPanel.setOpaque(false);
		
		pzPanel.add(positionLabel);
		pzPanel.add(dimensionsLabel);
		pzPanel.add(zoomLabel);
		pzPanel.add(fileSizeLabel);
		titlePanel.add(titleLabel, BorderLayout.CENTER);
		titlePanel.add(pzPanel, BorderLayout.EAST);
		statusSubPanel.add(archivedLabel);
		statusSubPanel.add(deletedLabel);
		statusSubPanel.add(modifiedLabel);
		statusSubPanel.add(doesNotExistLabel);
		statusPanel.add(statusLabelLabel, BorderLayout.WEST);
		statusPanel.add(statusSubPanel, BorderLayout.CENTER);
		tagsPanel.add(tagsLabelLabel, BorderLayout.WEST);
		tagsPanel.add(tagsInput, BorderLayout.CENTER);
		textPanel.add(titlePanel);
		textPanel.add(statusPanel);
		textPanel.add(tagsPanel);
		mainPanel.add(textPanel, BorderLayout.SOUTH);
		
		getContentPane().add(mainPanel);
		this.setBackground(Color.ORANGE); // Shouldn't show up!
		
		kci.addBinding(KeyEvent.VK_LEFT, "/pa4/ui/goToPrevious");
		kci.addBinding(KeyEvent.VK_RIGHT, "/pa4/ui/goToNext");
		kci.addBinding(KeyEvent.VK_PAGE_DOWN, "/pa4/ui/goToDelta", Integer.valueOf(10));
		kci.addBinding(KeyEvent.VK_PAGE_UP, "/pa4/ui/goToDelta", Integer.valueOf(-10));
		kci.addBinding(KeyEvent.VK_RIGHT, "/pa4/ui/goToNext");
		kci.addBinding(KeyEvent.VK_HOME, "/pa4/ui/goToFirst");
		kci.addBinding(KeyEvent.VK_END, "/pa4/ui/goToLast");
		kci.addBinding(KeyEvent.VK_EQUALS, "/pa4/ui/zoomIn");
		kci.addBinding(KeyEvent.VK_PLUS, "/pa4/ui/zoomIn");
		kci.addBinding(KeyEvent.VK_MINUS, "/pa4/ui/zoomOut");
		kci.addBinding(KeyEvent.VK_DELETE, "/pa4/ui/toggleCurrentDeleted");
		kci.addBinding(KeyEvent.VK_K, "/pa4/ui/quickCompress");
		kci.addBinding(KeyEvent.VK_C, "/pa4/ui/compress");
		kci.addBinding(KeyEvent.VK_O, "/pa4/ui/restoreCurrentOriginal");
		kci.addBinding(KeyEvent.VK_R, "/pa4/ui/rotateCurrentRight");
		kci.addBinding(KeyEvent.VK_L, "/pa4/ui/rotateCurrentLeft");
		kci.addBinding(KeyEvent.VK_H, "/pa4/ui/flipHorizontal");
		kci.addBinding(KeyEvent.VK_V, "/pa4/ui/flipVertical");
		kci.addBinding(KeyEvent.VK_A, "/pa4/ui/toggleCurrentArchived");
		kci.addBinding(KeyEvent.VK_T, "/pa4/ui/editTags");
		kci.addBinding(KeyEvent.VK_ENTER, "/pa4/ui/editTags");
		kci.addBinding(KeyEvent.VK_F11, "/pa4/ui/toggleFullscreen");
		kci.addBinding(KeyEvent.VK_F3, "/pa4/ui/reloadImageList");
		
		addKeyListener(kci);
		
		MouseWheelListener mwl = new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if( e.getWheelRotation() < 0 ) {
					doCommand("/pa4/ui/zoomIn");
				} else {
					doCommand("/pa4/ui/zoomOut");
				}
				e.consume();
			}
		};
		
		ip.setWheelScrollingEnabled(false);
		ip.addMouseWheelListener(mwl);
		addMouseWheelListener(mwl);
		
		class TagBoxListener implements FocusListener, KeyListener {
			String focusText = null;
			
			public void focusGained(FocusEvent e) {
				focusText = tagsInput.getText();
			}
			public void focusLost(FocusEvent e) {}
			
			public void keyPressed(KeyEvent e) {
				if( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
					setCurrentTags(focusText);
					PAMainWindow.this.requestFocus();
				} else if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
					saveCurrentTags();
					PAMainWindow.this.requestFocus();
				}
			}
			public void keyReleased(KeyEvent e) {}
			public void keyTyped(KeyEvent e) {};
		}
		TagBoxListener tbl = new TagBoxListener();
		
		tagsInput.addFocusListener(tbl);
		tagsInput.addKeyListener(tbl);
		
		requestFocusInWindow();
	}
	
	protected boolean isTrue( Map metadata, String name ) {
		Object value = metadata.get(name);
		if( value == null ) return false;
		if( value == Boolean.FALSE ) return false;
		return true;
	}
	
	
	public void toggleCurrentDeleted() {
		if( isTrue(state.metadata, ImageManager.ISDELETED) ) {
			imageManager.undelete(state.fakeUri);
		} else {
			imageManager.delete(state.fakeUri);
		}
	}
	public void toggleCurrentArchived() {
		if( isTrue(state.metadata, ImageManager.ISARCHIVED) ) {
			imageManager.unarchive(state.fakeUri);
		} else {
			imageManager.archive(state.fakeUri);
		}
	}
	public void rotateCurrentRight() {
		imageManager.rotateRight(state.fakeUri);
	}
	public void rotateCurrentLeft() {
		imageManager.rotateLeft(state.fakeUri);
	}
	public void flipCurrentHorizontal() {
		imageManager.flipHorizontal(state.fakeUri);
	}
	public void flipCurrentVertical() {
		imageManager.flipVertical(state.fakeUri);
	}
	public void restoreCurrentOriginal() {
		imageManager.restoreOriginal(state.fakeUri);
	}
	public void saveCurrentTags() {
		imageManager.saveTags(state.fakeUri, getCurrentTags());
	}
	public void quickCompress() { imageManager.compressToUnder(state.fakeUri, 300000); }
	public void compress() { imageManager.compressAgain(state.fakeUri); }
	
	protected String getCurrentTags() {
		return tagsInput.getText().trim();
	}
	
	protected void setCurrentTags( String tags ) {
		tagsInput.setText(tags.trim());
	}

	public String formatNumber(long n) {
		String s = String.valueOf(n);
		String f = "";
		for( int i=0; i<s.length(); ++i ) {
			if( i % 3 == 0 && f.length() > 0 ) f = "_" + f;
			f = s.charAt(s.length()-i-1) + f;
		}
		return f;
	}

	static final String TIMES = String.valueOf((char)0xD7);
	
	public void updateLabels( State s ) {
		Map metadata = s.metadata;
		String fakeUri = s.fakeUri;
		if( fakeUri == null ) {
			titleLabel.setText("(no image)");
			zoomLabel.setText("");
		} else {
			String title = UriUtil.uriDecode(fakeUri);
			// There's no good built-in way to cut the text's left end with ellipses.
			// https://stackoverflow.com/questions/19519940/putting-3-dots-at-the-beginning-of-jlabel
			// Do some guessing about how many chars can fit:
			int maxTitleLength = titleLabel.getWidth() / 8;
			if( title.length() > maxTitleLength ) {
				title = "..." + title.substring(title.length()-maxTitleLength+3);
			}
			titleLabel.setText(title);
			zoomLabel.setText("(" + new DecimalFormat("0.00").format(s.scale) + "x)");
		}
		if( s.listIndex >= 0 && imageUriList != null ) {
			positionLabel.setText(" " + (s.listIndex+1) + " of " + imageUriList.size() + " ");
		} else {
			positionLabel.setText("");
		}

		dimensionsLabel.setText(s.image != null ? s.image.getWidth(null)+TIMES+s.image.getHeight(null) : "N/A");

		Long fileSize = (Long)metadata.get(ImageManager.FILESIZE);
		fileSizeLabel.setText(fileSize == null ? "N/A" : formatNumber(fileSize.longValue())+" bytes");
		
		statusLabelLabel.setVisible(true);
		deletedLabel.setVisible(false);
		modifiedLabel.setVisible(false);
		archivedLabel.setVisible(false);
		doesNotExistLabel.setVisible(false);
		if( isTrue( metadata, ImageManager.ISDELETED ) ) {
			deletedLabel.setVisible(true);
		}
		if( isTrue( metadata, ImageManager.ISMODIFIEDFROMORIGINAL ) ) {
			modifiedLabel.setVisible(true);
		}
		if( isTrue( metadata, ImageManager.ISARCHIVED ) ) {
			archivedLabel.setVisible(true);
		}
		if( isTrue( metadata, ImageManager.DOESNOTEXIST ) ) {
			doesNotExistLabel.setVisible(true);
		}
		
		String tags = ImageManager.getString( metadata.get(ImageManager.SUBJECTTAGS) );
		if( tags == null ) tags = "";
		tagsLabelLabel.setVisible(true);
		tagsInput.setVisible(true);
		setCurrentTags( tags );
	}

	public void setState( State s, boolean autoscale ) {
		this.state = s;
		if( autoscale ) {
			// Update the labels so that the image panel is the right size...
			updateLabels(s); validate();
			// ...so it can auto-scale to that size 
			ip.setImageAutoscale(s.image);
			s.scale = ip.scale;
			updateLabels(s);
		} else {
			ip.setImage(s.image, s.scale);
			updateLabels(s);
		}
	}
	
	public void changeScale( float multiply ) {
		state.scale *= multiply;
		setState( state, false );
	}
	
	public void setImage( int listIndex, Image img, String fakeUri, String realUri, Map metadata ) {
		State s = new State();
		s.listIndex = listIndex;
		s.image = img;
		s.fakeUri = fakeUri;
		s.realUri = realUri;
		s.metadata = metadata;
		setState( s, true );
	}
	
	protected boolean nonFullscreenUndecorated = false;
	int nonFullscreenEstate = 0;
	Dimension nonFullscreenSize = null;
	protected boolean fullscreen = false;
	
	public void setFullscreen( boolean fullscreen ) {
		if( this.fullscreen == fullscreen ) return;
		this.fullscreen = fullscreen;

		dispose();
		if( fullscreen ) {
			nonFullscreenEstate = getExtendedState();
			nonFullscreenSize = getSize();
			setUndecorated(true);
			setExtendedState(nonFullscreenEstate | JFrame.MAXIMIZED_BOTH);
			validate();
		} else {
			setUndecorated(nonFullscreenUndecorated);
			if( nonFullscreenSize != null) {
				setExtendedState(nonFullscreenEstate);
				setSize(nonFullscreenSize);
				validate();
			} else {
				setExtendedState(getExtendedState() & ~JFrame.MAXIMIZED_BOTH);
				pack();
			}
		}
		setVisible(true);
		requestFocusInWindow();
	}
	
	public void toggleFullscreen() {
		setFullscreen( !fullscreen );
	}
	
	public void setImage( int listIndex, String fakeUri, Map metadata ) {
		if( fakeUri == null ) {
			setImage( listIndex, null, null, null, metadata );
		}
		
		FoundResource fi;
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			StatusLog.log("Locating "+fakeUri);
			fi = ImageManager.findImage(fakeUri);
		} finally {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		
		if( fi == null ) {
			setImage( listIndex, null, fakeUri, null, metadata );
		} else if( fi.content == null ) {
			setImage( listIndex, null, fakeUri, fi.uri, metadata );
		} else {
			setImage( listIndex, (Image)fi.content, fakeUri, fi.uri, metadata );
		}
	}
	
	public void setImage( int listIndex, String fakeUri ) {
		setImage( listIndex, fakeUri, fakeUri == null ? Collections.EMPTY_MAP : imageManager.loadMetadata(fakeUri) );
	}
	
	public void refresh( boolean reloadMetadata ) {
		setImage( state.listIndex, state.fakeUri, reloadMetadata ? imageManager.loadMetadata(state.fakeUri) : state.metadata );
	}
	
	public void setRootImageUriList( List roots ) {
		rootImageUriList = roots;
		reloadImageList();
	}
	public void setImageUriList( List roots, List l ) {
		rootImageUriList = roots;
		imageUriList = l;
	}
	
	protected void reloadImageList() {
		String oldFilename = null;
		if( imageUriList != null && imageUriList.size() > state.listIndex ) {
			oldFilename = (String)imageUriList.get(state.listIndex);
		}
		
		Set l = new HashSet();
		for( Iterator itr = rootImageUriList.iterator(); itr.hasNext(); ) {
			collectImageUris( (String)itr.next(), l );
		}
		StatusLog.log( l.size() + " files found" );
		imageUriList = new ArrayList(l);
		Collections.sort(imageUriList);
		
		goToFile(oldFilename);
	}
	
	public void goToIndex( int i ) {
		String uri;
		if( i<0 || i>=imageUriList.size() ) {
			uri = null;
		} else {
			uri = (String)imageUriList.get(i);
		}
		setImage( i, uri );
	}
	
	public void goToModIndex(int n) {
		if( imageUriList == null ) return;
		if( n < 0 ) n = imageUriList.size() - (-n % imageUriList.size());
		else n = n % imageUriList.size();
		goToIndex(n);
	}
	
	public void goToFile( String uri ) {
		int idx = imageUriList.indexOf(uri);
		if( idx != -1 ) {
			goToIndex(idx);
		} else {
			System.err.println("Couldn't find "+uri+" in file list");
		}
	}
	
	public void goToPrevious() {
		goToModIndex( state.listIndex - 1 );
	}
	
	public void goToNext() {
		goToModIndex( state.listIndex + 1 );
	}

	protected static boolean looksLikeImagePath( String path ) {
		path = path.toLowerCase();
		// TODO: add more image format extensions....
		return path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".jpeg") || path.endsWith(".tiff") || path.endsWith(".tif");
	}
	
	protected static String getFakePath( String path ) {
		int delidx = path.indexOf("/.deleted/");
		if( delidx != -1 ) {
			path = path.substring(0,delidx) + path.substring(delidx+9);
		}
		delidx = path.indexOf("/.originals/");
		if( delidx != -1 ) {
			path = path.substring(0,delidx) + path.substring(delidx+11);
		}
		return path;
	}
	
	protected static void collectImageUris( File base, Set imageUriSet ) {
		if( base.isDirectory() ) {
			File[] subs = base.listFiles();
			for( int i=0; i<subs.length; ++i ) {
				File sub = subs[i];
				if( !sub.getName().startsWith(".") || sub.getName().equals(".deleted") ) {
					collectImageUris(sub, imageUriSet);
				}
			}
		} else if( looksLikeImagePath(base.getPath()) ) {
			StatusLog.log( imageUriSet.size()+" images found..." );
			imageUriSet.add(getFakePath(PathUtil.maybeNormalizeFileUri(base.getAbsolutePath())));
		}
	}
	
	protected static void collectImageUris( String base, Set uriList ) {
		File file = ImageManager.getFile(base, true);
		collectImageUris( file, uriList );
	}
	
	@Override
	public void resourceUpdated(ResourceUpdateEvent evt) {
		if( !evt.getResourceUri().equals(this.state.fakeUri) ) return;
		
		//this.state.metadata.putAll(evt.getChangedMetadata());
		//refresh(false);
		refresh(true);
	}
	
	public static String USAGE =
		"Usage: pa4 [options] <infile> <infile> <infile> ...\n" +
		"Where <infile> is the path to a file or directory to operate on.\n" +
		"Options:\n" +
		"  -noborder\n" +
		"  -maximize\n" +
		"  -fullscreen\n" +
		"  -cmdline ; read commands from stdin\n"+
		"  -linker {fsutil|ln|copy}\n" +
		"  -disable-touching ; don't try to update directory timestamps or delete\n" +
		"                    ; .ccouch-uri files when moving/deleting files.\n" +
		"  -archive-map <input dir> <archive dir>";
	
	public static void main(String[] args) {
		HashSet rootUriSet = new HashSet();
		boolean maximize = false;
		boolean undecorate = false;
		boolean fullscreen = false;
		boolean inputsSpecified = false;
		HashMap archiveDirectoryUriMap = new HashMap();
		boolean disableTouching = false;
		boolean cmdline = false;
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-noborder".equals(arg) ) {
				undecorate = true;
			} else if( "-cmdline".equals(arg) ) {
				cmdline = true;
			} else if( "-maximize".equals(arg) ) {
				maximize = true;
			} else if( "-fullscreen".equals(arg) ) {
				fullscreen = true;
			} else if( "-archive-map".equals(arg) ) {
				String inDir = ImageManager.getFileUri(args[++i]);
				String outDir = ImageManager.getFileUri(args[++i]);
				StatusLog.log("Adding normalized mapping: " + inDir + " -> "+outDir);
				archiveDirectoryUriMap.put(inDir,outDir);
			} else if( "-disable-touching".equals(arg) ) {
				disableTouching = true;
			} else if( !arg.startsWith("-") ) {
				String uri = PathUtil.maybeNormalizeFileUri(arg);
				StatusLog.log("Collecting image URIs...");
				rootUriSet.add(uri);
				inputsSpecified = true;
			} else if( "-v".equals(arg) ) {
				StatusLog.setInstance( new StatusLog.VerboseStderrLogger() );
			} else if( "-linker".equals(arg) ) {
				++i;
				String linkerName = args[i];
				if( "ln".equals(linkerName) ) {
					Linker.instance = new Linker.LnLinker();
				} else if( "copy".equals(linkerName) ) {
					Linker.instance = new Linker.CopyLinker();
				} else if( "fsutil".equals(linkerName) ) {
					Linker.instance = new Linker.FSUtilLinker();
				} else {
					throw new RuntimeException("Unrecognised linker: '"+linkerName+"'");
				}
			} else if( "-?".equals(arg) || "-h".equals(arg) || "-help".equals(arg) || "--help".equals(arg) ) {
				System.out.println(USAGE);
				System.exit(0);
			} else {
				System.err.println("Unrecognised argument: " + arg);
				System.err.println(USAGE);
				System.exit(1);
			}
		}
		
		if( !inputsSpecified ) {
			System.err.println("No files or directories specified!");
			System.err.println("Run with -? for usage information.");
			System.exit(0);
		}
		
		//collectImageUris( uri, uriSet );
		//StatusLog.log( uriSet.size() + " files found" );

		ArrayList rootUriList = new ArrayList(rootUriSet);
		Collections.sort(rootUriList);
		
		/*
		if( uriList.size() == 0 ) {
			System.err.println("No files found");
			System.exit(0);
		}
		*/
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch( Exception e ) {
			System.err.print("Couldn't change look and feel: ");
			e.printStackTrace();
		}
		
		StatusLog.log("Initializing window");
		final PAMainWindow pam = new PAMainWindow();
		if( disableTouching ) pam.imageManager.touchingEnabled = false;
		pam.imageManager.archiveDirectoryUriMap = archiveDirectoryUriMap;
		pam.nonFullscreenUndecorated = undecorate;
		if( undecorate | fullscreen ) pam.setUndecorated(true);
		if( maximize | fullscreen ) {
			pam.setExtendedState(pam.getExtendedState() | JFrame.MAXIMIZED_BOTH);
			pam.validate();
		} else {
			pam.pack();
		}
		pam.setVisible(true);
		pam.addWindowListener(new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
				pam.dispose();
			}
			public void windowClosed( WindowEvent e ) {}
		});
		pam.setRootImageUriList(rootUriList);
		StatusLog.log("Go to index 0...");
		pam.goToIndex(0);
		
		if( !cmdline ) return;
		
		try {
			String line;
			BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in));
			while( (line = lineReader.readLine()) != null ) {
				line.trim();
				if( line.startsWith("#") || line.length() == 0 ) continue;
				
				String cmd;
				if( "n".equals(line) ) {
					cmd = "/pa4/ui/goToNext";
				} else if( "p".equals(line) ) {
					cmd = "/pa4/ui/goToPrevious";
				} else if( "first".equals(line) ) {
					cmd = "/pa4/ui/goToFirst";
				} else if( "last".equals(line) ) {
					cmd = "/pa4/ui/goToLast";
				} else if( "a".equals(line) || "archive".equals(line) ) {
					cmd = "/pa4/ui/toggleCurrentArchived";
				} else if( "d".equals(line) || "delete".equals(line) ) {
					cmd = "/pa4/ui/toggleCurrentDeleted";
				} else if( "l".equals(line) || "left".equals(line) ) {
					cmd = "/pa4/ui/rotateCurrentLeft";
				} else if( "r".equals(line) || "right".equals(line) ) {
					cmd = "/pa4/ui/rotateCurrentRight";
				} else if( "h".equals(line) || "flip-horizontal".equals(line) ) {
					cmd = "/pa4/ui/flipHorizontal";
				} else if( "v".equals(line) || "flip-vertical".equals(line) ) {
					cmd = "/pa4/ui/flipVertical";
				} else if( "c".equals(line) || "compress".equals(line) ) {
					cmd = "/pa4/ui/compress";
				} else if( "k".equals(line) || "quickCompress".equals(line) ) {
					cmd = "/pa4/ui/quickCompress";
				} else if( "o".equals(line) || "revert".equals(line) ) {
					cmd = "/pa4/ui/restoreCurrentOriginal";
				} else if( "+".equals(line) || "zoom-in".equals(line) ) {
					cmd = "/pa4/ui/zoomIn";
				} else if( "-".equals(line) || "zoom-out".equals(line) ) {
					cmd = "/pa4/ui/zoomOut";
				} else if( "q".equals(line) ) {
					cmd = "/pa4/ui/quit";
				} else {
					cmd = "/pa4/ui/"+line;
				}
				
				if( "/pa4/ui/quit".equals(cmd) ) {
					System.exit(0);
				} else {
					pam.doCommand(cmd);
				}
			}
		} catch( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
