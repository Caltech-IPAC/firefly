/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



//============================================================
//============================================================


/**
 *
 * @param drawLayerTypeId
 * @param create
 * @param {function} getDrawDataFunc a function that will take the drawing layer and data the type.
 *                   signature: getDrawDataFunc(dataType, plotId, drawLayer, action, lastDataRet)
 *                   where dataType is a string with constants in DrawLayers.DataType.
 *                   lastDataRet is the data that was return in the last call. If nothing has change
 *                   then then lastDataRet can be the return value.
 * @param {function} [getLayerChanges] get the changes to incorporate into the drawing layer object
 *                    A function that returns an object literal the that has the field changes.
 *                    <br>signature: getLayerChanges(drawLayer,action)
 *                    it may return null or empty object if there are no changes
 * @param uiComponent
 */
export function makeFactoryDef(drawLayerTypeId, create, getDrawDataFunc,
    getLayerChanges, uiComponent= null) {
    return {drawLayerTypeId, create, getDrawDataFunc, getLayerChanges, uiComponent};
}

class DrawLayerFactory {

    constructor(factoryDefsAry) {
        this.registry=  {};
        factoryDefsAry.forEach( (fd) => {
            if (fd.create && fd.drawLayerTypeId) {
                this.register(fd);
            }
            else if (fd.factoryDef && fd.factoryDef.create && fd.factoryDef.drawLayerTypeId) {
                this.register(fd.factoryDef);
            }
        });
    }


    /**
     *
     * @param drawLayerTypeId
     * @param initPayload
     * @return {*}
     */
    create(drawLayerTypeId, initPayload) {
        if (!this.registry[drawLayerTypeId]) return null;
        return this.registry[drawLayerTypeId].create(initPayload);
    }

    hasGetDrawData(drawLayer) {
        if (!drawLayer || !this.registry[drawLayer.drawLayerTypeId]) return false;
        return this.registry[drawLayer.drawLayerTypeId].getDrawDataFunc ? true : false;

    }

    getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
        if (!drawLayer || !this.registry[drawLayer.drawLayerTypeId]) return false;
        var f= this.registry[drawLayer.drawLayerTypeId].getDrawDataFunc;
        return f ? f(dataType, plotId, drawLayer, action, lastDataRet) : lastDataRet;
    }

    hasGetLayerChanges(drawLayer) {
        if (!drawLayer || !this.registry[drawLayer.drawLayerTypeId]) return false;
        return this.registry[drawLayer.drawLayerTypeId].getLayerChanges ? true : false;
    }

    getLayerChanges(drawLayer, action) {
        if (!drawLayer || !this.registry[drawLayer.drawLayerTypeId]) return {};
        var f= this.registry[drawLayer.drawLayerTypeId].getLayerChanges;
        return f ? f(drawLayer, action) : {};
    }

    /**
     *
     * @param factoryDef
     */
    register(factoryDef) {
        this.registry[factoryDef.drawLayerTypeId]= factoryDef;
    }

    static makeFactory(...factoryDefs) {
        return new DrawLayerFactory(factoryDefs);
    }
}


export default DrawLayerFactory;



