
import {isArray, once} from 'lodash';
import {toRGB} from '../../util/Color';
import {synchronizeAsyncFunctionById} from '../../util/SynchronizeAsync';
import wgslSingleBandSource from './wgsl/imageSingleBand.wgsl?raw';
import wgsl3ColorSource from './wgsl/image3ColorBand.wgsl?raw';
import wgslImageRemapColor from './wgsl/imageRemapColor.wgsl?raw';
import wgslImageMask from './wgsl/imageMask.wgsl?raw';

/*
 * This file uses the new WebGPU graphics api. A GPU job is done by loading the wgsl language file the process
 * the data in the gpu. Each job has its own wgsl file.
 *
 * Firefly uses it to render single band image, 3 color images, mask images and hips colored tiles.
 *
 * Image come from the server as a byte array of stretched FITS data. This code converts them into a bitmap tha
 * is rendered to a canvas.
 *
 * HiPS are recolored by taking the byte array from the png file and remapping the colors into a new bitmap.
 *
 * Most of the js script code is just the setup code to call the wgsl function in the gpu
 *   - load the buffers
 *   - load the wsgl file
 *   - map the buffers to the gpu
 *   - run and get the results out of the output buffer
 *   - create a bitmap from the output
 *
 * Notes -
 *   - Since the key functions in WEBGpu are async then all the jobs are async.
 *   - This file can be used in either the main thread or a worker
 */


const modules= {};
let webGpuDevice= undefined;

async function initWebGPUDevice() {
    if (!webGpuDevice) {
        webGpuDevice= await synchronizeAsyncFunctionById('webGpuDevice', () => initDevice());
       if (!webGpuDevice) console.log('warning: web gpu cannot initialize webGPU device');
    }
    return webGpuDevice;
}

function destroyWebGPUDevice() {
    if (webGpuDevice) webGpuDevice.destroy();
    webGpuDevice = undefined;
}


async function initDevice() {
    const adapter = await navigator?.gpu?.requestAdapter();
    if (!adapter) return undefined;
    const newDevice= await adapter.requestDevice();

    modules.getWgslSingleBand= once(() => ({module:newDevice.createShaderModule({ code: wgslSingleBandSource }), wgSize:256}));
    modules.getWgsl3ColorBand= once(() => ({module:newDevice.createShaderModule({ code: wgsl3ColorSource }), wgSize:256}));
    modules.getWgslImageRemapColor= once(() => ({module:newDevice.createShaderModule({ code: wgslImageRemapColor}), wgSize:256}));
    modules.getWgslImageMask= once(() => ({module:newDevice.createShaderModule({ code: wgslImageMask}), wgSize:256}));

    newDevice.lost.then( (info) => {
        console.error(`WebGPU device was lost: ${info.message}`);
        console.error('Attempting reinit');
        destroyWebGPUDevice();
    });
    return newDevice;
}





function getOffsetShift(bias,contrast) {
    const offset = (127*(bias-0.5)*-4);
    const shift = (127*(1-contrast));
    return Math.trunc(offset+shift);
}

function makeBindGroup(device, pipeline, buffers) {
    return  device.createBindGroup(
        {
            layout: pipeline.getBindGroupLayout(0),
            entries: buffers.map((buffer, idx) => ({binding: idx, resource: {buffer}}) )
        });
}

function makeFloatUniform(device, uniformAry) {
    const uniformData = Float32Array.of(...uniformAry);
    const uniformBuf = device.createBuffer({
        size: uniformData.byteLength,
        usage: GPUBufferUsage.UNIFORM | GPUBufferUsage.COPY_DST
    });
    device.queue.writeBuffer(uniformBuf, 0, uniformData);
    return uniformBuf;
}

function makeUnsignedIntUniform(device, uniformAry) {
    const uniformData = Uint32Array.of(...uniformAry);
    const uniformBuf = device.createBuffer({
        size: uniformData.byteLength,
        usage: GPUBufferUsage.UNIFORM | GPUBufferUsage.COPY_DST
    });
    device.queue.writeBuffer(uniformBuf, 0, uniformData);
    return uniformBuf;
}


function makePixelBuf(device, pixelAry) {
    const mapLength= (pixelAry.length % 4) ? pixelAry.length + (4-(pixelAry.length % 4)) : pixelAry.length;
    const pixelBuf = device.createBuffer({
        mappedAtCreation: true,
        size: mapLength, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_DST
    });
    new Uint8ClampedArray(pixelBuf.getMappedRange()).set(pixelAry);
    pixelBuf.unmap(); // detaches the CPU-side view of the GPU buffer
    return pixelBuf;
}

