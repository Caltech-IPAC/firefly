package edu.caltech.ipac.uman.server;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.security.JOSSOAdapter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.uman.server.persistence.SsoDao;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import static edu.caltech.ipac.uman.data.UmanConst.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



@SearchProcessorImpl(id = "UmanProcessor")
public class UmanProcessor extends IpacTablePartProcessor {

    private static final Logger.LoggerImpl LOG = Logger.getLogger();

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
        } else if (action.equals(NEW_EMAIL)) {
            return changeEmail(request);
        } else if (action.equals(ADD_ROLE)) {
            return addRole(request);
        } else if (action.equals(REMOVE_ROLE)) {
            return removeRole(request);
        } else if (action.equals(SHOW_ROLES)) {
            return showRoles(request);
        } else if (action.equals(SHOW_ACCESS)) {
            return showAccess(request);
        }

        return null;
    }

    @Override
    public boolean doCache() {
        return false;
    }


    private File updateUser(TableServerRequest request) throws IOException, DataAccessException {
        UserInfo user = getUserInfo(request);

        boolean isUpdated = SsoDao.getInstance().updateUser(user);
        if (isUpdated) {
            return createReturnMsg(request, "Your information has been updated.");
        } else {
            sendErrorMsg("Nothing to update.");
        }
        return null;
    }

    private File changeEmail(TableServerRequest request) throws IOException, DataAccessException {
        String loginName = request.getParam(LOGIN_NAME);
        String toEmail = request.getParam(TO_EMAIL);
        String ctoEmail = request.getParam(CONFIRM_TO_EMAIL);

        List<String> msgs = doUserCheck(null, toEmail);
        doEmailCheck(msgs, toEmail, ctoEmail);

        if (msgs.size() == 0) {
            try {
                SsoDao.getInstance().updateUserEmail(loginName, toEmail);
                JOSSOAdapter.logout(ServerContext.getRequestOwner().getAuthKey());
                return createReturnMsg(request, "Your email has been updated.  You are now logged out.");
            } catch (Exception e) {
                sendErrorMsg(e.getMessage());
            }
        } else {
            sendErrorMsg(msgs.toArray(new String[msgs.size()]));
        }
        return null;
    }

    private File changePassword(TableServerRequest request) throws IOException, DataAccessException {
        String nPassword = request.getParam(NPASSWORD);
        String cPassword = request.getParam(CPASSWORD);

        List<String> msgs = doPasswordCheck(null, nPassword, cPassword);
        if (msgs.size() == 0) {
            try {
                request.setParam(PASSWORD, nPassword);
                UserInfo user = getUserInfo(request);
                SsoDao.getInstance().updateUserPassword(user.getLoginName(), user.getPassword());
                return createReturnMsg(request, "Your password has been updated.");
            } catch (Exception e) {
                sendErrorMsg(e.getMessage());
            }
                
        } else {
            sendErrorMsg(msgs.toArray(new String[msgs.size()]));
        }
        return null;
    }

    private File addUser(TableServerRequest request) throws IOException, DataAccessException {
        String loginName = request.getParam(LOGIN_NAME);
        String password = request.getParam(PASSWORD);
        String cPassword = request.getParam(CPASSWORD);

        List<String> msgs = doUserCheck(null, loginName);
        doPasswordCheck(msgs, password, cPassword);

        if (msgs.size() == 0) {
            UserInfo user = getUserInfo(request, false);

            SsoDao.getInstance().addUser(user);
            return createReturnMsg(request, "User " + loginName + " has been added.");
        } else {
            sendErrorMsg(msgs.toArray(new String[msgs.size()]));
        }
        return null;
    }

    private RoleList.RoleEntry getRoleEntry(TableServerRequest req) {
        RoleList.RoleEntry re = new RoleList.RoleEntry();
        if (req.containsParam(MISSION_ID)) {
            re.setMissionId(req.getIntParam(MISSION_ID));
        }
        if (req.containsParam(MISSION_NAME)) {
            re.setMissionName(req.getParam(MISSION_NAME));
        }
        if (req.containsParam(GROUP_ID)) {
            re.setGroupId(req.getIntParam(GROUP_ID));
        }
        if (req.containsParam(GROUP_NAME)) {
            re.setGroupName(req.getParam(GROUP_NAME));
        }
        return re;
    }

    private File addRole(TableServerRequest req) throws IOException, DataAccessException {

        hasAccess(ADMIN_ROLE);

        RoleList.RoleEntry re = getRoleEntry(req);
        
        if (SsoDao.getInstance().addRole(re)) {
            return createReturnMsg(req, "Role " + re.toString() + " has been added.");
        } else {
            sendErrorMsg("Unable to add Role:" + re.toString());
        }
        return null;
    }
    
    private File removeRole(TableServerRequest req) throws IOException, DataAccessException {
        hasAccess(ADMIN_ROLE);

        RoleList.RoleEntry re = getRoleEntry(req);

        if (SsoDao.getInstance().removeRole(re)) {
            return createReturnMsg(req, "Role " + re.toString() + " has been removed.");
        } else {
            sendErrorMsg("Unable to remove Role:" + re.toString());
        }
        return null;
    }

    private File showRoles(TableServerRequest req) throws IOException, DataAccessException {

        hasAccess(ADMIN_ROLE);
        String mission = req.getParam(MISSION_NAME);

        DataGroup roles = SsoDao.getInstance().getRoles(mission);
        File f = this.createFile(req);
        if (roles != null) {
            IpacTableWriter.save(f, roles);
        }
        return f;
    }

    private File showAccess(TableServerRequest req) throws IOException, DataAccessException {

        hasAccess(ADMIN_ROLE);
        String mission = req.getParam(MISSION_NAME);
        String user = req.getParam(EMAIL);

        DataGroup access = SsoDao.getInstance().getAccess(mission, user);
        File f = this.createFile(req);
        if (access != null) {
            IpacTableWriter.save(f, access);
        }
        return f;
    }

    private boolean hasAccess(final String role) throws DataAccessException {
        UserInfo user = ServerContext.getRequestOwner().getUserInfo();
        if (StringUtils.isEmpty(role) || user.getRoles().hasAccess(role)) {
            return true;
        } else {
            String msg = user.isGuestUser() ? "Not logged in" : "You are not allowed to perform that operation.";
            throw new DataAccessException(msg);
        }

    }


    private File createReturnMsg(TableServerRequest request, String msg) throws IOException, DataAccessException {
        DataType col = new DataType("user", String.class);
        DataGroup retval = new DataGroup("header-only", new DataType[]{col});
        retval.addAttributes(new DataGroup.Attribute("Message", msg));
        File f = createFile(request);
        IpacTableWriter.save(f, retval);
        return f;
    }

    private File sendErrorMsg(String... msgs) throws DataAccessException {
        String details = "";
        for(String msg : msgs) {
            details += "<li>" + msg;
        }
        String errString = msgs.length == 1 ? "Error" : "Errors";
        throw new DataAccessException(new EndUserException("Validation " + errString + " <br>" + details, ""));
    }

    private UserInfo getUserInfo(TableServerRequest request) throws DataAccessException {
        return getUserInfo(request, true);
    }

    private UserInfo getUserInfo(TableServerRequest request, boolean doValidate) throws DataAccessException {

        UserInfo user = new UserInfo();

        if(request.containsParam(LOGIN_NAME)) {
            String loginName = request.getParam(LOGIN_NAME);
            user.setEmail(loginName);
            user.setLoginName(loginName);
        }
        if (doValidate) {
            UserInfo cUser = ServerContext.getRequestOwner().getUserInfo();
            if (cUser == null || !cUser.getLoginName().equals(user.getLoginName())) {
                String msg = cUser == null ? "Not logged in" : "The given Login Name does not match";
                throw new DataAccessException(msg);
            }
        }

        user.setPassword(request.getParam(PASSWORD));
        user.setEmail(request.getParam(EMAIL));
        user.setFirstName(request.getParam(FIRST_NAME));
        user.setLastName(request.getParam(LAST_NAME));
        user.setAddress(request.getParam(ADDRESS));
        user.setCity(request.getParam(CITY));
        user.setCountry(request.getParam(COUNTRY));
        user.setInstitute(request.getParam(INSTITUTE));
        user.setPhone(request.getParam(PHONE));
        user.setPostcode(request.getParam(POSTCODE));
        return user;
    }

    private List<String> doUserCheck(List<String> msgs, String loginName) {

        msgs = msgs == null ? new ArrayList<String>() : msgs;
        if (StringUtils.isEmpty(loginName)) {
            msgs.add("Email is missing");
        } else {
            if (SsoDao.getInstance().isUser(loginName)) {
                msgs.add("A user with this email already exists.  <br> If you do not remember your password, you may reset your password for this account at the login page.");
            }
        }
        return msgs;
    }

    private List<String> doPasswordCheck(List<String> msgs, String password, String cPassword) {

        msgs = msgs == null ? new ArrayList<String>() : msgs;
        if (StringUtils.isEmpty(password)) {
            msgs.add("Password is missing");
        }
        if (StringUtils.isEmpty(cPassword)) {
            msgs.add("Confirm Password is missing");
        }
        if (password != null && cPassword != null && !password.equals(cPassword)) {
            msgs.add("Confirm Password password does not match with Password");
        }
        return msgs;
    }

    private List<String> doEmailCheck(List<String> msgs, String email, String cemail) {

        msgs = msgs == null ? new ArrayList<String>() : msgs;
        if (StringUtils.isEmpty(email)) {
            msgs.add("New Email is missing");
        }
        if (StringUtils.isEmpty(cemail)) {
            msgs.add("Confirm New Email is missing");
        }
        if (email != null && cemail != null && !email.equals(cemail)) {
            msgs.add("Confirm New Email does not match with New Email");
        }
        return msgs;
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
