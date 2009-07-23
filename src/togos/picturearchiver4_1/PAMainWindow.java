package togos.picturearchiver4_1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import contentcouch.path.PathUtil;

public class PAMainWindow extends JFrame {
	public static final String NS = "http://ns.nuke24.net/PictureArchiver4.1/";
	public static final String DOESNOTEXIST = NS + "doesNotExist";
	public static final String ISARCHIVED = NS + "isArchived";
	public static final String ISDELETED = NS + "isDeleted";
	public static final String ISMODIFIEDFROMORIGINAL = NS + "isModifiedFromOriginal";
	public static final String SUBJECTTAGS = NS + "subjectTags";
	
	class InnerImagePanel extends JPanel implements Scrollable {
		protected Image image;
		protected float scale;
		
		public InnerImagePanel() {
			super();
			setBackground(Color.BLACK);
		}
		
		protected Dimension getScaledImageSize() {
			if( image == null ) {
				return new Dimension(0,0);
			}
			int w = image.getWidth(null);
			int h = image.getHeight(null);
			return new Dimension( (int)(w*scale), (int)(h*scale) );
		}
		
		public Dimension getPreferredSize() {
			Dimension viewportSize = getParent().getSize();
			int w, h;
			Dimension s = getScaledImageSize();
			w = s.width;
			h = s.height;
			if( w < viewportSize.width ) w = viewportSize.width;
			if( h < viewportSize.height ) h = viewportSize.height;
			return new Dimension(w,h);
		}
		
		public void setImage(Image i, float scale) {
			this.image = i;
			this.scale = scale;
			repaint();
		}
		
		public void paint(Graphics g) {
			super.paint(g);
			if( image != null ) {
				Dimension size = getParent().getSize();
				Dimension scaledImageSize = getScaledImageSize();
				int ix = (size.width > scaledImageSize.width) ? (size.width - scaledImageSize.width) / 2 : 0;
				int iy = (size.height > scaledImageSize.height) ? (size.height - scaledImageSize.height) / 2 : 0;
				g.drawImage(image, ix, iy, scaledImageSize.width, scaledImageSize.height, null);
			}
		}
		
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}
		
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
		
