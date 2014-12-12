package edu.caltech.ipac.util.action;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Date: Feb 16, 2006
 *
 * @author Trey Roby
 * @version $id:$
 */
@Target({ElementType.CONSTRUCTOR, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoCreateConstructor { }
