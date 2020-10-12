// import {GPU} from 'gpu.js';
import {getGlobalObj} from '../../util/WebUtil.js';
import {isArrayBuffer, once} from 'lodash';
import {TILE_SIZE} from './RawDataCommon.js';

export const getGPUOps= once((GPU) => {

    const gpu= new GPU({mode:'gpu'});

    const standRawDataTileGPU = gpu.createKernel(function(pixelAry, colorModel,height, contrast, offsetShift) {
        const pixel= pixelAry[height-this.thread.y][this.thread.x];
        if (pixel!==255) {
            let pixelIdx= pixel*3;
            if (offsetShift!==0) {
                const newPixel = Math.floor( offsetShift+(pixel*contrast));
                pixelIdx= (newPixel > 254) ? 762 : (newPixel<0) ? 0 : newPixel*3;
            }
            this.color( colorModel[pixelIdx]/255, colorModel[pixelIdx+1]/255, colorModel[pixelIdx+2]/255);
        }
        else {
            this.color( 0,0,0);
        }
    },{graphical:true, dynamicOutput:true, dynamicArguments:true, tactic: 'speed'});


    const threeCRawDataTileGPU = gpu.createKernel(function(redAry, greenAry, blueAry, useR, useG, useB, contrast, offsetShift, width,height) {
        const idx= (height - this.thread.y) * width + this.thread.x;
        let r= useR ? redAry[idx] : 0;
        let g= useG ? greenAry[idx] : 0;
        let b= useB ? blueAry[idx] : 0;
        if (offsetShift!==0) {
            if (r!==0) {
                r= Math.floor( offsetShift+(r*contrast));
                r= (r > 254) ? 254 : (r<0) ? 0 : r;
            }
            if (g!==0) {
                g= Math.floor( offsetShift+(g*contrast));
                g= (g > 254) ? 254 : (g<0) ? 0 : g;
            }
            if (b!==0) {
                b= Math.floor( offsetShift+(b*contrast));
                b= (b > 254) ? 254 : (b<0) ? 0 : b;
            }
        }
        this.color( r/255, g/255, b/255, 1);
    },{graphical:true, dynamicOutput:true, dynamicArguments:true, tactic: 'speed'});


    async function createTransitionalTileWithGPU(inData, colorModel, isThreeColor, bias=.5, contrast=1, bandUse) {
        const canvas= createTileWithGPU(inData,colorModel,isThreeColor, bias,contrast, bandUse);

        const g= getGlobalObj();
        if (!g.document && g.createImageBitmap) {
            const workerTmpTile= await g.createImageBitmap(canvas);
            return { ...inData, workerTmpTile };
        }
        else {
            return { ...inData, workerTmpTile:canvas };
        }
    }


    const get8BitAry= (a) => isArrayBuffer(a) ? new Uint8ClampedArray(a) : a;

    /**
     *
     * @param {RawTileData} inData
     * @param colorModel
     * @param isThreeColor
     * @return {HTMLCanvasElement|OffscreenCanvas}
     */
    function createTileWithGPU(inData, colorModel, isThreeColor, bias=.5, contrast=1, bandUse) {
        const {width,height, pixelData3C, pixelDataStandard}= inData;

        return isThreeColor ?
            createRawDataTile3CRGBDataGPU(pixelData3C.map( (a) => a && get8BitAry(a)), width,height,bias,contrast,bandUse) :
            createRawDataTileImageRGBDataGPU(colorModel, get8BitAry(pixelDataStandard), width,height,bias,contrast);
    }

    function createRawDataTileImageRGBDataGPU(colorModel, pixelData, width,height,bias=.5, contrast=1) {

        const offset = (127*(bias-0.5)*-4);
        const shift = (127*(1-contrast));
        standRawDataTileGPU.setOutput([width,height]);
        standRawDataTileGPU(
            GPU.input(pixelData,[width,height]),
            colorModel,
            height,
            contrast,
            offset+shift,
           );
        return makeRetData(standRawDataTileGPU.canvas, width, height);
    }


    function createRawDataTile3CRGBDataGPU(pixelDataAry, width,height,bias=.5, contrast=1, bandUse={}) {
        const {useRed=true,useGreen=true,useBlue=true}= bandUse;
        const offset = (127*(bias-0.5)*-4);
        const shift = (127*(1-contrast));
        threeCRawDataTileGPU.setOutput([width,height]);
        threeCRawDataTileGPU(
            pixelDataAry[0]||new Uint8ClampedArray(1),
            pixelDataAry[1]||new Uint8ClampedArray(1),
            pixelDataAry[2]||new Uint8ClampedArray(1),
            Boolean(pixelDataAry[0]&&useRed), Boolean(pixelDataAry[1]&&useGreen), Boolean(pixelDataAry[2]&&useBlue),
            contrast,
            offset+shift,
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
            c.getContext('2d').drawImage(standRawDataTileGPU.canvas,0,gpuCanvas.height-height,width,height,0,0,width,height);
        }
        return c;

    }

    return {createTransitionalTileWithGPU, createTileWithGPU};
});


