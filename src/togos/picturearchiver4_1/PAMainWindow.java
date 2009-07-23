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
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;

import contentcouch.misc.UriUtil;
import contentcouch.path.PathUtil;

public class PAMainWindow extends JFrame {
	public static final String NS = "http://ns.nuke24.net/PictureArchiver4.1/";
	public static final String DOESNOTEXIST = NS + "doesNotExist";
	public static final String ISARCHIVED = NS + "isArchived";
	public static final String ISDELETED = NS + "isDeleted";
	public static final String ISMODIFIEDFROMORIGINAL = NS + "isModifiedFromOriginal";
	public static final String SUBJECTTAGS = NS + "subjectTags";
	
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
	
	List imageUriList;
	State state;
	
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
	
	float zoomUnit = 1.5f;
	
	public PAMainWindow() {
		super();
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
		
		addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if( e.getKeyCode() == KeyEvent.VK_LEFT ) {
					goToPrevious();
					e.consume();
				} else if( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
					goToNext();
					e.consume();
				} else if( e.getKeyCode() == KeyEvent.VK_EQUALS || e.getKeyCode() == KeyEvent.VK_PLUS ) {
					changeScale(zoomUnit);
					e.consume();
				} else if( e.getKeyCode() == KeyEvent.VK_MINUS ) {
					changeScale(1/zoomUnit);
					e.consume();
				} else if( e.getKeyCode() == KeyEvent.VK_T ) {
					// To prevent the 't' from being inserted on random occasions
					tagsInput.setEnabled(false);
					tagsInput.requestFocus();
					tagsInput.setCaretPosition(tagsInput.getText().length());
					tagsInput.setEnabled(true);
					e.consume();
				} else {
					System.err.println("Key pressed " + e.getKeyCode());
				}
			}
			public void keyReleased(KeyEvent e) {
			}
			public void keyTyped(KeyEvent e) {
			}
		});
		
		MouseWheelListener mwl = new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if( e.getWheelRotation() < 0 ) {
					changeScale(zoomUnit);
				} else {
					changeScale(1/zoomUnit);
				}
				//e.consume();
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
					saveTags(state.fakeUri, getCurrentTags());
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
	
	protected List getList( Object o ) {
		if( o == null ) return null;
		if( o instanceof List ) return (List)o;
		if( o instanceof String ) o = ((String)o).split(",\\s*");
		if( o instanceof Object[] ) return Arrays.asList((Object[])o);
		throw new RuntimeException("Don't know how to convert " + o.getClass().getName() + " to List");
	}
	
	protected String getString( Object o ) {
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
		
		statusLabelLabel.setVisible(false);
		if( isTrue( metadata, ISDELETED ) ) {
			deletedLabel.setVisible(true);
			statusLabelLabel.setVisible(true);
		}
		if( isTrue( metadata, ISMODIFIEDFROMORIGINAL ) ) {
			modifiedLabel.setVisible(true);
			statusLabelLabel.setVisible(true);
		}
		if( isTrue( metadata, ISARCHIVED ) ) {
			archivedLabel.setVisible(true);
			statusLabelLabel.setVisible(true);
		}
		if( isTrue( metadata, DOESNOTEXIST ) ) {
			doesNotExistLabel.setVisible(true);
			statusLabelLabel.setVisible(true);
		}
		
		String tags = getString( metadata.get(SUBJECTTAGS) );
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
	
	public void saveTags( String taggedUri, String tags ) {
		System.err.println("Save tags " + tags);
	}
	
	public Image getImage( String url ) {
		try {
			return ImageIO.read(new URL(url));
		} catch( IOException e ) {
			return null;
		}
	}
	
	class FoundImage {
		public FoundImage( String uri, Image img ) {
			this.uri = uri;
			this.image = img;
		}
		public Image image;
		public String uri;
	}
	
	public FoundImage findImage( String fakeUri ) {
		Image i;
		String realUri = fakeUri;
		
		i = getImage( realUri ); if( i != null ) return new FoundImage( realUri, i );

		int ls = fakeUri.lastIndexOf('/');
		if( ls != -1 ) {
			String fakeRoot = fakeUri.substring(0,ls);
			String fakeFile = fakeUri.substring(ls+1);
			
			realUri = fakeRoot + "/.deleted/" + fakeFile;
			i = getImage( realUri ); if( i != null ) return new FoundImage( realUri, i );
			
			realUri = fakeRoot + "/.originals/" + fakeFile;
			i = getImage( realUri ); if( i != null ) return new FoundImage( realUri, i );
		}
		return null;
	}
	
	public void setImage( int listIndex, String fakeUri ) {
		HashMap metadata = new HashMap();
		metadata.put(ISDELETED, Boolean.TRUE);
		metadata.put(SUBJECTTAGS, "foo, bar");
		
		if( fakeUri == null ) {
			setImage( listIndex, null, null, null, metadata );
		}
		
		FoundImage fi;
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			fi = findImage(fakeUri);
		} finally {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		
		if( fi.uri.indexOf("/.deleted/") != -1 || fi.uri.indexOf("/.originals/") != -1 ) {
			metadata.put(ISDELETED, Boolean.TRUE);
		}
		
		if( fi.image == null ) {
			setImage( listIndex, null, fakeUri, null, metadata );
		} else {
			setImage( listIndex, fi.image, fakeUri, fi.uri, metadata );
		}
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

	protected static File getFile(String uri) {
		if( uri.startsWith("file:" ) ) {
			return new File(PathUtil.parseFilePathOrUri(uri).toString());
		}
		return null;
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
			imageUriSet.add(PathUtil.maybeNormalizeFileUri(getFakePath(base.getAbsolutePath())));
		}
	}
	
	protected static void collectImageUris( String base, Set uriList ) {
		File file = getFile(base);
		if( file == null ) {
			throw new RuntimeException("I only handle file URIs, for now...");
		}
		collectImageUris( file, uriList );
	}
	
	public static void main(String[] args) {
		HashSet uriSet = new HashSet();
		boolean maximize = false;
		boolean undecorate = false;
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-noborder".equals(arg) ) {
				undecorate = true;
			} else if( "-maximize".equals(arg) ) {
				maximize = true;
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
		if( undecorate ) pam.setUndecorated(true);
		pam.pack();
		if( maximize ) pam.setExtendedState(pam.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		pam.setVisible(true);
		pam.addWindowListener(new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
				pam.dispose();
			}
			public void windowClosed( WindowEvent e ) {
				System.exit(0);
			}
		});
		pam.setImageUriList(uriList);
		pam.goToIndex(1);
	}
}
