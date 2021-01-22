// import {GPU} from 'gpu.js';
import {getGlobalObj} from '../../util/WebUtil.js';
import {isArrayBuffer, once, isArray} from 'lodash';
import {TILE_SIZE} from './RawDataCommon.js';

export const getGPUOps= once((GPU) => {

    const gpu= new GPU({mode:'gpu'});


    const standardFunc= Function('pixelAry', 'colorModel','height', 'contrast', 'offsetShift', `
            const pixel= pixelAry[height-this.thread.y][this.thread.x];
            if (pixel!==255) {
                let pixelIdx= pixel*3;
                if (offsetShift!==0) {
                    const newPixel = Math.floor( offsetShift+(pixel*contrast));
                    pixelIdx= (newPixel > 254) ? 762 : (newPixel<0) ? 0 : newPixel*3;
                }
                this.color( colorModel[pixelIdx], colorModel[pixelIdx+1], colorModel[pixelIdx+2]);
            }
            else {
                this.color( 0,0,0);
            }`
    );


    // const standardFunc= function(pixelAry, colorModel,height, contrast, offsetShift) {
    //         const pixel= pixelAry[height-this.thread.y][this.thread.x];
    //         if (pixel!==255) {
    //             let pixelIdx= pixel*3;
    //             if (offsetShift!==0) {
    //                 const newPixel = Math.floor( offsetShift+(pixel*contrast));
    //                 pixelIdx= (newPixel > 254) ? 762 : (newPixel<0) ? 0 : newPixel*3;
    //             }
    //             this.color( colorModel[pixelIdx], colorModel[pixelIdx+1], colorModel[pixelIdx+2]);
    //         }
    //         else {
    //             this.color( 0,0,0);
    //         }

    const standRawDataTileGPU = gpu.createKernel(standardFunc,{graphical:true, dynamicOutput:true, dynamicArguments:true, tactic: 'speed'});
    
    const threeCFunc = Function(
        'redAry', 'greenAry', 'blueAry', 'use', 'contrast', 'offsetShift', 'width','height', `
        const idx= (height - this.thread.y) * width + this.thread.x;
        let r= use[0]===1 ? redAry[idx] : 0;
        let g= use[1]===1 ? greenAry[idx] : 0;
        let b= use[2]===1 ? blueAry[idx] : 0;
        if (r!==0 && offsetShift[0]!==0) {
            r= Math.floor( offsetShift[0]+(r*contrast[0]));
            r= (r > 254) ? 254 : (r<0) ? 0 : r;
        }
        if (g!==0 && offsetShift[1]!==0) {
            g= Math.floor( offsetShift[1]+(g*contrast[1]));
            g= (g > 254) ? 254 : (g<0) ? 0 : g;
        }
        if (b!==0&& offsetShift[2]!==0) {
            b= Math.floor( offsetShift[2]+(b*contrast[2]));
            b= (b > 254) ? 254 : (b<0) ? 0 : b;
        }
        this.color( r/255, g/255, b/255, 1);`
    );

    // const threeCFuncX = function(redAry, greenAry, blueAry, use, contrast, offsetShift, width,height) {
    //     const idx= (height - this.thread.y) * width + this.thread.x;
    //     let r= use[0]===1 ? redAry[idx] : 0;
    //     let g= use[1]===1 ? greenAry[idx] : 0;
    //     let b= use[2]===1 ? blueAry[idx] : 0;
    //     if (r!==0 && offsetShift[0]!==0) {
    //         r= Math.floor( offsetShift[0]+(r*contrast[0]));
    //         r= (r > 254) ? 254 : (r<0) ? 0 : r;
    //     }
    //     if (g!==0 && offsetShift[1]!==0) {
    //         g= Math.floor( offsetShift[1]+(g*contrast[1]));
    //         g= (g > 254) ? 254 : (g<0) ? 0 : g;
    //     }
    //     if (b!==0&& offsetShift[2]!==0) {
    //         b= Math.floor( offsetShift[2]+(b*contrast[2]));
    //         b= (b > 254) ? 254 : (b<0) ? 0 : b;
    //     }
    //     this.color( r/255, g/255, b/255, 1);
    // };

    const threeCRawDataTileGPU = gpu.createKernel(threeCFunc,{graphical:true, dynamicOutput:true, dynamicArguments:true, tactic: 'speed'});

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
     * @param bias
     * @param contrast
     * @param bandUse
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


    function createRawDataTile3CRGBDataGPU(pixelDataAry, width,height,bias=[.5,.5,.5], contrast=[1,1,1], bandUse={}) {
        const {useRed=true,useGreen=true,useBlue=true}= bandUse;

        if (!isArray(bias)) bias= [.5,.5,.5];
        if (!isArray(contrast)) contrast= [1,1,1];
        const offsetShift= new Float32Array(3);
        const contrast32= new Float32Array(3);
        for(let i=0;i<3; i++) {
            const offset = (127*(bias[i]-0.5)*-4);
            const shift = (127*(1-contrast[i]));
            offsetShift[i]= offset+shift;
            contrast32[i]= contrast[i];
        }

        threeCRawDataTileGPU.setOutput([width,height]);
        const use= new Uint8ClampedArray(
            [pixelDataAry[0]&&useRed?1:0, pixelDataAry[1]&&useGreen?1:0, pixelDataAry[2]&&useBlue?1:0]);
        threeCRawDataTileGPU(
            pixelDataAry[0]||new Uint8ClampedArray(1),
            pixelDataAry[1]||new Uint8ClampedArray(1),
            pixelDataAry[2]||new Uint8ClampedArray(1),
            use,
            contrast32,
            offsetShift,
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
