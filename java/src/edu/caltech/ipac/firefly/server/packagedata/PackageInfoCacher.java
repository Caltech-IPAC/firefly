package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundReport;
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
public class PackageInfoCacher {

    //private static final int TWO_DAYS_SECS= 2*24*3600;
    private final StringKey _key;
    private static final String CHECK_CACHE_WARN=  "Check to make sure the system " +
                                 "is not overwelmed or the cache is " +
                                 "expiring the object to quickly";


    //======================================================================
    //----------------------- Constructors ---------------------------------
    //======================================================================

    /**
     * Only use this constructor is you expect that the object is already in the cache
     * @param key the key for this object
     */
    public PackageInfoCacher(String key) {
        _key= new StringKey(key);
    }

    /**
     * Use this constructor when you want to initialize the object in the cache
     * @param key the key for this object
     * @param email user's email
     * @param baseFileName base name
     * @param title title
     */
    public PackageInfoCacher(String key, String email, String baseFileName, String title, EventTarget target) {
        this(key);
        updatePI(null, email, baseFileName, title, target, false);
    }

    public PackageInfo getPackageInfo() {
        try {
            return getPI();
        } catch (IllegalPackageStateException e) {
            return null;
        }
    }

    //======================================================================
    //----------------------- Public Methods -------------------------------
    //======================================================================

    public void setReport(BackgroundReport report) throws IllegalPackageStateException {
        PackageInfo pi= getPI();
        updatePI(report, pi.getEmailAddress(), pi.getBaseFileName(), pi.getTitle(), pi.getEventTarget(), pi.isCanceled());
        ServerSentEvent ev= new ServerSentEvent(ServerSentEventNames.SVR_BACKGROUND_REPORT,
                                                pi.getEventTarget(), new EventData(report));
        ServerEventManager.fireEvent(ev);
    }




    public BackgroundReport getReport() throws IllegalPackageStateException {
        return getPI().getReport();
    }

    public void cancel() throws IllegalPackageStateException {
        PackageInfo pi= getPI();
        updatePI(pi.getReport(), pi.getEmailAddress(), pi.getBaseFileName(), pi.getTitle(), pi.getEventTarget(), true);
    }

    public boolean isCanceled() {
        boolean retval;
        try {
            retval= getPI().isCanceled();
        } catch (IllegalPackageStateException e) {
            retval= true;
            Logger.info("PackageInfoCacher returning isCanceled()==true because " +
                    "object is not longer in cache.",
                    CHECK_CACHE_WARN);
        }
        return retval;
    }

    public void setEmailAddress(String email) throws IllegalPackageStateException {
        PackageInfo pi= getPI();
        updatePI(pi.getReport(), email, pi.getBaseFileName(), pi.getTitle(), pi.getEventTarget(), pi.isCanceled());
    }

    public String getEmailAddress() throws IllegalPackageStateException { return getPI().getEmailAddress(); }

    public void setBaseFileName(String baseFileName) throws IllegalPackageStateException {
        PackageInfo pi= getPI();
        updatePI(pi.getReport(), pi.getEmailAddress(), baseFileName, pi.getTitle(), pi.getEventTarget(), pi.isCanceled());
    }

    public String getBaseFileName() throws IllegalPackageStateException { return getPI().getBaseFileName(); }


    public String getTitle() throws IllegalPackageStateException { return getPI().getTitle(); }


    public EventTarget getEventTarget() throws IllegalPackageStateException { return getPI().getEventTarget(); }

    //======================================================================
    //----------------------- Private Methods -------------------------------
    //======================================================================

    private void updatePI(BackgroundReport report,
                          String           email,
                          String           baseFileName,
                          String           title,
                          EventTarget      target,
                          boolean          canceled) {
        PackageInfo pi= new PackageInfo(report,email, baseFileName, title, target, canceled);
        BackgroundEnv.getCache().put(_key, pi);
    }

    private PackageInfo getPI() throws IllegalPackageStateException {
        Cache cache= BackgroundEnv.getCache();
        PackageInfo retval= null;
        if (cache.isCached(_key)) {
            PackageInfo pi= (PackageInfo)cache.get(_key);
            if (pi!=null) {
                retval= new PackageInfo(pi.getReport(),
                                        pi.getEmailAddress(),
                                        pi.getBaseFileName(),
                                        pi.getTitle(),
                                        pi.getEventTarget(),
                                        pi.isCanceled());
            }
        }
        else {
            throw new IllegalPackageStateException("PackageInfo not found in cache, key: "+_key);
        }
        return retval;
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
