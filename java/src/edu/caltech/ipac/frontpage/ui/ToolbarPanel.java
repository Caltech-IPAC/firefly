package edu.caltech.ipac.frontpage.ui;
/**
 * User: roby
 * Date: 11/1/13
 * Time: 9:01 AM
 */


import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.frontpage.data.DataType;
import edu.caltech.ipac.frontpage.data.DisplayData;

/**
 * @author Trey Roby
 */
public class ToolbarPanel {

    private final int COL= 4;
    public static final int MAX_PANELS= 6;
    private AbsolutePanel labPanel= new AbsolutePanel();
    private HTML moreLabel= new HTML("More");
    private SimplePanel panel= new SimplePanel();


    public ToolbarPanel(String id, JsArray<DisplayData> dataAry) {
        makeUI(id, dataAry);

    }



    private void makeUI(String id, JsArray<DisplayData> dataAry) {

        RootPanel root= FFToolEnv.getRootPanel(id);
        root.setStyleName("largeToolBar");
        FlowPanel hp= new FlowPanel();
        for(int i= 0; (i<dataAry.length()); i++) {
            DisplayData d= dataAry.get(i);
            if (d.getType()== DataType.LINK) {
                hp.add(makeLink(d));
            }
            else if (d.getType()==DataType.MENU) {
                hp.add(makeMenu(d));
            }

            if (i<dataAry.length()-1) {
                hp.add(makeSeparator());

            }
//            hp.add(new Label(d.getName()));
        }
        root.add(panel);
        panel.setWidget(hp);
        panel.setStyleName("largeToolBarMenuWrapper");
        hp.setStyleName("largeToolBarMenu");
    }

    private void changeToHighlight(boolean on) {
    }

    private HTML makeLink(DisplayData d) {
        String linkStr= "<a title=\""+ d.getTip() +
                         "\" class=\"toolbarText\" href=\""+ d.getHref()+
                         "\">"+ d.getName()+"</a>";
        HTML html= new HTML(linkStr);
        html.setStyleName("toolbarElement");
        return html;
    }

    private HTML makeMenu(final DisplayData d) {
        final HTML html= new HTML(d.getName());
        html.setStyleName("toolbarElement");
        html.addStyleName("toolbarText");
        Widget content= makeDropDownContent(d);
        MorePullDown pd= new MorePullDown(html,content, new DataSetHighlightLook(html));
        pd.setOffset(0,0);

        return html;
    }

    private HTML makeSeparator() {
        HTML html= new HTML("|");
        html.setStyleName("toolbarElement");
        html.addStyleName("toolbarSeparator");
        return html;
    }


    private Widget makeDropDownContent(DisplayData d) {
        VerticalPanel vp= new VerticalPanel();
        HTML title= new HTML(d.getName());
        title.setStyleName("dropDownTitle");

        vp.add(title);


        JsArray<DisplayData> ddAry= d.getDrop();

        int rows= ddAry.length()/COL;
        if (ddAry.length() % COL > 0) rows++;

        Grid grid= new Grid(rows,4);
        grid.setCellSpacing(10);

        for(int i=0; (i<ddAry.length()); i++) {
            if (ddAry.get(i).getHref()!=null) {
//                HTML w= new HTML(ddAry.get(i).getName());
                HTML w= makeItem(ddAry.get(i));
                grid.setWidget(i/COL, i % COL, w);
            }
        }
        vp.add(grid);
        return vp;
    }


    private HTML makeItem(DisplayData d) {
        String linkStr= "<a title=\""+ d.getTip() +
                "\" class=\"dropDownTableItem\" href=\""+ d.getHref()+
                "\">"+ d.getName()+"</a>";
        HTML html= new HTML(linkStr);
        html.setStyleName("dropDownItem");
        return html;
    }

//    private String makeLink(DataSetDesc d) {
//        String image= "<img alt=\""+ d.getTip() +"\" title=\""+ d.getTip()+" \" src=\""+d.getImage()+ "\">";
//        String anchor= "<a href=\""+ d.getUrl() +"\">" + image + "</a>";
//        String div= "<div class=\"mission-icon\">"+anchor+"</div>";
//        return div;
//    }

    private class DataSetHighlightLook implements MorePullDown.HighlightLook {

        private final Widget dropWidget;

        private DataSetHighlightLook(Widget dropWidget) {
            this.dropWidget= dropWidget;
        }

        public void enable() {
            dropWidget.addStyleName("toolbarTextSelected");
        }
        public void disable() {
            dropWidget.removeStyleName("toolbarTextSelected");
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
