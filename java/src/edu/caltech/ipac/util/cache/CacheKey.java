/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.cache;

import java.io.Serializable;

/**
 * Date: Jul 18, 2008
 * @author loi
 * @version $Id: CacheKey.java,v 1.2 2009/06/23 23:48:09 roby Exp $
 */
public interface CacheKey extends Serializable {
    String getUniqueString();
}
