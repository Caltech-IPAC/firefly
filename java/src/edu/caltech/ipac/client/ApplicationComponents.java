package edu.caltech.ipac.client;

import edu.caltech.ipac.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: roby
 * Date: Mar 29, 2007
 * Time: 2:05:44 PM
 */


public class ApplicationComponents {

    private final Map<String, Object> _map=
                         new HashMap<String,Object>(9);

//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================

    public ApplicationComponents() { }

    public ApplicationComponents(Object... comp) {
        for(Object o : comp) {
            put(o);
        }
    }

//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================
    public Object get(String key) {
        return _map.get(key);
    }

    public Object put(String key, Object component) {
        return _map.put(key, component);
    }
    public Object put(Object component) {
        String key= StringUtil.getShortClassName(component.getClass());
        return put(key, component);
    }

    public Collection<Object> getComponentList() {
        return Collections.unmodifiableCollection(_map.values());
    }

    public Object[] getObjects(Class... classAry) {
        List<Object> retObj= new ArrayList<Object>(classAry.length);

        for(Class c : classAry) {
            for(Object o : _map.values()) {
                if (c.isInstance(o)) {
                    retObj.add(o);
                    break;
                }
            }
        }
        Object retAry[]= null;
        if (retObj.size() == classAry.length) {
            retAry= retObj.toArray();
        }
        return retAry;
    }

    public String listClasses() {
        StringBuffer retval= new StringBuffer(200);
        Object o;
        for(Iterator<Object> i= _map.values().iterator(); (i.hasNext());) {
            retval.append(i.next().getClass().getName());
            if (i.hasNext())  retval.append(", ");
        }
        return retval.toString();
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
