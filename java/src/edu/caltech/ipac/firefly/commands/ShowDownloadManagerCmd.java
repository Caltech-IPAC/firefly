package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.background.BackgroundManager;
import edu.caltech.ipac.firefly.util.WebClassProperties;


@Deprecated
public class ShowDownloadManagerCmd extends GeneralCommand {

    private static WebClassProperties _prop= new WebClassProperties(ShowDownloadManagerCmd.class);

    public static final String COMMAND_NAME= "ShowDownloadManagerCmd";
    private final BackgroundManager _dmanager;

    public ShowDownloadManagerCmd(BackgroundManager dmanager) {
        super(COMMAND_NAME);
        _dmanager= dmanager;
    }


    protected void doExecute() {
//        if (_dmanager.size()>0) {
//            _dmanager.changeState(BackgroundManager.VisState.EXPANDED);
//        }
//        else {
//            PopupUtil.showError(_prop.getTitle("empty"),
//                            _prop.getError("empty"));
//        }

    }

    @Override
    public Image createCmdImage() {
        IconCreator ic= IconCreator.Creator.getInstance();
        String iStr= getIconProperty();
        if (iStr!=null && iStr.equals(COMMAND_NAME+".Icon"))  {
            return new Image(ic.getOneGearSingle());
        }
        return null;
    }




}