package edu.caltech.ipac.fftools.core;

import edu.caltech.ipac.firefly.core.Creator;
import edu.caltech.ipac.firefly.core.DefaultRequestHandler;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;

import java.util.Map;

public class FireflyToolsEmbededCreator implements Creator {


    public FireflyToolsEmbededCreator() {
    }





    public LayoutManager makeLayoutManager() { return null; }

    public boolean isApplication() { return false; }



    public Toolbar getToolBar() { return null; }
    public Map makeCommandTable() { return null; }
    public RequestHandler makeCommandHandler() { return new DefaultRequestHandler(); }
    public LoginManager makeLoginManager() { return null; }
    public String getLoadingDiv() { return null; }
    public String getAppDesc() { return null; }
    public String getAppName() { return null; }







}