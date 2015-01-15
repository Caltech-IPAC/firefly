/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.util.Assert;

public class PackagerItem {

    private final RequestOwner _requestOwner;
    private final Packager _packager;
    private boolean _running = false;
    private Thread _thread = null;
    private final long _entryTime;
    private long _packagingStartTime;
    private final long _sizeInByte;

    public PackagerItem(Packager packager, RequestOwner requestOwner) {
        Assert.argTst(packager != null, "packager must not be null");
        Assert.argTst(packager.getBackgroundInfoCacher() != null, "packager.getPackageInfo() returns null");
        _packager = packager;
        _entryTime = System.currentTimeMillis();

        BackgroundStatus status = packager.estimate();
        _sizeInByte = (status != null) ? status.getTotalSizeInBytes() : 0;
        _requestOwner= requestOwner;
    }

    public RequestOwner getRequestOwner() {
        return _requestOwner;
    }

    public Packager getPackager() {
        return _packager;
    }

    public String getID() {
        return _packager.getID();
    }

    public boolean isLarge() {
        return _sizeInByte > PackagingController.LARGE_PACKAGE;
    }

    public boolean isRunning() {
        return _running;
    }

    public void setRunning(boolean run) {
        _running = run;
    }

    public void setThread(Thread thread) {
        _thread = thread;
    }

    public Thread getThread() {
        return _thread;
    }

    public void markStartTime() {
        _packagingStartTime = System.currentTimeMillis();
    }

    public long getMillsSincePackagingStart() {
        return System.currentTimeMillis() - _packagingStartTime;
    }

    public long getMillsSinceSubmited() {
        return System.currentTimeMillis() - _entryTime;
    }
}
