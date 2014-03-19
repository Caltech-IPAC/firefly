package edu.caltech.ipac.uman.server;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.EMailUtil;
import edu.caltech.ipac.firefly.server.util.EMailUtilException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.uman.data.UserRoleEntry;
import edu.caltech.ipac.uman.server.persistence.SsoDao;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.lang.RandomStringUtils;

import javax.mail.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static edu.caltech.ipac.uman.data.UmanConst.*;


public class SsoDataManager {
    public static final Logger.LoggerImpl logger = Logger.getLogger();

    /**
     * string1: user's name string2: sso_base_url, ie.  http://***REMOVED*** string3: user's password
     */
    private static final String NEW_ACCT_SUBJ = "New IPAC Account created";
    private static final String NEW_ACCT_MSG = "Dear %1$s,\n" +
            "\nA new IPAC account has been created for you." +
            "\nYour password is: %2$s\n" +
            "\nTo log in, enter your Email and password at our Login page:\n" +
            "\n%3$s\n" +
            "\n\nOnce you have successfully logged in, you should change your password to something you can remember on your profile page.";

    private static final String RESET_PASS_SUBJ = "IPAC Password Reset verification e-mail";
    private static final String RESET_PASS_MSG = "Dear %1$s,\n" +
            "\nHere is the new password you requested: %2$s\n" +
            "\nTo log in, enter your Email and password at our Login page:\n" +
            "\n%3$s\n" +
            "\n\nOnce you have successfully logged in, you should change your password to something you can remember on your profile page.";

    public static class Response<T> {
        public enum Status {
            OK(0), PERMISSION_DENIED(77), WARNING(-1), SYSTEM_ERROR(71), ERROR(1), VALIDATION_ERROR(2);
            int code;

            Status(int code) {
                this.code = code;
            }

            public int code() {
                return code;
            }
        }

        private List<String> messages;
        private T value;
        private Status status;

        public Response() {
            this.status = Status.OK;
        }

        public Response(Status status, String message) {
            this(status, null, message);
        }

        public Response(Status status, T value, String... message) {
            this.status = status;
            this.value = value;
            if (message != null) {
                for (String m : message) {
                    addMessage(m);
                }
            }
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public boolean isOk() {
            return status == Status.OK;
        }

        public boolean isError() {
            return status.code > 0;
        }

        public String getMessage() {
            return getMessage("; ");
        }

        public String getMessage(String separator) {
            return status.name() + (messages == null || messages.size() == 0 ? "" : ": " + StringUtils.toString(messages, separator));
        }

        public String getMessagesAsString() {
            return StringUtils.toString(messages, "; ");
        }

        public List<String> getMessages() {
            return messages;
        }

        public T getValue() {
            return value;
        }

        public Status getStatus() {
            return status;
        }

        public Response<T> addMessage(String msg) {
            if (messages == null) {
                messages = new ArrayList<String>();
            }
            if (!StringUtils.isEmpty(msg)) {
                messages.add(msg);
            }
            return this;
        }

        public Response combine(Response r) {
            Response rep = new Response();
            rep.status = status != Status.OK ? status : r.status;
            rep.messages = new ArrayList<String>();
            if (!isOk() && getMessages() != null) {
                rep.messages.addAll(getMessages());
            }
            if (!r.isOk() && r.getMessages() != null) {
                rep.messages.addAll(r.getMessages());
            }
            if (rep.messages.size() == 0) {
                rep.messages = null;
            }
            return rep;
        }
    }


    public static void sendUserAddedEmail(String loginUrl, String emailTo, UserInfo user, String emailMsg, String emailFrom, String emailSubject) {

        String userEmail = user.getEmail();
        emailTo = StringUtils.isEmpty(emailTo) ? userEmail : emailTo.replaceAll("\\$user", userEmail);

        emailFrom = StringUtils.isEmpty(emailFrom) ? "donotreply@ipac.caltech.edu" : emailFrom;
        emailMsg = StringUtils.isEmpty(emailMsg) ? NEW_ACCT_MSG : emailMsg;
        emailSubject = StringUtils.isEmpty(emailSubject) ? NEW_ACCT_SUBJ : emailSubject;

        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", "mail0.ipac.caltech.edu");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.from", emailFrom);
        props.put("mail.smtp.starttls.enable", "true");

