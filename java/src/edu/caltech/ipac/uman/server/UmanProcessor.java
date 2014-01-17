package edu.caltech.ipac.uman.server;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.security.JOSSOAdapter;
import edu.caltech.ipac.firefly.server.util.EMailUtil;
import edu.caltech.ipac.firefly.server.util.EMailUtilException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.uman.data.UserRoleEntry;
import edu.caltech.ipac.uman.server.persistence.SsoDao;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.mail.Session;

import static edu.caltech.ipac.uman.data.UmanConst.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


@SearchProcessorImpl(id = "UmanProcessor")
public class UmanProcessor extends IpacTablePartProcessor {

    public static final Logger.LoggerImpl logger = Logger.getLogger();

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
        String action = request.getParam(ACTION);

        if (StringUtils.isEmpty(action)) throw new DataAccessException("Mission required parameter action");

        if (action.equals(REGISTER)) {
            return addUser(request);
        } else if (action.equals(PROFILE)) {
            return updateUser(request);
        } else if (action.equals(NEW_PASS)) {
            return changePassword(request);
        } else if (action.equals(RESET_PASS)) {
            return resetPassword(request);
        } else if (action.equals(NEW_EMAIL)) {
            return changeEmail(request);
        } else if (action.equals(ADD_ROLE)) {
            return addRole(request);
        } else if (action.equals(ADD_ACCESS)) {
            return addAccess(request);
        } else if (action.equals(REMOVE_ROLE)) {
            return removeRole(request);
        } else if (action.equals(REMOVE_ACCESS)) {
            return removeAccess(request);
        } else if (action.equals(SHOW_ROLES)) {
            return showRoles(request);
        } else if (action.equals(SHOW_ACCESS)) {
            return showAccess(request);
        } else if (action.equals(SHOW_USERS)) {
            return showUsers(request);
        } else if (action.equals(SHOW_MISSION_XREF)) {
            return showMissions(request);
        } else if (action.equals(ADD_MISSION_XREF)) {
            return addMissions(request);
        } else if (action.equals(USER_LIST)) {
            return getUsers(request);
        } else if (action.equals(USERS_BY_ROLE)) {
            return getUsersByRole(request);
        }
        return null;
    }

    @Override
    public boolean doLogging() {
        return false;
    }

    @Override
    public boolean doCache() {
        return false;
    }

    private File updateUser(TableServerRequest request) throws IOException, DataAccessException {

        String successMsg = "Your information has been updated.";
        SsoDataManager.Response<UserInfo> res = SsoDataManager.updateUser(request);
        logStats(PROFILE, successMsg);
        return sendResponse(request, res, successMsg);
    }

    private File changeEmail(TableServerRequest request) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.changeEmail(request);
        logStats(NEW_EMAIL, res.getMessage());
        if (res.isOk()) {
            JOSSOAdapter.logout(ServerContext.getRequestOwner().getAuthKey());
            String successMsg = "Your email has been updated.  You are now logged out.";
            return sendResponse(request, res, successMsg);
        }
        return sendResponse(request, res);
    }

    private File changePassword(TableServerRequest request) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.changePassword(request);
        logStats(NEW_PASS, res.getMessage());
        return sendResponse(request, res);
    }

    private File resetPassword(TableServerRequest request) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.resetPassword(request);
        logStats(RESET_PASS, res.getMessage());
        return sendResponse(request, res);
    }

    private File addUser(TableServerRequest request) throws IOException, DataAccessException {
        SsoDataManager.Response<UserInfo> res = SsoDataManager.addUser(request);
        logStats(ADD_ACCOUNT, res.getMessage());
        String msg = res.getMessage();
        boolean doGeneratePassword = request.getBooleanParam(GEN_PASS);
        if (doGeneratePassword) {
            UserInfo user = res.getValue();
            String ssoBaseUrl = ServerContext.getRequestOwner().getBaseUrl();
            String sendTo = user.getEmail();
            try {
                SsoDataManager.sendUserAddedEmail(ssoBaseUrl, sendTo, user);
                msg += " ==> email sent";
            } catch (Exception e) {
                msg += " ==> fail to notify via email.";
            }
        }
        return sendResponse(request, res, msg);
    }

    private void logStats(String category, String successMsg) {
        logger.stats(category, successMsg.trim().replaceAll("\n", "; ") + " (By " + ServerContext.getRequestOwner().getUserInfo().getLoginName() + ")");
    }

    private File addAccess(TableServerRequest req) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.addAccess(req);
        logStats(ADD_ACCESS, res.getMessage());
        return sendResponse(req, res);
    }

    private File addRole(TableServerRequest req) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.addRole(req);
        logStats(ADD_ROLE, res.getMessage());
        return sendResponse(req, res);
    }

    private File removeRole(final TableServerRequest req) throws IOException, DataAccessException {

        String rl = req.getParam(ROLE_LIST);
        final String[] rlist = rl.split(",");
        final SsoDataManager.Response res = new SsoDataManager.Response();
        TransactionTemplate txTemplate = JdbcFactory.getTransactionTemplate(JdbcFactory.getDataSource(DbInstance.josso));
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (String r : rlist) {
                    SsoDataManager.Response rr = removeARole(r);
                    if (rr.isError()) {
                        res.setStatus(SsoDataManager.Response.Status.ERROR);
                        res.addMessage(rr.getMessagesAsString());
                    }
                }
                if (res.isError()) {
                    status.setRollbackOnly();
                    res.addMessage("Due to error(s), all requests are aborted.");
                } else {
                    res.addMessage(rlist.length + " role(s) were removed.");
                }
            }
        });
        logStats(REMOVE_ROLE, res.getMessage());
        return sendResponse(req, res, StringUtils.toString(res.getMessages(), " <li>"));
    }

    private SsoDataManager.Response removeARole(String roleStr) {
        RoleList.RoleEntry re = RoleList.RoleEntry.parse(roleStr);
        ServerRequest sr = new ServerRequest();
        sr.setParam(MISSION_NAME, re.getMissionName());
        sr.setParam(MISSION_ID, String.valueOf(re.getMissionId()));
        sr.setParam(GROUP_NAME, re.getGroupName());
        sr.setParam(GROUP_ID, String.valueOf(re.getGroupId()));
        sr.setParam(PRIVILEGE, re.getPrivilege());
        return SsoDataManager.removeRole(sr);
    }

    private File removeAccess(TableServerRequest req) throws IOException, DataAccessException {

        String al = req.getParam(ACCESS_LIST);
        final String[] alist = al.split(",");
        final SsoDataManager.Response res = new SsoDataManager.Response();
        TransactionTemplate txTemplate = JdbcFactory.getTransactionTemplate(JdbcFactory.getDataSource(DbInstance.josso));
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (String r : alist) {
                    SsoDataManager.Response rr = removeAnAccess(r);
                    if (rr.isError()) {
                        res.setStatus(SsoDataManager.Response.Status.ERROR);
                        res.addMessage(rr.getMessagesAsString());
                    }
                }
                if (res.isError()) {
                    status.setRollbackOnly();
                    res.addMessage("Due to error(s), all requests are aborted.");
                } else {
                    res.addMessage(alist.length + " access entries were removed.");
                }
            }
        });
        logStats(REMOVE_ACCESS, res.getMessage());
        return sendResponse(req, res, StringUtils.toString(res.getMessages(), " <li>"));
    }

    private SsoDataManager.Response removeAnAccess(String accessStr) {
        UserRoleEntry ure = UserRoleEntry.parse(accessStr);
        ServerRequest sr = new ServerRequest();
        sr.setParam(MISSION_NAME, ure.getRole().getMissionName());
        sr.setParam(MISSION_ID, String.valueOf(ure.getRole().getMissionId()));
        sr.setParam(GROUP_NAME, ure.getRole().getGroupName());
        sr.setParam(GROUP_ID, String.valueOf(ure.getRole().getGroupId()));
        sr.setParam(PRIVILEGE, ure.getRole().getPrivilege());
        sr.setParam(LOGIN_NAME, ure.getLoginName());
        return SsoDataManager.removeRole(sr);
    }

    private File showRoles(TableServerRequest req) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.showRoles(req);
        return sendResponse(req, res);
    }

    private File showAccess(TableServerRequest req) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.showAccess(req);
        return sendResponse(req, res);
    }

    private File getUsersByRole(TableServerRequest req) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.getUsersByRole(req);
        return sendResponse(req, res);
    }

    private File showMissions(TableServerRequest req) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.showMissions(req);
        return sendResponse(req, res);
    }

    private File getUsers(TableServerRequest req) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.getUserIds(req);
        return sendResponse(req, res);
    }

    private File showUsers(TableServerRequest req) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.showUsers(req);
        return sendResponse(req, res);
    }

    private File addMissions(TableServerRequest req) throws IOException, DataAccessException {
        SsoDataManager.Response res = SsoDataManager.addMissions(req);
        return sendResponse(req, res);
    }

    private File sendResponse(TableServerRequest req, SsoDataManager.Response res)  throws IOException, DataAccessException {
        return sendResponse(req, res, null);
    }

    private File sendResponse(TableServerRequest req, SsoDataManager.Response res, String overrideMsg)  throws IOException, DataAccessException {
        if (res.isError()) {
            String details = "";
            List<String> v = res.getMessages();
            for(String msg : v) {
                details += "<li>" + msg;
            }
            String errString = res.getMessages().size() == 1 ? "Error" : "Errors";
            throw new DataAccessException(new EndUserException("Validation " + errString + " <br>" + details, ""));
        } else if (res.getValue() instanceof DataGroup) {
            File f = createFile(req);
            IpacTableWriter.save(f, (DataGroup) res.getValue());
            return f;
        } else {
            String msg = StringUtils.isEmpty(overrideMsg) ? res.getMessage() : overrideMsg;
            DataType col = new DataType("user", String.class);
            DataGroup retval = new DataGroup("header-only", new DataType[]{col});
            retval.addAttributes(new DataGroup.Attribute("Message", msg));
            File f = createFile(req);
            IpacTableWriter.save(f, retval);
            return f;
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
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
