package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 11/3/11
 * Time: 2:14 PM
 */


import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopoutToolbar;


/**
* @author Trey Roby
*/
public class InlineTitleLayoutPanel extends LayoutPanel {

    private static final int TITLE_SHOW_DELAY= 1000;
    private static final int TITLE_HIDE_DELAY= 250;
    private int _titleHideDelay = TITLE_HIDE_DELAY;
    private static final int INLINE_TITLE_HEIGHT= 25;
    private static final int TOOL_HEIGHT= 25;
    private enum Delay {USE_DELAY, NODELAY}

    private ReshowInLineTitleTimer _reshowTitleTimer = null;
    private HideInLineTitleTimer _hideTitleTimer= null;
    private boolean   _showInlineTitle= false;
    private boolean   _mouseOverInlineTitle= false;
    private HTML      _inlineTitle;
    private boolean   _titleIsAd= false;
    private final  MiniPlotWidget _mpw;
    private String _titleLabelHtml;
    private boolean controlPopoutToolbar= false;
    private PopoutToolbar popoutToolbar= null;

    public InlineTitleLayoutPanel(MiniPlotWidget mpw) {
        _mpw= mpw;
        add(_mpw.getPlotView());
//        setWidgetLeftRight(_mpw.getPlotView(), 0, Style.Unit.PX, 2, Style.Unit.PX);
//        setWidgetTopBottom(_mpw.getPlotView(), 0, Style.Unit.PX, 2, Style.Unit.PX);
        addListeners();
    }

    void setPlotIsExpanded(boolean expanded) {
        if (controlPopoutToolbar) {
            setInlineToolPanelVisible(!expanded);
        }

    }

    private void setInlineTitleVisible(boolean v) {
        if (v) {
            setWidgetTopHeight(_inlineTitle, 0, Style.Unit.PX,
                                                         INLINE_TITLE_HEIGHT, Style.Unit.PX);
            setWidgetLeftRight(_inlineTitle, 0,Style.Unit.PX, 40, Style.Unit.PX );
        }
        else {
            setWidgetTopHeight(_inlineTitle,0, Style.Unit.PX,0, Style.Unit.PX);
        }
        forceLayout();
    }

    private void setInlineToolPanelVisible(final boolean v) {
        if (controlPopoutToolbar) {
            if (v) {
                setWidgetTopHeight(popoutToolbar, 0, Style.Unit.PX,
                                   TOOL_HEIGHT, Style.Unit.PX);
            }
            else {
                setWidgetTopHeight(popoutToolbar,0, Style.Unit.PX,0, Style.Unit.PX);
            }
            forceLayout();
        }
    }


    public void setShowInlineTitle(boolean show) {
        _showInlineTitle= show;
        updateInLineTitle(_titleLabelHtml);
    }

    public void setTitleIsAd(boolean ad) {
        _titleIsAd= ad;
        _titleHideDelay= _titleIsAd ? 2000 : TITLE_HIDE_DELAY;
    }

    public boolean getShowInlineTitle() { return _showInlineTitle; }

    public void updateInLineTitle(String html) {
        _titleLabelHtml= html;
        if (_showInlineTitle) {
            if (_inlineTitle==null) createInlineTitle();
            setInlineTitleVisible(true);
            if (_hideTitleTimer !=null) _hideTitleTimer.cancel();
            String s= "<span class=\"inline-plot-title\">" + _titleLabelHtml+ "</span>";
            _inlineTitle.setHTML(s);
        }
        else {
            if (_inlineTitle!=null) {
                setInlineTitleVisible(false);
            }
        }
    }

