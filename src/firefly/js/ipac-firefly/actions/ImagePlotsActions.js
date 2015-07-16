/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import alt from '../core/AppAlt.js';

class ImagePlotsActions {

    constructor() {
        //this.generateActions(
        //    'somethingHere', 'somethingElseHere'
        //);
    }

    anyChange() {
        this.dispatch({});
    }


}


var imagePlotsActions= alt.createActions(ImagePlotsActions);


var allPlots= ffgwt.Visualize.AllPlots.getInstance();
allPlots.addListener({
    eventNotify  : function() {
        imagePlotsActions.anyChange();
    }
});


export default imagePlotsActions;
