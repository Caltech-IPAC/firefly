package edu.caltech.ipac.frontpage.core;

import com.google.gwt.core.client.EntryPoint;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.NetworkMode;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;

/**
 * @author Trey Roby
 */
public class FrontpageEntryPoint implements EntryPoint {


    public void onModuleLoad() {
        Application.disableCSSEject();
        Application.setCreator(new FrontpageEmbededCreator());
        final Application app= Application.getInstance();
        if (!FrontpageUtils.isDirect()) Application.getInstance().setNetworkMode(NetworkMode.JSONP);

        String rootURL= FrontpageUtils.getURLRoot();
        if (rootURL!=null) FFToolEnv.setRootPath(rootURL);
        FrontpageUtils.markNewToolbarRunning();
        app.start(null, new AppReady());
    }

    public class AppReady implements Application.ApplicationReady {
        public void ready() {
            if (FrontpageUtils.isFrontpage()) new ComponentsCmd().execute();
            else                              new AppMenuBarCmd().execute();
            Application.getInstance().getLoginManager().makeLoginRegion();
            Application.getInstance().getLoginManager().refreshUserInfo(false);
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
