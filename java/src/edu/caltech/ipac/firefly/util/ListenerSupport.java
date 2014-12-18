package edu.caltech.ipac.firefly.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Mar 2, 2009
 *
 * @author loi
 * @version $Id: ListenerSupport.java,v 1.1 2009/03/04 17:04:05 loi Exp $
 */
public class ListenerSupport<Type> {
    private ArrayList<Type> listeners;
    private HashMap<String, List<Type>> namedListeners;

    public void addListener(Type listener) {
        getListeners().add(listener);
    }

    public void addListener(String name, Type listener) {
        getListeners(name).add(listener);
    }

    public boolean removeListener(Type listener) {
        return getListeners().remove(listener);
    }

    public void removeListener(String name) {
        getNamedListeners().remove(name);
    }

    public List<Type> getListeners() {
        if (listeners == null) {
            listeners = new ArrayList<Type>(2);
        }
        return listeners;
    }

    public List<Type> getListeners(String name) {
        List<Type> l = getNamedListeners().get(name);
        if (l == null || l.size() == 0) {
            l = new ArrayList<Type>(2);
            getNamedListeners().put(name, l);
        }
        return l;
    }

    public void fireEvent(Function<Type> function) {

        if (listeners != null) {
            ArrayList<Type> l = new ArrayList<Type>(getListeners());
            for(Type type : l) {
                function.execute(type);
            }
        }
    }

    public void fireEvent(String name, Function<Type> function) {

        if (namedListeners != null && getNamedListeners().containsKey(name)) {
            List<Type> l = new ArrayList<Type>(getListeners(name));
            for(Type type : l) {
                function.execute(type);
            }
        }
    }

//====================================================================
//
//====================================================================

    Map<String, List<Type>> getNamedListeners() {
        if (namedListeners == null) {
            namedListeners = new HashMap<String, List<Type>>(2);
        }
        return namedListeners;
    }



//====================================================================
//
//====================================================================

    public interface Function<Type> {
        void execute(Type param);
    }

}
