package edu.caltech.ipac.util;


import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;
import javax.swing.undo.UndoableEdit;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;

/**
 *  This class extends the UndoManager so that it is a singleton class that
 *  supports listeners. For a singleton class you do not use a constructor. 
 *  Instead use the getInstance method.  This maintains that only one instance
 *  of the class ever exist. Everybody shares this instance.
 *
 * @see javax.swing.undo.UndoManager
 *
 * @author Trey Roby
 * @version $Id: SingletonUndoManager.java,v 1.3 2005/12/08 22:31:15 tatianag Exp $
 *
 */
public class SingletonUndoManager extends UndoManager{

    static private SingletonUndoManager _theInstance= null;
    private UndoableEditSupport         _support;

    private SingletonUndoManager() {
         super();
         _support= new UndoableEditSupport(this);
    }

    /**
     * Return the only instance of the SingletonUndoManager.
     */
    public static SingletonUndoManager getInstance() {
         if (_theInstance == null) _theInstance= new SingletonUndoManager();
         return _theInstance;
    }

    /**
     * when an undo has happend add it and then pass it on.
     */
    public synchronized boolean addEdit(UndoableEdit undo) {
       boolean b= super.addEdit(undo);
       if (b) {
           _support.postEdit(undo);
       }
       return b;
    }

    /**
     * when an undo has happend add it and then then pass it on.
     */
    public synchronized void undoabledEditHappened( UndoableEditEvent ev) {
       addEdit(ev.getEdit());
    }

    /**
     * go through the list of UndoableEdit's and remove any that implements
     * the passed class.  This is a way remove all of the undos of a certain
     * type. 
     */
    public void removeAllUndosByInterface(Class c) {
                 // go through all the UndoableEdit's in the edit list
                 // and look for a UndoableEdit that also implements the
                 // interface passed to the method.  Note: the edits
                 // variable is an instance variable inheirited from
                 // the base class
        int length= edits.size();
        Object o;
        for(int i=length-1; i>=0; i--) {
              o= edits.get(i);
              if (c.isInstance(o)) {
                    trimEdits(i,i);
              }
        }
        _support.postEdit(null);
    }
    

    /**
     * add a listener
     */
    public synchronized void addUndoableEditListener(UndoableEditListener l) {
          _support.addUndoableEditListener(l);
    }

    /**
     * remove a listener
     */
    public synchronized void 
                        removeUndoableEditListener(UndoableEditListener l) {
          _support.removeUndoableEditListener(l);
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
