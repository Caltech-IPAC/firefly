package edu.caltech.ipac.fftools.core;

import com.google.gwt.core.client.GWT;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HtmlRegionLoader;
import edu.caltech.ipac.firefly.core.layout.IrsaLayoutManager;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;


/**
 * Date: Feb 23, 2010
 *
 * @author loi
 * @version $Id: ResizableLayoutManager.java,v 1.26 2012/10/03 22:18:11 loi Exp $
 */
public class FFToolsStandaloneLayoutManager extends IrsaLayoutManager {
    private final static String FIREFLY_LOGO= GWT.getModuleBaseURL()+  "images/fftools-logo-56x56.png";

    public FFToolsStandaloneLayoutManager() {
        super();
        getLayoutSelector().setHub(FFToolEnv.getHub());
    }

    public void layout(String rootId) {

        Application.getInstance().getProperties().setProperty("BackToSearch.show", "false");
        super.layout(rootId);
        ((FFToolsStandaloneCreator)Application.getInstance().getCreator()).getStandaloneUI().init();



        HtmlRegionLoader footer= new HtmlRegionLoader();
        footer.load("irsa_footer_minimal.html",LayoutManager.FOOTER_REGION);
    }

}

