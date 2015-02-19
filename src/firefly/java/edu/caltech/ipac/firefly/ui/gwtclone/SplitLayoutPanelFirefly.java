/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.caltech.ipac.firefly.ui.gwtclone;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.ArrayList;
import java.util.List;

/**
 * COPIED from GWT 2.0, based of r9061 from gwt svn trunk
 * <p/>
 * A panel that adds user-positioned splitters between each of its child
 * widgets.
 * <p/>
 * <p>
 * This panel is used in the same way as {@link DockLayoutPanel}, except that
 * its children's sizes are always specified in {@link Unit#PX} units, and each
 * pair of child widgets has a splitter between them that the user can drag.
 * </p>
 * <p/>
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that
 * the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
 * declaration.
 * </p>
 * <p/>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-SplitLayoutPanel { the panel itself }</li>
 * <li>.gwt-SplitLayoutPanel .gwt-SplitLayoutPanel-HDragger { horizontal dragger
 * }</li>
 * <li>.gwt-SplitLayoutPanel .gwt-SplitLayoutPanel-VDragger { vertical dragger }
 * </li>
 * </ul>
 * <p/>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.SplitLayoutPanelExample}
 * </p>
 */
public class SplitLayoutPanelFirefly extends DockLayoutPanel {

    private List<Splitter> vSplitterList = new ArrayList<Splitter>(3);
    private List<Splitter> hSplitterList = new ArrayList<Splitter>(3);
    private int minCenterWidth = 5;
    private int minCenterHeight = 5;
    private ResizeListener resizeListener = null;
    private HandlerRegistration handreg = null;

    private int _cacheHeight = -1;
    private int _cacheLocY = -1;
    private int _cacheWidth = -1;
    private int _cacheLocX = -1;

    class HSplitter extends Splitter {
        public HSplitter(Widget target, boolean reverse) {
            super(target, reverse);
            getElement().getStyle().setPropertyPx("width", SPLITTER_SIZE);
            setStyleName("gwt-SplitLayoutPanel-HDragger");
        }

        @Override
        protected int getAbsolutePosition() {
            return getAbsoluteLeft();
        }

        @Override
        protected int getEventPosition(Event ev) {
            JsArray<Touch> touches = ev.getTouches();
            int retval;
            if (touches != null && touches.length() > 0) {
                Touch t = touches.get(0);
                retval = t.getPageX();
//            retval= t.getClientX() - getAbsoluteLeft();
//            GwtUtil.showDebugMsg("retval="+retval+ ", cx="+t.getClientX()+ ", al=" +getAbsoluteLeft());

            } else {
                retval = ev.getClientX() + Window.getScrollLeft();
            }
            return retval;
        }

        @Override
        protected int getTargetPosition() {
            return target.getAbsoluteLeft();
        }

        @Override
        protected int getTargetSize() {
            return target.getOffsetWidth();
        }
    }

    abstract class Splitter extends Widget {
        protected final Widget target;

        private int offset;
        private boolean mouseDown;
        private Command layoutCommand;

        private final boolean reverse;
        private int minSize;

        public Splitter(Widget target, boolean reverse) {
            this.target = target;
            this.reverse = reverse;

            setElement(Document.get().createDivElement());
            sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEMOVE
                               | Event.ONDBLCLICK | Event.ONTOUCHSTART | Event.ONTOUCHEND | Event.ONTOUCHMOVE);
        }

        @Override
        public void onBrowserEvent(Event event) {
            switch (event.getTypeInt()) {
                case Event.ONTOUCHSTART:
                case Event.ONMOUSEDOWN:
                    mouseDown = true;
                    offset = getEventPosition(event) - getAbsolutePosition();
                    Event.setCapture(getElement());
                    event.preventDefault();
//            GwtUtil.showDebugMsg("start: offset: " + offset);
                    break;

                case Event.ONTOUCHEND:
                case Event.ONMOUSEUP:
//            GwtUtil.showDebugMsg("end");
                    mouseDown = false;
                    Event.releaseCapture(getElement());
                    event.preventDefault();
                    break;

                case Event.ONTOUCHMOVE:
                case Event.ONMOUSEMOVE:
                    if (mouseDown) {
                        int size;
                        int evPos = getEventPosition(event);
                        if (reverse) {
                            size = getTargetPosition() + getTargetSize()
                                    - evPos - offset;
                        } else {
                            size = evPos - getTargetPosition() - offset;
                        }
//              GwtUtil.showDebugMsg("move: size: " + size);
                        if (!isOverlapping(evPos, size)) {
                            setAssociatedWidgetSize(size);
                        }
                        event.preventDefault();
                    }
                    break;
            }
        }

