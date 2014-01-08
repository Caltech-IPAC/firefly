package edu.caltech.ipac.frontpage.data;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

/**
 * User: roby
 * Date: Jun 18, 2010
 * Time: 3:27:03 PM
 */


/**
 * @author Trey Roby
 */
public class DisplayData extends JavaScriptObject {

    protected DisplayData() {}
    public final native String getName()         /*-{ return this.name; }-*/;

    public final native JsArray<DisplayData> getDrop() /*-{
          if ("drop" in this) {
              return this.drop;
          }
          else {
              return null;
          }
    }-*/;


    public final native String getTip() /*-{
          if ("tip" in this) {
              return this.tip;
          }
          else {
              return "";
          }
    }-*/;


    public final native boolean isSeparator() /*-{
          if ("separator" in this) {
              return this.separator;
          }
          else {
              return false;
          }
    }-*/;

    public final native String getHref()         /*-{
         if ("href" in this) {
             return this.href;
         }
         else {
             return null;
         }
     }-*/;

    public final native String getAbstractStart()         /*-{
        if ("abstractStart" in this) {
            return this.abstractStart;
        }
        else {
            return null;
        }
    }-*/;

    public final native String getAbstract()         /*-{
        if ("abstractDesc" in this) {
            return this.abstractDesc;
        }
        else {
            return null;
        }
    }-*/;

    public final native String getImage()         /*-{
        if ("image" in this) {
            return this.image;
        }
        else {
            return null;
        }
    }-*/;


    public final native boolean isPrimary()         /*-{
        if ("primary" in this) {
            return this.primary;
        }
        else {
            return false;
        }
    }-*/;



//    public final native String getMethod()         /*-{ return this.method; }-*/;

    public final native boolean containsKey(String key) /*-{
            return key in this;
  }-*/;

    public final DataType getType() {
        if (containsKey("separator")) {
            return DataType.SEPARATOR;
        }
        else if (containsKey("drop")) {
            return DataType.MENU;
        }
        else if (containsKey("href")) {
            return DataType.LINK;
        }
        else if (containsKey("image")) {
            return DataType.IMAGE;
        }
        else if (containsKey("abstractDesc")) {
            return DataType.ONLY_ABSTRACT;
        }
        else {
            return DataType.NONE;
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
