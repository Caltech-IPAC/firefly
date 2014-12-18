package edu.caltech.ipac.util.dd;
/**
 * User: roby
 * Date: Sep 29, 2010
 * Time: 1:25:23 PM
 */


/**
 * @author Trey Roby
 */
public interface CanValidate {

    public boolean validate(Object aValue) throws ValidationException;


}

