package edu.caltech.ipac.util.action;




/**
 * A subclass of DateTimeAction	 that suports entering Date values and 
 * checking.  Only some acceptible values may be entered in this field <p>
 *
 * @author Xiuqin Wu
 * @see DateTimeAction	
 */
public class DateAction	 extends DateTimeAction	 {

    private static final String _acceptedStrings [] = {
                      "dd-MMM-yyyy", "yyyy-MMM-dd",
                      "yyyy MMM d", "yy MM d",
                      "MMM d yy", "MMM d, yy", "MM/dd/yy", "MM-dd-yy",
                      "yyyy.MM.d", "d.MM.yy", "d MMM yy", "d MMM yyyy" };

    private static final String _standardString = "yyyy MMM d";
    private static final String PROP = "DateAction";

    public DateAction(String propName, InputContainer inputContainer) {
	 super(propName, _standardString, _acceptedStrings, inputContainer);
    }

    public DateAction(String propName) {
	 this(propName, null);
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
