import {isArray, once} from 'lodash';
import {createCanvas} from '../../util/WebUtil';
import {getGpuJsImmediate} from './GpuJsConfig';
import {TILE_SIZE} from './RawDataCommon';

let gpu= undefined;
let GPU= undefined;
let onGpuJsContextLost= undefined;

/**
 * init gpu js. We hope to removed gpu.js in the next couple of years. So this is only for older systems
 */
export const initGpuJs= once(() => {

    if (!gpu) {
        GPU= getGpuJsImmediate();
        if (!GPU) {
            console.log('Gpu.js is not loaded. It should be loaded first with "await getGpuJs()" ');
            return undefined;
        }
        const initGpu= new GPU({mode:'gpu'});

        const standardFuncSimple= Function('pixelAry', 'colorModel','height',  `
                const pixel= pixelAry[height-this.thread.y-1][this.thread.x];
                let pixelIdx= pixel*3;
                this.color( colorModel[pixelIdx], colorModel[pixelIdx+1], colorModel[pixelIdx+2]);
                `
        );

        const standardFuncWithContrast= Function('pixelAry', 'colorModel','height', 'contrast', 'offsetShift', `
                const pixel= pixelAry[height-this.thread.y-1][this.thread.x];
                let pixelIdx= pixel*3;
                const newPixel = Math.floor( offsetShift+(pixel*contrast));
                pixelIdx= (newPixel > 254) ? 762 : (newPixel<0) ? 0 : newPixel*3;
                this.color( colorModel[pixelIdx], colorModel[pixelIdx+1], colorModel[pixelIdx+2]);
                `
        );

        const threeCFunc = Function(
            'redAry', 'greenAry', 'blueAry', 'use', 'contrast', 'offsetShift', 'width','height', `
            const idx= (height - this.thread.y-1) * width + this.thread.x;
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

        const hipsAnyTile= Function('colorModel', 'data','contrast', 'offsetShift', `
                     const i= ((this.constants.height-this.thread.y-1) * this.constants.width) + this.thread.x;
                     const dI=i*4;
                     let pixel= Math.trunc((data[dI]+data[dI+1]+data[dI+2])/3);
                     if (pixel===255) pixel= 254;
                     let pixelIdx= pixel*3;
                     if (offsetShift!==0) {
                         const newPixel = Math.floor( offsetShift+(pixel*contrast));
                         pixelIdx= (newPixel > 254) ? 762 : (newPixel<0) ? 0 : newPixel*3;
                     }
                     this.color( colorModel[pixelIdx], colorModel[pixelIdx+1], colorModel[pixelIdx+2],1);`);

        const baseGpuParams= {graphical:true, tactic: 'speed'};
        const imageGpuParams= {...baseGpuParams, dynamicOutput:true, dynamicArguments:true};

        const shaders= {
            standardFuncSimple: { source: standardFuncSimple, gpuParams: imageGpuParams, func: undefined, },
            standardFuncWithContrast: { source: standardFuncWithContrast, gpuParams: imageGpuParams, func: undefined, },
            threeCFunc: { source: threeCFunc, gpuParams: imageGpuParams, func: undefined, },
            hips512Tile: {
                source: hipsAnyTile,
                gpuParams: { ...baseGpuParams, constants: {width:512,height:512}, output:[512,512]},
                func: undefined,
            },
            hips862by928AllSky: {
                source: hipsAnyTile,
                gpuParams: { ...baseGpuParams, constants: {width:864,height:928}, output:[864,928], },
                func: undefined,
            },
        };

        const initShader= (name) => {
            const ref= shaders[name];
            if (ref.func) return ref.func;
            ref.func= initGpu.createKernel(ref.source,ref.gpuParams);
            ref.name= name;
            ref.func.canvas.addEventListener('webglcontextlost', (ev) => {
                ev.preventDefault();
                ref.func= undefined;
                onGpuJsContextLost?.();
                console.log(`${name}, reset gpu context`);
            }, false);
            return ref.func;
        };

        gpu= {...initGpu,
            getStandRawDataTileSimpleGPU: () => initShader('standardFuncSimple'),
            getStandRawDataTileWithContrastGPU: () => initShader('standardFuncWithContrast'),
            getThreeCRawDataTileGPU:() => initShader('threeCFunc'),
            getHips512TileGPU:() => initShader('hips512Tile'),
            getHips862by928AllSkyGPU:() => initShader('hips862by928AllSky'),
        };
    }
    return gpu;
});

export const setOnGpuJsContextLostListener= (l) => onGpuJsContextLost= l;


export async function gpujs_createRawDataTileImageRGB(colorModel, pixelData, width, height, bias=.5, contrast=1) {

    const gpu= initGpuJs();
    if (!gpu) return;
    const offset = (127*(bias-0.5)*-4);
    const shift = (127*(1-contrast));
    const offsetShift=Math.trunc(offset+shift);
    if (offsetShift===0) {
        const standRawDataTileSimpleGPU= gpu.getStandRawDataTileSimpleGPU();
        standRawDataTileSimpleGPU.setOutput([width,height]);
        standRawDataTileSimpleGPU( GPU.input(pixelData,[width,height]), colorModel, height);

        return makeRetData(standRawDataTileSimpleGPU.canvas, width, height);
    }
    else {
        const standRawDataTileWithContrastGPU= gpu.getStandRawDataTileWithContrastGPU();
        standRawDataTileWithContrastGPU.setOutput([width,height]);
        standRawDataTileWithContrastGPU(
            GPU.input(pixelData,[width,height]), colorModel, height, contrast, offsetShift);
        return makeRetData(standRawDataTileWithContrastGPU.canvas, width, height);
    }
}


export async function gpujs_createRawDataTile3CRGB(pixelDataAry, width, height, bias=[.5,.5,.5], contrast=[1,1,1], bandUse={}) {
    const gpu= initGpuJs();
    if (!gpu) return;
    const {useRed=true,useGreen=true,useBlue=true}= bandUse;
    const threeCRawDataTileGPU= gpu.getThreeCRawDataTileGPU();

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

export function gpujs_hips512TileGPU(cm, data, bias=.5, contrast=1) {
    const gpu= initGpuJs();
    if (!gpu) return;
    const offset = (127*(bias-0.5)*-4);
    const shift = (127*(1-contrast));
    const hips512TileGPU= gpu.getHips512TileGPU();
    hips512TileGPU( cm, data, contrast, offset+shift);
    return hips512TileGPU.canvas;
}

export function gpujs_hips862by928AllSkyGPU(cm, data, bias=.5, contrast=1) {
    const gpu= initGpuJs();
    if (!gpu) return;
    const offset = (127*(bias-0.5)*-4);
    const shift = (127*(1-contrast));
    const hips862by928AllSkyGPU= gpu.getHips862by928AllSkyGPU();
    hips862by928AllSkyGPU( cm, data, contrast, offset+shift);
    return hips862by928AllSkyGPU.canvas;
}



async function makeRetData(gpuCanvas,width,height)  {
    const c= createCanvas(width,height);
    if (gpuCanvas.width===width && gpuCanvas.height===height) {
        c.getContext('2d').drawImage(gpuCanvas,0,0);
    }
    else if (width===TILE_SIZE && height===TILE_SIZE) {
        c.getContext('2d').drawImage(gpuCanvas,0,0);
    }
    else {
        c.getContext('2d').drawImage(gpuCanvas,0,gpuCanvas.height-height,width,height,0,0,width,height);
    }
    return globalThis.createImageBitmap(c);
}