        if (loginUrl == null) {
            loginUrl = ServerContext.getRequestOwner().getHostUrl() + "/account/signon/login.do";
        }
        Session mailSession = Session.getDefaultInstance(props);

        String[] sendTo = emailTo.split(",");
        try {
            EMailUtil.sendMessage(sendTo, null, null, emailSubject,
                    String.format(emailMsg, user.getEmail(), user.getPassword(), loginUrl),
                    mailSession, false);
        } catch (EMailUtilException e) {
            throw new RuntimeException(user.getLoginName() + ": Unable to send email to " + CollectionUtil.toString(sendTo));
        }
    }

    /**
     * update the user information given the data in request. Params used: LOGIN_NAME, PASSWORD, EMAIL, FIRST_NAME,
     * LAST_NAME, ADDRESS, CITY, COUNTRY, INSTITUTE, PHONE, POSTCODE
     */
    public static Response<UserInfo> updateUser(ServerRequest request) {

        UserInfo user = extractUserInfo(request);
        try {
            Response<UserInfo> rep = hasUpdateAccess(user);
            if (!rep.isOk()) return rep;

            if (!SsoDao.getInstance().isUser(user.getLoginName())) {
                return new Response<UserInfo>(Response.Status.VALIDATION_ERROR, user.getLoginName() + " is not a user in the system.");
            }

            if (SsoDao.getInstance().updateUser(user)) {
                return new Response<UserInfo>(Response.Status.OK, user, user.getLoginName() + " successfully updated.");
            } else {
                return new Response<UserInfo>(Response.Status.WARNING, user, user.getLoginName() + " has nothing to update.");
            }
        } catch (Exception e) {
            return new Response<UserInfo>(Response.Status.SYSTEM_ERROR, "Unable to update " + user.getLoginName() + ".  --  " + e.getMessage());
        }
    }

    /**
     * Params used: LOGIN_NAME, TO_EMAIL, CONFIRM_TO_EMAIL
     */
    public static Response changeEmail(ServerRequest request) {

        UserInfo user = extractUserInfo(request);
        try {
            Response rep = hasUpdateAccess(user);
            if (!rep.isOk()) return rep;

            String toEmail = request.getParam(TO_EMAIL);
            String ctoEmail = request.getParam(CONFIRM_TO_EMAIL);

            Response ur = doUserIdCheck(toEmail, true);
            Response er = doEmailCheck(user.getLoginName(), toEmail, ctoEmail);

            if (ur.isOk() && er.isOk()) {
                SsoDao.getInstance().updateUserEmail(user.getLoginName(), toEmail);
                return new Response(Response.Status.OK, "Email successfully updated.");
            } else {
                return ur.combine(er);
            }
        } catch (Exception e) {
            return new Response(Response.Status.SYSTEM_ERROR, "Unable to change " + user.getLoginName() + "'s email.  --  " + e.getMessage());
        }
    }

    /**
     * Params used: LOGIN_NAME, NPASSWORD, CPASSWORD
     */
    public static Response changePassword(ServerRequest request) {

        UserInfo user = extractUserInfo(request);
        try {
            Response res = hasUpdateAccess(user);
            if (!res.isOk()) return res;

            String nPassword = request.getParam(NPASSWORD);
            String cPassword = request.getParam(CPASSWORD);

            res = doPasswordCheck(user.getLoginName(), nPassword, cPassword);
            if (!res.isOk()) return res;

            user.setPassword(nPassword);
            SsoDao.getInstance().updateUserPassword(user.getLoginName(), user.getPassword());
            return new Response(Response.Status.OK, "Password successfully updated.");
        } catch (Exception e) {
            return new Response(Response.Status.SYSTEM_ERROR, "Unable to change " + user.getLoginName() + "'s password.  --  " + e.getMessage());
        }
    }

