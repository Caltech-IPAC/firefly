package edu.caltech.ipac.visualize.draw;
/**
 * User: roby
 * Date: 11/13/12
 * Time: 1:59 PM
 */


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Trey Roby
 */
public class DynamicShapeFactory {

    private Map<String,DynamicShapeInfo> _map= new HashMap<String,DynamicShapeInfo>(7);
    private static DynamicShapeFactory _instance= null;

    private DynamicShapeFactory() {}

    public static DynamicShapeFactory getInstance() {
        if (_instance==null) _instance= new DynamicShapeFactory();
        return _instance;
    }

    public void addShape(String name, DynamicShapeInfo dynShape) { _map.put(name, dynShape); }

    public DynamicShapeInfo getShape(String name) { return _map.get(name); }

    public Set<Map.Entry<String,DynamicShapeInfo>> getEntrySet() {
        return _map.entrySet();
    }

    public int size() { return _map.size(); }
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