		public boolean getScrollableTracksViewportWidth() {
			return false;
		}
		
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			int amt; 
			if( orientation == SwingConstants.HORIZONTAL ) {
				amt = (int)(visibleRect.getWidth() / 8);
			} else {
				amt = (int)(visibleRect.getHeight() / 8);
			}
			if( amt < 1 ) amt = 1;
			return amt;
		}
		

		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return getScrollableBlockIncrement(visibleRect, orientation, direction);
		}
	}
	
	class DraggableScrollPane extends JScrollPane {
		abstract class MouseDragHelper implements MouseListener, MouseMotionListener {
			Component cursorComponent;
			public MouseDragHelper( Component cursorComponent ) {
				this.cursorComponent = cursorComponent;
			}
			
			protected Cursor oldCursor;
			
			protected boolean dragging;
			
			protected int x, y;
			protected float moveScaleX;
			protected float moveScaleY;
			
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
			public void mouseMoved(MouseEvent e) {
				if( dragging ) {
					int dx = (int)moveScaleX*(e.getX() - x);
					int dy = (int)moveScaleY*(e.getY() - y);
					dragDelta( dx, dy );
				}
				this.x = e.getX();
				this.y = e.getY();
			}
			public void mouseClicked(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {
				this.x = e.getX();
				this.y = e.getY();
				if( e.getButton() == MouseEvent.BUTTON2 ) {
					dragging = !dragging;
					if( dragging ) {
						Dimension viewSize = getViewport().getView().getPreferredSize();
						moveScaleX = 2 * viewSize.width / getWidth();
						moveScaleY = 2 * viewSize.height / getHeight();
						cursorComponent.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					} else {
						cursorComponent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
				} else {
					dragging = true;
					moveScaleX = -1;
					moveScaleY = -1;
					cursorComponent.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				}
			}
			public void mouseReleased(MouseEvent e) {
				if( e.getButton() == MouseEvent.BUTTON2 ) return;
				dragging = false;
				cursorComponent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
			
			abstract void dragDelta( int dx, int dy );
		}
		
		public DraggableScrollPane() {
			super();
			MouseDragHelper mdh = new MouseDragHelper(this) {
				void dragDelta( int dx, int dy ) {
					Point oldPosition = getViewport().getViewPosition();
					Dimension viewSize = getViewport().getView().getPreferredSize();
					int maxX = viewSize.width - getViewport().getWidth();
					int maxY = viewSize.height - getViewport().getHeight();
					int newX = oldPosition.x + dx;
					int newY = oldPosition.y + dy;
					if( newX > maxX ) newX = maxX; if( newX < 0 ) newX = 0;
					if( newY > maxY ) newY = maxY; if( newY < 0 ) newY = 0;
					getViewport().setViewPosition(
						new Point( newX, newY )
					);
				}
			};
			
			addMouseListener(mdh);
			addMouseMotionListener(mdh);
		}
	}
	
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
			float scaleQ = (scaleX < scaleY ? scaleX : scaleY) * 0.95f;
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
	JLabel titleLabel;
	JLabel tagsLabelLabel;
	JLabel tagsLabel;
	
	JLabel statusLabelLabel;
	// status labels
	JLabel archivedLabel;
	JLabel deletedLabel;
	JLabel modifiedLabel;
	JLabel doesNotExistLabel;
	
	float zoomUnit = 1.5f;
	
	public PAMainWindow() {
		super();
		setTitle("PictureArchiver4");
		JPanel mainPanel = new JPanel(new BorderLayout());

		ip = new ImagePanel();
		ip.setBorder(null);
		ip.setPreferredSize(new Dimension(512,384));
		//ip.setBorder(new LineBorder(Color.BLUE));
		mainPanel.add(ip, BorderLayout.CENTER);
		
		textPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		textPanel.setBackground(Color.BLACK);
		
		titleLabel = new JLabel();
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setText("");
		
		statusLabelLabel = new JLabel("Status:");
		statusLabelLabel.setVisible(false);
		statusLabelLabel.setOpaque(true);
		statusLabelLabel.setBackground(Color.LIGHT_GRAY);
		statusLabelLabel.setForeground(Color.BLACK);
		
		archivedLabel = new JLabel("Archived");
		archivedLabel.setVisible(false);
		archivedLabel.setForeground(Color.YELLOW);
		
		deletedLabel = new JLabel("Deleted");
		deletedLabel.setVisible(false);
		deletedLabel.setForeground(Color.YELLOW);

		modifiedLabel = new JLabel("Modified");
		modifiedLabel.setVisible(false);
		modifiedLabel.setForeground(Color.YELLOW);

		doesNotExistLabel = new JLabel("Not Found");
		doesNotExistLabel.setVisible(false);
		doesNotExistLabel.setForeground(Color.YELLOW);
		
		tagsLabelLabel = new JLabel("Tags:");
		tagsLabelLabel.setOpaque(true);
		tagsLabelLabel.setVisible(false);
		tagsLabelLabel.setBackground(Color.LIGHT_GRAY);
		tagsLabelLabel.setForeground(Color.BLACK);
		
		tagsLabel = new JLabel("");
		tagsLabel.setVisible(false);
		tagsLabel.setForeground(Color.CYAN);

		textPanel.add(titleLabel);
		textPanel.add(statusLabelLabel);
		textPanel.add(archivedLabel);
		textPanel.add(deletedLabel);
		textPanel.add(modifiedLabel);
		textPanel.add(doesNotExistLabel);
		textPanel.add(tagsLabelLabel);
		textPanel.add(tagsLabel);
		mainPanel.add(textPanel, BorderLayout.SOUTH);
		
		getContentPane().add(mainPanel);
		this.setBackground(Color.ORANGE); // Shouldn't show up!
		
		addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if( e.getKeyCode() == KeyEvent.VK_LEFT ) {
					goToPrevious();
				} else if( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
					goToNext();
				} else if( e.getKeyCode() == KeyEvent.VK_EQUALS || e.getKeyCode() == KeyEvent.VK_PLUS ) {
					changeScale(zoomUnit);
				} else if( e.getKeyCode() == KeyEvent.VK_MINUS ) {
					changeScale(1/zoomUnit);
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
	
	public void updateLabels( State s ) {
		Map metadata = s.metadata;
		String fakeUri = s.fakeUri;
		String title;
		String scaleText;
		if( fakeUri == null ) {
			title = "(No image)";
			scaleText = "";
		} else {
			title = fakeUri;
			int maxTitleLength = 40;
			if( title.length() > 40 ) {
				title = "..." + title.substring(title.length()-maxTitleLength+3);
			}
			scaleText = " (" + new DecimalFormat("0.00").format(s.scale) + "x)";
		}
		if( s.listIndex >= 0 && imageUriList != null ) {
			title += " (" + (s.listIndex+1) + " of " + imageUriList.size() + ")";
		}
		titleLabel.setText(title+scaleText);
		
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
		if( tags != null && tags.length() > 0 ) {
			tagsLabel.setVisible(true);
			tagsLabel.setText(tags);
		} else {
			tagsLabel.setVisible(false);
		}
	}
	
	public void setState( State s, boolean autoscale ) {
		this.state = s;
		if( autoscale ) {
			ip.setImageAutoscale(s.image);
			s.scale = ip.scale;
		} else {
			ip.setImage(s.image, s.scale);
		}
		updateLabels(s);
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