function makeFloatBuf(device, valueAry) {
    const valueAry32 = Float32Array.of(...valueAry);
    const buf = device.createBuffer({
        size: valueAry32.byteLength, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_DST
    });
    device.queue.writeBuffer(buf, 0, valueAry32);
    return buf;
}

function makeUIntBuf(device, valueAry) {
    const valueAry32 = Uint32Array.of(...valueAry);
    const buf = device.createBuffer({
        size: valueAry32.byteLength, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_DST
    });
    device.queue.writeBuffer(buf, 0, valueAry32);
    return buf;
}


function makeEncoder(device, bindGroup, pipeline, count, wgSize) {
    const encoder = device.createCommandEncoder();
    const pass = encoder.beginComputePass();
    pass.setPipeline(pipeline);
    pass.setBindGroup(0, bindGroup);
    pass.dispatchWorkgroups(Math.ceil((count) / wgSize));
    pass.end();
    return encoder;
}

async function makeBitmapFromBuffer(readBuf,width,height) {
    await readBuf.mapAsync(GPUMapMode.READ);
    const data= new ImageData(new Uint8ClampedArray(readBuf.getMappedRange()), width, height);
    const bitMap= await globalThis.createImageBitmap(data,
    {
      colorSpaceConversion: 'none', // Do not let the browser "correct" it
      premultiplyAlpha: 'none'      // Keep raw color values intact
    }
    );
    readBuf.unmap();
    return bitMap;
}

function runIt(device, encoder, outBuf, outSize) {
    const readBuf = device.createBuffer({ size: outSize, usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.MAP_READ });
    encoder.copyBufferToBuffer(outBuf, 0, readBuf, 0, outSize);
    device.queue.submit([encoder.finish()]);
    return readBuf;
}

function destroyBuffers(buffers=[]) {
    for (let i = 0; i < buffers.length; i++) {
        buffers[i]?.destroy();
        buffers[i]= undefined;
    }
}


async function submitAndGetResults(device,pipeline,bindGroup, outBuf,width,height,wgSize,buffers) {
    const encoder= makeEncoder(device, bindGroup, pipeline, width*height, wgSize);
    const readBuf= runIt(device, encoder, outBuf, width * height * 4);
    const bitmap= makeBitmapFromBuffer(readBuf,width,height);
    destroyBuffers(buffers);
    return bitmap;
}


/**
 *
 * @param {Uint32Array} colorModelU32
 * @param {Uint8ClampedArray} pixelAry
 * @param {number} width
 * @param {number} height
 * @param {number} bias
 * @param {number} contrast
 * @return {Promise<ArrayBuffer|SharedArrayBuffer>}
 */
export async function processSingleBandTileViaWebGPU(colorModelU32, pixelAry, width, height, bias=.5, contrast= 1.0) {
    const device= await initWebGPUDevice();
    const colorBuf = device.createBuffer({
        size: colorModelU32.byteLength, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_DST
    });
    device.queue.writeBuffer(colorBuf, 0, colorModelU32);

    const pixelBuf= makePixelBuf(device, pixelAry);
    const outBuf = device.createBuffer({ size: width * height * 4, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_SRC });
    const uniformBuf = makeFloatUniform(device, [contrast, getOffsetShift(bias,contrast)]);

    const {module,wgSize}= modules.getWgslSingleBand();
    const pipeline = device.createComputePipeline({ layout: 'auto', compute: { module, entryPoint: 'main' }
    });

    const buffers= [colorBuf, pixelBuf, outBuf, uniformBuf];
    const bindGroup= makeBindGroup(device, pipeline, buffers);

    return submitAndGetResults(device, pipeline, bindGroup, outBuf,width,height,wgSize, buffers);
}

/**
 *
 * @param {Uint32Array} colorModelU32
 * @param {Uint8ClampedArray} colorRGBAAry - array of repeating r,g,b,a
 * @param {number} width
 * @param {number} height
 * @param {number} bias
 * @param {number} contrast
 * @return {Promise<ImageBitmap>}
 */
export async function remapColorsViaWebGpu(colorModelU32, colorRGBAAry, width, height, bias=.5, contrast= 1.0) {
    const device = await initWebGPUDevice();
    const colorBuf = device.createBuffer({
        size: colorModelU32.byteLength, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_DST
    });
    device.queue.writeBuffer(colorBuf, 0, colorModelU32);

    const colorRGBABuf= makePixelBuf(device, colorRGBAAry);
    const outBuf = device.createBuffer({ size: colorRGBAAry.length, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_SRC });
    const uniformBuf = makeFloatUniform(device, [contrast/3, getOffsetShift(bias,contrast)]);

    const {module,wgSize}= modules.getWgslImageRemapColor();
    const pipeline = device.createComputePipeline({ layout: 'auto', compute: { module, entryPoint: 'main' }
    });

    const buffers= [colorBuf, colorRGBABuf, outBuf, uniformBuf];
    const bindGroup= makeBindGroup(device, pipeline, buffers);
    return submitAndGetResults(device, pipeline, bindGroup, outBuf,width,height, wgSize, buffers);
}


