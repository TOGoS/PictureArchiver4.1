/**
 * 
 */
package togos.picturearchiver4_1;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

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