// -------------
// WebGPU Shading Language - processing single band, false color images
// optimzation: mostly done
// -------------

struct Pixels { p: array<u32>, }

struct Params {
    contrast    : f32,
    offsetShift : f32,
}

@group(0) @binding(0) var<storage, read> colorModelPacked : array<u32>; // color lookup table
@group(0) @binding(1) var<storage, read> pixelAry : Pixels; // each 32 bit entry contains 4 8 bit pixels
@group(0) @binding(2) var<storage, read_write> outBuf : array<u32>; //each output 32 big contains a r,g,b,a
@group(0) @binding(3) var<uniform>  params : Params;

const SCALE_MIN= vec4<i32>(0);
const SCALE_MAX=vec4<i32>(254);
const NAN_PIX=vec4<u32>(255u);
const NAN_PIX_IDX=vec4<u32>(255u);

@compute @workgroup_size(256)
fn main(@builtin(global_invocation_id) gid: vec3<u32>) {
    let idx = gid.x;
    let numPixels = arrayLength(&pixelAry.p) * 4u;
    let outBaseIdx= idx*4;

    if (outBaseIdx >= numPixels) { return; }
    let outBufLength= arrayLength(&outBuf);
    let v= pixelAry.p[idx];

    // now we are using vector opperations, so I am working with 4 values at a time
    let pixels = vec4<u32>( (v>>  0u) & 255u, (v>>  8u) & 255u, (v>> 16u) & 255u, (v>> 24u) & 255u); // extract out 4 pixels into a vector
    let scaled = vec4<f32>(pixels) * params.contrast + params.offsetShift; // vector and scalar math produces a vector
    let processedIdx= vec4<u32>(clamp(vec4<i32>(scaled), SCALE_MIN, SCALE_MAX)); // make sure all indices fall between 0 and 254
    let colorIdx = select(processedIdx, NAN_PIX_IDX, pixels == NAN_PIX); // // vector compare -> produces a vec4<bool> -> then a vec of indexes

    let color= vec4<u32>(
        colorModelPacked[colorIdx.x], colorModelPacked[colorIdx.y],
        colorModelPacked[colorIdx.z], colorModelPacked[colorIdx.w]
    );

    let outPixelCnt = min(4u, outBufLength- outBaseIdx);
    if (outPixelCnt == 4u) {
       outBuf[outBaseIdx + 0u] = color.x;
       outBuf[outBaseIdx + 1u] = color.y;
       outBuf[outBaseIdx + 2u] = color.z;
       outBuf[outBaseIdx + 3u] = color.w;
    }
    else {
       if (outPixelCnt >= 1u) {outBuf[outBaseIdx + 0u] = color.x;}
       if (outPixelCnt >= 2u) {outBuf[outBaseIdx + 1u] = color.y;}
       if (outPixelCnt >= 3u) {outBuf[outBaseIdx + 2u] = color.z;}
    }
}