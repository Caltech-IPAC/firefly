import BrowserInfo from '../../util/BrowserInfo';
import {createCanvas} from '../../util/WebUtil.js';
import {
    gpujs_hips512TileGPU, gpujs_hips862by928AllSkyGPU, setOnGpuJsContextLostListener
} from '../rawData/GpuJsJobs';
import {getColorModel, getColorModelByGPUType} from '../rawData/ColorTable.js';
import {once} from 'lodash';
import {remapColorsViaWebGpu} from '../rawData/WebGpuJobs';
import {removeNonNativeCachedTiles} from './HiPSTileCache.js';

export const logGpuState= once(() => {
    const gpuType= BrowserInfo.supportsWebGpu() ? 'webgpu' : 'webgl (using gpu.js)';
    console.log(`HiPS color gpu : ${gpuType}`);
});


export const getHipsColorOps= once(() => {

    setOnGpuJsContextLostListener(removeNonNativeCachedTiles);
    logGpuState();

    const findPixelIdx= function(pixel, contrast, offsetShift) {
        if (pixel===255) pixel= 254;
        let pixelIdx= pixel*3;
        if (offsetShift!==0) {
            const newPixel = Math.floor( offsetShift+(pixel*contrast));
            pixelIdx= (newPixel > 254) ? 762 : (newPixel<0) ? 0 : newPixel*3;
        }
        return pixelIdx;
    };

    const changeHiPSColor= async (image, ct, bias=.5, contrast=1) => {
        const width= image.width;
        const height= image.height;
        const c= createCanvas(width,height);
        const outCtx= c.getContext('2d',{willReadFrequently:true});
        let inCtx;
        if (image instanceof HTMLCanvasElement) {
            inCtx= image.getContext('2d', {willReadFrequently:true});
        }
        else {
            outCtx.drawImage(image,0,0);
            inCtx= outCtx;
        }

        const getInData= (x,y,w,h) => inCtx.getImageData(x,y,w,h).data;

        const cm= getColorModelByGPUType(ct);

        if (width===512 && height===512) {
            if (BrowserInfo.supportsWebGpu()) {
                const bitMap= await remapColorsViaWebGpu(cm,getInData(0,0,width,height),width,height,bias,contrast);
                outCtx.drawImage(bitMap,0,0);
            }
            else {
                const gpuCanvas= gpujs_hips512TileGPU(cm,getInData(0,0,width,height),bias,contrast);
                outCtx.drawImage(gpuCanvas,0,gpuCanvas.height-512,width,height,0,0,width,height);
            }
        }
        else if (width===1728 && height===1856) {
            const updateAllSky= async (gx,gy) => {
                const w2=864;
                const h2=928;
                if (BrowserInfo.supportsWebGpu()) {
                    const bitMap= await remapColorsViaWebGpu(cm,getInData(w2*gx,h2*gy,w2,h2),w2,h2,bias,contrast);
                    outCtx.drawImage(bitMap, 0,0,w2,h2, w2*gx,h2*gy,w2,h2);
                }
                else {
                    const gpuCanvas= gpujs_hips862by928AllSkyGPU(cm, getInData(w2*gx,h2*gy,w2,h2), bias,contrast);
                    outCtx.drawImage(gpuCanvas, 0,gpuCanvas.height-h2,w2,h2, w2*gx,h2*gy,w2,h2);
                }
            };

            const pAry= [];
            for(let i=0; (i<2); i++) {
                for(let j=0; (j<2); j++) {
                    pAry.push(updateAllSky(i, j));
                }
            }
            await Promise.all(pAry);
        }
        else { // non-GPU approach
            const cm= getColorModel(ct);
            let pixel, pixelIdx;
            const imData= inCtx.getImageData(0,0,width,height);
            const data= imData.data;
            const len= imData.data.length;
            for(let i= 0, j=0; i<len; i+=4, j++) {
                const offset = (127*(bias-0.5)*-4);
                const shift = (127*(1-contrast));
                const offsetShift= offset+shift;
                pixel= Math.trunc((data[i]+data[i+1]+data[i+2])/3);
                pixelIdx= findPixelIdx(pixel,contrast,offsetShift);
                data[i] = cm[pixelIdx];
                data[i+1] = cm[pixelIdx + 1];
                data[i+2] = cm[pixelIdx + 2];
            }
            outCtx.putImageData(imData,0,0);
        }
        return c;
    };
    return {changeHiPSColor};
});