        private boolean isOverlapping(int position, int size) {

            boolean overlap = false;
            boolean horizontal = (this instanceof HSplitter);
            if (horizontal && hSplitterList.size() > 1) {
                int x = this.getAbsoluteLeft();
                for (Splitter splitter : hSplitterList) {
                    if (splitter != this) {
                        if (splitter.getAbsoluteLeft() > x - 3 && splitter.getAbsoluteLeft() < x + 3) {
                            overlap = true;
                            break;
                        }
                    }
                }
            } else if (vSplitterList.size() > 1) {
                int y = this.getAbsoluteTop();
                for (Splitter splitter : vSplitterList) {
                    if (splitter != this) {
                        if (splitter.getAbsoluteTop() > y - 3 && splitter.getAbsoluteTop() < y + 3) {
                            overlap = true;
                            break;
                        }
                    }
                }
            }

            if (!overlap && getCenter() != null) {
                updateCachePos();
                if (horizontal) {
                    if (reverse) {
                        overlap = position < (getCenter().getAbsoluteLeft() + minCenterWidth);
                    } else {
                        overlap = position > _cacheLocX + _cacheWidth;
                    }
                } else {
                    if (reverse) {
                        overlap = position < (getCenter().getAbsoluteTop() + minCenterHeight);
                    } else {
                        overlap = position > _cacheLocX + _cacheHeight;
                    }

                }
            }
            return overlap;
        }

        public void setMinSize(int minSize) {
            this.minSize = minSize;
            LayoutData layout = (LayoutData) target.getLayoutData();

            // Try resetting the associated widget's size, which will enforce the new
            // minSize value.
            setAssociatedWidgetSize((int) layout.size);
        }

        protected abstract int getAbsolutePosition();

        protected abstract int getEventPosition(Event event);

        protected abstract int getTargetPosition();

        protected abstract int getTargetSize();