    /**
     * Params used: EMAIL, SENDTO_EMAIL
     */
    public static Response resetPassword(ServerRequest request) {

        Response rep = hasAccess(SYS_ADMIN_ROLE);
        if (!rep.isOk()) return rep;

        String emailTo = request.getParam(SENDTO_EMAIL);
        String userEmail = request.getParam(EMAIL);

        try {
            if (!SsoDao.getInstance().isUser(userEmail)) {
                return new Response(Response.Status.ERROR, userEmail + ": Is not a user in the system.");
            }

            String newPassword = RandomStringUtils.randomAlphanumeric(8);  // generate random password
            SsoDao.getInstance().updateUserPassword(userEmail, newPassword);
            rep = new Response(Response.Status.OK, "Reset " + userEmail + "'s password.");
            if (!StringUtils.isEmpty(emailTo)) {
                sendUserAddedEmail(null, emailTo, new UserInfo(emailTo, newPassword), RESET_PASS_MSG, null, RESET_PASS_SUBJ);
                rep.addMessage("New password sent to " + emailTo);
            }
            return rep;
        } catch (Exception e) {
            return new Response(Response.Status.SYSTEM_ERROR, "Unable to reset " + userEmail + "'s password.  --  " + e.getMessage());
        }
    }

    /**
     * Params used: GEN_PASS, LOGIN_NAME, PASSWORD, CPASSWORD, EMAIL, FIRST_NAME, LAST_NAME, ADDRESS, CITY, COUNTRY,
     * INSTITUTE, PHONE, POSTCODE
     */
    public static Response<UserInfo> addUser(ServerRequest request) {

        UserInfo user = null;
        try {

            boolean doGeneratePassword = request.getBooleanParam(GEN_PASS);
            if (doGeneratePassword) {
                String pw = RandomStringUtils.randomAlphanumeric(8);
                request.setParam(PASSWORD, pw);
                request.setParam(CPASSWORD, pw);
            }
            String cPassword = request.getParam(CPASSWORD);

            user = extractUserInfo(request);
            Response ur = doUserIdCheck(user.getLoginName(), false);
            Response pr = doPasswordCheck(user.getLoginName(), user.getPassword(), cPassword);
            Response combined = ur.combine(pr);
            if (!combined.isOk()) return new Response<UserInfo>(combined.status, combined.getMessagesAsString());

            if (SsoDao.getInstance().addUser(user)) {
                return new Response<UserInfo>(Response.Status.OK, user, user.getLoginName() + " added.");
            } else {
                return new Response<UserInfo>(Response.Status.ERROR, user, user.getLoginName() + " NOT added.");
            }
        } catch (Exception e) {
            String name = user == null ? "null" : user.getLoginName();
            return new Response<UserInfo>(Response.Status.SYSTEM_ERROR, name + " NOT added.  --  " + e.getMessage());
        }
    }

    /**
     * Params used: EMAIL, MISSION_ID, MISSION_NAME, GROUP_ID, GROUP_NAME, PRIVILEGE
     */
    public static Response addAccess(ServerRequest req) {
        UserRoleEntry ure = null;
        String email = req.getParam(LOGIN_NAME);
        try {
            RoleList.RoleEntry re = extraRoleEntry(req);
            ure = new UserRoleEntry(email, re);

            Response rep = hasAccess(ure.getRole().getMissionName() + ADMIN_ROLE);
            if (!rep.isOk()) return rep;

            rep = doCheckAccess(ure);
            if (!rep.isOk()) return rep;

            if (SsoDao.getInstance().addAccess(ure)) {
                return new Response(Response.Status.OK, email + " added to  " + re + ".");
            } else {
                return new Response(Response.Status.WARNING, email + " is already a member of " + re + ".");
            }
        } catch (Exception e) {
            return new Response(Response.Status.SYSTEM_ERROR, email + " NOT added to " + (ure == null ? "NULL" : ure.getRole()) + ".  --  " + e.getMessage());
        }
    }

