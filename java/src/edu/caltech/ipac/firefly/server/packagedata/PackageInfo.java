package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundReport;

import java.io.Serializable;
/**
 * User: roby
 * Date: Sep 26, 2008
 * Time: 8:52:44 AM
 */


/**
 * @author Trey Roby
 */
class PackageInfo implements Serializable {

    private final BackgroundReport _report;
    private final boolean _canceled;
    private final String _email;
    private final String _baseFileName;
    private final String _title;
//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PackageInfo() { this(null,null,null,null, false); }

    public PackageInfo(BackgroundReport report) { this(report,null,null,null,false); }

    public PackageInfo(BackgroundReport report,
                       String email,
                       String baseFileName,
                       String title,
                       boolean canceled) {
        _report= report;
        _canceled= canceled;
        _email= email;
        _baseFileName = baseFileName;
        _title = title;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================
    
    public void setReport(BackgroundReport report)  throws IllegalPackageStateException {
        throw new IllegalPackageStateException("updates not supported in this implementation of PackageInfo");
    }

    public BackgroundReport getReport() throws IllegalPackageStateException { return _report; }

    public void cancel() throws IllegalPackageStateException {
        throw new IllegalPackageStateException("updates not supported in this implementation of PackageInfo");
    }
    public boolean isCanceled() { return _canceled; }


    public void setEmailAddress(String email) throws IllegalPackageStateException {
        throw new IllegalPackageStateException( "updates not supported in this implementation of PackageInfo");
    }

    public String getEmailAddress() throws IllegalPackageStateException { return _email; }

    public String getBaseFileName() throws IllegalPackageStateException { return _baseFileName; }

    public String getTitle() throws IllegalPackageStateException { return _title; }
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