/**
 * Create a mask  tile
 * @param {String} maskColor
 * @param {Uint8ClampedArray} pixelData - each byte in pixelData represents 8 pixels, so the data is compressed into bits
 * @param {number} width
 * @param {number} height
 * @return {Promise<ImageBitmap>}
 */

export async function processMaskTileViaWebGPU(maskColor, pixelData,width,height) {
    const device = await initWebGPUDevice();
    const [r,g,b]= toRGB(maskColor);
    const color= 255<<24 | b<<16 | g<<8 | r; // Pack into little-endian, byte array will be r,g,b,a so write a,b,g,r

    const pixelBuf= makePixelBuf(device, pixelData);
    const outBuf = device.createBuffer({ size: width*height*4, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_SRC });
    const colorBuf = makeUnsignedIntUniform(device, [color]);
    const outputPixelsBuf = makeUnsignedIntUniform(device, [width * height]);
    const {module,wgSize}= modules.getWgslImageMask();
    const pipeline = device.createComputePipeline({ layout: 'auto', compute: { module, entryPoint: 'main' } });
    const buffers= [pixelBuf, outBuf, colorBuf, outputPixelsBuf];
    const bindGroup= makeBindGroup(device, pipeline, buffers);
    const encoder= makeEncoder(device, bindGroup, pipeline, pixelData.length / 4, wgSize);
    const readBuf= runIt(device, encoder, outBuf, width * height * 4);
    const bitmap= makeBitmapFromBuffer(readBuf,width,height, wgSize);
    destroyBuffers(buffers);
    return bitmap;
}




/**
 *
 * @param {Array.<Uint8ClampedArray>} pixelDataAry
 * @param {number} width
 * @param {number} height
 * @param {Array.<Number>} bias
 * @param {Array.<Number>} contrast
 * @param {Array.<Boolean>} bandUse
 * @return {Promise<HTMLCanvasElement | OffscreenCanvas>}
 */
export async function process3CTileViaWebGPU(pixelDataAry, width,height,bias=[.5,.5,.5], contrast=[1,1,1], bandUse={}) {
    const device= await initWebGPUDevice();
    const {useRed=true,useGreen=true,useBlue=true}= bandUse;
    if (!isArray(bias)) bias= [.5,.5,.5];
    if (!isArray(contrast)) contrast= [1,1,1];
    const offsetShift= new Float32Array(3);
    const contrast32= new Float32Array(3);
    for(let i=0;i<3; i++) {
        offsetShift[i]= getOffsetShift(bias[i],contrast[i]);
        contrast32[i]= contrast[i];
    }

    const ensureAry= (ary) => ary||new Uint8ClampedArray(4);
    const pixelRed= ensureAry(pixelDataAry[0]);
    const pixelGreen= ensureAry(pixelDataAry[1]);
    const pixelBlue= ensureAry(pixelDataAry[2]);
    const use= Uint32Array.of(pixelDataAry[0]&&useRed?1:0, pixelDataAry[1]&&useGreen?1:0, pixelDataAry[2]&&useBlue?1:0);

    const pixelBufRed= makePixelBuf(device, pixelRed);
    const pixelBufGreen= makePixelBuf(device, pixelGreen);
    const pixelBufBlue= makePixelBuf(device, pixelBlue);
    const offsetShiftBuf= makeFloatBuf(device, offsetShift);
    const contrastBuf= makeFloatBuf(device, contrast);
    const useBuf= makeUIntBuf(device, use);
    const outBuf = device.createBuffer({ size: width * height * 4, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_SRC });


    const {module,wgSize}= modules.getWgsl3ColorBand();
    const pipeline = device.createComputePipeline({ layout: 'auto', compute: { module, entryPoint: 'main' } });

    const buffers= [ pixelBufRed, pixelBufGreen, pixelBufBlue, offsetShiftBuf, contrastBuf, useBuf, outBuf];
    const bindGroup= makeBindGroup(device, pipeline, buffers);
    return submitAndGetResults(device, pipeline, bindGroup, outBuf,width,height, wgSize, buffers);
}


