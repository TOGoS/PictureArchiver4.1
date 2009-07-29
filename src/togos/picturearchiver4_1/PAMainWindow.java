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
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import togos.mf.MessageIterator;
import togos.picturearchiver4_1.ImageManager.FoundResource;
import togos.picturearchiver4_1.comframework.BaseCommandHandler;
import togos.picturearchiver4_1.comframework.MappedCommandHandler;
import togos.rra.Arguments;
import togos.rra.BaseRequest;
import togos.rra.Request;
import contentcouch.misc.UriUtil;
import contentcouch.path.PathUtil;

public class PAMainWindow extends JFrame implements ResourceUpdateListener {
	class ImagePanel extends DraggableScrollPane {
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
			float scaleX = (float)getViewport().getWidth() / i.getWidth(null);
			float scaleY = (float)getViewport().getHeight() / i.getHeight(null);
			float scaleQ = (scaleX < scaleY ? scaleX : scaleY);
			setImage( i, scaleQ < 1.0 ? scaleQ : 1.0f );
		}
	}
	
	class State {
		int listIndex;
		String fakeUri;
		String realUri;
		Image image;
		Map metadata;
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
	JLabel       zoomLabel;
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

	List imageUriList;
	State state = new State();
	float zoomUnit = 1.5f;
	
	protected void doCommand(String name) {
		Request req = new BaseRequest(Request.VERB_POST, name);
		if( requestSender.call(req) == null ) {
			System.err.println("Unhandled command <" + name + ">");
		}
	}
	
	public PAMainWindow() {
		super();
		
		imageManager.addResourceUpdateListener(this);
		
		requestSender.putHandler("goToDelta", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				goToModIndex(state.listIndex + ((Integer)((Arguments)command.getContent()).getPositionalArguments().get(0)).intValue());
				return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("goToNext", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				goToNext(); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("goToPrevious", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				goToPrevious(); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("goToFirst", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				goToIndex(0); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("goToLast", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				goToIndex(imageUriList.size()-1); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("editTags", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				tagsInput.requestFocus(); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("toggleCurrentDeleted", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				toggleCurrentDeleted(); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("toggleCurrentArchived", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				toggleCurrentArchived(); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("rotateCurrentRight", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				rotateCurrentRight(); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("rotateCurrentLeft", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				rotateCurrentLeft(); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("restoreCurrentOriginal", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				restoreCurrentOriginal(); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("toggleFullscreen", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				toggleFullscreen(); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("zoomIn", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				changeScale(zoomUnit); return MessageIterator.NORESPONSE;
			}
		});
		requestSender.putHandler("zoomOut", new BaseCommandHandler() {
			public MessageIterator _open(Request command) {
				changeScale(1/zoomUnit); return MessageIterator.NORESPONSE;
			}
		});
		
		setTitle("PictureArchiver4");
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(Color.BLACK);

		ip = new ImagePanel();
		ip.setBorder(null);
		ip.setPreferredSize(new Dimension(512,384));
		mainPanel.add(ip, BorderLayout.CENTER);
		
		titleLabel = new JLabel("", JLabel.LEADING);
		titleLabel.setHorizontalTextPosition(JLabel.TRAILING);
		titleLabel.setForeground(Color.WHITE);
		
		positionLabel = new JLabel("");
		positionLabel.setForeground(Color.GRAY);
		
		zoomLabel = new JLabel("");
		zoomLabel.setForeground(Color.LIGHT_GRAY);
		
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
		pzPanel.add(zoomLabel);
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
		kci.addBinding(KeyEvent.VK_PAGE_DOWN, "/pa4/ui/goToDelta", new Integer(10));
		kci.addBinding(KeyEvent.VK_PAGE_UP, "/pa4/ui/goToDelta", new Integer(-10));
		kci.addBinding(KeyEvent.VK_RIGHT, "/pa4/ui/goToNext");
		kci.addBinding(KeyEvent.VK_HOME, "/pa4/ui/goToFirst");
		kci.addBinding(KeyEvent.VK_END, "/pa4/ui/goToLast");
		kci.addBinding(KeyEvent.VK_EQUALS, "/pa4/ui/zoomIn");
		kci.addBinding(KeyEvent.VK_PLUS, "/pa4/ui/zoomIn");
		kci.addBinding(KeyEvent.VK_MINUS, "/pa4/ui/zoomOut");
		kci.addBinding(KeyEvent.VK_DELETE, "/pa4/ui/toggleCurrentDeleted");
		kci.addBinding(KeyEvent.VK_O, "/pa4/ui/restoreCurrentOriginal");
		kci.addBinding(KeyEvent.VK_R, "/pa4/ui/rotateCurrentRight");
		kci.addBinding(KeyEvent.VK_L, "/pa4/ui/rotateCurrentLeft");
		kci.addBinding(KeyEvent.VK_A, "/pa4/ui/toggleCurrentArchived");
		kci.addBinding(KeyEvent.VK_T, "/pa4/ui/editTags");
		kci.addBinding(KeyEvent.VK_ENTER, "/pa4/ui/editTags");
		kci.addBinding(KeyEvent.VK_F11, "/pa4/ui/toggleFullscreen");
		
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
		this.requestFocus();
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
	public void restoreCurrentOriginal() {
		imageManager.restoreOriginal(state.fakeUri);
	}
	public void saveCurrentTags() {
		imageManager.saveTags(state.fakeUri, getCurrentTags());
	}
	
	protected String getCurrentTags() {
		return tagsInput.getText().trim();
	}
	
	protected void setCurrentTags( String tags ) {
		tagsInput.setText(tags.trim());
	}
	
	public void updateLabels( State s ) {
		Map metadata = s.metadata;
		String fakeUri = s.fakeUri;
		if( fakeUri == null ) {
			titleLabel.setText("(no image)");
			zoomLabel.setText("");
		} else {
			String title = UriUtil.uriDecode(fakeUri);
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
			fi = ImageManager.findImage(fakeUri);
		} finally {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		
		if( fi.content == null ) {
			setImage( listIndex, null, fakeUri, fi.uri, metadata );
		} else {
			setImage( listIndex, (Image)fi.content, fakeUri, fi.uri, metadata );
		}
	}
	
	public void setImage( int listIndex, String fakeUri ) {
		setImage( listIndex, fakeUri, imageManager.loadMetadata(fakeUri) );
	}
	
	public void refresh( boolean reloadMetadata ) {
		setImage( state.listIndex, state.fakeUri, reloadMetadata ? imageManager.loadMetadata(state.fakeUri) : state.metadata );
	}
	
	public void setImageUriList( List l ) {
		imageUriList = l;
	}
	public void goToIndex( int i ) {
		String uri;
		if( i<0 || i>imageUriList.size() ) {
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
			imageUriSet.add(getFakePath(PathUtil.maybeNormalizeFileUri(base.getAbsolutePath())));
		}
	}
	
	protected static void collectImageUris( String base, Set uriList ) {
		File file = ImageManager.getFile(base, true);
		collectImageUris( file, uriList );
	}
	
	public static void main(String[] args) {
		HashSet uriSet = new HashSet();
		boolean maximize = false;
		boolean undecorate = false;
		boolean fullscreen = false;
		HashMap archiveDirectoryUriMap = new HashMap();
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-noborder".equals(arg) ) {
				undecorate = true;
			} else if( "-maximize".equals(arg) ) {
				maximize = true;
			} else if( "-fullscreen".equals(arg) ) {
				fullscreen = true;
			} else if( "-archive-map".equals(arg) ) {
				String inDir = ImageManager.getFileUri(args[++i]);
				String outDir = ImageManager.getFileUri(args[++i]);
				archiveDirectoryUriMap.put(inDir,outDir);
			} else if( !arg.startsWith("-") ) {
				String uri = PathUtil.maybeNormalizeFileUri(arg);
				collectImageUris( uri, uriSet );
			} else {
				System.err.println("Unrecognised argument: " + arg);
				System.exit(1);
			}
		}
		
		ArrayList uriList = new ArrayList(uriSet);
		Collections.sort(uriList);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch( Exception e ) {
			System.err.print("Couldn't change look and feel: ");
			e.printStackTrace();
		}
		
		final PAMainWindow pam = new PAMainWindow();
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
		pam.setImageUriList(uriList);
		pam.goToIndex(0);
	}

	public void resourceUdated(ResourceUpdateEvent evt) {
		if( !evt.getResourceUri().equals(this.state.fakeUri) ) return;

		this.state.metadata.putAll(evt.getChangedMetadata());
		refresh(false);
	}
}
