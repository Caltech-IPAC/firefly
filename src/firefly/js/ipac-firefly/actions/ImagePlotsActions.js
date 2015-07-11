/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {application} from '../core/Application.js';

class ImagePlotsActions {

    constructor() {
        //this.generateActions(
        //    'initState', 'mountComponent', 'validateForm'
        //);
    }

    anyChange() {
        this.dispatch({});
    }


}


var imagePlotsActions= application.alt.createActions(ImagePlotsActions);


var allPlots= ffgwt.Visualize.AllPlots.getInstance();
allPlots.addListener({
    eventNotify  : function() {
        imagePlotsActions.anyChange();
    }
});


export default imagePlotsActions;
