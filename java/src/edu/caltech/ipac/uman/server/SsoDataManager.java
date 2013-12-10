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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static edu.caltech.ipac.uman.data.UmanConst.*;


public class SsoDataManager {
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


    public static class Response<T> {
        private int status = 0;
        private List<String> messages;
        private T value;

        public Response() {
        }

        public Response(int status, T value, String... message) {
            this.status = status;
            this.value = value;
            if (message != null) {
                for (String m : message) {
                    addMessage(m);
                }
            }
        }

        public boolean isOk() {
            return status == 0;
        }

        public boolean isError() {
            return status > 0;
        }

        public String getMessage() {
            return getMessage("\n");
        }

        public String getMessage(String separator) {
            return StringUtils.toString(messages, separator);
        }

        public List<String> getMessages() {
            return messages;
        }

        public T getValue() {
            return value;
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
            rep.status = status + r.status;
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



    public static void sendUserAddedEmail(String ssoBaseUrl, String emailTo, UserInfo user) {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", "mail0.ipac.caltech.edu");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.from", "donotreply@ipac.caltech.edu");
        props.put("mail.smtp.starttls.enable", "true");

        if (ssoBaseUrl == null) {
            ssoBaseUrl = ServerContext.getRequestOwner().getBaseUrl();
        }
        Session mailSession = Session.getDefaultInstance(props);

        String sendTo = StringUtils.isEmpty(emailTo) ? user.getEmail() : emailTo;
        try {
            EMailUtil.sendMessage(new String[]{sendTo}, null, null, "New IPAC Account created",
                    String.format(NEW_ACCT_MSG, user.getEmail(), user.getPassword(), ssoBaseUrl),
                    mailSession, false);
        } catch (EMailUtilException e) {
            throw new RuntimeException(user.getLoginName() + ": Unable to send email to " + sendTo);
        }
    }

    /**
     * update the user information given the data in request.
     * Params used:
     *      LOGIN_NAME, PASSWORD, EMAIL, FIRST_NAME, LAST_NAME, ADDRESS, CITY, COUNTRY, INSTITUTE, PHONE, POSTCODE
     * @param request
     * @return
     * @throws IOException
     * @throws DataAccessException
     */
    public static Response<UserInfo> updateUser(ServerRequest request) {

        UserInfo user = extractUserInfo(request);
        try {
            Response rep = hasUpdateAccess(user);
            if (!rep.isOk()) return rep;

            if (!SsoDao.getInstance().isUser(user.getLoginName())) {
                return new Response<UserInfo>(1, null, user.getLoginName() + ": Is not a user in the system.");
            }

            if (SsoDao.getInstance().updateUser(user)) {
                return new Response(0, user, user.getLoginName() + ": Information successfully updated.");
            } else {
                return new Response(-1, user, user.getLoginName() + ": Nothing to update.");
            }
        } catch (Exception e) {
            return new Response<UserInfo>(1, user, user.getLoginName() +":  Unable to update user.  --  " +  e.getMessage());
        }
    }

    /**
     * Params used:
     *      LOGIN_NAME, TO_EMAIL, CONFIRM_TO_EMAIL
     * @param request
     * @return
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
                    return new Response(0, null, user.getLoginName() + ": Email successfully updated.");
            } else {
                return ur.combine(er);
            }
        } catch (Exception e) {
            return new Response(1, null, user.getLoginName() + ": Unable to change email.  --  " +  e.getMessage());
        }
    }

    /**
     * Params used:
     *      LOGIN_NAME, NPASSWORD, CPASSWORD
     * @param request
     * @return
     */
    public static Response changePassword(ServerRequest request) {

        UserInfo user = extractUserInfo(request);
        try {
            Response rep = hasUpdateAccess(user);
            if (!rep.isOk()) return rep;

            String nPassword = request.getParam(NPASSWORD);
            String cPassword = request.getParam(CPASSWORD);

            rep = doPasswordCheck(user.getLoginName(), nPassword, cPassword);
            if (!rep.isOk()) return rep;

            user.setPassword(nPassword);
            SsoDao.getInstance().updateUserPassword(user.getLoginName(), user.getPassword());
            return new Response(0, null, user.getLoginName() + ": Password successfully updated.");
        } catch (Exception e) {
            return new Response(1, null, user.getLoginName() + ": Unable to change password.  --  " +  e.getMessage());
        }
    }

