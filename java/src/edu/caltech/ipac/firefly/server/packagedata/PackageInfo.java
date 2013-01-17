package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundReport;

/**
 * User: roby
 * Date: Sep 26, 2008
 * Time: 8:49:35 AM
 */
public interface PackageInfo {
    public void setReport(BackgroundReport report) throws IllegalPackageStateException;
    public BackgroundReport getReport() throws IllegalPackageStateException;
    public void cancel() throws IllegalPackageStateException;
    public boolean isCanceled();
    public void setEmailAddress(String email) throws IllegalPackageStateException;
    public String getEmailAddress() throws IllegalPackageStateException;
    public String getBaseFileName() throws IllegalPackageStateException;
    public String getTitle() throws IllegalPackageStateException;

}
