import {createCanvas} from '../../util/WebUtil.js';
import {getColorModel} from '../rawData/rawAlgorithm/ColorTable.js';
import {once} from 'lodash';
import {removeNonNativeCachedTiles} from './HiPSTileCache.js';



export const getHipsColorOps= once((GPU) => {

    const gpu = new GPU({mode: 'gpu'});

    const findPixelIdx= function(pixel, contrast, offsetShift) {
        if (pixel===255) pixel= 254;
        let pixelIdx= pixel*3;
        if (offsetShift!==0) {
            const newPixel = Math.floor( offsetShift+(pixel*contrast));
            pixelIdx= (newPixel > 254) ? 762 : (newPixel<0) ? 0 : newPixel*3;
        }
        return pixelIdx;
    };


    // const hipsAnyTileSAVE= function(colorModel, data, contrast, offsetShift) {
    //     const i= ((this.constants.height-this.thread.y-1) * this.constants.width) + this.thread.x;
    //     const dI=i*4;
    //     let pixel= Math.trunc((data[dI]+data[dI+1]+data[dI+2])/3);
    //     if (pixel===255) pixel= 254;
    //     let pixelIdx= pixel*3;
    //     if (offsetShift!==0) {
    //         const newPixel = Math.floor( offsetShift+(pixel*contrast));
    //         pixelIdx= (newPixel > 254) ? 762 : (newPixel<0) ? 0 : newPixel*3;
    //     }
    //     this.color( colorModel[pixelIdx], colorModel[pixelIdx+1], colorModel[pixelIdx+2],1);
    // };

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


    let hips512TileGPU;
    let hips862by928AllSkyGPU;

    function createKernals() {
        hips512TileGPU= gpu.createKernel(hipsAnyTile,
            {
                graphical:true,
                // dynamicArguments:true,
                tactic: 'speed',
                constants: {width:512,height:512},
                output:[512,512],
            });

        // hips1728by1856AllSkyGPU= gpu.createKernel(hipsAnyTile,
        //     {
        //         graphical:true,
        //         dynamicArguments:true,
        //         tactic: 'speed',
        //         constants: {width:1728,height:1856},
        //         output:[1728,1856],
        //     });

        hips862by928AllSkyGPU= gpu.createKernel(hipsAnyTile,
            {
                graphical:true,
                // dynamicArguments:true,
                tactic: 'speed',
                constants: {width:864,height:928},
                output:[864,928],
            });
    }

    createKernals();
    const gpuCanvas = hips512TileGPU.canvas;
    gpuCanvas.addEventListener('webglcontextlost', (ev) => {
        ev.preventDefault();
        createKernals();
        removeNonNativeCachedTiles();
        console.log('reset gpu context');
    }, false);


    const changeHiPSColor= (image, ct, bias=.5, contrast=1) => {
        const width= image.width;
        const height= image.height;
        const c= createCanvas(width,height);
        const outCtx= c.getContext('2d');
        let inCtx;
        if (image instanceof HTMLCanvasElement) {
            inCtx= image.getContext('2d');
        }
        else {
            outCtx.drawImage(image,0,0);
            inCtx= outCtx;
        }

        const getInData= (x,y,w,h) => inCtx.getImageData(x,y,w,h).data;

        const cm= getColorModel(ct);
        const offset = (127*(bias-0.5)*-4);
        const shift = (127*(1-contrast));
        const offsetShift= offset+shift;

        if (width===512 && height===512) {
            hips512TileGPU( cm, getInData(0,0,width,height), contrast, offset+shift);
            const gpuCanvas= hips512TileGPU.canvas;
            outCtx.drawImage(hips512TileGPU.canvas,0,gpuCanvas.height-512,width,height,0,0,width,height);
        }
        else if (width===1728 && height===1856) {
            const updateAllSky= (gx,gy) => {
                const w2=864;
                const h2=928;
                hips862by928AllSkyGPU(cm, getInData(w2*gx,h2*gy,w2,h2), contrast, offset+shift);
                outCtx.drawImage(hips862by928AllSkyGPU.canvas,
                    0,hips862by928AllSkyGPU.canvas.height-h2,w2,h2,
                    w2*gx,h2*gy,w2,h2);
            };

            for(let i=0; (i<2); i++) {
                for(let j=0; (j<2); j++) {
                    updateAllSky(i, j);
                }
            }
        }
        else { // non-GPU approach
            let pixel, pixelIdx;
            const imData= inCtx.getImageData(0,0,width,height);
            const data= imData.data;
            const len= imData.data.length;
            for(let i= 0, j=0; i<len; i+=4, j++) {
                pixel= Math.trunc((data[i]+data[i+1]+data[i+2])/3);
                pixelIdx= findPixelIdx(pixel,contrast,offsetShift);
                data[i] = cm[pixelIdx] * 255;
                data[i+1] = cm[pixelIdx + 1] * 255;
                data[i+2] = cm[pixelIdx + 2] * 255;
            }
            outCtx.putImageData(imData,0,0);
        }
        return c;
    };
    return {changeHiPSColor};
});
