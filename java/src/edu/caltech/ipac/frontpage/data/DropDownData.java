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
public class DropDownData extends JavaScriptObject {

    protected DropDownData() {}
    public final native String getName()         /*-{ return this.name; }-*/;

    public final native JsArray<DropDownData> getDrop() /*-{
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

    public final native int getIndex() /*-{
          if ("index" in this) {
              return this.index;
          }
          else {
              return -1;
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
