package edu.caltech.ipac.util;

import edu.caltech.ipac.util.dd.MappedFieldDefSource;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
/**
 * User: roby
 * Date: Feb 10, 2010
 * Time: 1:31:38 PM
 */


/**
 * @author Trey Roby
 */
public class MutableFieldDefSource extends MappedFieldDefSource implements HasPropertyChange {

    private final PropertyChangeSupport pcSupport= new PropertyChangeSupport(this);

    private MutableFieldDefSource() {}
    public MutableFieldDefSource(String fieldDefType) { super(fieldDefType); }
    public MutableFieldDefSource(MappedFieldDefSource fds) { super(fds); }
    public MutableFieldDefSource(String name, String fieldDefType) { super(name, fieldDefType); }

    public void set(String key, String value) {
        String old= get(key);
        super.set(key,value);
        if (pcSupport!=null) pcSupport.firePropertyChange(key,old,value);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcSupport.removePropertyChangeListener(listener);

    }
    public void addPropertyChangeListener( String propertyName, PropertyChangeListener listener) {
        pcSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener( String propertyName, PropertyChangeListener listener) {
        pcSupport.removePropertyChangeListener(propertyName, listener);
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
