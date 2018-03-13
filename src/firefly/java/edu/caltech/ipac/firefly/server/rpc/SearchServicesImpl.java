/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;

import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.JobAttributes;
import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.firefly.server.query.BackgroundEnv;

import java.util.List;

/**
 * @author tatianag
 * $Id: SearchServicesImpl.java,v 1.14 2012/10/03 22:18:11 loi Exp $
 */
public class SearchServicesImpl {



    public BackgroundStatus getStatus(String id, boolean polling) { return BackgroundEnv.getStatus(id, polling); }

    public boolean addIDToPushCriteria (String id) {
        BackgroundEnv.addIDToPushCriteria(id);
        return true;
    }

    public boolean cleanup(String id) { return BackgroundEnv.cleanup(id); }

    public boolean cancel(String id) { return BackgroundEnv.cancel(id); }


    public boolean setAttribute(String id, JobAttributes attribute) {
        BackgroundEnv.setAttribute(id,attribute);
        return true;
    }

    public boolean setAttribute(List<String> idList, JobAttributes attribute) {
        BackgroundEnv.setAttribute(idList,attribute);
        return true;
    }

    public boolean setEmail(String id, String email) {
        BackgroundEnv.setEmail(id,email);
        return true;
    }

    public boolean setEmail(List<String> idList, String email) {
        BackgroundEnv.setEmail(idList,email);
        return true;
    }

    public String getEmail(String id) {
        return BackgroundEnv.getEmail(id);
    }


    public String createDownloadScript(String id,
                                       String fname,
                                       String dataSource,
                                       List<ScriptAttributes> attributes) {

        BackgroundEnv.ScriptRet retval= BackgroundEnv.createDownloadScript(id, fname, dataSource, attributes);
        return retval!=null ? retval.getServlet() : null;
    }



    public boolean resendEmail(List<String> idList, String email) {
        BackgroundEnv.resendEmail(idList,email);
        return true;
    }

    public boolean clearPushEntry(String id, int idx) {
        BackgroundEnv.clearPushEntry(id,idx);
        return true;
    }

    public boolean reportUserAction(String channel, String desc, String data) {
        BackgroundEnv.reportUserAction(channel,desc,data);
        return true;
    }

    public DownloadProgress getDownloadProgress(String fileKey) {
        return BackgroundEnv.getDownloadProgress(fileKey);
    }


    /**
     * Utility/Convenience class.
     * Use SearchServices.App.getInstance() to access static instance of SearchServicesAsync
     */
    public enum DownloadProgress { STARTING, WORKING, DONE, UNKNOWN, FAIL}
}
