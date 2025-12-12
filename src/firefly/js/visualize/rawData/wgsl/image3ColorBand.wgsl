// -------------
// WebGPU Shading Language - processing three color images, a r,g,b array into a image array
// optimzation: mostly done
// -------------

@group(0) @binding(0) var<storage, read> redAry : array<u32>;
@group(0) @binding(1) var<storage, read> greenAry : array<u32>;
@group(0) @binding(2) var<storage, read> blueAry : array<u32>;
@group(0) @binding(3) var<storage, read> offsetShiftAry : array<f32>;
@group(0) @binding(4) var<storage, read> contrastAry : array<f32>;
@group(0) @binding(5) var<storage, read> useAry : array<u32>;
@group(0) @binding(6) var<storage, read_write> outBuf : array<u32>;


const ALPHA_SHIFT = vec4<u32>(255u << 24u);
const SCALE_MIN= vec4<i32>(0);
const SCALE_MAX=vec4<i32>(255);
const SHIFT16= vec4<u32>(16u);
const SHIFT8= vec4<u32>(8u);


/**
 * process 4 values at one time, using vector opperation, return a vector
 * each v has 4 bytes, each with band value
 */
fn processBand(v:u32, band:u32) -> vec4<u32> {
    if (useAry[band]==0) {
       return vec4<u32>(0); // if band not used return a vector of all 0
    }
    let vals = vec4<u32>( (v>>  0u) & 255u, (v>>  8u) & 255u, (v>> 16u) & 255u, (v>> 24u) & 255u); // extract out 4 pixels into a vector
    let scaled= vec4<f32>(vals) * contrastAry[band] + offsetShiftAry[band]; // scale all 4 values
    return vec4<u32>(clamp(vec4<i32>(floor(scaled)), SCALE_MIN, SCALE_MAX)); // clamp all 4 value between 0 & 255
}


@compute @workgroup_size(256)
fn main(@builtin(global_invocation_id) gid: vec3<u32>) {
    let idx = gid.x;

    let r= processBand(redAry[idx],0);
    let g= processBand(greenAry[idx],1);
    let b= processBand(blueAry[idx],2);

    let packed = ALPHA_SHIFT | (b << SHIFT16) | (g << SHIFT8) | r;
    let outIdx= idx*4;
    outBuf[outIdx + 0u] = packed.x;
    outBuf[outIdx + 1u] = packed.y;
    outBuf[outIdx + 2u] = packed.z;
    outBuf[outIdx + 3u] = packed.w;
}