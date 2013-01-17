package edu.caltech.ipac.firefly.server.query;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author tatianag
 *         $Id: SearchProcessorImpl.java,v 1.1 2010/08/04 20:18:51 roby Exp $
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SearchProcessorImpl {
    String id();
    ParamDoc[] params() default {};
}
