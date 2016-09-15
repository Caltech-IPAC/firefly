/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */




/**
 * @typedef {Object} DrawLayerDefinition
 * The object that can create and manager a draw layer type.
 *
 * @prop {String} drawLayerTypeId
 * @prop {function} create
 * @prop {DrawLayerDefinition~getDrawData} getDrawDataFunc
 * @prop {DrawLayerDefinition~getLayerChanges} getLayerChanges
 * @prop {object} getUIComponent
 */

/**
 * @name DrawLayerDefinition~getDrawData
 * 
 * Get the changes to incorporate into the drawing layer object
 *  A function that returns an object literal the that has the field changes.
 *  It may return null or empty object if there are no changes. getLayerChanges is a super set of
 *  getDrawDataFunc. Usually you only need one or the other. getLayerChanges is better for more
 *  interactive like select area or distance tool. makeFactoryDef~getDrawData works better for data intensive layers like catalogs.
 *
 * @function
 * @param {DrawLayer} drawLayer
 * @param {Action} action
 */

/**
 * @name DrawLayerDefinition~getLayerChanges
 * 
 * Get the changes to incorporate into the drawing layer object
 *  A function that returns an object literal the that has the field changes.
 *  It may return null or empty object if there are no changes. getLayerChanges is a super set of
 *  getDrawDataFunc. Usually you only need one or the other. getLayerChanges is better for more
 *  interactive like select area or distance tool. makeFactoryDef~getDrawData works better for data intensive layers like catalogs.
 *
 * @function
 * @param {DrawLayer} drawLayer
 * @param {Action} action
 */

/**
 * @name DrawLayerDefinition~creator
 * 
 * Create a new draw layer
 * @function
 * @param {object} initPayload
 * @return {DrawLayer}
 *
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
 *
 * @param drawLayerTypeId
 * @param {DrawLayerDefinition~creator} create
 * @param {DrawLayerDefinition~getDrawData} [getDrawDataFunc]
 * @param {DrawLayerDefinition~getLayerChanges} [getLayerChanges] get the changes to incorporate into the drawing layer object
 * @param {object} onDetachAction
 * @param {object} [getUIComponent] react component react pure function component, optional- react component
 * @return {DrawLayerDefinition}
 */
export function makeFactoryDef(drawLayerTypeId,
                               create,
                               getDrawDataFunc= null,
                               getLayerChanges= null,
                               onDetachAction= null,
                               getUIComponent= null) {
    return {drawLayerTypeId, create, getDrawDataFunc, getLayerChanges, onDetachAction,getUIComponent};
}

class DrawLayerFactory {

    constructor(factoryDefsAry) {
        this.registry=  {};
        this.defaults= {};
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
        if (!this.registry[drawLayerTypeId]) {
            console.warn(`DrawingLayerType: ${drawLayerTypeId} does not exist in the registry, did you forget to add it?`);
            return null;
        }
        return this.registry[drawLayerTypeId].create(initPayload, this.defaults[drawLayerTypeId]);
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

    onDetachAction(drawLayer, action) {
        if (!drawLayer || !this.registry[drawLayer.drawLayerTypeId]) return {};
        var f= this.registry[drawLayer.drawLayerTypeId].onDetachAction;
        if (f) f(drawLayer,action);
    }

    getGetUIComponentFunc(drawLayer) {
        if (!drawLayer || !this.registry[drawLayer.drawLayerTypeId]) return false;
        return this.registry[drawLayer.drawLayerTypeId].getUIComponent;
    }

    setDrawLayerDefaults(drawLayerTypeId, def) {
        if (this.defaults[drawLayerTypeId]) {
            this.defaults[drawLayerTypeId]= Object.assign(this.defaults[drawLayerTypeId], def);
        }
    }


    /**
     *
     * @param {{drawLayerTypeId: string, create: function, getDrawDataFunc: function, getLayerChanges: function, uiComponent: object}} factoryDef
     */
    register(factoryDef) {
        this.registry[factoryDef.drawLayerTypeId]= factoryDef;
        this.defaults[factoryDef.drawLayerTypeId]= {};
    }

    static makeFactory(...factoryDefs) {
        return new DrawLayerFactory(factoryDefs);
    }
}

export default DrawLayerFactory;



