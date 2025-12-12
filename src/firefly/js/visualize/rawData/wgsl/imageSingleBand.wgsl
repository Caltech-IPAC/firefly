// -------------
// WebGPU Shading Language - processing single band, false color images
// optimzation: mostly done
// -------------

struct Pixels { p: array<u32>, }

struct Params {
    contrast    : f32,
    offsetShift : f32,
}

@group(0) @binding(0) var<storage, read> colorModel : array<u32>; // color lookup table
@group(0) @binding(1) var<storage, read> pixelAry : Pixels; // each 32 bit entry contains 4 8 bit pixels
@group(0) @binding(2) var<storage, read_write> outBuf : array<u32>; //each output 32 big contains a r,g,b,a
@group(0) @binding(3) var<uniform>  params : Params;

const ALPHA_SHIFT = vec4<u32>(255u << 24u);
const SCALE_MIN= vec4<i32>(0);
const SCALE_MAX=vec4<i32>(254);
const NAN_PIX=vec4<u32>(255u);
const NAN_PIX_IDX=vec4<u32>(765u);
const SHIFT16= vec4<u32>(16u);
const SHIFT8= vec4<u32>(8u);

@compute @workgroup_size(256)
fn main(@builtin(global_invocation_id) gid: vec3<u32>) {
    let idx = gid.x;
    let numPixels = arrayLength(&pixelAry.p) * 4u;
    let outBaseIdx= idx*4;

    if (outBaseIdx >= numPixels) { return; }
    let v= pixelAry.p[idx];

    // now we are using vector opperations, so I am working with 4 values at a time
    let pixels = vec4<u32>( (v>>  0u) & 255u, (v>>  8u) & 255u, (v>> 16u) & 255u, (v>> 24u) & 255u); // extract out 4 pixels into a vector
    let scaled = vec4<f32>(pixels) * params.contrast + params.offsetShift; // vector and scalar math produces a vector
    let clamped = clamp(vec4<i32>(floor(scaled)), SCALE_MIN, SCALE_MAX);
    let processedIdx = vec4<u32>(clamped * vec4<i32>(3)); //all 4 clamped value multipled by three, before dealing with 255
    let colorIdx = select(processedIdx, NAN_PIX_IDX, pixels == NAN_PIX); // // vector compare, produces a vec4<bool>, real idx of color map

    let rIdx = colorIdx + 0u;
    let gIdx = colorIdx + 1u;
    let bIdx = colorIdx + 2u;

    let r = vec4<u32>( colorModel[rIdx.x], colorModel[rIdx.y], colorModel[rIdx.z], colorModel[rIdx.w] );
    let g = vec4<u32>( colorModel[gIdx.x], colorModel[gIdx.y], colorModel[gIdx.z], colorModel[gIdx.w] );
    let b = vec4<u32>( colorModel[bIdx.x], colorModel[bIdx.y], colorModel[bIdx.z], colorModel[bIdx.w] );

    let color= ALPHA_SHIFT | (b << SHIFT16) | (g << SHIFT8) | r; // Pack into little-endian, byte array will be r,g,b,a so write a,b,g,r
    outBuf[outBaseIdx + 0u] = color.x;
    outBuf[outBaseIdx + 1u] = color.y;
    outBuf[outBaseIdx + 2u] = color.z;
    outBuf[outBaseIdx + 3u] = color.w;
}