    /**
     * Params used:
     *      EMAIL, SENDTO_EMAIL
     * @param request
     * @return
     */
    public static Response resetPassword(ServerRequest request) {

        Response rep = hasAccess(SYS_ADMIN_ROLE);
        if (!rep.isOk()) return rep;

        String emailTo = request.getParam(SENDTO_EMAIL);
        String userEmail = request.getParam(EMAIL);

        try {
            if (!SsoDao.getInstance().isUser(userEmail)) {
                return new Response(1, null, userEmail + ": Is not a user in the system.");
            }

            String newPassword = RandomStringUtils.randomAlphanumeric(8);  // generate random password
            SsoDao.getInstance().updateUserPassword(userEmail, newPassword);
            rep = new Response(0, null, userEmail + ": Password successfully reset.");
            if (!StringUtils.isEmpty(emailTo)) {
                String ssoBaseUrl = ServerContext.getRequestOwner().getBaseUrl();
                sendUserAddedEmail(ssoBaseUrl, emailTo, new UserInfo(emailTo, newPassword));
                rep.addMessage("New password sent to " + emailTo);
            }
            return rep;
        } catch (Exception e) {
            return new Response(1, null, userEmail + ": Unable to reset password.  --  " +  e.getMessage());
        }
    }

    /**
     * Params used:
     *      GEN_PASS, LOGIN_NAME, PASSWORD, CPASSWORD, EMAIL, FIRST_NAME, LAST_NAME, ADDRESS, CITY, COUNTRY, INSTITUTE, PHONE, POSTCODE
     * @param request
     * @return
     */
    public static Response<UserInfo> addUser(ServerRequest request) {

        UserInfo user =  null;
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
            if (!ur.isOk() || !pr.isOk()) return ur.combine(pr);

            if (SsoDao.getInstance().addUser(user)) {
                Response<UserInfo> rep = new Response<UserInfo>(0, user, user.getLoginName() + ":  User successfully added.");
                return rep;
            } else  {
                return new Response<UserInfo>(1, user, user.getLoginName() + ": Unable to add user.");
            }
        } catch (Exception e) {
            String name = user == null ? "null" : user.getLoginName();
            return new Response(1, null, name + ":  Unable to add user.  --  " +  e.getMessage());
        }
    }

    /**
     * Params used:
     *      EMAIL, MISSION_ID, MISSION_NAME, GROUP_ID, GROUP_NAME, PRIVILEGE
     * @param req
     * @return
     */
    public static Response addAccess(ServerRequest req) {
        UserRoleEntry ure = null;
        String email = req.getParam(EMAIL);
        try {
            RoleList.RoleEntry re = extraRoleEntry(req);
            ure = new UserRoleEntry(email, re);

            Response rep = hasAccess(ure.getRole().getMissionName() + "::ADMIN");
            if (!rep.isOk()) return rep;

            rep = doCheckAccess(ure);
            if (!rep.isOk()) return rep;

            if (SsoDao.getInstance().addAccess(ure)) {
                return new Response(0, null, email + ": Added to  " + ure.toString() + ".");
            } else {
                return new Response(-1, null, email + ": Is already a member of " + re + ".");
            }
        } catch (Exception e) {
            return new Response(1, null, email + ": Unable to add to " + ure + ".  --  " +  e.getMessage());
        }
    }

    /**
     * Params used:
     *      MISSION_ID, MISSION_NAME, GROUP_ID, GROUP_NAME, PRIVILEGE
     * @param req
     * @return
     */
    public static Response addRole(ServerRequest req) {
        try {
            RoleList.RoleEntry re = extraRoleEntry(req);
            Response rep = hasAccess(re.getMissionName() + "::ADMIN");
            if (!rep.isOk()) return rep;

            if (SsoDao.getInstance().roleExists(re)) {
                return new Response(-1, null, re + ": Is already in the system.");
            }

            if (SsoDao.getInstance().addRole(re)) {
                return new Response(0, null, re.toString() + ": Role successfully added.");
            } else {
                return new Response(-1, null, re.toString() + ": Unable to add role.");
            }
        } catch (Exception e) {
            return new Response(1, null, req.toString() + ": Unable to add role.  --  " +  e.getMessage());
        }
    }

