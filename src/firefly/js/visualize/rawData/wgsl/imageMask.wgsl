// -------------
// WebGPU Shading Language - processing mask
// optimzation: done, very optimized
// -------------
@group(0) @binding(0) var<storage, read> pixelData : array<u32>; // each 32 bit entry contains 32 output colors, clear or set
@group(0) @binding(1) var<storage, read_write> outBuf : array<u32>; //each output 32 big contains a r,g,b,a
@group(0) @binding(2) var<uniform> color : u32;
@group(0) @binding(3) var<uniform> pixelCount : u32;
    

const NO_COLOR= 0u;
const ALPHA_SHIFT = 255u << 24u;

@compute @workgroup_size(256)
fn main(@builtin(global_invocation_id) gid: vec3<u32>) {
    let idx = gid.x;
    if (idx >= pixelCount) { return; }
    let v = pixelData[idx];
    let i= idx*32;
//  var bit: u32 = 1u;
//  for (var j = 0u; (j < 32u); j++) {
//     outBuf[i+j] = select(NO_COLOR, color, (v & bit) == 0u);
//     bit <<= 1u;
//  }
// the following does the commented out loop (above) but is more optimal

    outBuf[i+ 0]= select(NO_COLOR, color, (v & (1 <<  0)) == 0u);
    outBuf[i+ 1]= select(NO_COLOR, color, (v & (1 <<  1)) == 0u);
    outBuf[i+ 2]= select(NO_COLOR, color, (v & (1 <<  2)) == 0u);
    outBuf[i+ 3]= select(NO_COLOR, color, (v & (1 <<  3)) == 0u);
    outBuf[i+ 4]= select(NO_COLOR, color, (v & (1 <<  4)) == 0u);
    outBuf[i+ 5]= select(NO_COLOR, color, (v & (1 <<  5)) == 0u);
    outBuf[i+ 6]= select(NO_COLOR, color, (v & (1 <<  6)) == 0u);
    outBuf[i+ 7]= select(NO_COLOR, color, (v & (1 <<  7)) == 0u);
    outBuf[i+ 8]= select(NO_COLOR, color, (v & (1 <<  8)) == 0u);
    outBuf[i+ 9]= select(NO_COLOR, color, (v & (1 <<  9)) == 0u);
    outBuf[i+10]= select(NO_COLOR, color, (v & (1 << 10)) == 0u);
    outBuf[i+11]= select(NO_COLOR, color, (v & (1 << 11)) == 0u);
    outBuf[i+12]= select(NO_COLOR, color, (v & (1 << 12)) == 0u);
    outBuf[i+13]= select(NO_COLOR, color, (v & (1 << 13)) == 0u);
    outBuf[i+14]= select(NO_COLOR, color, (v & (1 << 14)) == 0u);
    outBuf[i+15]= select(NO_COLOR, color, (v & (1 << 15)) == 0u);
    outBuf[i+16]= select(NO_COLOR, color, (v & (1 << 16)) == 0u);
    outBuf[i+17]= select(NO_COLOR, color, (v & (1 << 17)) == 0u);
    outBuf[i+18]= select(NO_COLOR, color, (v & (1 << 18)) == 0u);
    outBuf[i+19]= select(NO_COLOR, color, (v & (1 << 19)) == 0u);
    outBuf[i+20]= select(NO_COLOR, color, (v & (1 << 20)) == 0u);
    outBuf[i+21]= select(NO_COLOR, color, (v & (1 << 21)) == 0u);
    outBuf[i+22]= select(NO_COLOR, color, (v & (1 << 22)) == 0u);
    outBuf[i+23]= select(NO_COLOR, color, (v & (1 << 23)) == 0u);
    outBuf[i+24]= select(NO_COLOR, color, (v & (1 << 24)) == 0u);
    outBuf[i+25]= select(NO_COLOR, color, (v & (1 << 25)) == 0u);
    outBuf[i+26]= select(NO_COLOR, color, (v & (1 << 26)) == 0u);
    outBuf[i+27]= select(NO_COLOR, color, (v & (1 << 27)) == 0u);
    outBuf[i+28]= select(NO_COLOR, color, (v & (1 << 28)) == 0u);
    outBuf[i+29]= select(NO_COLOR, color, (v & (1 << 29)) == 0u);
    outBuf[i+30]= select(NO_COLOR, color, (v & (1 << 30)) == 0u);
    outBuf[i+31]= select(NO_COLOR, color, (v & (1 << 31)) == 0u);
}