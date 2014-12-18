package edu.caltech.ipac.firefly.linker;
/**
 * User: roby
 * Date: 2/28/14
 * Time: 3:03 PM
 */

import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.linker.CrossSiteIframeLinker;

/**
 * @author Trey Roby
 */
@LinkerOrder(LinkerOrder.Order.PRIMARY)
@Shardable
public class NoWaitingLinker extends CrossSiteIframeLinker {


    /**
     * Returns the name of the {@code JsIsBodyLoaded} script.  By default,
     * returns {@code "com/google/gwt/core/ext/linker/impl/isBodyLoaded.js"}.
     *
     * @param context a LinkerContext
     */
//    protected String getJsIsBodyLoaded(LinkerContext context) {
//        System.out.println("getJsIsBodyLoaded");
//        return "edu/caltech/ipac/firefly/linker/nowaitIsBodyLoaded.js";
//    }
//
//    @Override
//    protected boolean shouldInstallCode(LinkerContext context) {
//        System.out.println("shouldInstallCode returns:"+ super.shouldInstallCode(context));
//        return super.shouldInstallCode(context);

}

