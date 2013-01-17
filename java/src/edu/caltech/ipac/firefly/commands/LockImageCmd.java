package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.ui.LockOptionsPopup;


public class LockImageCmd extends BaseGroupVisCmd {
    private enum NextAction { CHANGE_TO_LOCKED, ADJUST_LOCK}

    private NextAction _nextAction = null;

    public static final String CommandName= "LockImageCmd";
    private static final String  _lockedIcon  = "LockImageCmd.locked.Icon";
    private static final String _unlockedIcon= "LockImageCmd.unlocked.Icon";

    public LockImageCmd() {
        super(CommandName);
        changeNextAction(NextAction.CHANGE_TO_LOCKED);

        AllPlots.getInstance().getEventManager().addListener(new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (getPlotView()!=null) {
                    boolean isLocked= getPlotView().isLockedHint();
                    changeNextAction(isLocked ? NextAction.ADJUST_LOCK : NextAction.CHANGE_TO_LOCKED);
                }
            }
        });
    }

    public boolean init() {
        return true;
    }

    protected void doExecute() {
        if (_nextAction==NextAction.CHANGE_TO_LOCKED) {
            LockOptionsPopup.showNewLock(getMiniPlotWidget());
        }
        else if (_nextAction==NextAction.ADJUST_LOCK) {
                LockOptionsPopup.showAdjustLock(getMiniPlotWidget());
        }
        else {
            WebAssert.argTst(false, "unsupport NextAction");
        }
    }

    private void changeNextAction(NextAction newNextAction) {
        if (_nextAction != newNextAction) {
            _nextAction = newNextAction;
            switch (_nextAction) {
                case CHANGE_TO_LOCKED:
                    setIconProperty(_unlockedIcon);
                    break;
                case ADJUST_LOCK:
                    setIconProperty(_lockedIcon);
                    break;
                default :
                    WebAssert.argTst(false, "only support for SelectType of CHANGE_TO_LOCKED or ADJUST_LOCK");
                    break;
            }
        }
    }

//    @Override
//    public boolean isIE6IconBundleSafe() { return true; }

    @Override
    public Image createCmdImage() {

        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {

            if (iStr.equals("LockImageCmd.Icon"))  {
                return new Image(ic.getLocked());
            }
            else if (iStr.equals("LockImageCmd.locked.Icon"))  {
                return new Image(ic.getLocked());
            }
            else if (iStr.equals("LockImageCmd.unlocked.Icon"))  {
                return new Image(ic.getUnlocked());
            }
        }

        return null;
    }


}