/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.AllPlots;


public class LockRelatedImagesCmd extends BaseGroupVisCmd {

    public static final String COMMAND_NAME= "LockRelated";
    private boolean _relatedLocked= true;
    private final String _lockIcon= "LockRelated.lock.Icon";
    private final String _unlockIcon= "LockRelated.unlock.Icon";

    public LockRelatedImagesCmd() {
        super(COMMAND_NAME);
        setLockRelated(false);
    }

    public boolean init() {
        return true;
    }

    protected void doExecute() {

        setLockRelated(!_relatedLocked);
        AllPlots.getInstance().getActiveGroup().setLockRelated(_relatedLocked);
    }


    public void setLockRelated(boolean locked) {
        setIconProperty(locked ? _lockIcon : _unlockIcon);
        _relatedLocked= locked;
    }

    private boolean getLockRelated() { return _relatedLocked; }

    @Override
    public Image createCmdImage() {

        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {
            if (iStr.equals(_lockIcon))  {
                return new Image(ic.getLockImages());
            }
            else if (iStr.equals(_unlockIcon))  {
                return new Image(ic.getUnlockedImages());
            }
            else {
                return new Image(ic.getLockImages());
            }
        }
        return null;
    }



}