        private void setAssociatedWidgetSize(int size) {
            if (size < minSize) {
                size = minSize;
            }

            LayoutData layout = (LayoutData) target.getLayoutData();
            if (size == layout.size) {
                return;
            }

            layout.size = size;

            // Defer actually updating the layout, so that if we receive many
            // mouse events before layout/paint occurs, we'll only update once.
            if (layoutCommand == null) {
                layoutCommand = new Command() {
                    public void execute() {
                        layoutCommand = null;
                        forceLayout();
                    }
                };
                DeferredCommand.addCommand(layoutCommand);
            }
        }
    }

    class VSplitter extends Splitter {
        public VSplitter(Widget target, boolean reverse) {
            super(target, reverse);
            getElement().getStyle().setPropertyPx("height", SPLITTER_SIZE);
            setStyleName("gwt-SplitLayoutPanel-VDragger");
        }

        @Override
        protected int getAbsolutePosition() {
            return getAbsoluteTop();
        }

        @Override
        protected int getEventPosition(Event ev) {
            JsArray<Touch> touches = ev.getTouches();
            int retval;
            if (touches != null && touches.length() > 0) {
                Touch t = touches.get(0);
                retval = t.getPageY();
//          retval= t.getClientY() - getAbsoluteTop();
//          GwtUtil.showDebugMsg("retval="+retval+ ", cx="+t.getClientY()+ ", al=" +getAbsoluteTop());
            } else {
                retval = ev.getClientY() + Window.getScrollTop();
            }
            return retval;
        }

        @Override
        protected int getTargetPosition() {
            return target.getAbsoluteTop();
        }

        @Override
        protected int getTargetSize() {
            return target.getOffsetHeight();
        }
    }


    private static final int SPLITTER_SIZE = 8;

    public SplitLayoutPanelFirefly() {
        super(Unit.PX);
        setStyleName("gwt-SplitLayoutPanel");

    }

    @Override
    public void insert(Widget child, Direction direction, double size, Widget before) {
        super.insert(child, direction, size, before);
        if (direction != Direction.CENTER) {
            insertSplitter(child, before);
        }
    }

    @Override
    public boolean remove(Widget child) {
        assert !(child instanceof Splitter) : "Splitters may not be directly removed";

        if (super.remove(child)) {
            // Remove the associated splitter, if any.
            int idx = getWidgetIndex(child);
            if (idx < getWidgetCount() - 1) {
                Widget w = getWidget(idx + 1);
                if (w instanceof HSplitter) {
                    hSplitterList.remove(w);
                } else if (w instanceof VSplitter) {
                    vSplitterList.remove(w);
                }
                // Call super.remove(), or we'll end up recursing.
                super.remove(getWidget(idx + 1));
            }
            return true;
        }
        return false;
    }

    /**
     * Sets the minimum allowable size for the given widget.
     * <p/>
     * <p>
     * Its assocated splitter cannot be dragged to a position that would make it
     * smaller than this size. This method has no effect for the
     * {@link DockLayoutPanel.Direction#CENTER} widget.
     * </p>
     *
     * @param child   the child whose minimum size will be set
     * @param minSize the minimum size for this widget
     */
    public void setWidgetMinSize(Widget child, int minSize) {
        Splitter splitter = getAssociatedSplitter(child);
        splitter.setMinSize(minSize);
    }

    private Splitter getAssociatedSplitter(Widget child) {
        // If a widget has a next sibling, it must be a splitter, because the only
        // widget that *isn't* followed by a splitter must be the CENTER, which has
        // no associated splitter.
        int idx = getWidgetIndex(child);
        if (idx < getWidgetCount() - 2) {
            Widget splitter = getWidget(idx + 1);
            assert splitter instanceof Splitter : "Expected child widget to be splitter";
            return (Splitter) splitter;
        }
        return null;
    }

    private void insertSplitter(Widget widget, Widget before) {
        assert getChildren().size() > 0 : "Can't add a splitter before any children";

        LayoutData layout = (LayoutData) widget.getLayoutData();
        Splitter splitter = null;
        switch (layout.direction) {
            case WEST:
                splitter = new HSplitter(widget, false);
                hSplitterList.add(splitter);
                break;
            case EAST:
                splitter = new HSplitter(widget, true);
                hSplitterList.add(splitter);
                break;
            case NORTH:
                splitter = new VSplitter(widget, false);
                vSplitterList.add(splitter);
                break;
            case SOUTH:
                splitter = new VSplitter(widget, true);
                vSplitterList.add(splitter);
                break;
            default:
                assert false : "Unexpected direction";
        }

        super.insert(splitter, layout.direction, SPLITTER_SIZE, before);
    }


    public void setMinCenterSize(int w, int h) {
        minCenterWidth = w;
        minCenterHeight = h;
    }


    @Override
    protected void onLoad() {
        super.onLoad();
        if (resizeListener == null) {
            resizeListener = new ResizeListener();
            WebEventManager.getAppEvManager().addListener(Name.WINDOW_RESIZE, resizeListener);
        }
        if (handreg == null) {
            handreg = Window.addResizeHandler(new CheckWindowResize());
        }
    }

    @Override
    protected void onUnload() {
        super.onUnload();
        if (resizeListener != null) {
            WebEventManager.getAppEvManager().removeListener(resizeListener);
            resizeListener = null;
        }
        if (handreg != null) {
            handreg.removeHandler();
            handreg = null;
        }
    }

    protected void checkSplitters() {
        for (Splitter splitter : vSplitterList) {
            if (splitter.reverse && splitter.getAbsolutePosition() - this.getAbsoluteTop() < minCenterHeight) {
                int size = splitter.getTargetSize() - (this.getAbsoluteTop() - splitter.getAbsolutePosition() + minCenterHeight);
                splitter.setAssociatedWidgetSize(size);
            }

        }
        for (Splitter splitter : hSplitterList) {
            if (splitter.reverse && splitter.getAbsolutePosition() - this.getAbsoluteLeft() < minCenterWidth) {
                int size = splitter.getTargetSize() - (this.getAbsoluteLeft() - splitter.getAbsolutePosition() + minCenterWidth);
                splitter.setAssociatedWidgetSize(size);

            }
        }

    }

    private void updateCachePos() {
        if (_cacheHeight == -1) _cacheHeight = SplitLayoutPanelFirefly.this.getOffsetHeight();
        if (_cacheLocY == -1) _cacheLocY = SplitLayoutPanelFirefly.this.getAbsoluteTop();
        if (_cacheWidth == -1) _cacheWidth = SplitLayoutPanelFirefly.this.getOffsetWidth();
        if (_cacheLocX == -1) _cacheLocX = SplitLayoutPanelFirefly.this.getAbsoluteLeft();
    }

    private void resetCachePos() {
        _cacheHeight = -1;
        _cacheLocY = -1;
        _cacheWidth = -1;
        _cacheLocX = -1;

    }

    private class ResizeListener implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    if (GwtUtil.isOnDisplay(SplitLayoutPanelFirefly.this)) {
                        resetCachePos();
                        checkSplitters();
                    }
                }
            });
        }
    }

    public class CheckWindowResize implements ResizeHandler {
        public void onResize(ResizeEvent event) {
            if (GwtUtil.isOnDisplay(SplitLayoutPanelFirefly.this)) {
                resetCachePos();
                checkSplitters();
                SplitLayoutPanelFirefly.this.onResize();
            }
        }
    }
}