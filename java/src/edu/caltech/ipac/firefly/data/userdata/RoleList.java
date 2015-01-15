/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.userdata;

import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Date: Apr 2, 2010
 *
 * @author loi
 * @version $Id: RoleList.java,v 1.10 2012/10/03 22:18:11 loi Exp $
 */
public class RoleList extends ArrayList<RoleList.RoleEntry> implements Serializable {
    public static final String ALL = "ALL";
    private static final String WITH_ID_STR = ".*\\(-?[0-9]+\\)";

    public boolean hasAccess(String mission, String group) {
        return hasAccess(mission, group, null);
    }

    public boolean hasAccess(String mission, String group, String privilege) {
        for (RoleEntry re : this) {
            if (re.hasAccess(mission, group, privilege)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return true if this list contains a role that hasAccess to any one of the
     * given role strings.
     * @param roleStr
     * @return
     */
    public boolean hasAccess(String... roleStr) {
        for (String s : roleStr) {
            RoleEntry re = RoleEntry.parse(s);
            if (re != null) {
                for (RoleEntry r : this) {
                    if (r.hasAccess(re)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * RoleEntry has 3 parts; mission, group, and privilege
     * Each part may optionally contain an ID.
     * This class provide toString() and parse for serialize/deserialize to/from string.
     * The string format is written as mission[mission-id]:group[group-id]:privilege[privilege[id]
     */
    public static class RoleEntry implements Serializable {
        private static final String KW_SEP = ":";
        transient int roleId = -1;
        String missionName;
        String groupName;
        String privilege = "";
        int missionId = -1;
        int groupId = -1;

        public RoleEntry() {
        }

        public RoleEntry(String missionName, int missionId, String groupName, int groupId, String privilege) {
            this.missionName = missionName;
            this.missionId = missionId;
            this.groupName = groupName;
            this.groupId = groupId;
            setPrivilege(privilege);
        }

        public boolean hasAccess(RoleEntry re) {
            
            if (StringUtils.isEmpty(re.getMissionName())) {
                return hasAccess(re.getMissionId(), re.getGroupId(), re.getPrivilege());
            } else {
                if (StringUtils.isEmpty(re.getGroupName())) {
                    return hasAccess(re.getMissionName(), re.getGroupId(), re.getPrivilege());
                } else {
                    return hasAccess(re.getMissionName(), re.getGroupName(), re.getPrivilege());
                }
            }
        }

        public boolean hasAccess(String missionName, int groupId, String privilege) {
            return isMatch(this.missionName, missionName) &&
                   isMatch(this.groupName, this.groupId, groupId) &&
                   isMatch(this.privilege, privilege);
        }

        public boolean hasAccess(int missionId, int groupId, String privilege) {
            return isMatch(this.missionName, this.missionId, missionId) &&
                   isMatch(this.groupName, this.groupId, groupId) &&
                   isMatch(this.privilege, privilege);
        }

        public boolean hasAccess(String missionName, String groupName, String privilege) {
            return isMatch(this.missionName, missionName) &&
                   isMatch(this.groupName, groupName) &&
                   isMatch(this.privilege, privilege);
        }

        private boolean isMatch(String matchName, int matchId, int value) {
            if (value < 0) {
                return true;
            }
            return matchName.equals(ALL) || matchId == value;
        }

        private boolean isMatch(String matchStr, String value) {
            if (StringUtils.isEmpty(value)) {
                return true;
            }
            return matchStr.equals(ALL) || matchStr.equals(value );
        }

        public int getRoleId() {
            return roleId;
        }

        public void setRoleId(int roleId) {
            this.roleId = roleId;
        }

        public void setMissionName(String missionName) {
            this.missionName = missionName;
        }

        public void setMissionId(int missionId) {
            this.missionId = missionId;
        }

        public void setGroupId(int groupId) {
            this.groupId = groupId;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public void setPrivilege(String privilege) {
            this.privilege = privilege == null ? "" : privilege;
        }

        public String getGroupName() {
            return groupName;
        }

        public String getPrivilege() {
            return privilege;
        }

        public String getMissionName() {
            return missionName;
        }

        public int getMissionId() {
            return missionId;
        }

        public int getGroupId() {
            return groupId;
        }

        public static RoleEntry parse(String role) {
            if (!StringUtils.isEmpty(role)) {
                String[] parts = role.split(KW_SEP, 3);
                String mission = parts[0];
                String group = parts.length < 2 ? "" : parts[1];
                String privilege = parts.length < 3 ? "" : parts[2];
                int missionId = -1;
                int groupId = -1;

                if (mission.matches(WITH_ID_STR)) {
                    missionId = getId(mission);
                    mission = getName(mission);
                }
                if (group.matches(WITH_ID_STR)) {
                    groupId = getId(group);
                    group = getName(group);
                }
                return new RoleEntry(mission, missionId, group, groupId, privilege);
            }
            return null;
        }
        
        private static int getId(String s) {
            String[] parts = s.split("\\(", 2);
            if (parts.length == 2) {
                return Integer.parseInt(parts[1].substring(0, parts[1].indexOf(")")));
            }
            return 1;
            
        }

        private static String getName(String s) {
            String[] parts = s.split("\\(", 2);
            return parts[0];
        }

        @Override
        public String toString() {
            String missionPart = missionName + (missionId != -1 ? "(" + missionId + ")" : "");
            String groupPart = groupName + (groupId != -1 ? "(" + groupId + ")" : "");
            String privPart = privilege;

            return missionPart + KW_SEP + groupPart + KW_SEP + privPart;
        }

        @Override
        public boolean equals(Object o) {
            return toString().equals(String.valueOf(o));
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        /**
         * return true if this RoleInfo contains enough information to construct a valid role
         * @return
         */
        public boolean isValid() {

            if (StringUtils.isEmpty(missionName)) {
                return false;
            }
//            return isValid(missionName, missionId) && isValid(groupName, groupId) && isValid(privilege, privilegeId);
            return isValid(missionName, missionId) && isValid(groupName, groupId);
        }

        /**
         * return true if both are empty or both are populated.
         * @param name
         * @param id
         * @return
         */
        private boolean isValid(String name, int id) {
             
            if (StringUtils.isEmpty(name) ) {
                return id == -1;
            } else {
                if (name.equals(ALL)) {
                    return id == -99;
                } else {
                    return  id > 0;
                }
            }
        }

    }
    
}