    /**
     * Params used: MISSION_ID, MISSION_NAME, GROUP_ID, GROUP_NAME, PRIVILEGE
     */
    public static Response addRole(ServerRequest req) {
        RoleList.RoleEntry re = null;
        try {
            re = extraRoleEntry(req);
            Response rep = hasAccess(re.getMissionName() + ADMIN_ROLE);
            if (!rep.isOk()) return rep;

            if (SsoDao.getInstance().roleExists(re)) {
                return new Response(Response.Status.WARNING, re + " is already in the system.");
            }

            if (SsoDao.getInstance().addRole(re)) {
                return new Response(Response.Status.OK, re.toString() + " added.");
            } else {
                return new Response(Response.Status.ERROR, re.toString() + " NOT added.");
            }
        } catch (Exception e) {
            return new Response(Response.Status.SYSTEM_ERROR, (re != null ? re.toString() : "NULL") + " NOT added.  --  " + e.getMessage());
        }
    }

    /**
     * Params used: LOGIN_NAME
     */
    public static Response<UserInfo> removeUser(ServerRequest request) {
        Response rep = hasAccess(SYS_ADMIN_ROLE);
        if (!rep.isOk()) return new Response<UserInfo>(rep.status, rep.getMessagesAsString());

        UserInfo user = extractUserInfo(request);
        try {
            SsoDao.getInstance().removeUser(user.getLoginName());
            return new Response<UserInfo>(Response.Status.OK, user, user.getLoginName() + " removed.");
        } catch (Exception e) {
            return new Response<UserInfo>(Response.Status.SYSTEM_ERROR, user.getLoginName() + " NOT removed.  --  " + e.getMessage());
        }
    }


    /**
     * remove a role and all of its access entries Params used: MISSION_ID, MISSION_NAME, GROUP_ID, GROUP_NAME,
     * PRIVILEGE
     */
    public static Response removeRole(ServerRequest req) {


        RoleList.RoleEntry re = extraRoleEntry(req);
        Response rep = hasAccess(re.getMissionName() + ADMIN_ROLE);
        if (!rep.isOk()) return rep;

        try {
            if (SsoDao.getInstance().removeRole(re)) {
                return new Response(Response.Status.OK, re + " removed.");
            } else {
                return new Response(Response.Status.ERROR, re + " NOT removed.");
            }
        } catch (Exception e) {
            return new Response(Response.Status.SYSTEM_ERROR, re + " NOT removed. -- " + e.getMessage());
        }
    }

    /**
     * remove an access entry Params used: EMAIL, MISSION_ID, MISSION_NAME, GROUP_ID, GROUP_NAME, PRIVILEGE
     */
    public static Response removeAccess(ServerRequest req) {

        UserRoleEntry ure = null;
        String email = req.getParam(LOGIN_NAME);
        try {
            RoleList.RoleEntry re = extraRoleEntry(req);
            ure = new UserRoleEntry(email, re);

            Response rep = hasAccess(ure.getRole().getMissionName() + ADMIN_ROLE);
            if (!rep.isOk()) return rep;

            rep = doCheckAccess(ure);
            if (!rep.isOk()) return rep;

            if (SsoDao.getInstance().removeAccess(ure)) {
                return new Response(Response.Status.OK, ure + " removed.");
            } else {
                return new Response(Response.Status.ERROR, ure + " NOT removed.");

            }
        } catch (Exception e) {
            return new Response(Response.Status.SYSTEM_ERROR, ure + " NOT removed.  --  " + e.getMessage());
        }
    }

