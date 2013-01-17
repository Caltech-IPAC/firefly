package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Trey Roby
 * @version $Id: DisabledPane.java,v 1.2 2009/08/21 17:18:55 roby Exp $
 */
public class DisabledPane extends Component {

    private AbsolutePanel panel = new AbsolutePanel();
    private FlowPanel labelPanel= new FlowPanel();
    private Widget messageWidget;

    private BrowserHandler _blist;


    /**
     *
     * @param messageWidget  widget
     *
     * */
  public DisabledPane(Widget messageWidget) {

         _blist= new BrowserHandler();

        this.messageWidget = messageWidget;
        this.initWidget(panel);

        build();

    }

    public void setPixelSize(int width, int height) {
        super.setPixelSize(width,height);
        locateMask();
    }

    public void build() {
        panel.setStyleName("firefly-disable-panel");

        labelPanel.add(messageWidget);
        labelPanel.setStyleName("maskingMessage-ui");
        panel.add(labelPanel,0,0);

        locateMask();
        Window.addResizeHandler(_blist);
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        locateMask();
    }

    public void update() { locateMask(); }

    private void locateMask() {
        final int w= labelPanel.getOffsetWidth();
        final int h= labelPanel.getOffsetHeight();
        if (w>0 || h>0) {
            int left = panel.getOffsetWidth()/2 - w/2;
            int top = panel.getOffsetHeight()/2 - h/2;
            panel.setWidgetPosition(labelPanel,left,top);

//            panel.setWidth(w+"px");
//            panel.setHeight(h+"px");
        }
    }


    private int reloCnt= 0;
    private class BrowserHandler implements ResizeHandler {
        public void onResize(ResizeEvent event) {
            reloCnt++;
            locateMask();
        }

    }

}