package edu.caltech.ipac.firefly.data;

/**
 * Date: Mar 8, 2010
 *
 * @author loi
 * @version $Id: HasAccessInfos.java,v 1.1 2010/03/08 22:40:04 loi Exp $
 */
public interface HasAccessInfos {
    int getSize();
    boolean hasAccess(int index);
}
