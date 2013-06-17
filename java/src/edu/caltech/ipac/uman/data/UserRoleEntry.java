package edu.caltech.ipac.uman.data;

import edu.caltech.ipac.firefly.data.userdata.RoleList;

/**
 * Date: 3/13/12
 * string format:  email ==> role
 * @author loi
 * @version $Id: UserRoleEntry.java,v 1.2 2012/11/16 01:16:56 loi Exp $
 */
public class UserRoleEntry {

    private String loginName;
    private RoleList.RoleEntry role;

    public UserRoleEntry(String email, RoleList.RoleEntry role) {
        this.loginName = email;
        this.role = role;
    }

    public UserRoleEntry(String email, String missionName, int missionId, String groupName, int groupId, String accessPriv) {
        this( email, new RoleList.RoleEntry(missionName, missionId, groupName,
                groupId, accessPriv) );
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public RoleList.RoleEntry getRole() {
        return role;
    }

    public void setRole(RoleList.RoleEntry role) {
        this.role = role;
    }

    public static UserRoleEntry parse(String s) {
        String[] parts = s.split("==>", 2);
        if (parts.length == 2) {
            RoleList.RoleEntry re = RoleList.RoleEntry.parse(parts[1].trim());
            return new UserRoleEntry(parts[0].trim(), re);
        }
        return null;
    }

    @Override
    public String toString() {
        return loginName + " ==> " + role;
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
