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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
