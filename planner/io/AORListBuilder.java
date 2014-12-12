package edu.caltech.ipac.planner.io;

import edu.caltech.ipac.data.BaseConstraint;
import edu.caltech.ipac.data.RequestInterface;
import edu.caltech.ipac.planner.AORDisplay;
import edu.caltech.ipac.planner.AORList;
import edu.caltech.ipac.planner.AotFactory;
import edu.caltech.ipac.planner.ConstraintListInterface;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.targetgui.TargetList;
import edu.caltech.ipac.util.Assert;

import java.beans.PropertyVetoException;
import java.util.List;

/**
 * This class builds AORList, TargetList and ConstraintList.
 * @author Carlos Campos, Xiuqin Wu, Michael Nguyen
 * @see edu.caltech.ipac.planner.AORList
 * @see edu.caltech.ipac.planner.AORDisplay
 * @see edu.caltech.ipac.planner.ConstraintListInterface
 * @see edu.caltech.ipac.targetgui.TargetList
 * @see edu.caltech.ipac.data.BaseConstraint
 * @see edu.caltech.ipac.target.Target
 * @see edu.caltech.ipac.data.validate.InstrumentValidator
 */

public class AORListBuilder {

    public final static int OPEN_DISCARD    = 1;
    public final static int ALLOW_DUPLICATE = 2;
    public final static int FILE_OVERWRITE  = 3;


    private AORList                 _aors        = null;
    private TargetList              _targets     = null;
    private ConstraintListInterface _cons        = null;
    private AotFactory              _aotFactory  = null;
    private int                     _optionValue = ALLOW_DUPLICATE;
    private StringBuffer   constraints_deleted_in_overwrite_mode =
                                                 new StringBuffer(101);

    /**
     * Constructor
     * @param aors the AORList
     * @param cons the Constraint List
     * @param aotFactory for creating AORs
     * @param targets the Target List
     */
    public AORListBuilder(AORList                 aors,
                          ConstraintListInterface cons,
                          AotFactory              aotFactory,
                          TargetList              targets) {
        this(aors, cons, aotFactory, targets, ALLOW_DUPLICATE); 
    }


    /**
     * Constructor
     * @param aors the AORList
     * @param cons the Constraint List
     * @param aotFactory for creating AORs
     * @param targets the Target List
     */
    public AORListBuilder(AORList                 aors,
                          ConstraintListInterface cons,
                          AotFactory              aotFactory,
                          TargetList              targets,
                          int                     optionValue) {
        Assert.tst(optionValue == OPEN_DISCARD    ||
                   optionValue == ALLOW_DUPLICATE ||
                   optionValue == FILE_OVERWRITE);
        _aors       = aors;
        _targets    = targets;
        _cons       = cons;
        _aotFactory = aotFactory;
        _optionValue= optionValue;
    }



    /**
     * Get deleted constraints
     * @return a StringBuffer object
     */
    public String getDeletedConstraint() {
        return constraints_deleted_in_overwrite_mode.toString();
    }

    /**
     * Update AORList and TargetList
     * @param reqs the list of Requests
     * @param tmpcons the list of ConstraintList
     */
    public void updateAORListAndTargetList(List reqs, List tmpcons) {
        RequestInterface request;
       _aors.beginBulkUpdate();
       _targets.beginBulkUpdate();
        if (reqs != null && !reqs.isEmpty()) {
            for (int i = 0; i < reqs.size(); i++) {
                request = (RequestInterface) reqs.get(i);
                try {
                    _aors.addAOR(getAORDisplay(request));
                   Assert.tst(true, 
                    "taregt=null. this should never happen since I got the AOR");
                    Target tempTarget =
                            (Target)request.getTarget().clone();
                    _targets.addTarget(tempTarget);
                } catch (DumpAorException e) {
                    //the request that caused this exception 
                    //flags all temp constraints
                    //associated with it to be deleted
                    removeTempContraintsAssociatedwithRequest(request, tmpcons);
                }
            }
        }
       _aors.endBulkUpdate();
       _targets.endBulkUpdate();
       int index = _aors.size() - 1;
       if (index >= 0) _aors.setCurrentAOR(0);

    }
    /**
      * Update AORList
      * @param reqs the list of programs
      */
     public void updateAORList(List reqs) {
         RequestInterface request;
       _aors.beginBulkUpdate();
         if (reqs != null && !reqs.isEmpty()) {
             for (int i = 0; i < reqs.size(); i++) {
                 request = (RequestInterface) reqs.get(i);
                 try {
                     _aors.addAOR(getAORDisplay(request));

                 } catch (DumpAorException e) {
                     //the request that caused this exception
                     //flags all temp constraints
                     //associated with it to be deleted
                     System.out.println("AORLISTBuilder.updateAORList: DumpAORException: Problem with request");
                     //removeTempContraintsAssociatedwithRequest(request, tmpcons);
                 }
             }
         }
       _aors.endBulkUpdate();
         int index = _aors.size() - 1;
         if (index >= 0) _aors.setCurrentAOR(0);
     }
   
