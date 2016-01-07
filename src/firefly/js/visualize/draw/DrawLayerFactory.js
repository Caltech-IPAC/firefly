/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * This function is just a convenience to create a draw factory definition object literal. You don't have
 * to use it.  You can create the object directly.
 * Every drawing layer needs a drawing
 * factory definition to manage it. All drawing layer modules must export an factoryDef object. The factoryDef should
 * the following properties.
 *
 * drawLayerTypeId - string, required- type id of the drawLayer
 * create -function, function, required-  that will create a new draw layer of this type
 * getDrawDataFunc - function, optional- a function that will take the drawing layer and data the type.
 *                   signature: getDrawDataFunc(dataType, plotId, drawLayer, action, lastDataRet)
 *                   where dataType is a string with constants in DrawLayers.DataType.
 *                   lastDataRet is the data that was return in the last call. If nothing has change
 *                   then then lastDataRet can be the return value.
 * getLayerChanges - function, optional- get the changes to incorporate into the drawing layer object
 *                    A function that returns an object literal the that has the field changes.
 *                    <br>signature: getLayerChanges(drawLayer,action)
 *                    it may return null or empty object if there are no changes. getLayerChanges is a super set of
 *                    getDrawDataFunc. Usually you only need one or the other. getLayerChanges is better for more
 *                    interactive like select area or distance tool.
 *                    getDrawDataFunc works better for data intensive layers like catalogs.
 * uiComponent - react pure function component, optional- react component
 *
 * @param drawLayerTypeId
 * @param create
 * @param {function} [getDrawDataFunc]
 * @param {function} [getLayerChanges] get the changes to incorporate into the drawing layer object
 *                    A function that returns an object literal the that has the field changes.
 *                    <br>signature: getLayerChanges(drawLayer,action)
 *                    it may return null or empty object if there are no changes
 * @param {object} [getUIComponent] react component
 * @return {{drawLayerTypeId: string, create: function, getDrawDataFunc: function, getLayerChanges: function, getUIComponent: object}}
 */
export function makeFactoryDef(drawLayerTypeId,
                               create,
                               getDrawDataFunc= null,
                               getLayerChanges= null,
                               getUIComponent= null) {
    return {drawLayerTypeId, create, getDrawDataFunc, getLayerChanges, getUIComponent};
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

    getGetUIComponentFunc(drawLayer) {
        if (!drawLayer || !this.registry[drawLayer.drawLayerTypeId]) return false;
        return this.registry[drawLayer.drawLayerTypeId].getUIComponent;
    }



    /**
     *
     * @param {{drawLayerTypeId: string, create: function, getDrawDataFunc: function, getLayerChanges: function, uiComponent: object}} factoryDef
     */
    register(factoryDef) {
        this.registry[factoryDef.drawLayerTypeId]= factoryDef;
    }

    static makeFactory(...factoryDefs) {
        return new DrawLayerFactory(factoryDefs);
    }
}

export default DrawLayerFactory;