    private void createInlineTitle() {
        _inlineTitle= new HTML();
        _inlineTitle.addMouseOverHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                hideInlineTitleTemporarily(Delay.USE_DELAY);
                _mouseOverInlineTitle= true;
            }
        });
        _inlineTitle.addMouseOutHandler(new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent event) {
                _mouseOverInlineTitle= false;
            }
        });
        _inlineTitle.addTouchStartHandler(new TouchStartHandler() {
            public void onTouchStart(TouchStartEvent event) {
                hideInlineTitleTemporarily(Delay.NODELAY);
            }
        });
        add(_inlineTitle);
        setWidgetTopHeight(_inlineTitle,0, Style.Unit.PX,INLINE_TITLE_HEIGHT, Style.Unit.PX);
        setWidgetLeftRight(_inlineTitle,0, Style.Unit.PX,0, Style.Unit.PX);
    }


    public void enableControlPopoutToolbar() {
        controlPopoutToolbar= true;
        popoutToolbar= _mpw.getPopoutToolbar();
        popoutToolbar.setExpandIconImage(new Image(IconCreator.Creator.getInstance().getBorderedExpandIcon()));
        popoutToolbar.setBackgroundAlwaysTransparent(true);
        popoutToolbar.showToolbar(!_mpw.isExpanded());
        add(popoutToolbar);
        setWidgetTopHeight(popoutToolbar, 0, Style.Unit.PX, TOOL_HEIGHT, Style.Unit.PX);
        setWidgetRightWidth(popoutToolbar,0,Style.Unit.PX,_mpw.getToolbarWidth(), Style.Unit.PX);
        GwtUtil.setStyle(popoutToolbar, "backgroundColor", "transparent");
    }

    private void hideInlineTitleTemporarily(Delay delay) {
        if (_showInlineTitle) {
            if (_reshowTitleTimer ==null) _reshowTitleTimer = new ReshowInLineTitleTimer();
            if (_hideTitleTimer==null)   _hideTitleTimer = new HideInLineTitleTimer();

            if (delay==Delay.USE_DELAY) {
                _hideTitleTimer.schedule(_titleHideDelay);
            }
            else {
                setInlineTitleVisible(false);
            }
            _reshowTitleTimer.scheduleDelay();
        }
    }

    private void cancelHideInlineTitle() {
        if (_showInlineTitle) {
            setInlineTitleVisible(true);
            if (_reshowTitleTimer!=null) _reshowTitleTimer.cancel();
            if (_hideTitleTimer!=null) _hideTitleTimer.cancel();
        }
    }

    private void addListeners() {
        WebPlotView.MouseAll ma= new WebPlotView.DefMouseAll() {
            @Override
            public void onMouseOver(WebPlotView pv, ScreenPt spt) {
                _mouseOverInlineTitle= true;

            }

            @Override
            public void onMouseOut(WebPlotView pv) {
                cancelHideInlineTitle();
                _mouseOverInlineTitle= false;
            }

            @Override
            public void onMouseMove(WebPlotView pv, ScreenPt spt) {
                if (spt.getIY() - pv.getScrollY() < INLINE_TITLE_HEIGHT+1) {
                    hideInlineTitleTemporarily(Delay.USE_DELAY);
                    _mouseOverInlineTitle= true;
                }
                else {
                    cancelHideInlineTitle();
                    _mouseOverInlineTitle= false;
                }
            }

            @Override
            public void onTouchStart(WebPlotView pv, ScreenPt spt, TouchStartEvent ev) {
                hideInlineTitleTemporarily(Delay.NODELAY);
            }

        };

        _mpw.getPlotView().addPersistentMouseInfo(new WebPlotView.MouseInfo(ma, ""));
    }



    private class ReshowInLineTitleTimer extends Timer {

        @Override
        public void run() {
            if (_inlineTitle!=null && _showInlineTitle) {
                if (!_mouseOverInlineTitle) {
                    setInlineTitleVisible(true);
                    _hideTitleTimer.cancel();
                }
                else {
                    scheduleDelay();
                }
            }
        }

        private void scheduleDelay() {
            this.cancel();
            this.schedule(_titleHideDelay);
        }
    }

    private class HideInLineTitleTimer extends Timer {

        @Override
        public void run() {
            if (_inlineTitle!=null) {
                setInlineTitleVisible(false);
            }
        }
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