    /**
     * Params used:
     *      LOGIN_NAME
     * @param request
     * @return
     */
    public static Response<UserInfo> removeUser(ServerRequest request) {
        UserInfo user = extractUserInfo(request);
        try {
            SsoDao.getInstance().removeUser(user.getLoginName());
            return new Response<UserInfo>(0, null, user.getLoginName() + ": User successfully removed.");
        } catch (Exception e) {
            return new Response<UserInfo>(1, null, user.getLoginName() + ": Unable to remove user.  --  " + e.getMessage());
        }
    }



    /**
     * a list of serialized role separated by comma.
     * Params used:
     *      ROLE_LIST
     * @param req
     * @return
     * @throws IOException
     * @throws DataAccessException
     */
    public static Response removeRole(ServerRequest req) {

        String rl = req.getParam(ROLE_LIST);
        try {
            String[] rlist = rl.split(",");
            Response res = new Response(-1, null);
            for (String r : rlist) {
                RoleList.RoleEntry re = RoleList.RoleEntry.parse(r);
                Response rep = hasAccess(re.getMissionName() + "::ADMIN");
                if (!rep.isOk()) return rep;

                try {
                    if (!SsoDao.getInstance().removeRole(re)) {
                        res.addMessage(r + ": Unable to remove role.");
                    }
                } catch (Exception e) {
                    res.addMessage(r + ": Unable to remove role.  --  " + e.getMessage());
                }
            }
            if (res.getMessage().length() > 0) {
                return res;
            } else {
                return new Response();
            }
        } catch (Exception e) {
            return new Response(1, null, rl + ": Unable to remove role(s).  --  " + e.getMessage());
        }
    }

    /**
     * a list of serialized access separated by comma.
     * Params used:
     *      ACCESS_LIST
     * @param req
     * @return
     */
    public static Response removeAccess(ServerRequest req) {

        String al = req.getParam(ACCESS_LIST);
        try {
            String[] alist = al.split(",");
            Response res = new Response(-1, null);
            for (String r : alist) {
                UserRoleEntry ure = null;
                try {
                    ure = UserRoleEntry.parse(r);
                    Response rep = hasAccess(ure.getRole().getMissionName() + "::ADMIN");
                    if (!rep.isOk()) return rep;

                    if (!SsoDao.getInstance().removeAccess(ure)) {
                        res.addMessage(ure + ": Unable to remove access.");
                    }
                } catch (Exception e) {
                    res.addMessage(ure + ": Unable to remove access.  --  " + e.getMessage());
                }
            }
            if (res.getMessage().length() > 0) {
                return res;
            } else {
                return new Response();
            }
        } catch (Exception e) {
            return new Response(1, null, al + ": Unable to remove access.  --  " + e.getMessage());
        }
    }

    /**
     * filter by a list of mission name separated by comma.  All accessible missions if MISSION_NAME is not given.
     * Params used:
     *      MISSION_NAME
     * @param req
     * @return
     */
    public static Response<DataGroup> showRoles(ServerRequest req) {

        try {
            String[] missions = null;
            String name = req.getParam(MISSION_NAME);
            if (!StringUtils.isEmpty(name)) {
                missions = name.split(",");
            }
            Response<String[]> res = getAdminMissions(missions);
            if (!res.isOk()) return new Response<DataGroup>(1, null, res.getMessage());

            DataGroup roles = SsoDao.getInstance().getRoles(res.getValue());
            return new Response<DataGroup>(0, roles);
        } catch (Exception e) {
            return new Response(1, null, "Unable to list roles.  --  " + e.getMessage());
        }
    }

