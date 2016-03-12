/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import PointDataObj from '../visualize/draw/PointDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';

const ID= 'STATS_POINT';
const TYPE_ID= 'STATS_POINT_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator, null, getLayerChanges,null,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;


function creator(initPayload) {
    const drawingDef = makeDrawingDef();

    idCnt++;

    var options= {
        isPointData:true,
        canUserChangeColor: ColorChangeType.DYNAMIC
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, 'highlighted point', options, drawingDef);

}


function getLayerChanges(drawLayer, action) {
    switch (action.type) {
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            let drawData = {};
            const {changes} = action.payload;

            if (changes) {
                drawData[DataTypes.DATA] = computeDrawLayer(changes['worldPt']);
            }

            return {drawData};
    }
    return null;
}


function computeDrawLayer(wp) {
     return wp ? [PointDataObj.make(wp)] : [];
}


