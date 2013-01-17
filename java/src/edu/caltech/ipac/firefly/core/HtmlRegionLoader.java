package edu.caltech.ipac.firefly.core;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.ui.GwtUtil;
/**
 * User: roby
 * Date: Nov 29, 2010
 * Time: 10:28:08 AM
 */


/**
 * @author Trey Roby
 */
public class HtmlRegionLoader {



//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void load(String url, final String regionName) {
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
        try {
            builder.sendRequest(null, new RequestCallback(){
                public void onResponseReceived(com.google.gwt.http.client.Request req, Response res) {
                    updateHtml(res.getText(), regionName);
                }

                public void onError(com.google.gwt.http.client.Request request, Throwable exception) {
                }
            });
        } catch (RequestException e) {
            e.printStackTrace();
        }

    }

    public void unload(String regionName) {
        Region region= Application.getInstance().getLayoutManager().getRegion(regionName);
        region.setDisplay(null);
    }

    private void updateHtml(String htmlStr, String regionName) {
        if (LayoutManager.FOOTER_REGION.equals(regionName)) {
            updateFooter(htmlStr,regionName);
        }
        else {
            HTML html= new HTML(htmlStr);
            Region region= Application.getInstance().getLayoutManager().getRegion(regionName);
            if (region!=null) region.setDisplay(html);
        }

    }

    private void updateFooter(String htmlStr, String regionName) {
        FlowPanel view = new FlowPanel();
        GwtUtil.setStyle(view, "align", "center");
        final Label versionLabel= new Label();
        versionLabel.setHorizontalAlignment(Label.ALIGN_RIGHT);
        GwtUtil.setStyle(versionLabel, "fontSize", "6pt");
        HTML html= new HTML(htmlStr);
//        html.setSize("100%", "100%");
        view.add(html);
        view.add(versionLabel);
        view.setSize("100%", "100%");
        Region region= Application.getInstance().getLayoutManager().getRegion(regionName);
        if (region!=null) region.setDisplay(view);

        Application.getInstance().findVersion(new AsyncCallback<Version>() {
            public void onFailure(Throwable caught) { }

            public void onSuccess(Version v) {
                versionLabel.setText(v.toString());
            }
        });
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
