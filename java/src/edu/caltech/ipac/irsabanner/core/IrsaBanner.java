package edu.caltech.ipac.irsabanner.core;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.MetaElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import edu.caltech.ipac.irsabanner.resbundle.images.BannerImage;

import java.util.ArrayList;
import java.util.List;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class IrsaBanner implements EntryPoint {

    public static final String PRIMARY_DEF = "http://irsa.ipac.caltech.edu/irsa-banner.js";
    public static final String PRIMARY = "IRSA:menu-primary";
    public static final String SECONDARY = "IRSA:menu-secondary";

    private static AbsolutePanel _layoutPanel= new AbsolutePanel();
    private static IMenuBar _rootMenuBar;
    private static List<String> _secondaryURLs= new ArrayList<String>(2);

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        RootPanel root = RootPanel.get("irsa-banner");
        init(root);
        registerExternalJS();
        registerTestJS();

        String primaryURL= PRIMARY_DEF;

        NodeList<Element> list= Document.get().getElementsByTagName("meta");
        if (list!=null) {
            for(int i= 0; i<list.getLength(); i++) {
                try {
                    Element e= list.getItem(i);
                    MetaElement me= MetaElement.as(e);
                    if (PRIMARY.equals(me.getName())) {
                        primaryURL= me.getContent();
                    }
                    else if (SECONDARY.equals(me.getName())) {
                        _secondaryURLs.add(me.getContent());
                    }
                    else {

                    }
                } catch (Exception e) {
                    //do nothing
                }
            }
        }
        getJson(primaryURL);
    }



    private static void init(RootPanel root) {

        BannerImage images= BannerImage.Creator.getInstance();

        SimplePanel belowBanner= new SimplePanel();
        root.add(belowBanner);
        root.setSize("100%","91px");
        DOM.setStyleAttribute(root.getElement(), "overflow", "hidden");


        belowBanner.setWidget(_layoutPanel);
        belowBanner.setStyleName("banner-background");
        belowBanner.setSize("100%","91px");

        Image banner= new Image(images.getBannerMain());
        Image dropBackLeft= new Image(images.getDropDownBackLeft());
        /*
        Label clickArea= new Label();
        clickArea.setSize("150px", "90px");
        clickArea.setTitle("Return to IRSA Home page");
        clickArea.setStyleName("go-home-area");
        clickArea.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                Window.Location.assign("http://irsa.ipac.caltech.edu");
            }
        });
        */

        String anchor= "<a class=\"go-home-area\" " +
                       "title= \"Return to IRSA home page\" "+
                       "href=http://irsa.ipac.caltech.edu>" +
                       "<div style=\"width:150px;height:72px\"></div></a>";
        HTML clickArea= new HTML(anchor);


        _layoutPanel.add(banner,0,0);
        _layoutPanel.add(dropBackLeft,0,70);
        _layoutPanel.add(clickArea,15,8);

    }

    private static void buildMenuBar(DropDownData data[]) {
        BannerImage images= BannerImage.Creator.getInstance();
        _rootMenuBar= createMenuBar(data,false);
        HorizontalPanel hp= new HorizontalPanel();
        hp.add(_rootMenuBar);
        hp.add(new Image(images.getDropDownBackRight()));
        _layoutPanel.add(hp,183,70);
        _rootMenuBar.setHeight("22px");
        _layoutPanel.setSize("100%", "100%");

        for(String url :_secondaryURLs) getJson(url);
        _secondaryURLs.clear();
        _secondaryURLs= null;
    }


    private static IMenuBar createMenuBar(DropDownData data[],
                                         boolean vertical) {
        IMenuBar mbar= new IMenuBar(vertical);

        for(DropDownData d : data) {
            if (d.getType()==DataType.SEPARATOR) {
                if (d.getIndex()>-1)  mbar.insertSeparator(d.getIndex());
                else                  mbar.addSeparator();
            }
            else if (d.getType()==DataType.MENU) {
                MenuBar subm= createMenuBar(d.getDrop(),true);
                mbar.addItem(new IMenuItem(d,subm));
            }
            else {
                mbar.addItem(new IMenuItem(d));
            }
        }
        return mbar;

    }


    private static void addToMenuBar(IMenuBar     mbar,
                                     DropDownData data[]) {
        if (mbar==null || data==null) return;
        for(DropDownData d : data) {
            if (d.getType()==DataType.SEPARATOR) {
                if (d.getIndex()>-1)  mbar.insertSeparator(d.getIndex());
                else                  mbar.addSeparator();
            }
            else {
                IMenuItem item= findItem(mbar,d);
                if (item==null) {
                    if (d.getType()==DataType.MENU) {
                        IMenuBar subm= new IMenuBar(true);
                        mbar.addItem(new IMenuItem(d,subm));
                        addToMenuBar(subm,d.getDrop());
                    }
                    else {
                        if (d.getIndex()>-1)  mbar.insertItem(new IMenuItem(d),d.getIndex());
                        else                  mbar.addItem(new IMenuItem(d));
                    }

                }
                else {
                    if (d.getType()==DataType.MENU) {
                        addToMenuBar((IMenuBar)item.getSubMenu(),d.getDrop());
                    }
                }

            }
        }

    }

    private static IMenuItem findItem(IMenuBar mbar, DropDownData data) {
        IMenuItem  retval= null;
        for(MenuItem mi : mbar.getItems()) {
            if (  ((IMenuItem)mi).getData().getName().equals(data.getName())) {
                retval= (IMenuItem)mi;
            }
        }
        return retval;
    }

    public static void setBannerData(DropDownData data[]) {
        if (_rootMenuBar==null) {
            buildMenuBar(data);
        }
        else {
            addToMenuBar(_rootMenuBar,data);
        }
    }


    private static native void registerExternalJS() /*-{
              $wnd.irsaBannerSetData = @edu.caltech.ipac.irsabanner.core.IrsaBanner::setBannerData([Ledu/caltech/ipac/irsabanner/core/DropDownData;);
}-*/;

    /**
     * Make call to remote server.
     * @param url the url of the json data
     */
      public native static void getJson(String url)  /*-{
       // [1] Create a script element.
       var script = document.createElement("script");
       script.setAttribute("src", url);
       script.setAttribute("type", "text/javascript");
       document.body.appendChild(script);
      }-*/;


