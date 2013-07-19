package edu.caltech.ipac.uman.server.persistence;

import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.spring.mapper.DataGroupUtil;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.uman.data.UserRoleEntry;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Date: 3/13/12
 *
 * @author loi
 * @version $Id: SsoDao.java,v 1.13 2012/12/03 22:15:07 loi Exp $
 */
public class SsoDao {
    
    public static final int UNKNOW_INT = Integer.MIN_VALUE;

    private static SsoDao instance;
    final SimpleJdbcTemplate jdbcTemplate = JdbcFactory.getSimpleTemplate(DbInstance.josso);
    private static Logger.LoggerImpl logger = Logger.getLogger();

    public SsoDao() {

    }

    public static SsoDao getInstance() {
        if (instance == null) instance = new SsoDao();
        return instance;
    }

    public boolean addUser(final UserInfo user) throws DataAccessException {

        final JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.josso);

        final Ref<Boolean> isAdded = new Ref<Boolean>(false);
        final TransactionTemplate txTemplate = JdbcFactory.getTransactionTemplate(jdbcTemplate.getDataSource());
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    if (!StringUtils.isEmpty(user.getLoginName())) {

                        if (!isUser(user.getLoginName())) {
                            String sql = "insert into sso_users (pass, login_name) values (?, ?)";
                            Object[] args = new Object[]{SsoDao.getMD5Hash(user.getPassword()), getStr(user.getLoginName())};
                            logger.briefDebug("SsoDao:addUser sql:" + sql + "  args:" + StringUtils.toString(args, ","));
                            isAdded.setSource(jdbcTemplate.update(sql, args) > 0);
                            sql = "select LAST_INSERT_ID()";
                            int userId = jdbcTemplate.queryForInt(sql);
                            user.setUserId(userId);

                            // update user's info
                            updateUserProperty(user);
                            try {
                                // add guest role
                                addAccess(new UserRoleEntry(user.getLoginName(), "USER", -1, null, -1, null));
                            } catch (DataAccessException e) {
                                logger.error(e);
                            }
                        } else {
                            throw new IllegalArgumentException("Fail to add user:" + user.getLoginName() + ".  User already exists.");
                        }
                    } else {
                        throw new IllegalArgumentException("Missing login_name parameter");
                    }
                } catch (RuntimeException e) {
                    status.setRollbackOnly();
                    if (isAdded.getSource()) {
                        try {
                            removeUser(user.getLoginName());
                        } catch (DataAccessException e1) {
                            // ignore during rollback.
                        }
                        isAdded.setSource(false);
                    }
                    throw e;
                }
            }

            
        });
        return isAdded.getSource();
    }

    /**
     *
     * @param oldEmail
     * @param newEmail
     * @return
     * @throws DataAccessException
     */
    public boolean updateUserEmail(String oldEmail, String newEmail) throws DataAccessException {

        int cUserId = getUserId(oldEmail);
        if (cUserId < 0) {
            throw new IllegalArgumentException("User does not exists:" + oldEmail);
        }

        if (isUser(newEmail)) {
            throw new IllegalArgumentException("A user with the email you specified already exists:" + newEmail);
        }

        String sql = "update sso_users set login_name = ? where user_id = ?";
        logger.briefDebug("SsoDao:updateUserEmail:" + sql + "  args:" + newEmail + ", " + cUserId);
        boolean isChanged =  jdbcTemplate.update(sql, newEmail, cUserId) > 0;

        return  isChanged && updateUserProperty(cUserId, "email", newEmail);
    }

    /**
     *
     * @param loginName
     * @param newPassword
     * @return
     * @throws DataAccessException
     */
    public boolean updateUserPassword(String loginName, String newPassword) throws DataAccessException {

        int cUserId = getUserId(loginName);
        if (cUserId < 0) {
            throw new IllegalArgumentException("User does not exists:" + loginName);
        }

        String sql = "update sso_users set pass = ? where user_id = ?";
        logger.briefDebug("SsoDao:updateUserPassword:" + sql + "  args:" + newPassword + ", " + cUserId);
        return jdbcTemplate.update(sql, getMD5Hash(newPassword), cUserId) > 0;
    }

    /**
     * update the current one.
     * @param user
     * @throws DataAccessException
     */
    public boolean updateUser(final UserInfo user) throws DataAccessException {

        final JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.josso);
        

        final TransactionTemplate txTemplate = JdbcFactory.getTransactionTemplate(jdbcTemplate.getDataSource());
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    if (user.getUserId() < 0) {
                        user.setUserId(getUserId(user.getLoginName()));
                    }

                    if (user.getUserId() < 0) {
                        throw new IllegalArgumentException("This user: " + user.getLoginName() + " does not exists.");
                    }

                    // change user password
                    if (!StringUtils.isEmpty(user.getPassword())) {
                        String sql = "update sso_users set pass = ? where user_id = ?";
                        Object[] args = new Object[]{getMD5Hash(user.getPassword()), user.getUserId()};
                        logger.briefDebug("SsoDao:updatePassword sql:" + sql + "  userId:" + user.getUserId() + "  passwd:" + user.getPassword());
                        jdbcTemplate.update(sql, args);
                    }

                    // update user's info
                    updateUserProperty(user);
                } catch (RuntimeException e) {
                    status.setRollbackOnly();
                    throw e;
                }
            }
        });
        return true;
    }

    public int getUserId(String loginName) {
        if (loginName == null) return -1;

        String sql = "select user_id from sso_users where login_name = ?";
        Object[] args = new Object[]{loginName};
        logger.briefDebug("SsoDao:getUserId sql:" + sql + "  args:" + StringUtils.toString(args, ","));
        try {
            return jdbcTemplate.queryForInt(sql, args);
        } catch (Exception ex) {
            return -1;
        }
    }

    public DataGroup getRoles(String... mission) {

        List args = new ArrayList();
        String sql = "select mission_name, mission_id, group_name, group_id, privilege from sso_roles";
        if (mission != null) {
            String inStr = "";
            for (int i = 0; i < mission.length; i++) {
                if (!StringUtils.isEmpty(mission[i])) {
                    inStr += "?" + (i<mission.length-1 ? "," : "");
                    args.add(mission[i]);
                }
            }
            if (args.size() > 0) {
                sql = sql + " where mission_name in (" + inStr + ")";
            }
        }
        sql = sql + " order by mission_name, group_id";

        logger.briefDebug("SsoDao:getRoles sql:" + sql);
        try {
            return jdbcTemplate.queryForObject(sql, new ParameterizedRowMapper<DataGroup>() {
                public DataGroup mapRow(ResultSet rs, int i) throws SQLException {
                    return DataGroupUtil.processResults(rs, null, -1);
                }
            }, args.toArray(new Object[args.size()]));
        } catch (Exception ex) {
            System.out.println("SQL:" + sql);
            ex.printStackTrace();
            return null;
        }
    }

    public DataGroup getAccess(String user, String... mission) {
        List args = new ArrayList();
        String sql = "select login_name, mission_name, mission_id, group_name, group_id, privilege " +
                "from sso_roles r, sso_users u, sso_user_roles ur where " +
                "r.role_id = ur.role_id and u.user_id = ur.user_id";
        if (!StringUtils.isEmpty(user)) {
            sql = sql + " and u.login_name = '?'";
            args.add(user);
        }
        if (mission != null) {
            String inStr = "";
            for (int i = 0; i < mission.length; i++) {
                if (!StringUtils.isEmpty(mission[i])) {
                    inStr += "?" + (i<mission.length-1 ? "," : "");
                    args.add(mission[i]);
                }
            }
            if (args.size() > 0) {
                sql = sql + " and r.mission_name in (" + inStr + ")";
            }
        }
        sql = sql + " order by login_name, group_id";
        logger.briefDebug("SsoDao:getUserRoles sql:" + sql);
        try {
            return jdbcTemplate.queryForObject(sql, new ParameterizedRowMapper<DataGroup>() {
                public DataGroup mapRow(ResultSet rs, int i) throws SQLException {
                    return DataGroupUtil.processResults(rs, null, -1);
                }
            }, args.toArray(new Object[args.size()]));
        } catch (Exception ex) {
            logger.briefDebug("SQL:" + sql);
            ex.printStackTrace();
            return null;
        }
    }

    public DataGroup getUsersByRole(RoleList.RoleEntry role) throws DataAccessException {
        RoleList.RoleEntry re = findRole(role);
        List args = new ArrayList();
        String sql = "select u.login_name " +
                "from sso_users u, sso_user_roles ur where " +
                "u.user_id = ur.user_id";
        if (re != null && re.getRoleId() > 0) {
            sql = sql + " and ur.role_id = ?";
            args.add(re.getRoleId());
        }
        sql = sql + " order by login_name";
        logger.briefDebug("SsoDao:getAccessByRole sql:" + sql);
        try {
            return jdbcTemplate.queryForObject(sql, new ParameterizedRowMapper<DataGroup>() {
                public DataGroup mapRow(ResultSet rs, int i) throws SQLException {
                    return DataGroupUtil.processResults(rs, null, -1);
                }
            }, args.toArray(new Object[args.size()]));
        } catch (Exception ex) {
            logger.briefDebug("SQL:" + sql);
            ex.printStackTrace();
            return null;
        }
    }

    public DataGroup getUserInfo(String user) {
        return getUserInfo(user, false);
    }

    public DataGroup getUserInfo(String user, boolean isBrief) {
        String sql = "select distinct pname from sso_user_props";
        List<Map<String, Object>> names = jdbcTemplate.queryForList(sql);
        
        List<DataType> types = new ArrayList<DataType>();
        types.add(new DataType("first_name", String.class));
        types.add(new DataType("last_name", String.class));
        types.add(new DataType("institute", String.class));
        types.add(new DataType("login_name", String.class));
        if (!isBrief) {
            types.add(new DataType("email", String.class));
            types.add(new DataType("address1", String.class));
            types.add(new DataType("city", String.class));
            types.add(new DataType("postcode", String.class));
            types.add(new DataType("country", String.class));
            types.add(new DataType("phone_number", String.class));
        }

        final DataGroup dg = new DataGroup("users", types);

        sql = "select login_name, pname, pvalue from sso_users u, sso_user_props up where u.user_id = up.user_id";
        if (!StringUtils.isEmpty(user)) {
            sql = sql + " and login_name = '" + user + "'";
        }
        sql = sql + " order by login_name";

        logger.briefDebug("SsoDao:getUserInfo sql:" + sql);
        try {
            jdbcTemplate.queryForObject(sql, new ParameterizedRowMapper<DataGroup>() {
                public DataGroup mapRow(ResultSet rs, int i) throws SQLException {
                    DataObject row = null;
                    String cUser = "";
                    do {
                        String user = rs.getString("login_name");
                        if (!cUser.equals(user)) {
                            row = new DataObject(dg);
                            row.setDataElement(row.getDataType("login_name"), user);
                            dg.add(row);
                            cUser = user;
                        }
                        DataType cname = row.getDataType(rs.getString("pname"));
                        if (cname != null && dg.containsKey(cname.getKeyName())) {
                            row.setDataElement(cname, rs.getString("pvalue"));
                        }
                    } while (rs.next());
                    return dg;
                }
            });
        } catch (Exception ex) {
            return null;
        }
        dg.shrinkToFitData();
        return dg;
    }

    private void updateUserProperty(UserInfo user) {

        if (user.getUserId() < 0) {
            return;
        }

        String delSql = "delete from sso_user_props where user_id = ?";
        int c = jdbcTemplate.update(delSql, user.getUserId());

        String sql = "insert into sso_user_props (user_id, pname, pvalue) values (?, ?, ?)";
        
        List<Object[]> args = new ArrayList();
        args.add(new Object[]{user.getUserId(), UserInfo.EMAIL, getStr(user.getEmail())});
        args.add(new Object[]{user.getUserId(), UserInfo.FIRSTNAME, getStr(user.getFirstName())});
        args.add(new Object[]{user.getUserId(), UserInfo.LASTNAME, getStr(user.getLastName())});
        args.add(new Object[]{user.getUserId(), UserInfo.ADDRESS, getStr(user.getAddress())});
        args.add(new Object[]{user.getUserId(), UserInfo.CITY, getStr(user.getCity())});
        args.add(new Object[]{user.getUserId(), UserInfo.COUNTRY, getStr(user.getCountry())});
        args.add(new Object[]{user.getUserId(), UserInfo.POSTCODE, getStr(user.getPostcode())});
        args.add(new Object[]{user.getUserId(), UserInfo.PHONE, getStr(user.getPhone())});
        args.add(new Object[]{user.getUserId(), UserInfo.INSTITUTE, getStr(user.getInstitute())});

        jdbcTemplate.batchUpdate(sql, args);
        logger.briefDebug("SsoDao:updateUserProperty sql:" + sql + "  args:" + user);
    }

    private boolean updateUserProperty(int userId, String name, String value) {
        value = value == null ? "" : value;
        String sql;
        String countSql = "select count(*) from sso_user_props where user_id = ? and pname = ?";
        int count = jdbcTemplate.queryForInt(countSql, new Object[]{userId, getStr(name)});
        if (count > 0) {
            sql = "update sso_user_props set pvalue = ? where user_id = ? and pname = ?";
        } else {
            sql = "insert into sso_user_props (pvalue, user_id, pname) values (?, ?, ?)";
        }
        Object[] args = new Object[]{getStr(value), userId, getStr(name)};
        logger.briefDebug("SsoDao:updateUserProperty sql:" + countSql + "  args:" + StringUtils.toString(args, ","));
        return jdbcTemplate.update(sql, args) > 0;
    }

    public List<String> getUserIDs() {
        String sql = "select login_name from sso_users order by 1";
        logger.briefDebug("SsoDao:getUserIDs sql:" + sql);

        return jdbcTemplate.queryForObject(sql, new ParameterizedRowMapper<List<String>>() {
            public List<String> mapRow(ResultSet rs, int i) throws SQLException {
                List<String> users = new ArrayList<String>();
                do {
                    users.add(rs.getString("login_name"));
                } while (rs.next());
                return users;
            }
        });
    }

    public UserInfo getUser(String loginName) {
        String sql = "select u.user_id, u.login_name, pname, pvalue from sso_users u, sso_user_props up where u.user_id = up.user_id and u.login_name = ?";
        Object[] args = new Object[]{loginName};
        logger.briefDebug("SsoDao:getUser sql:" + sql + "  args:" + StringUtils.toString(args, ","));

        return jdbcTemplate.queryForObject(sql, new ParameterizedRowMapper<UserInfo>() {
                    public UserInfo mapRow(ResultSet rs, int i) throws SQLException {
                        UserInfo user = new UserInfo();
                        do {
                            user.setUserId(rs.getInt(1));
                            user.setLoginName(rs.getString(2));
                            user.setProperty(rs.getString(3), rs.getString(4));
                        } while (rs.next());
                        return user;
                        }
                }, loginName);
    }

    public boolean isUser(String loginName) {
        if (StringUtils.isEmpty(loginName)) return false;

        String countSql = "select count(*) from sso_users where login_name = ?";
        return jdbcTemplate.queryForInt(countSql, loginName) > 0;
    }

    public int getMissionID(String missionName) {
        try {
            String sql = "select mission_id from sso_mission_xref where mission_name = ?";
            logger.briefDebug("SsoDao:getMissionID sql:" + sql + "  args:" + missionName);
            int id = jdbcTemplate.queryForInt(sql, getStr(missionName));
            return id == 0 ? UNKNOW_INT : id;
        } catch (Exception e) {
            return UNKNOW_INT;
        }
    }

    public String getMissionName(int missionId) {
        try {
            String sql = "select mission_name from sso_mission_xref where mission_id = ?";
            logger.briefDebug("SsoDao:getMissionName sql:" + sql + "  args:" + missionId);
            Map<String, Object> values= jdbcTemplate.queryForMap(sql, missionId);
            if (values != null && values.containsKey("mission_name")) {
                return (String) values.get("mission_name");
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    public int getNextMissionID() {
        try {
            String sql = "select max(mission_id) from sso_mission_xref";
            logger.briefDebug("SsoDao:getNextMissionID sql:" + sql);
            return Math.max(0, jdbcTemplate.queryForInt(sql)) + 1;
        } catch (Exception e) {
            return UNKNOW_INT;
        }

    }

    public DataGroup getMissionXRefs() {
        String sql = "select mission_id, mission_name from sso_mission_xref";
        try {
            return jdbcTemplate.queryForObject(sql, new ParameterizedRowMapper<DataGroup>() {
                        public DataGroup mapRow(ResultSet rs, int i) throws SQLException {
                            return DataGroupUtil.processResults(rs, null, -1);
                                }
                    });
        } catch (Exception ex) {
            logger.briefDebug("SQL:" + sql);
            ex.printStackTrace();
            return null;
        }
    }

    public boolean addMissionXRef(int id, String name)  throws DataAccessException {
        int xid = getMissionID(name);
        if (xid != UNKNOW_INT) {
            throw new DataAccessException(name + " already exists in the system.");
        }
        String sql = "insert into sso_mission_xref values (?, ?)";
        try {
            return jdbcTemplate.update(sql, id, name) > 0;
        } catch (Exception ex) {
            logger.briefDebug("SQL:" + sql + "   args:" + id + " " + name);
            ex.printStackTrace();
            return false;
        }
    }

    public String getGroupName(String missionName, int groupId) throws DataAccessException {
        try {
            String sql = "select distinct group_name from sso_roles where mission_name = ? and group_id = ?";
            logger.briefDebug("SsoDao:getGroupName sql:" + sql + "  args:" + missionName + "," + groupId);
            Map<String, Object> values = jdbcTemplate.queryForMap(sql, getStr(missionName), groupId);
            if (values != null && values.containsKey("group_name")) {
                return (String) values.get("group_name");
            }
        } catch (Exception e) { }
        return null;
    }

    public int getGroupID(String missionName, String groupName) throws DataAccessException {
        try {
            String sql = "select distinct group_id from sso_roles where mission_name = ? and group_name = ?";
            logger.briefDebug("SsoDao:getGroupID sql:" + sql + "  args:" + missionName + "," + groupName);
            int id = jdbcTemplate.queryForInt(sql, getStr(missionName), getStr(groupName));
            return id == 0 ? UNKNOW_INT : id;
        } catch (Exception e) {
            return UNKNOW_INT;
        }
    }

    public int getNextGroupID(String missionName) {
        try {
            String sql = "select max(group_id) from sso_roles where mission_name = ?";
            logger.briefDebug("SsoDao:getNextGroupID sql:" + sql + "  args:" + missionName);
            return Math.max(0, jdbcTemplate.queryForInt(sql, getStr(missionName))) + 1;
        } catch (Exception e) {
            return UNKNOW_INT;
        }
    }


    /**
     * Add a new role if one does not exists
     * @param role
     * @return true if a new role is added
     * @throws DataAccessException
     */
    public boolean addRole(RoleList.RoleEntry role) throws DataAccessException {

        if (!role.isValid()) {
            throw new DataAccessException("Bad role definition:" + role);
        }

        if (!isMission(role.getMissionName(), role.getMissionId())) {
            throw new DataAccessException("This mission is not in the system:" + role.getMissionName() + "(" + role.getMissionId() + ")");
        }
        
        try {
            String sql = "insert into sso_roles (mission_name, mission_id, group_name, group_id, privilege) values (?, ?, ?, ?, ?)";

            Object[] args = new Object[]{
                    getStr(role.getMissionName()),
                    role.getMissionId(),
                    getStr(role.getGroupName()),
                    role.getGroupId(),
                    getStr(role.getPrivilege())};
            logger.briefDebug("SsoDao:addRole sql:" + sql + "  args:" + StringUtils.toString(args, ","));
            return jdbcTemplate.update(sql, args) > 0;
        } catch (Exception e) {
            if (e.getMessage().contains("Duplicate")) {
                throw new DataAccessException(role + " already exists in the system.  Add request ignored.");
            }
        }
        return false;
    }

    private String getStr(String s) {
        return StringUtils.isEmpty(s) ? "" : s.trim();
    }

    private boolean isMission(String missionName, int missionId) {
        try {
            String sql = "select count(*) from sso_mission_xref where mission_name = ? and mission_id = ?";
            logger.briefDebug("SsoDao:isMission sql:" + sql + "  args:" + missionName + "," + missionId);
            return jdbcTemplate.queryForInt(sql, getStr(missionName), missionId) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean roleExists(RoleList.RoleEntry role) throws DataAccessException {
        String sql = "select count(*) from sso_roles where";
            sql += " (mission_name = '" + role.getMissionName() + "'";
            sql += " and group_name = '" + role.getGroupName() + "'";
            sql += " and privilege = '" + role.getPrivilege() + "') or";
            sql += " (mission_id = '" + role.getMissionId() + "'";
            sql += " and group_id = '" + role.getGroupId() + "'";
            sql += " and privilege = '" + role.getPrivilege() + "')";
//            sql += " and mission_id = " + role.getMissionId();
//            sql += " and group_id = " + role.getGroupId();

        logger.briefDebug("SsoDao:roleExists sql:" + sql);

        return jdbcTemplate.queryForInt(sql) > 0;
    }


    public RoleList.RoleEntry findRole(RoleList.RoleEntry role) throws DataAccessException {
        String sql = "select * from sso_roles where  mission_name = ? and group_name = ? and privilege = ? and mission_id = ? and group_id = ?";

        Object[] args = {
                        getStr(role.getMissionName()),
                        getStr(role.getGroupName()),
                        getStr(role.getPrivilege()),
                        role.getMissionId(),
                        role.getGroupId()
                };

        logger.briefDebug("SsoDao:findRole sql:" + sql);
        try{
            return jdbcTemplate.queryForObject(sql, new ParameterizedRowMapper<RoleList.RoleEntry>() {
                                public RoleList.RoleEntry mapRow(ResultSet rs, int i) throws SQLException {
                                    String mname = rs.getString("mission_name");
                                    String gname = rs.getString("group_name");
                                    String access = rs.getString("privilege");
                                    int mid = rs.getInt("mission_id");
                                    int gid = rs.getInt("group_id");
                                    mid = mid == 0 ? -1 : mid;
                                    gid = gid == 0 ? -1 : gid;
                                    RoleList.RoleEntry role = new RoleList.RoleEntry(mname, mid, gname, gid, access);
                                    role.setRoleId(rs.getInt("role_id"));
                                    return role;
                                }
                            }, args);
        } catch (Exception ex) {
            return null;
        }
    }


    /**
     * add a role to user based on the given info
     * @param roleMapping
     * @return true if one is added, else return false
     * @throws DataAccessException
     */
    public boolean addAccess(UserRoleEntry roleMapping) throws DataAccessException {

        RoleList.RoleEntry role = findRole(roleMapping.getRole());
        UserInfo user = getUser(roleMapping.getLoginName());
        if (role != null && user != null) {
            if (!isUserARole(user.getUserId(), role)) {
                String sql = "insert into sso_user_roles (user_id, role_id) values (?, ?)";

                Object[] args = new Object[]{user.getUserId(), role.getRoleId()};
                logger.briefDebug("SsoDao:addRoleMapping sql:" + sql + "  args:" + StringUtils.toString(args, ","));
                return jdbcTemplate.update(sql, args) > 0;
            } else {
                throw new DataAccessException(user.getLoginName() + " is already a member of " + role);
            }
        }
        return false;
    }

    public void removeUser(String loginName)  throws DataAccessException {
        int userId = getUserId(loginName);
        if (userId > 0) {
            String sql = "delete from sso_user_props where user_id = ?";
            logger.briefDebug("SsoDao:addUser removeUser:" + sql + "  args:" + userId);
            jdbcTemplate.update(sql, userId);

            sql = "delete from sso_user_roles where user_id = ?";
            logger.briefDebug("SsoDao:addUser removeUser:" + sql + "  args:" + userId);
            jdbcTemplate.update(sql, userId);

            sql = "delete from sso_users where user_id = ?";
            logger.briefDebug("SsoDao:addUser removeUser:" + sql + "  args:" + userId);
            jdbcTemplate.update(sql, userId);
        } else {
            throw new DataAccessException(loginName + " is NOT a valid user.");
        }
    }

    public boolean removeRole(RoleList.RoleEntry roleEntry) throws DataAccessException {
        RoleList.RoleEntry role = findRole(roleEntry);
        int c = 0;
        if (role != null) {
            String sql = "delete from sso_user_roles where role_id = ?";
            logger.briefDebug("SsoDao:removeRole:" + sql + "  args:" + role.getRoleId());
            c = jdbcTemplate.update(sql, role.getRoleId());

            sql = "delete from sso_roles where role_id = ?";
            logger.briefDebug("SsoDao:removeRole:" + sql + "  args:" + role.getRoleId());
            c += jdbcTemplate.update(sql, role.getRoleId());
        } else {
            throw new DataAccessException(roleEntry + " is NOT a valid role.");
        }
        return c > 0;
    }

    public boolean removeAccess(UserRoleEntry roleMapping) throws DataAccessException {

        RoleList.RoleEntry role = findRole(roleMapping.getRole());
        UserInfo user = getUser(roleMapping.getLoginName());
        if (role != null && user != null) {
            if (isUserARole(user.getUserId(), role)) {
                String sql = "delete from sso_user_roles where user_id=? and role_id=?";

                Object[] args = new Object[]{user.getUserId(), role.getRoleId()};
                logger.briefDebug("SsoDao:removeRoleMapping sql:" + sql + "  args:" + StringUtils.toString(args, ","));
                return jdbcTemplate.update(sql, args) > 0;
            } else {
                logger.briefDebug("removeAccess: " + user.getLoginName() + " is NOT a member of " + role);
                throw new DataAccessException(user.getLoginName() + " is NOT a member of " + role);
            }
        }
        return false;
    }

    private boolean isUserARole(int userId, RoleList.RoleEntry role) throws DataAccessException {
        if (role == null) return false;
        
        if (role.getRoleId() <= 0) {
            role = findRole(role);
        }
        
        if (role != null && userId > 0) {
            String sql = "select count(*) from sso_user_roles where user_id = ? and role_id = ?";

            Object[] args = new Object[]{userId, role.getRoleId()};
            logger.briefDebug("SsoDao:isRoleMappingExist sql:" + sql + "  args:" + StringUtils.toString(args, ","));
            return jdbcTemplate.queryForInt(sql, args) > 0;
        }
        return false;
    }


    public static String getMD5Hash(String data) {
        return DigestUtils.md5Hex(data);
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
