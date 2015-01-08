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
    private final String footerHtmlFile;

    public FFToolsStandaloneLayoutManager(int topOffset,String footerHtmlFile) {
        super(DEF_MIN_WIDTH,DEF_MIN_HEIGHT, topOffset);
        getLayoutSelector().setHub(FFToolEnv.getHub());
        this.footerHtmlFile= footerHtmlFile;
    }

    public void layout(String rootId) {

        Application.getInstance().getProperties().setProperty("BackToSearch.show", "false");
        super.layout(rootId);
        ((FFToolsStandaloneCreator)Application.getInstance().getCreator()).getStandaloneUI().init();



        HtmlRegionLoader footer= new HtmlRegionLoader();
//        footer.load("irsa_footer_minimal.html",LayoutManager.FOOTER_REGION);
        if (footerHtmlFile!=null) footer.load(footerHtmlFile,LayoutManager.FOOTER_REGION);
    }

}

