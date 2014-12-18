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

