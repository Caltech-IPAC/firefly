/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



export default {makeDrawObj};

function makeDrawObj() {

    var obj= {
        // color:
        // selected:
        // highlighted
        // representCnt
        // lineWidth : 1;

        renderOptions : {}, // can contain keys: shadow,translation,rotAngle
                            // shadow  - a shadow object, use makeShadow()
                            // translation - a ScreenPt use Point.makeScreenPt
                            // rotAngle - the angle, a number

        type : 'DrawObj',
        getCanUsePathEnabledOptimization : () => false,
        getScreenDist: (plot,pt) => 0,
        getSupportsWebPlot: () => true,
        getCenterPt : () => null,
        draw : (ctx,plot,def,vpPtM,onlyAddToPath) => null,
        toRegion : (plot,def) => [],
		translateTo : (plot,worldPt) => this,
		rotateAround :(plot, angle, wprldPt) => this
    };

    return obj;
}



