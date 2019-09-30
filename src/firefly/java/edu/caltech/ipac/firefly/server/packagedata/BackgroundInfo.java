/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.data.ServerEvent;

import java.io.Serializable;
/**
 * User: roby
 * Date: Sep 26, 2008
 * Time: 8:52:44 AM
 */


/**
 * @author Trey Roby
 */
class BackgroundInfo implements Serializable {

    private BackgroundStatus bgStat;
    private boolean canceled;
    private String email;
    private String baseFileName;
    private String title;
    private String dataTag;
    private String workspaceDestPath;           // if this is not null, push the packaged files to workspace
    private ServerEvent.EventTarget eventTarget;
//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public BackgroundInfo(BackgroundStatus bgStat,
                          String email,
                          String baseFileName,
                          String title,
                          String dataTag,
                          String workspaceDestPath,
                          ServerEvent.EventTarget eventTarget,
                          boolean canceled) {
        this.bgStat= bgStat;
        this.canceled= canceled;
        this.email= email;
        this.baseFileName = baseFileName;
        this.dataTag = dataTag;
        this.eventTarget= eventTarget;
        this.title = title;
        this.workspaceDestPath = workspaceDestPath;

    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setEmail(String email) {
        this.email = email;
    }

    public void setBgStat(BackgroundStatus bgStat) {
        this.bgStat = bgStat;
    }

    public BackgroundStatus getStatus() { return bgStat; }

    public boolean isCanceled() { return canceled; }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
        if (bgStat != null) {
            bgStat.setState(BackgroundState.CANCELED);
        }
    }

    public String getEmailAddress() { return email; }

    public String getBaseFileName() { return baseFileName; }

    public void setBaseFileName(String baseFileName) {
        this.baseFileName = baseFileName;
    }

    public String getTitle() { return title; }

    public String getWorkspaceDestPath() {
        return workspaceDestPath;
    }

    public void setTitle(String title) {
        this.title = title;
        bgStat.setParam(BackgroundStatus.TITLE, title);
    }

    public String getDataTag() {
        return dataTag;
    }

    public ServerEvent.EventTarget getEventTarget() { return eventTarget; }
}