    /**
     * filter by a list of mission name separated by comma.  All accessible missions if MISSION_NAME is not given.
     * if EMAIL is given, then only list access for the given user.
     * Params used:
     *      EMAIL, MISSION_NAME
     * @param req
     * @return
     */
    public static Response<DataGroup> showAccess(ServerRequest req) {

        try {
            String user = req.getParam(EMAIL);

            String[] missions = null;
            String name = req.getParam(MISSION_NAME);
            if (!StringUtils.isEmpty(name)) {
                missions = name.split(",");
            }
            Response<String[]> res = getAdminMissions(missions);
            if (!res.isOk()) return new Response<DataGroup>(1, null, res.getMessage());

            DataGroup access = SsoDao.getInstance().getAccess(user, res.getValue());
            return new Response<DataGroup>(0, access);
        } catch (Exception e) {
            return new Response(1, null, "Unable to show access.  --  " + e.getMessage());
        }
    }

    /**
     * Params used:
     *      MISSION_ID, MISSION_NAME, GROUP_ID, GROUP_NAME, PRIVILEGE
     * @param req
     * @return
     */
    public static Response<DataGroup> getUsersByRole(ServerRequest req) {

        RoleList.RoleEntry role = extraRoleEntry(req);
        try {
            Response rep = hasAccess(role.getMissionName() + "::ADMIN");
            if (!rep.isOk()) return rep;

            DataGroup users = SsoDao.getInstance().getUsersByRole(role);
            return new Response<DataGroup>(0, users);
        } catch (Exception e) {
            return new Response(1, null, role.toString() + ": Unable to list users by role.  --  " + e.getMessage());
        }

    }

    /**
     * Params used:
     *      none
     * @param req
     * @return
     */
    public static Response<DataGroup> showMissions(ServerRequest req) {

        try {
            Response res = hasAccess(SYS_ADMIN_ROLE);
            if (!res.isOk()) return res;

            DataGroup missions = SsoDao.getInstance().getMissionXRefs();
            return new Response<DataGroup>(0, missions);
        } catch (Exception e) {
            return new Response(1, null, "Unable to list missions.  --  " + e.getMessage());
        }

    }

    /**
     * list all users' login_name in the system
     * Params used:
     *      none
     * @param req
     * @return
     */
    public static Response<DataGroup> getUserIds(ServerRequest req) {

        try {
            // list users is no longer a sys-admin level operation.
//        Response res = hasAccess(SYS_ADMIN_ROLE);
//        if (!res.isOk()) return res;

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
            return new Response<DataGroup>(0, dg);
        } catch (Exception e) {
            return new Response(1, null, "Unable to list users' IDs.  --  " + e.getMessage());
        }

    }

    public static Response<DataGroup> showUsers(ServerRequest req) {
        return showUsers(req, false);
    }

    /**
     * Params used:
     *      LOGIN_NAME
     * @param req
     * @return
     */
    public static Response<DataGroup> showUsers(ServerRequest req, boolean brief) {

        try {
              // list users is no longer a sys-admin level operation.
//          Response res = hasAccess(SYS_ADMIN_ROLE);
//          if (!res.isOk()) return res;
            String name = req.getParam(LOGIN_NAME);

            DataGroup dg = SsoDao.getInstance().getUserInfo(name, brief);
            return new Response<DataGroup>(0, dg);
        } catch (Exception e) {
            return new Response(1, null, "Unable to list users.  --  " + e.getMessage());
        }
    }

    /**
     * Params used:
     *      MISSION_ID, MISSION_NAME
     * @param req
     * @return
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
                return new Response(0, null, fname + ": Mission successfully added.");
            } else {
                return new Response(-1, null, fname + ": Mission is already in the system.");
            }
        } catch (Exception e) {
            return new Response(1, null, fname + ": Unable to add mission.  --  " + e.getMessage());
        }

    }

    /**
     * does current login user has access to the given role.
     * @param role
     * @return
     */
    public static Response hasAccess(final String role) {
        UserInfo user = ServerContext.getRequestOwner().getUserInfo();
        try {
            if (StringUtils.isEmpty(role) || user.getRoles().hasAccess(role)) {
                return new Response();
            } else {
                String msg = user.getLoginName() + ": You are not allowed access to " + role + ".";
                return new Response(1, null, msg);
            }
        } catch (Exception e) {
            return new Response(1, null, user.getLoginName() + ": Unexpected error while checking for privilege.  --  " + e.getMessage());
        }

    }

