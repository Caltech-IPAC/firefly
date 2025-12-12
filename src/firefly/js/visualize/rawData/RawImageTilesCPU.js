
export async function createWorkerTileWithCPU(inData, colorModel, isThreeColor) {
    const {width,height, pixelData3C, pixelDataStandard}= inData;

    let workerBitMapTile= isThreeColor ?
        createRawDataTile3CRGBData(pixelData3C, width,height) :
        createRawDataTileImageRGBData(colorModel, pixelDataStandard, width,height);


    const imageData= new ImageData(new Uint8ClampedArray(workerBitMapTile),width,height);
    workerBitMapTile= await globalThis.createImageBitmap(imageData);
    return { ...inData, workerBitMapTile };
}


function createRawDataTileImageRGBData(colorModel, pixelData, width,height) {
    const len= width*height*4;
    const data= new Uint8ClampedArray(len);
    let pixel, cmPixel;
    for(let i= 0, j=0; i<len; i+=4, j++) {
        pixel= pixelData[j];
        cmPixel= pixel*3;
        if (pixel!==255) {
            data[i]  = colorModel[cmPixel];
            data[i+1]= colorModel[cmPixel+1];
            data[i+2]= colorModel[cmPixel+2];
            data[i+3]= 255;
        }
        else {
            data[i]  = 0;
            data[i+1]= 0;
            data[i+2]= 0;
            data[i+3]= 254;
        }
    }
    return data.buffer;
}

function createRawDataTile3CRGBData(pixelDataAry, width,height) {
    const len= width*height*4;
    const data= new Uint8ClampedArray(len);

    const rIdx= 0;
    const gIdx= 1;
    const bIdx= 2;
    const hasR= Boolean(pixelDataAry[rIdx]);
    const hasG= Boolean(pixelDataAry[gIdx]);
    const hasB= Boolean(pixelDataAry[bIdx]);

    for(let i= 0, j=0; j<len; i+=4, j++) {
        data[i]  = hasR ? pixelDataAry[rIdx][j] : 0;
        data[i+1]= hasG ? pixelDataAry[gIdx][j] : 0;
        data[i+2]= hasB ? pixelDataAry[bIdx][j] : 0;
        data[i+3]= 255;
    }
    return data.buffer;
}

