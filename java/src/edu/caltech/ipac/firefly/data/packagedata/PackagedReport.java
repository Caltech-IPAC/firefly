package edu.caltech.ipac.firefly.data.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundPart;
import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.PackageProgress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * User: roby
 * Date: Sep 24, 2008
 * Time: 12:26:51 PM
 */


/**
 * @author Trey Roby
 */
public class PackagedReport extends BackgroundReport {

    private long _sizeInBytes;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PackagedReport() {
    }

    public PackagedReport(String packageID,
                          PackagedBundle bundleAry[],
                          long sizeInBytes,
                          BackgroundState state) {
        super(packageID, toList(bundleAry), state);
        _sizeInBytes = sizeInBytes;
        addAttribute(JobAttributes.Zipped);
        addAttribute(JobAttributes.CanSendEmail);
        addAttribute(JobAttributes.DownloadScript);
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public long getTotalSizeInByte() {
        return _sizeInBytes;
    }

    /**
     * @param state new state
     * @return report with new state
     */
    @Override
    public BackgroundReport cloneWithState(BackgroundState state) {
        PackagedReport rpt = new PackagedReport(getID(), null, getSizeInBytes(state), state);
        rpt.copyMessagesFrom(this);
        rpt.copyPartsFrom(this);
        rpt.setDataSource(this.getDataSource());
        rpt.copyAttributesFrom(this);
        return rpt;
    }

    @Override
    public PackageProgress getPartProgress(int i) {
        PackagedBundle part = (PackagedBundle) this.get(i);
        return part.makePackageProgress();
    }


//======================================================================
//------------------ to String Methods -----------------------
//======================================================================


    public String toString() {
        String sAry[] = toStringAry();
        StringBuffer retval = new StringBuffer(sAry.length * 2 + 2);
        for (String s : sAry) {
            retval.append(s).append("\n");
        }
        return retval.toString();
    }

    public String[] toStringAry() {
        ArrayList<String> retval = new ArrayList<String>(20);


        if (isOneFile()) {
            retval.add( "Package Report: "+getID() +", 1 File, state: "+ getState()+ ", sizeInBytes: " + _sizeInBytes );
        }
        else {
            int messCnt = getNumMessages();
            retval.add("Package Report: "+getID() +", state: "+ getState()+ ", msg cnt: " + messCnt+
                               ", sizeInBytes: " + _sizeInBytes );
            retval.add("PackagedBundle cnt: " + getPartCount());
            StringBuilder bundleStr;
            PackagedBundle b;
            for (BackgroundPart p : this) {
                b = (PackagedBundle) p;
                bundleStr = new StringBuilder(100);
                bundleStr.append("            #");
                bundleStr.append(b.getPackageIdx()).append(" - ");
                bundleStr.append("state: ").append(b.getState());
                bundleStr.append("   files: ").append(b.getNumFiles());
                retval.add(bundleStr.toString());
            }
        }


        return retval.toArray(new String[retval.size()]);
    }

    public boolean isOneFile() {
        boolean retval= false;
        if (getPartCount()==1) {
            PackagedBundle pb= (PackagedBundle)get(0);
            retval= pb.getNumFiles()==1;
        }
        return retval;
    }

    public String firstFileName() {
        String retval= null;
        if (getPartCount()>0) {
            PackagedBundle pb= (PackagedBundle)get(0);
            retval= pb.getUrl();
        }
        return retval;

    }

    public String toBriefBundleString() {
        StringBuilder bundleStr = new StringBuilder(200);
        boolean first = true;
        PackagedBundle b;
        if (getPartCount() < 5 || isFail()) {
            for (BackgroundPart p : this) {
                b = (PackagedBundle) p;
                if (!first) bundleStr.append(",  ");
                first = false;
                bundleStr.append("#");
                bundleStr.append(b.getPackageIdx()).append("- ");
                bundleStr.append(b.getState());
                bundleStr.append(" ");
                bundleStr.append(b.getNumFiles()).append(" files");
            }
        } else {
            int sCnt = 0;
            int wCnt = 0;
            for (BackgroundPart p : this) {
                b = (PackagedBundle) p;
                if (b.getState() == BackgroundState.SUCCESS) sCnt++;
                else wCnt++;
            }
            bundleStr.append(getPartCount());
            bundleStr.append(" parts, ");
            bundleStr.append(sCnt);
            bundleStr.append(" completed, ");
            bundleStr.append(wCnt);
            bundleStr.append(" still working.");

        }
        return bundleStr.toString();
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static List<BackgroundPart> toList(PackagedBundle bundleAry[]) {
        List<BackgroundPart> l = null;
        if (bundleAry != null) {
            l = new ArrayList<BackgroundPart>(bundleAry.length);
            l.addAll(Arrays.asList(bundleAry));
        }
        return l;
    }

    /**
     * @param state background state
     * @return processed bytes if all bundles were processed successfully, otherwise previously estimated size in bytes
     */
    private long getSizeInBytes(BackgroundState state) {
        long processedSize = 0;
        if (state == BackgroundState.SUCCESS) {
            for (int i = 0; i < getPartCount(); i++) {
                BackgroundPart part = get(i);
                if (part.getState() == BackgroundState.SUCCESS && (part instanceof PackagedBundle)) {
                    processedSize += ((PackagedBundle) part).getProcessedBytes();
                } else {
                    processedSize = 0;
                    break;
                }
            }
        }
        if (processedSize > 0) {
            return processedSize;
        } else {
            return _sizeInBytes;
        }

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
