/**
 * 
 */
package togos.picturearchiver4_1;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JScrollPane;

class DraggableScrollPane extends JScrollPane
{
	private static final long serialVersionUID = 1L;
	
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