    /**
     * filter by a list of mission name separated by comma.  All accessible missions if MISSION_NAME is not given.
     * Params used: MISSION_NAME
     */
    public static Response<DataGroup> showRoles(ServerRequest req) {

        try {
            String[] missions = null;
            String name = req.getParam(MISSION_NAME);
            if (!StringUtils.isEmpty(name)) {
                missions = name.split(",");
            }
            Response<String[]> res = getAdminMissions(missions);
            if (!res.isOk())
                return new Response<DataGroup>(Response.Status.PERMISSION_DENIED, res.getMessagesAsString());

            DataGroup roles = SsoDao.getInstance().getRoles(res.getValue());
            return new Response<DataGroup>(Response.Status.OK, roles);
        } catch (Exception e) {
            return new Response<DataGroup>(Response.Status.SYSTEM_ERROR, "Unable to list roles.  --  " + e.getMessage());
        }
    }

    /**
     * filter by a list of mission name separated by comma.  All accessible missions if MISSION_NAME is not given. if
     * EMAIL is given, then only list access for the given user. Params used: EMAIL, MISSION_NAME
     */
    public static Response<DataGroup> showAccess(ServerRequest req) {

        try {
            String user = req.getParam(LOGIN_NAME);

            String[] missions = null;
            String name = req.getParam(MISSION_NAME);
            if (!StringUtils.isEmpty(name)) {
                missions = name.split(",");
            }
            Response<String[]> res = getAdminMissions(missions);
            if (!res.isOk())
                return new Response<DataGroup>(Response.Status.PERMISSION_DENIED, res.getMessagesAsString());

            DataGroup access = SsoDao.getInstance().getAccess(user, res.getValue());
            return new Response<DataGroup>(Response.Status.OK, access);
        } catch (Exception e) {
            return new Response<DataGroup>(Response.Status.SYSTEM_ERROR, "Unable to show access.  --  " + e.getMessage());
        }
    }

    /**
     * Params used: MISSION_ID, MISSION_NAME, GROUP_ID, GROUP_NAME, PRIVILEGE
     */
    public static Response<DataGroup> getUsersByRole(ServerRequest req) {

        RoleList.RoleEntry role = extraRoleEntry(req);
        try {
            Response rep = hasAccess(role.getMissionName() + ADMIN_ROLE);
            if (!rep.isOk()) return new Response<DataGroup>(rep.status, rep.getMessagesAsString());

            DataGroup users = SsoDao.getInstance().getUsersByRole(role);
            return new Response<DataGroup>(Response.Status.OK, users);
        } catch (Exception e) {
            return new Response<DataGroup>(Response.Status.SYSTEM_ERROR, "Unable to list users of " + role.toString() + ".  --  " + e.getMessage());
        }

    }

    /**
     * Params used: none
     */
    public static Response<DataGroup> showMissions(ServerRequest req) {

        try {
            Response res = hasAccess(SYS_ADMIN_ROLE);
            if (!res.isOk()) return new Response<DataGroup>(res.status, res.getMessagesAsString());

            DataGroup missions = SsoDao.getInstance().getMissionXRefs();
            return new Response<DataGroup>(Response.Status.OK, missions);
        } catch (Exception e) {
            return new Response<DataGroup>(Response.Status.SYSTEM_ERROR, "Unable to list missions.  --  " + e.getMessage());
        }

    }

    /**
     * list all users' login_name in the system Params used: none
     */
    public static Response<DataGroup> getUserIds(ServerRequest req) {

        try {
            // any admin role may view users
            Response res = hasAccess(ADMIN_ROLE);
            if (!res.isOk()) return res;

            List<String> users = SsoDao.getInstance().getUserIDs();
            DataType dtemail = new DataType(DB_EMAIL, String.class);
            DataGroup dg = new DataGroup("users", new DataType[]{dtemail});
            if (users != null) {
                for (String s : users) {
                    DataObject row = new DataObject(dg);
                    row.setDataElement(dtemail, s);
                    dg.add(row);
                }
            }
            return new Response<DataGroup>(Response.Status.OK, dg);
        } catch (Exception e) {
            return new Response<DataGroup>(Response.Status.SYSTEM_ERROR, "Unable to list user IDs.  --  " + e.getMessage());
        }

    }

    public static Response<DataGroup> showUsers(ServerRequest req) {
        return showUsers(req, false);
    }

