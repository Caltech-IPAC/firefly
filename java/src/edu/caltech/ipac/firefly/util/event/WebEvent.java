package edu.caltech.ipac.firefly.util.event;

import edu.caltech.ipac.firefly.util.WebAssert;

import java.util.EventObject;

/**
 * The event that is passed when ...
 * @author Trey Roby
 */
public class WebEvent <DataType> extends EventObject {


    private final Name _name;
    private final transient DataType _data;

    /**
     * Create a client event
     * @param source source of the event, may not be null.
     * @param name the name of the event, may not be null
     * @throws IllegalArgumentException  if either source or name is null
     */
    public WebEvent(Object source, Name name) {
        this(source,name,null);
    }


    /**
     * Create a client event
     * @param source source of the event, may not be null.
     * @param name the name of the event, may not be null
     * @param data data associated with this event (if any)
     * @throws IllegalArgumentException  if either source or name is null
     */
    public WebEvent(Object source, Name name, DataType data) {
        super(source);
        WebAssert.argTst(source!=null && name!=null, "You must pass a non-null value " +
                                                  "for both source and name");
        _name= name;
        _data= data;
    }

    public Name getName() { return _name; }
    public DataType getData() { return _data; }


    public String toString() {
        return "WebEvent- "+ _name +", Source: " + getSource() + ", Data: " + _data;
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
