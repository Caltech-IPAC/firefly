/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;

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

    private final BackgroundStatus bgStat;
    private final boolean canceled;
    private final String email;
    private final String baseFileName;
    private final String title;
    private final ServerEvent.EventTarget eventTarget;
//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public BackgroundInfo(BackgroundStatus bgStat,
                          String email,
                          String baseFileName,
                          String title,
                          ServerEvent.EventTarget eventTarget,
                          boolean canceled) {
        this.bgStat= bgStat;
        this.canceled= canceled;
        this.email= email;
        this.baseFileName = baseFileName;
        this.eventTarget= eventTarget;
        this.title = title;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================
    
    public BackgroundStatus getStatus() { return bgStat; }

    public boolean isCanceled() { return canceled; }

    public String getEmailAddress() { return email; }

    public String getBaseFileName() { return baseFileName; }

    public String getTitle() { return title; }

    public ServerEvent.EventTarget getEventTarget() { return eventTarget; }
}