//    private static DropDownData findData() {
//        IMenuBar mbar= _rootMenuBar;
//        IMenuItem item= null;
//        boolean found= false;
//        while(!found) {
//            item= (IMenuItem)mbar.getSelectedItem();
//            if (item.getSubMenu()==null) {
//                found= true;
//            }
//            else {
//                mbar= (IMenuBar)item.getSubMenu();
//            }
//
//        }
//        return item.getData();
//    }


    private static class IMenuItem extends MenuItem {

        private final DropDownData _data;


        public IMenuItem(DropDownData data) {
            super(data.getName(), (Command)null);
            _data= data;
            init(data);
        }

        public IMenuItem(DropDownData data, MenuBar subMenu) {
            super(data.getName(), subMenu);
            _data= data;
            init(data);
        }

        private void init(DropDownData data) {
            setTitle(data.getTip());
            if (data.getHref()!=null) {
                String html= "<a class=\"gwt-MenuItem\" href=\""+data.getHref() + "\"><div style=\"width:100%\">" + data.getName() + "</div></a>";
                setHTML(html);
            }
        }

        public DataType getDataType() {
            DataType retval= DataType.MENU;
            if (_data!=null)  retval= _data.getType();
            return  retval;
        }

        public DropDownData getData() { return _data;}
    }



    private static class IMenuBar extends MenuBar {

        public IMenuBar(boolean vertical) {
            super(vertical, BannerImage.Creator.getInstance());
            setAnimationEnabled(true);
            setAutoOpen(true);
            setStyleName("irsaMenuBar");
            if (vertical) {
                addStyleName("irsaMenuBar-vertical");
            }
            else {
                addStyleName("irsaMenuBar-horizontal");
            }
        }

        @Override
        public MenuItem getSelectedItem() {
            return super.getSelectedItem();
        }

        @Override
        public List<MenuItem> getItems() {
            return super.getItems();
        }

    }

    private static void testMethod() {
                Window.alert("made it");
    }

    private static native void registerTestJS() /*-{
              $wnd.testMethod = @edu.caltech.ipac.irsabanner.core.IrsaBanner::testMethod();
}-*/;


}