    /**
     * Params used: LOGIN_NAME
     */
    public static Response<DataGroup> showUsers(ServerRequest req, boolean brief) {

        try {
            // any admin role may view users
            Response res = hasAccess(ADMIN_ROLE);
            if (!res.isOk()) return res;

            String name = req.getParam(LOGIN_NAME);
            DataGroup dg = SsoDao.getInstance().getUserInfo(name, brief);
            return new Response<DataGroup>(Response.Status.OK, dg);
        } catch (Exception e) {
            return new Response<DataGroup>(Response.Status.SYSTEM_ERROR, "Unable to list users.  --  " + e.getMessage());
        }
    }

    /**
     * Params used: MISSION_ID, MISSION_NAME
     */
    public static Response addMissions(ServerRequest req) {

        String fname = null;
        try {
            Response res = hasAccess(SYS_ADMIN_ROLE);
            if (!res.isOk()) return res;

            int id = req.getIntParam(MISSION_ID);
            String name = req.getParam(MISSION_NAME);
            fname = name + "(" + id + ")";
            if (SsoDao.getInstance().addMissionXRef(id, name)) {
                return new Response(Response.Status.OK, fname + " added.");
            } else {
                return new Response(Response.Status.WARNING, fname + " is already in the system.");
            }
        } catch (Exception e) {
            return new Response(Response.Status.SYSTEM_ERROR, fname + " NOT added.  --  " + e.getMessage());
        }

    }

    /**
     * does current login user has access to the given role.
     */
    public static Response hasAccess(final String role) {
        UserInfo user = ServerContext.getRequestOwner().getUserInfo();
        try {
            if (StringUtils.isEmpty(role) || user.getRoles().hasAccess(role)) {
                return new Response();
            } else {
                String msg = user.getLoginName() + " is not allowed access to " + role + ".";
                return new Response(Response.Status.PERMISSION_DENIED, msg);
            }
        } catch (Exception e) {
            return new Response(Response.Status.SYSTEM_ERROR, "Unexpected error while checking for " + user.getLoginName() + "'s privileges.  --  " + e.getMessage());
        }

    }

    public static Response<UserInfo> hasUpdateAccess(UserInfo user) {
        try {
            UserInfo cUser = ServerContext.getRequestOwner().getUserInfo();
            if (cUser == null) {
                return new Response<UserInfo>(Response.Status.PERMISSION_DENIED, "Not logged in");
            } else if (user == null) {
                return new Response<UserInfo>(Response.Status.VALIDATION_ERROR, "Requested user is null.");
            } else if (StringUtils.isEmpty(user.getLoginName())) {
                return new Response<UserInfo>(Response.Status.VALIDATION_ERROR, "Requested user Login Name is missing");
            } else {
                if (!hasAccess(SYS_ADMIN_ROLE).isOk()) {
                    if (!cUser.getLoginName().equals(user.getLoginName())) {
                        return new Response<UserInfo>(Response.Status.PERMISSION_DENIED, cUser.getLoginName() + " is not allow to update other user's information.");
                    }
                }
            }
        } catch (Exception e) {
            new Response<UserInfo>(Response.Status.SYSTEM_ERROR, "Unable to update " + user.getLoginName() + " information  --  " + e.getMessage());
        }
        return new Response<UserInfo>();
    }

//====================================================================
//
//====================================================================

