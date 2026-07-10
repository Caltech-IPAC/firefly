// -------------
// WebGPU Shading Language - average r,g,b to make an index and remap color to a false color model
// optimzations: still todo
//          - possibly work on 4 indexes at a time using vec4<u32>, could be significant speed up
// -------------

struct Pixels { p: array<u32>, }

struct Params {
    contrastThird    : f32,
    offsetShift : f32,
}

@group(0) @binding(0) var<storage, read> colorModel : array<u32>; // color lookup table
@group(0) @binding(1) var<storage, read> pixelAry : Pixels; // each 32 bit entry contains 4 8 bit pixels
@group(0) @binding(2) var<storage, read_write> outBuf : array<u32>; //each output 32 big contains a r,g,b,a
@group(0) @binding(3) var<uniform>  params : Params;

@compute @workgroup_size(256)
fn main(@builtin(global_invocation_id) gid: vec3<u32>) {
    let idx = gid.x;
    if (idx >= arrayLength(&pixelAry.p)) { return; }
    let v= pixelAry.p[idx];

    let r= f32(v & 255u);
    let g= f32((v>>8) & 255u);
    let b= f32((v>>16) & 255u);
    let avg= (r+g+b) * params.contrastThird + params.offsetShift;
    outBuf[idx] = colorModel[u32(clamp(avg,0,254))];
}