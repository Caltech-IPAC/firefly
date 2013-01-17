package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.heritage.server.query.SearchManager;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

/**
 * Date: Mar 24, 2010
 *
 * @author loi
 * @version $Id: HeritageSecurityModule.java,v 1.5 2012/03/30 21:00:25 loi Exp $
 */
public class HeritageSecurityModule {
    private static StringKey proprietaryInfoKey = new StringKey(HeritageSecurityModule.class.getSimpleName(), "ProprietaryInfo");

    public static boolean checkHasAccess(String reqKey) {

        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        ProprietaryInfo propInfo = (ProprietaryInfo) cache.get(proprietaryInfoKey);

        if (propInfo == null) {
            propInfo = new SearchManager().getProprietary();
            cache.put(proprietaryInfoKey, propInfo, 7*24*60*60);  // expires after 1 day
        }
        
        ProprietaryInfo.Data reqInfo = propInfo.getData(reqKey);
        if (reqInfo == null || reqInfo.getReleaseDate().getTime() < System.currentTimeMillis()) {
            return true;
        } else {
            // it is proprietary data... need to check access
            UserInfo user = ServerContext.getRequestOwner().getUserInfo();
            if (user.isGuestUser()) {
                return false;
            }
            RoleList roles = user.getRoles();
            if (roles == null || roles.size() == 0) {
                return false;
            } else {
                return roles.hasAccess("SPITZER", reqInfo.getProgId());
            }
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
