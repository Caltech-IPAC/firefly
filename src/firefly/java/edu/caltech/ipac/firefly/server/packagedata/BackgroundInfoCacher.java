/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.server.query.BackgroundEnv;
import edu.caltech.ipac.firefly.server.sse.EventData;
import edu.caltech.ipac.firefly.server.sse.EventTarget;
import edu.caltech.ipac.firefly.server.sse.ServerEventManager;
import edu.caltech.ipac.firefly.server.sse.ServerSentEvent;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.ServerSentEventNames;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;

/**
 * User: roby
 * Date: Sep 26, 2008
 * Time: 8:52:44 AM
 */
public class BackgroundInfoCacher {

    private final StringKey _key;

    //======================================================================
    //----------------------- Constructors ---------------------------------
    //======================================================================

    /**
     * Only use this constructor is you expect that the object is already in the cache
     * @param key the key for this object
     */
    public BackgroundInfoCacher(String key) {
        _key= new StringKey(key);
    }

    /**
     * Use this constructor when you want to initialize the object in the cache
     * @param key the key for this object
     * @param email user's email
     * @param baseFileName base name
     * @param title title
     */
    public BackgroundInfoCacher(String key, String email, String baseFileName, String title, EventTarget target) {
        this(key);
        updateInfo(null, email, baseFileName, title, target, false);
    }

    public BackgroundInfo getPackageInfo() { return getInfo(); }

    //======================================================================
    //----------------------- Public Methods -------------------------------
    //======================================================================

    public void setStatus(BackgroundStatus bgStat) {
        BackgroundInfo info= getInfo();
        if (info!=null) {
            updateInfo(bgStat, info.getEmailAddress(), info.getBaseFileName(), info.getTitle(), info.getEventTarget(), info.isCanceled());
            ServerSentEvent ev= new ServerSentEvent(ServerSentEventNames.SVR_BACKGROUND_REPORT,
                                                    info.getEventTarget(), new EventData(bgStat));
            ServerEventManager.fireEvent(ev);
        }
    }




    public BackgroundStatus getStatus() {
        BackgroundInfo info= getInfo();
        return info==null ? null : info.getStatus();
    }

    public void cancel() {
        BackgroundInfo info= getInfo();
        if (info!=null) {
            updateInfo(info.getStatus(), info.getEmailAddress(), info.getBaseFileName(), info.getTitle(), info.getEventTarget(), true);
        }
    }

    public boolean isCanceled() {
        BackgroundInfo info= getInfo();
        return info==null ? true : info.isCanceled();
    }

    public void setEmailAddress(String email) {
        BackgroundInfo info= getInfo();
        if (info!=null) {
            updateInfo(info.getStatus(), email, info.getBaseFileName(), info.getTitle(), info.getEventTarget(), info.isCanceled());
        }
    }

    public String getEmailAddress() {
        BackgroundInfo info= getInfo();
        return (info==null) ? null : info.getEmailAddress();
    }

    public void setBaseFileName(String baseFileName) {
        BackgroundInfo info= getInfo();
        if (info!=null) {
            updateInfo(info.getStatus(), info.getEmailAddress(), baseFileName, info.getTitle(), info.getEventTarget(), info.isCanceled());
        }
    }

    public String getBaseFileName() {
        BackgroundInfo info= getInfo();
        return info==null ? null : info.getBaseFileName();
    }


    public String getTitle() {
        BackgroundInfo info= getInfo();
        return info==null ? null : info.getTitle();
    }


    public EventTarget getEventTarget() {
        BackgroundInfo info= getInfo();
        return info==null ? null : info.getEventTarget();
    }

    //======================================================================
    //----------------------- Private Methods -------------------------------
    //======================================================================

    private void updateInfo(BackgroundStatus status,
                            String email,
                            String baseFileName,
                            String title,
                            EventTarget target,
                            boolean canceled) {
        BackgroundInfo info= new BackgroundInfo(status,email, baseFileName, title, target, canceled);
        BackgroundEnv.getCache().put(_key, info);
    }

    private BackgroundInfo getInfo() {
        Cache cache= BackgroundEnv.getCache();
        BackgroundInfo retval= null;
        if (cache.isCached(_key)) {
            BackgroundInfo info= (BackgroundInfo)cache.get(_key);
            if (info!=null) {
                retval= new BackgroundInfo(info.getStatus(),
                                        info.getEmailAddress(),
                                        info.getBaseFileName(),
                                        info.getTitle(),
                                        info.getEventTarget(),
                                        info.isCanceled());
            }
        }
        else {
            Logger.error( "Could not update background info, BackgroundInfo not found in cache, key: "+_key );
        }
        return retval;
    }

}

