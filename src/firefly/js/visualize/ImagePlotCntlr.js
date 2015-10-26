


import {flux} from '../Firefly.js';



const ANY_CHANGE= 'ImagePlotCntlr/AnyChange';
const IMAGE_PLOT_KEY= 'allPlots';



function reducer(state={}, action={}) {
    return state;
}

/*globals ffgwt*/

if (ffgwt) {
    var allPlots= ffgwt.Visualize.AllPlots.getInstance();
    allPlots.addListener({
        eventNotify(ev) {
            console.log('ANY_CHANGE:' + ev.getName().getName());
            if (ev.getName().getName()==='Replot') {
                flux.process({type: ANY_CHANGE, payload: {} });
            }
        }
    });
}



//============ EXPORTS ===========
//============ EXPORTS ===========

var ImagePlotCntlr = {reducer, ANY_CHANGE, IMAGE_PLOT_KEY };
export default ImagePlotCntlr;

//============ EXPORTS ===========
//============ EXPORTS ===========