    public static Response hasUpdateAccess(UserInfo user) {
        try {
            UserInfo cUser = ServerContext.getRequestOwner().getUserInfo();
            if (cUser == null) {
                return new Response(1, null, "Not logged in");
            } else if (user == null) {
                return new Response(1, null, "Requested user is null.");
            } else if (StringUtils.isEmpty(user.getLoginName())) {
                return new Response(1, null, "Requested user Login Name is missing");
            } else {
                if (!hasAccess(SYS_ADMIN_ROLE).isOk()) {
                    if (!cUser.getLoginName().equals(user.getLoginName())) {
                        return new Response(1, null, user.getLoginName() + ": You are not allow to update this user's information.");
                    }
                }
            }
        } catch (Exception e) {
            new Response(1, null, user.getLoginName() + "Unable to update user.  --  " + e.getMessage());
        }
        return new Response();
    }

//====================================================================
//
//====================================================================

    /**
     * return the missions this person have ADMIN access to.
     * return an empty list when this person has no access to any mission.
     * return null when this person has access to all of the missions.
     * @return
     * @throws edu.caltech.ipac.firefly.server.query.DataAccessException
     */
    private static Response<String[]> getAdminMissions(String... filterBy) {
        List<String> missions = new ArrayList<String>();
        UserInfo user = ServerContext.getRequestOwner().getUserInfo();
        RoleList rl = user.getRoles();
        for (RoleList.RoleEntry r : rl) {
            if (!StringUtils.isEmpty(r.getPrivilege()) && r.getPrivilege().equals("ADMIN")) {
                if (r.getMissionName().equals("ALL")) {
                    // null mean all
                    return new Response<String[]>(0, filterBy);
                }
                if (filterBy == null || Arrays.binarySearch(filterBy, r.getMissionName()) >= 0) {
                    missions.add(r.getMissionName());
                }
            }
        }
        String[] vals = missions.toArray(new String[missions.size()]);
        if (vals.length == 0) {
            String msg = filterBy == null ? user.getLoginName() + ": You are not an admin of any project." : user.getLoginName() + ": You are not authorize to access project " + CollectionUtil.toString(filterBy) + ".";
            return new Response<String[]>(1, vals, msg);

        } else {
            return new Response<String[]>(0, vals);
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

        if(request.containsParam(LOGIN_NAME)) {
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


    private static Response doUserIdCheck(String loginName, boolean failOnDup) {
        try {
            if (StringUtils.isEmpty(loginName)) {
                return new Response(1, null, "Login name is missing.");
            } else if (loginName.length() < 8) {
                    return new Response(1, null, loginName + ": Login name must be at least 8 characters long.");
            } else {
                if (SsoDao.getInstance().isUser(loginName)) {
                    return new Response(failOnDup ? 1 : -1, null, loginName + ": A user with this login name already exists.");
                }
            }
            return new Response();
        } catch (Exception e) {
            return new Response(1, null, loginName + ":  Unable to validate user.  --  " + e.getMessage());
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
        if (msg != null) {
            msg = userid + ": " + msg;
        }
        return new Response(msg == null ? 0 : 1, null, msg);
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
        if (msg != null) {
            msg = userid + ": " + msg;
        }
        return new Response(msg == null ? 0 : 1, null, msg);
    }

    private static Response doCheckAccess(UserRoleEntry ure) {

        if (!SsoDao.getInstance().isUser(ure.getLoginName())) {
            return new Response(1, null, ure.getLoginName() + ": Is not a registered user.");
        }

        try {
            RoleList.RoleEntry role = SsoDao.getInstance().findRole(ure.getRole());
            if (role == null) {
                return new Response(1, null, ure.getRole() + ": Is not in the system.");
            }
        } catch (DataAccessException e) {
            return new Response(1, null, ure + ": Unable to validate access. --  " + e.getMessage());
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