    /**
     *  Updates the target list with the new aors obtained from the ascii file
     *  Maybe updated even if errors are found in the ascii file
     * @param newTargets the list of Requests
     */
    public void updateTargetListOnly(List newTargets) {
       _targets.beginBulkUpdate();
        if (!newTargets.isEmpty()) {
           for (int i = 0; i < newTargets.size(); i++) {
              Target t = ((Target) ((Target)newTargets.get(i)).clone());
              _targets.addTarget(t);
           }
        }
       _targets.endBulkUpdate();
    }

    /**
     * Updates the constraint list with the new constraints 
     * (Grouping: Group Within, * Sequencing, Chaining.  
     * Timing constraints are not updated here
     *
     * This method is only called if no errors were found in the ascii file
     * The Procedure:
     *  1.  Check to see that that all the new constraints from the 
     *      ascii file are unique with respect to the constraint list.
     *  2.  If there are any repeating names, change the name of the incomming
     *      constraints to make them unique with respect to the constraint list.
     *      Not with the Constraint editor tool.
     * @param tmpcons the temporary ConstraintList
     */
    public void updateConstraintList(List tmpcons) {
        if (_cons != null && tmpcons != null && !tmpcons.isEmpty()) {
            for (int j = 0; j < tmpcons.size(); j++) {
                BaseConstraint new_constraint = (BaseConstraint) tmpcons.get(j);
                _cons.insureUniqueLabel(new_constraint);
                _cons.addConstraint(new_constraint);
            }
            _cons.syncConstraitsWithAorList();
            _aors.allEntriesUpdated(); 
        }
    }


    public AORList getAORList() { return _aors; }


    /*
     * =================================================================
     * ----------------------- Protected / Private Methods -------------
     * =================================================================
     */

    /**
     * Remove temporary constraints that associated with request
     * @param req the Request
     * @param tmpcons the temporary constraints
     */
    private void removeTempContraintsAssociatedwithRequest(
                                                 RequestInterface req, 
                                                 List             tmpcons) {
        if(tmpcons!=null){                                                 
            for (int i = tmpcons.size() - 1; i >= 0; i--) {
                BaseConstraint c = (BaseConstraint) tmpcons.get(i);
                if (c.contains(req)) {
                   constraints_deleted_in_overwrite_mode.append(
                      "\r\n" + c.getName() + ". (from ascii file)");
                    tmpcons.remove(c);
                }
           }
       }
    }



    /**
     * Read the Target from the file
     * @return Target the Target object
     */
    private AORDisplay getAORDisplay(RequestInterface request)
                                      throws DumpAorException {


        if (request == null)
           return null;


        AORDisplay aor = _aotFactory.makeAORDisplay(request);
        if (aor == null)
           return null;

        if (_optionValue > 0) {
            //AORDisplay tempDis = (AORDisplay)aor;
            //Request tempAr = (Request)tempDis.getAr();
            RequestInterface tempAr = (RequestInterface) aor.getAr();
            AORDisplay existingAor = _aors.find(tempAr.getTitle());
            // find the AORDisplay that has the save title
            // as the input request in _aors


            if (existingAor != null && _optionValue == OPEN_DISCARD) {
                //aor with the same label is already present in AorList.
                throw new DumpAorException();
            }
            if (existingAor != null && _optionValue == FILE_OVERWRITE) {
                // need to erase the tempAr.getTitle() Aor from _aors, that's it!
                List tempList = null;
                if (_cons != null)
                      tempList = _cons.getInvalidCListIfAorRemoved(existingAor);
                if (tempList != null) {
                    for (int z = 0; z < tempList.size(); z++) {
                        constraints_deleted_in_overwrite_mode.append(
                                "\r\n" + 
                          ((BaseConstraint) tempList.get(z)).getName() + ". ");
                    }
                    // in overwrite mode must delete the
                    // constraints tied to the particular
                    // aor that got overwritten
                    // must notify user that those constraints
                    // were deleted in the process
                }
                try { 
                   _aors.removeAOR(existingAor);
                } catch (PropertyVetoException e) {
                   Assert.tst(true, 
                    "this should never happen since I am doing a bulk update");
                }
           } 
        }
        AORDisplay uniqueAor = (AORDisplay) aor;
        _aors.insureUniqueLabel(uniqueAor);
        return uniqueAor;
    }


    private class DumpAorException extends Exception { }
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
