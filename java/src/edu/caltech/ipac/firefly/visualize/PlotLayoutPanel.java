/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 11/3/11
 * Time: 2:14 PM
 */


import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopoutToolbar;


/**
* @author Trey Roby
*/
public class PlotLayoutPanel extends LayoutPanel {

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
    private boolean deleteEnabled= false;
    private PopoutToolbar popoutToolbar= null;
    private boolean inlineTitleVisible= false;
    private boolean inlineToolPanelVisible= false;
    private final DeckLayoutPanel deckPanel= new DeckLayoutPanel();
    private final HTML errorMsg= new HTML();
    private final SimplePanel errorWrapper= new SimplePanel(errorMsg);

    public PlotLayoutPanel(MiniPlotWidget mpw, PlotWidgetFactory plotWidgetFactory) {
        _mpw= mpw;

        deckPanel.add(mpw.getPlotView());
        deckPanel.add(errorWrapper);
        deckPanel.showWidget(mpw.getPlotView());
        add(deckPanel);

        errorMsg.addStyleName("mpw-error-msg");
        errorWrapper.addStyleName("mpw-error-wrapper");

//        add(mpw.getPlotView());

        addListeners();
        addStyleName("plot-layout-panel");

        if (plotWidgetFactory!=null) setupDelete(plotWidgetFactory);
    }

    void setPlotIsExpanded(boolean expanded) {
        if (controlPopoutToolbar) {
            setInlineToolPanelVisible(!expanded);
        }

    }

    public void showError(String error) {
        errorMsg.setHTML(error);
        deckPanel.showWidget(errorWrapper);
    }

    public void clearError() {
        deckPanel.showWidget(_mpw.getPlotView());
    }

    private void setInlineTitleVisible(boolean v) {
        if (v==inlineTitleVisible) return;

        inlineTitleVisible= v;
        if (v) {
            setWidgetTopHeight(_inlineTitle, 0, Style.Unit.PX,
                                                         INLINE_TITLE_HEIGHT, Style.Unit.PX);
            setWidgetLeftRight(_inlineTitle, 0,Style.Unit.PX, 40, Style.Unit.PX );
        }
        else {
            setWidgetTopHeight(_inlineTitle,0, Style.Unit.PX,0, Style.Unit.PX);
        }
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                forceLayout();
            }
        });
    }

    private void setInlineToolPanelVisible(final boolean v) {
        if (controlPopoutToolbar && popoutToolbar.getParent()==this) {
            if (inlineToolPanelVisible==v) return;
            inlineToolPanelVisible= v;
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
            if (_titleLabelHtml==null) _titleLabelHtml= "";
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

    public void setupDelete(final PlotWidgetFactory plotWidgetFactory) {

        if (plotWidgetFactory!=null) {
            deleteEnabled= true;
            Image delImage= new Image(IconCreator.Creator.getInstance().getBlueDelete10x10());
            Widget delButton= GwtUtil.makeImageButton(delImage,"Delete Image",new ClickHandler() {
                public void onClick(ClickEvent event) {
                    plotWidgetFactory.delete(_mpw);
                }
            });
            add(delButton);
            setWidgetTopHeight(delButton, 0, Style.Unit.PX, 12, Style.Unit.PX);
            setWidgetRightWidth(delButton,0,Style.Unit.PX,14, Style.Unit.PX);
            GwtUtil.setStyle(delButton, "backgroundColor", "transparent");
        }
    }

    public void enableControlPopoutToolbar() {
        controlPopoutToolbar= true;
        popoutToolbar= _mpw.getPopoutToolbar();
        _mpw.setDefaultToolbarHeight(20);
        popoutToolbar.setExpandIconImage(new Image(IconCreator.Creator.getInstance().getBorderedExpandIcon()));
        popoutToolbar.setBackgroundAlwaysTransparent(true);
        inlineToolPanelVisible= !AllPlots.getInstance().isExpanded();
        popoutToolbar.showToolbar(inlineToolPanelVisible);
        add(popoutToolbar);
        setWidgetTopHeight(popoutToolbar, 0, Style.Unit.PX, inlineToolPanelVisible?TOOL_HEIGHT:0, Style.Unit.PX);
        setWidgetRightWidth(popoutToolbar,deleteEnabled?14:0,Style.Unit.PX,_mpw.getToolbarWidth(), Style.Unit.PX);
        GwtUtil.setStyle(popoutToolbar, "backgroundColor", "transparent");
    }

    private void hideInlineTitleTemporarily(Delay delay) {
        if (false && _showInlineTitle) {
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

