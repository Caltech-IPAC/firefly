package edu.caltech.ipac.firefly.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SimplePanel;
import edu.caltech.ipac.firefly.core.GeneralCommand;


/**
 * Class that extends the standard MenuItem widget to display
 * some menu text as well as a 2nd component afte the text.
 *
 */public class IconMenuItem extends MenuItem{

    private HorizontalPanel theMenuItem = new HorizontalPanel();
    private Image _image= new Image();
    private static final int _delays[]= {2000, 2500, 2700, 1500, 3000, 3300, 1600, 3500, 3800, 4000, 2200, 2800};
    private static int _delayIdx= -1;
    private static String TMP_ICON= GWT.getModuleBaseURL()+"images/transparent-20x20.gif";

    /**
     * Constructor of the TwoComponentMenuItem - the result is a HTML MenuItem
     * that can be treated as such.
     *
     * @param image The image 
     * @param theCommand The GWT GeneralCommand that will be executed when the menu item is selected.
     */
    public IconMenuItem(Image image,
                        Command theCommand,
                        boolean first){
        super("",false,theCommand);
        _image= image;
        theMenuItem.add(_image);
        setStyleName("firefly-Widget-MenuItem");
        DOM.setStyleAttribute(theMenuItem.getWidget(0).getElement(), "padding", "0px");
        setStyleName(theMenuItem.getElement(),"holder",true);


        theMenuItem.remove(0);
        theMenuItem.add(_image);

        setImage(_image);

        boolean ieBad= true;
        if (theCommand instanceof GeneralCommand) {
           ieBad= !((GeneralCommand)theCommand).isIE6IconBundleSafe();
        }

//        if ( ieBad && BrowserUtil.isBrowser(Browser.IE) && BrowserUtil.getMajorVersion()<=6) {
//            setImageIE(image);
//        }
//        else {
            setImage(image);
//        }




//        if (BrowserUtil.isBrowser(Browser.IE) && BrowserUtil.getMajorVersion()<=7) {
//            setImageIE(imageUrl);
//        }
//        else {
//            setImage(imageUrl);
//        }


    }

    /**
     *
     * @param imageUrl The image to be placed as the 2nd item in the MenuItem.
     */
    public void setImage(final String imageUrl){
        setImageNow(imageUrl);
    }

//    public void setImageIE(final String imageUrl){
//        setImageNow(TMP_ICON);
//        Timer timer= new Timer() {
//            public void run() {
//                if (_image.getUrl().equals(TMP_ICON)) {
//                    setImageNow(imageUrl);
//                }
//            }
//        };
//        _delayIdx++;
//        if (_delayIdx>=_delays.length) _delayIdx= 0;
//        timer.schedule(_delays[_delayIdx++]);
//    }


    public void setImageIE(final Image inImage){
        setImageNow(TMP_ICON);
        Timer timer= new Timer() {
            public void run() {
                if (_image.getUrl().equals(TMP_ICON)) {
                    setImage(inImage);
                }
            }
        };
        _delayIdx++;
        if (_delayIdx>=_delays.length) _delayIdx= 0;
        timer.schedule(_delays[_delayIdx++]);
    }




    public void setImageNow(String imageUrl){
        if (imageUrl!=null) {
            _image.setUrl(imageUrl);
            SimplePanel dummyContainer = new SimplePanel();
            dummyContainer.add(theMenuItem);
            String test = DOM.getInnerHTML(dummyContainer.getElement());
            this.setHTML(test);
        }
    }

    public void setImage(Image image) {
        theMenuItem.remove(0);
        theMenuItem.add(image);
        _image= image;
        SimplePanel dummyContainer = new SimplePanel();
        dummyContainer.add(theMenuItem);
        String test = DOM.getInnerHTML(dummyContainer.getElement());
        this.setHTML(test);

    }



//
//    private static Image makeImage() {
//        final Image im= new Image();
//        im.addLoadListener(new LoadListener() {
//            public void onError(Widget sender) {
//                GwtUtil.showScrollingDebugMsg("could not load: " + im.getUrl());
//            }
//
//            public void onLoad(Widget sender) {
//                GwtUtil.showScrollingDebugMsg("loaded: " + im.getUrl());
//            }
//        });
//        return im;
//    }

}