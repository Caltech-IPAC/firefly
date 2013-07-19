package edu.caltech.ipac.uman.server;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
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

    /**
     * string1: user's name
     * string2: sso_base_url, ie.  http://***REMOVED***
     * string3: user's password
     */
    private static final String NEW_ACCT_MSG = "Dear %s,\n" +
            "\nA new IPAC account has been created for you." +
            "\nYour password is: %s\n" +
            "\nTo log in, enter your Email and password at our Login page:\n" +
            "\n%s/account/signon/login.do\n" +
            "\n\nOnce you have successfully logged in, you should change your password to something you can remember on your profile page.";

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
            RoleList.RoleEntry role = getRoleEntry(request);
            if (role != null && role.isValid()) {
                return getUsersByRole(request);
            } else {
                return getUsers(request);
            }
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

    public static void sendUserAddedEmail(String ssoBaseUrl, String emailTo, UserInfo user) {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", "mail0.ipac.caltech.edu");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.from", "donotreply@ipac.caltech.edu");
        props.put("mail.smtp.starttls.enable", "true");

        Session mailSession = Session.getDefaultInstance(props);

        String sendTo = StringUtils.isEmpty(emailTo) ? user.getEmail() : emailTo;
        try {
            EMailUtil.sendMessage(new String[]{sendTo}, null, null, "New IPAC Account created",
                    String.format(NEW_ACCT_MSG, user.getEmail(), user.getPassword(), ssoBaseUrl),
                    mailSession, false);
        } catch (EMailUtilException e) {
            throw new RuntimeException("Unable to send email to user/email:" + user.getLoginName() + "/" + user.getEmail());
        }
    }

    private File updateUser(TableServerRequest request) throws IOException, DataAccessException {
        UserInfo user = getUserInfo(request);

        String failMsg = "Nothing to update.";
        String successMsg = "Your information has been updated.";
        try {
            boolean isUpdated = SsoDao.getInstance().updateUser(user);
            if (isUpdated) {
                logStats(PROFILE, successMsg);
                return createReturnMsg(request, successMsg);
            } else {
                sendErrorMsg("Nothing to update.");
            }
        } catch (DataAccessException dax) {
            sendErrorMsg(failMsg, dax.getMessage());
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
                String successMsg = "Your email has been updated.  You are now logged out.";
                logStats(NEW_EMAIL, successMsg);
                return createReturnMsg(request, successMsg);
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
                String successMsg = "Your password has been updated.";
                logStats(NEW_PASS, successMsg);
                return createReturnMsg(request, successMsg);
            } catch (Exception e) {
                sendErrorMsg(e.getMessage());
            }
                
        } else {
            sendErrorMsg(msgs.toArray(new String[msgs.size()]));
        }
        return null;
    }

    private File resetPassword(TableServerRequest request) throws IOException, DataAccessException {
        String emailTo = request.getParam(SENDTO_EMAIL);
        String userEmail = request.getParam(EMAIL);

        hasAccess(SYS_ADMIN_ROLE);
        try {
            String newPassword = RandomStringUtils.randomAlphanumeric(8);  // generate random password
            SsoDao.getInstance().updateUserPassword(userEmail, newPassword);
            String successMsg = userEmail + "'s password has been reset.  New password sent to " + emailTo;
            if (!StringUtils.isEmpty(emailTo)) {
                String ssoBaseUrl = ServerContext.getRequestOwner().getBaseUrl();
                sendUserAddedEmail(ssoBaseUrl, emailTo, new UserInfo(emailTo, newPassword));
            }
            logStats(NEW_PASS, successMsg);
            return createReturnMsg(request, successMsg);
        } catch (Exception e) {
            sendErrorMsg(e.getMessage());
        }

        return null;
    }

    private File addUser(TableServerRequest request) throws IOException, DataAccessException {
        boolean doGeneratePassword = request.getBooleanParam(GEN_PASS);

        if (doGeneratePassword) {
            String pw = RandomStringUtils.randomAlphanumeric(8);
            request.setParam(PASSWORD, pw);
            request.setParam(CPASSWORD, pw);
        }

        String loginName = request.getParam(LOGIN_NAME);
        String password = request.getParam(PASSWORD);
        String cPassword = request.getParam(CPASSWORD);

        List<String> msgs = doUserCheck(null, loginName);
        doPasswordCheck(msgs, password, cPassword);

        if (msgs.size() == 0) {
            UserInfo user = getUserInfo(request, false);

            try {
                boolean added = SsoDao.getInstance().addUser(user);
                String successMsg = "User " + loginName + " has been added.";
                if (doGeneratePassword && added) {
                    String ssoBaseUrl = ServerContext.getRequestOwner().getBaseUrl();
                    String sendTo = user.getEmail();
                    try {
                        UmanProcessor.sendUserAddedEmail(ssoBaseUrl, sendTo, user);
                        successMsg += " ==> email sent";
                    } catch (Exception e) {
                        System.out.print(e.getMessage());
                    }
                }
                logStats(REGISTER, successMsg);
                return createReturnMsg(request, successMsg);
            } catch (Exception dax) {
                sendErrorMsg(msgs.toArray(new String[msgs.size()]));
            }
        } else {
            sendErrorMsg(msgs.toArray(new String[msgs.size()]));
        }
        return null;
    }

    private void logStats(String category, String successMsg) {
        logger.stats(category, successMsg.trim().replaceAll("\n", "; ") + " (By " + ServerContext.getRequestOwner().getUserInfo().getLoginName() + ")");
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
        if (req.containsParam(PRIVILEGE)) {
            re.setPrivilege(req.getParam(PRIVILEGE));
        }
        return re;
    }

    private File addAccess(TableServerRequest req) throws IOException, DataAccessException {
        String email = req.getParam(EMAIL);
        RoleList.RoleEntry re = getRoleEntry(req);
        UserRoleEntry ure = new UserRoleEntry(email, re);
        hasAccess(ure.getRole().getMissionName() + "::ADMIN");

        String failMsg = "Unable to add Access:" + ure.toString();
        String successMsg = "Access " + ure.toString() + " has been added.";
        try {
            if (SsoDao.getInstance().addAccess(ure)) {
                logStats(ADD_ACCESS, successMsg);
                return createReturnMsg(req, successMsg);
            } else {
                sendErrorMsg(failMsg);
            }
        } catch (Exception dax) {
            sendErrorMsg(failMsg, dax.getMessage());
        }
        return null;
    }

    private File addRole(TableServerRequest req) throws IOException, DataAccessException {
        RoleList.RoleEntry re = getRoleEntry(req);
        hasAccess(re.getMissionName() + "::ADMIN");

        String failMsg = "Unable to add Role:" + re.toString();
        String successMsg = "Role " + re.toString() + " has been added.";
        try {
            if (SsoDao.getInstance().addRole(re)) {
                logStats(ADD_ROLE, successMsg);
                return createReturnMsg(req, successMsg);
            } else {
                sendErrorMsg(failMsg);
            }
        } catch (Exception dax) {
            sendErrorMsg(failMsg, dax.getMessage());
        }

        return null;
    }

    private File removeRole(TableServerRequest req) throws IOException, DataAccessException {

        String rl = req.getParam(ROLE_LIST);
        String[] rlist = rl.split(",");
        String msg = "";
        boolean isSuccessful = false;
        for (String r : rlist) {
            try {
                RoleList.RoleEntry re = RoleList.RoleEntry.parse(r);
                hasAccess(re.getMissionName() + "::ADMIN");

                if (SsoDao.getInstance().removeRole(re)) {
                    isSuccessful = true;
                    msg += "\nRole " + re.toString() + " has been removed.";
                } else {
                    msg += "\nUnable to remove Role:" + r;
                }
            } catch (Exception e) {
                msg += "\nUnable to remove Role:" + r;
            }
        }
        if (msg.length() > 0 && isSuccessful) {
            logStats(REMOVE_ROLE, msg);
            createReturnMsg(req, msg);
        } else {
            sendErrorMsg(msg);
        }
        return null;
    }

    private File removeAccess(TableServerRequest req) throws IOException, DataAccessException {

        String al = req.getParam(ACCESS_LIST);
        String[] alist = al.split(",");
        String msg = "";
        boolean isSuccessful = false;
        for (String r : alist) {
            try {
                UserRoleEntry ure = UserRoleEntry.parse(r);
                hasAccess(ure.getRole().getMissionName() + "::ADMIN");

                if (SsoDao.getInstance().removeAccess(ure)) {
                    isSuccessful = true;
                    msg += "\nAccess " + ure.toString() + " has been removed.";
                } else {
                    LOG.briefDebug("Fail to remove access:" + r);
                    msg += "\nUnable to remove Access:" + r;
                }
            } catch (Exception e) {
                msg += "\nUnable to remove Access:" + r;
                e.printStackTrace();
            }
        }
        if (msg.length() > 0 && isSuccessful) {
            logStats(REMOVE_ACCESS, msg);
            createReturnMsg(req, msg);
        } else {
            sendErrorMsg(msg);
        }
        return null;
    }

    private File showRoles(TableServerRequest req) throws IOException, DataAccessException {

        String[] missions = getAdminMissions();

        DataGroup roles = SsoDao.getInstance().getRoles(missions);
        File f = this.createFile(req);
        if (roles != null) {
            IpacTableWriter.save(f, roles);
        }
        return f;
    }

    private File showAccess(TableServerRequest req) throws IOException, DataAccessException {

        String user = req.getParam(EMAIL);
        String[] missions = getAdminMissions();

        DataGroup access = SsoDao.getInstance().getAccess(user, missions);
        File f = this.createFile(req);
        if (access != null) {
            IpacTableWriter.save(f, access);
        }
        return f;
    }

    private File getUsersByRole(TableServerRequest req) throws IOException, DataAccessException {

        File f = this.createFile(req);
        RoleList.RoleEntry role = getRoleEntry(req);
        if (hasAccessToMission(role.getMissionName())) {
            DataGroup users = SsoDao.getInstance().getUsersByRole(role);
            if (users != null) {
                IpacTableWriter.save(f, users);
            }
        }
        return f;
    }

    private boolean hasAccessToMission(String name) {
        try {
            String[] missions = getAdminMissions();
            if (missions != null) {
                for (String s : missions) {
                    if (s.equalsIgnoreCase(name)) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        } catch (DataAccessException e) {
        }
        return false;
    }

    private File showMissions(TableServerRequest req) throws IOException, DataAccessException {

        boolean hasAccess = hasAccess(SYS_ADMIN_ROLE);
        File f = this.createFile(req);
        if (hasAccess) {
            DataGroup missions = SsoDao.getInstance().getMissionXRefs();
            IpacTableWriter.save(f, missions);
        }
        return f;
    }

    private File getUsers(TableServerRequest req) throws IOException, DataAccessException {

        boolean hasAccess = hasAccess(ADMIN_ROLE);
        File f = this.createFile(req);
        if (hasAccess) {
            List<String> users = SsoDao.getInstance().getUserIDs();
            DataType dtemail = new DataType(DB_EMAIL, String.class);
            DataGroup dg = new DataGroup("users", new DataType[]{dtemail});
            if (users != null) {
                for(String s : users) {
                    DataObject row = new DataObject(dg);
                    row.setDataElement(dtemail, s);
                    dg.add(row);
                }
            }
            IpacTableWriter.save(f, dg);
        }
        return f;
    }

    private File showUsers(TableServerRequest req) throws IOException, DataAccessException {
        boolean hasAccess = hasAccess(SYS_ADMIN_ROLE);
        File f = this.createFile(req);
        if (hasAccess) {
            DataGroup dg = SsoDao.getInstance().getUserInfo(null);
            if (dg != null) {
                IpacTableWriter.save(f, dg);
            }
        }
        return f;
    }

    private File addMissions(TableServerRequest req) throws IOException, DataAccessException {

        boolean hasAccess = hasAccess(SYS_ADMIN_ROLE);
        int id = req.getIntParam(MISSION_ID);
        String name = req.getParam(MISSION_NAME);
        if (hasAccess) {
            String successMsg = "Mission " + name + "(" + id + ") has been added.";
            String failMsg = "Unable to add Mission: " + "Mission " + name + "(" + id + ")";
            try {
                if (SsoDao.getInstance().addMissionXRef(id, name)) {
                    logStats(ADD_MISSION_XREF, successMsg);
                    return createReturnMsg(req, successMsg);
                } else {
                    sendErrorMsg(failMsg);
                }
            } catch(Exception ex) {
                sendErrorMsg(failMsg);
            }
        }
        return null;
    }

    /**
     * return the missions this person have ADMIN access to.
     * return an empty list when this person has no access to any mission.
     * return null when this person has access to all of the missions.
     * @return
     * @throws DataAccessException
     */
    private String[] getAdminMissions() throws DataAccessException {
        List<String> missions = new ArrayList<String>();
        UserInfo user = ServerContext.getRequestOwner().getUserInfo();
        RoleList rl = user.getRoles();
        for (RoleList.RoleEntry r : rl) {
            if (!StringUtils.isEmpty(r.getPrivilege()) && r.getPrivilege().equals("ADMIN")) {
                if (r.getMissionName().equals("ALL")) {
                    // null mean all
                    return null;
                }
                missions.add(r.getMissionName());
            }
        }
        return missions.toArray(new String[missions.size()]);
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
