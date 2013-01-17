package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.visualize.WebPlotView;

/**
 * @author Vianney
 *
 */
public class GFXSurface extends Widget {

	/**
	 * the DOJO surface it means the DOJO canvas 
	 */

    private JavaScriptObject _surface;
    private WebPlotView _pv;


        /**
	 * Creates a canvas
	 * @param el DOM element according to the GWT widget
         * @param pv the WebPlotView
	 *
	 */
	public GFXSurface(Element el, WebPlotView pv) {
		super();
            _pv= pv;
                setElement(el);
	    this.sinkEvents(Event.ONCLICK |
                            Event.MOUSEEVENTS);
	 }

	/**
	 * Creates a canvas, the DOM element of thf GWT widget
	 * will be a DIV
         * @param pv the WebPlotView
	 *
	 */
	public GFXSurface(WebPlotView pv) {
		this(DOM.createDiv(), pv);
	}
	
    /**
     * Creates the surface when the widget is attached.
     */		
	public void onAttach() {
		super.onAttach();
		_surface= initGraphics(getElement(), this, this.getOffsetWidth(), this.getOffsetHeight());
        }


    public JavaScriptObject createGroup() {
        return createGroup(_surface);
    }


        /**
	 * Releases the surface and the graphic components
	 * when the widget is detached from the browser
	 */
	public void onDetach() {
		releaseGraphics(this.getElement(), _surface);
		super.onDetach();
	}


	/**
	 * Returns the DOJO GFX canvas
	 * @return the DOJO GFX canvas
	 */
	JavaScriptObject getDojoCanvas() {
		return this._surface;
	}


	/**
	 *Manages the event from the browser  
	 *@param event the event
	 */
	public void onBrowserEvent(Event event) {
	    super.onBrowserEvent(event);

//            fireMouseEvent(this, event);
	  }
	
	
	/**
	 * Creates the DOJO surface for the canvas
	 * @param node the DOM Node 
	 * @param canvas the GWT canvas
	 * @param width the width of the surface
	 * @param height the height of the surface
	 * @return the DOJO surface
	 */
	private static native JavaScriptObject initGraphics(Element node, GFXSurface canvas, int width, int height) /*-{
		var surface = $wnd.dojox.gfx.createSurface(node, width, height);
//    surface.rawNode.style.position= "absolute";
//    surface.rawNode.style.left= "0px";
//    surface.rawNode.style.top= "0px";
//    surface.rawNode.style.backgroundColor= "green";

//    var rec= surface.createRect({x: 10, y: 10, width: 40, height: 70, stroke: "blue"});
//        rec.setFill([0, 255, 0, 1.0]);
//            var colorAry= [0, 255, 0, 1.0];
//            rec.setFill("red");
//            rec.setStroke({width : 5, color : colorAry});
//


//            surface.handleDragStart      = $wnd.dojo.connect(node,'ondragstart',$wnd.dojo, 'stopEvent');
//	    surface.handleSelectStart    = $wnd.dojo.connect(node,'onselectstart',$wnd.dojo, 'stopEvent');
		
		return surface;
	}-*/;

    public static native JavaScriptObject createGroup(JavaScriptObject surface) /*-{
          return surface.createGroup();

    }-*/;


        /**
	 * Removes the DOM element from the given elemet and release the canvas
	 * @param element the element
	 * @param surface the surface
	 * @see #releaseCanvas(JavaScriptObject)
	 */
	private static void releaseGraphics(Element element, JavaScriptObject surface) {
		final int c = DOM.getChildCount(element);
		for (int i=0; i<c; i++) {
			DOM.removeChild(element, DOM.getChild(element, 0));
		}
		releaseCanvas(surface);
	}
	
	
	/**
	 * Releases the GWT canvas in the DOJO surface 
	 * @param surface the surface
	 */
	private static native void releaseCanvas(JavaScriptObject surface) /*-{
//		$wnd.dojo.disconnect(surface.handleDragStart);
//		$wnd.dojo.disconnect(surface.handleSelectStart);
	}-*/;


    /**
     * A helper for widgets that source mouse events.
     *
     * @param sender the widget sending the event
     * @param event the {@link Event} received by the widget
     */
//    private void fireMouseEvent(Widget sender, Event event) {
//        final Element senderElem = sender.getElement();
//        int x = DOM.eventGetClientX(event)
//                - DOM.getAbsoluteLeft(senderElem)
//                + DOM.getElementPropertyInt(senderElem, "scrollLeft")
//                + Window.getScrollLeft();
//        int y = DOM.eventGetClientY(event)
//                - DOM.getAbsoluteTop(senderElem)
//                + DOM.getElementPropertyInt(senderElem, "scrollTop")
//                + Window.getScrollTop();
//
//        switch (DOM.eventGetType(event)) {
//            case Event.ONMOUSEDOWN:
//                _pv.fireMouseDown(sender, x, y);
//                break;
//            case Event.ONMOUSEUP:
//                _pv.fireMouseUp(sender, x, y);
//                break;
//            case Event.ONMOUSEMOVE:
//                _pv.fireMouseMove(sender, x, y);
//                break;
//            case Event.ONMOUSEOVER:
//                // Only fire the mouseEnter event if it's coming from outside this
//                // widget.
//                Element from = DOM.eventGetFromElement(event);
//                if (from == null || !DOM.isOrHasChild(senderElem, from)) {
//                    _pv.fireMouseEnter(sender);
//                }
//                break;
//            case Event.ONMOUSEOUT:
//                // Only fire the mouseLeave event if it's actually leaving this
//                // widget.
//                Element to = DOM.eventGetToElement(event);
//                if (to == null || !DOM.isOrHasChild(senderElem, to)) {
//                    _pv.fireMouseLeave(sender);
//                }
//                break;
//        }
//    }

		
}//end of class
