package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;

import java.util.Map;

public interface Creator {

	public Toolbar getToolBar();

	public Map makeCommandTable();     // a Map<String, GeneralCommand> of commands, keyed by command_name

	public RequestHandler makeCommandHandler();

    public LoginManager makeLoginManager();

    public LayoutManager makeLayoutManager();

    public String getLoadingDiv();

    public String getAppDesc();

    public String getAppName();

    public boolean isApplication();

}