    /**
     * return the missions this person have ADMIN access to. return an empty list when this person has no access to any
     * mission. return null when this person has access to all of the missions.
     */
    private static Response<String[]> getAdminMissions(String... filterBy) {
        List<String> missions = new ArrayList<String>();
        UserInfo user = ServerContext.getRequestOwner().getUserInfo();
        RoleList rl = user.getRoles();
        for (RoleList.RoleEntry r : rl) {
            if (!StringUtils.isEmpty(r.getPrivilege()) && r.getPrivilege().equals("ADMIN")) {
                if (r.getMissionName().equals("ALL")) {
                    // null mean all
                    return new Response<String[]>(Response.Status.OK, filterBy);
                }
                if (filterBy == null || Arrays.binarySearch(filterBy, r.getMissionName()) >= 0) {
                    missions.add(r.getMissionName());
                }
            }
        }
        String[] vals = missions.toArray(new String[missions.size()]);
        if (vals.length == 0) {
            String msg;
            if (user.isGuestUser()) {
                msg = "You are not logged in";
            } else {
                msg = filterBy == null ? user.getLoginName() + " is not an admin." : user.getLoginName() + " is not authorized to access project " + CollectionUtil.toString(filterBy) + ".";
            }
            return new Response<String[]>(Response.Status.PERMISSION_DENIED, msg);

        } else {
            return new Response<String[]>(Response.Status.OK, vals);
        }
    }

    private static RoleList.RoleEntry extraRoleEntry(ServerRequest req) {
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

    private static UserInfo extractUserInfo(ServerRequest request) {

        UserInfo user = new UserInfo();

        if (request.containsParam(LOGIN_NAME)) {
            String loginName = request.getParam(LOGIN_NAME);
            user.setEmail(loginName);
            user.setLoginName(loginName);
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


    private static Response<UserInfo> doUserIdCheck(String loginName, boolean failOnDup) {
        try {
            if (StringUtils.isEmpty(loginName)) {
                return new Response<UserInfo>(Response.Status.VALIDATION_ERROR, null, "Login name is missing.");
            } else if (loginName.length() < 8) {
                return new Response<UserInfo>(Response.Status.VALIDATION_ERROR, null, loginName + ": Login name must be at least 8 characters long.");
            } else {
                if (SsoDao.getInstance().isUser(loginName)) {
                    String msg = loginName + " already exists in the system.";
                    if (failOnDup) {
                        return new Response<UserInfo>(Response.Status.VALIDATION_ERROR, msg);
                    } else {
                        return new Response<UserInfo>(Response.Status.WARNING, msg);
                    }
                }
            }
            return new Response<UserInfo>();
        } catch (Exception e) {
            return new Response<UserInfo>(Response.Status.SYSTEM_ERROR, "Unable to validate " + loginName + " --  " + e.getMessage());
        }
    }

    private static Response doPasswordCheck(String userid, String password, String cPassword) {

        String msg = null;
        if (StringUtils.isEmpty(password)) {
            msg = "Password is missing.";
        } else if (StringUtils.isEmpty(cPassword)) {
            msg = "Confirm Password is missing.";
        } else if (password != null && cPassword != null && !password.equals(cPassword)) {
            msg = "Confirm Password password does not match with Password.";
        }
        return new Response(msg == null ? Response.Status.OK : Response.Status.VALIDATION_ERROR, msg);
    }

    private static Response doEmailCheck(String userid, String email, String cemail) {

        String msg = null;
        if (StringUtils.isEmpty(email)) {
            msg = "New Email is missing.";
        } else if (StringUtils.isEmpty(cemail)) {
            msg = "Confirm New Email is missing.";
        } else if (email != null && cemail != null && !email.equals(cemail)) {
            msg = "Confirm New Email does not match with New Email.";
        }
        return new Response(msg == null ? Response.Status.OK : Response.Status.VALIDATION_ERROR, msg);
    }

    private static Response doCheckAccess(UserRoleEntry ure) {

        if (!SsoDao.getInstance().isUser(ure.getLoginName())) {
            return new Response(Response.Status.VALIDATION_ERROR, ure.getLoginName() + ": Is not a registered user.");
        }

        try {
            RoleList.RoleEntry role = SsoDao.getInstance().findRole(ure.getRole());
            if (role == null) {
                return new Response(Response.Status.VALIDATION_ERROR, ure.getRole() + " is not in the system.");
            }
        } catch (DataAccessException e) {
            return new Response(Response.Status.SYSTEM_ERROR, "Unable to validate access of " + ure + ". --  " + e.getMessage());
        }
        return new Response();
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
