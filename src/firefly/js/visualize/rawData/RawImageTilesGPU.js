// import {GPU} from 'gpu.js';
import {getGlobalObj} from '../../util/WebUtil.js';
import {TILE_SIZE} from './RawDataConst.js';
import {once} from 'lodash';

export const getGPUOps= once((GPU) => {

    const gpu= new GPU({mode:'gpu'});

    const standRawDataTileGPU = gpu.createKernel(function(pixelAry, colorModel,width,height) {
        const idx= (height - this.thread.y) * width + this.thread.x;
        const pixel= pixelAry[idx];
        if (pixel!==255) {
            this.color( colorModel[pixel*3]/255, colorModel[pixel*3+1]/255, colorModel[pixel*3+2]/255, 1);
        }
        else {
            this.color( 0,0,0,1);
        }
    },{graphical:true, dynamicOutput:true, dynamicArguments:true, tactic: 'speed'});


    const threeCRawDataTileGPU = gpu.createKernel(function(redAry, greenAry, blueAry, useR, useG, useB, width,height) {
        const idx= (height - this.thread.y) * width + this.thread.x;
        const r= useR ? redAry[idx] : 0;
        const g= useG ? greenAry[idx] : 0;
        const b= useB ? blueAry[idx] : 0;
        this.color( r/255, g/255, b/255, 1);
    },{graphical:true, dynamicOutput:true, dynamicArguments:true, tactic: 'speed'});




    async function createTransitionalTileWithGPU(inData, colorModel, isThreeColor) {
        const canvas= createTileWithGPU(inData,colorModel,isThreeColor);

        const g= getGlobalObj();
        if (!g.document && g.createImageBitmap) {
            const workerTmpTile= await g.createImageBitmap(canvas);
            return { ...inData, workerTmpTile };
        }
        else {
            return { ...inData, workerTmpTile:canvas };
        }
    }


    /**
     *
     * @param {RawTileData} inData
     * @param colorModel
     * @param isThreeColor
     * @return {HTMLCanvasElement|OffscreenCanvas}
     */
    function createTileWithGPU(inData, colorModel, isThreeColor) {
        const {width,height, pixelData3C, pixelDataStandard}= inData;

        return isThreeColor ?
            createRawDataTile3CRGBDataGPU(pixelData3C.map( (a) => a && new Uint8ClampedArray(a)), width,height) :
            createRawDataTileImageRGBDataGPU(colorModel, new Uint8ClampedArray(pixelDataStandard), width,height);
    }

    function createRawDataTileImageRGBDataGPU(colorModel, pixelData, width,height) {

        standRawDataTileGPU.setOutput([width,height]);
        standRawDataTileGPU(pixelData,colorModel,width,height);
        return makeRetData(standRawDataTileGPU.canvas, width, height);
    }


    function createRawDataTile3CRGBDataGPU(pixelDataAry, width,height) {
        threeCRawDataTileGPU.setOutput([width,height]);
        threeCRawDataTileGPU(
            pixelDataAry[0]||new Uint8ClampedArray(1),
            pixelDataAry[1]||new Uint8ClampedArray(1),
            pixelDataAry[2]||new Uint8ClampedArray(1),
            Boolean(pixelDataAry[0]), Boolean(pixelDataAry[1]), Boolean(pixelDataAry[2]),
            width,height);
        return makeRetData(threeCRawDataTileGPU.canvas, width, height);
    }


    function makeRetData(gpuCanvas,width,height)  {
        const g= getGlobalObj();

        const c = g.document ? g.document.createElement('canvas') : new OffscreenCanvas(width,height);
        c.width = width;
        c.height = height;

        if (gpuCanvas.width===width && gpuCanvas.height===height) {
            c.getContext('2d').drawImage(standRawDataTileGPU.canvas,0,0);
        }
        else if (width===TILE_SIZE && height===TILE_SIZE) {
            c.getContext('2d').drawImage(standRawDataTileGPU.canvas,0,0);
        }
        else {
            c.getContext('2d').drawImage(standRawDataTileGPU.canvas,0,TILE_SIZE-height,width,height,0,0,width,height);
        }
        return c;

    }

    return {createTransitionalTileWithGPU, createTileWithGPU};
});


