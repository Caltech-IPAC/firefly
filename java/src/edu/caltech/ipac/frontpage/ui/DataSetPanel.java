package edu.caltech.ipac.frontpage.ui;
/**
 * User: roby
 * Date: 11/1/13
 * Time: 9:01 AM
 */


import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.frontpage.core.FrontpageUtils;
import edu.caltech.ipac.frontpage.data.DisplayData;

import java.util.Iterator;
import java.util.List;

/**
 * @author Trey Roby
 */
public class DataSetPanel {

    public static final int MAX_PANELS= 6;
    AbsolutePanel labPanel= new AbsolutePanel();
    HTML moreLabel= new HTML("More");


    public DataSetPanel(String id, List<DisplayData> dsList) {

        makeUI(id, dsList);

    }



    private void makeUI(String id, List<DisplayData> dsList) {

        RootPanel root= FFToolEnv.getRootPanel(id);
        HorizontalPanel hp= new HorizontalPanel();
        root.add(hp);
        Iterator<DisplayData> iterator= dsList.iterator();
        int i=0;

        for(; (i<MAX_PANELS && iterator.hasNext()); i++) {

            DisplayData d= iterator.next();
            String div= makeLink(d);
            HTML entry= new HTML(div);
            hp.add(entry);
        }

        if (i<dsList.size()) {
            labPanel.add(moreLabel, 10, 25);
            labPanel.addStyleName("mission-icon-size");
            labPanel.addStyleName("mission-icon");
            hp.add(labPanel);
            moreLabel.setStyleName("mission-more-label");
//            GwtUtil.setStyle(l,"color", "white");
//            GwtUtil.setStyle(l,"fontSize", "24pt");

            HorizontalPanel hpPop= new HorizontalPanel();
            for(; (iterator.hasNext());) {

                DisplayData d= iterator.next();
                String div= makeLink(d);
                HTML entry= new HTML(div);
                hpPop.add(entry);
            }
            SimplePanel p= new SimplePanel(hpPop);
            new MorePullDown(labPanel, p, new DataSetHighlighLook());
        }
    }

    private void changeToHighlight(boolean on) {
        if (on) {
            labPanel.addStyleName("more-label-highlight");
        }
        else {
            labPanel.removeStyleName("more-label-highlight");
        }
    }

    private String makeLink(DisplayData d) {
        String image= "<img alt=\""+ d.getTip() +"\" title=\""+ d.getTip()+" \" src=\""+componentURL(d.getImage())+ "\">";
        String anchor= "<a href=\""+ refURL(d.getHref()) +"\">" + image + "</a>";
        String div= "<div class=\"mission-icon\">"+anchor+"</div>";
        return div;
    }


    private String componentURL(String url) {
        String cRoot= FrontpageUtils.getComponentsRoot();
        return FFToolEnv.modifyURLToFull(url,cRoot,"/");
    }
    private String refURL(String url) {
        return FFToolEnv.modifyURLToFullAlways(url);
    }

    private class DataSetHighlighLook implements MorePullDown.HighlightLook {
        public void enable() { changeToHighlight(true); }
        public void disable() { changeToHighlight(false); }
    }


//    private List<DataSetDesc> getDataSetDesc() {
//        List<DataSetDesc> list= new ArrayList<DataSetDesc>(20);
//        list.add( new DataSetDesc("dsIcons/WISE_500x600.jpg", "WISE mission page", "Missions/wise.html"));
//        list.add( new DataSetDesc("dsIcons/SST_500x600.jpg",  "Spitzer mission page", "Missions/spitzer.html"));
//        list.add( new DataSetDesc("dsIcons/Herschel_500x600.jpg",  "Herschel mission page", "Missions/herschel.html"));
//        list.add( new DataSetDesc("dsIcons/Planck_500x600.jpg",  "Planck mission page", "Missions/planck.html"));
//        list.add( new DataSetDesc("dsIcons/2MASS_500x600.jpg",  "2MASS mission page", "Missions/2mass.html"));
//        list.add( new DataSetDesc("dsIcons/SOFIA_500x600.jpg",  "SOFIA mission page", "Missions/sofia.html"));
//        list.add( new DataSetDesc("dsIcons/BLAST_500x600.jpg",  "BLAST mission page", "Missions/blast.html"));
//        list.add( new DataSetDesc("dsIcons/BOLOCAM_500x600.jpg",  "BOLOCAM mission page", "Missions/bolocam.html"));
//        return list;
//    }

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
