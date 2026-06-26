// -------------
// WebGPU Shading Language - processing mask
// optimzation: done, very optimized
// -------------
@group(0) @binding(0) var<storage, read> pixelData : array<u32>; // each 32 bit entry contains 32 output colors, clear or set
@group(0) @binding(1) var<storage, read_write> outBuf : array<u32>; //each output 32 big contains a r,g,b,a
@group(0) @binding(2) var<uniform> color : u32;
@group(0) @binding(3) var<uniform> outputPixels : u32;


const NO_COLOR= 0u;

@compute @workgroup_size(256)
fn main(@builtin(global_invocation_id) gid: vec3<u32>) {
    let idx = gid.x;
    if (idx >= arrayLength(&pixelData)) { return; }
    let baseOutputIdx= idx*32;
    if (baseOutputIdx >= outputPixels) { return; }
    let v = pixelData[idx];
    let remainingPixels = min(32u, outputPixels - baseOutputIdx);

    if (remainingPixels == 32u) { // Fast path for full 32-bit groups (most common)
       outBuf[baseOutputIdx+ 0]= select(NO_COLOR, color, (v & (1 <<  0)) == 0u);
       outBuf[baseOutputIdx+ 1]= select(NO_COLOR, color, (v & (1 <<  1)) == 0u);
       outBuf[baseOutputIdx+ 2]= select(NO_COLOR, color, (v & (1 <<  2)) == 0u);
       outBuf[baseOutputIdx+ 3]= select(NO_COLOR, color, (v & (1 <<  3)) == 0u);
       outBuf[baseOutputIdx+ 4]= select(NO_COLOR, color, (v & (1 <<  4)) == 0u);
       outBuf[baseOutputIdx+ 5]= select(NO_COLOR, color, (v & (1 <<  5)) == 0u);
       outBuf[baseOutputIdx+ 6]= select(NO_COLOR, color, (v & (1 <<  6)) == 0u);
       outBuf[baseOutputIdx+ 7]= select(NO_COLOR, color, (v & (1 <<  7)) == 0u);
       outBuf[baseOutputIdx+ 8]= select(NO_COLOR, color, (v & (1 <<  8)) == 0u);
       outBuf[baseOutputIdx+ 9]= select(NO_COLOR, color, (v & (1 <<  9)) == 0u);
       outBuf[baseOutputIdx+10]= select(NO_COLOR, color, (v & (1 << 10)) == 0u);
       outBuf[baseOutputIdx+11]= select(NO_COLOR, color, (v & (1 << 11)) == 0u);
       outBuf[baseOutputIdx+12]= select(NO_COLOR, color, (v & (1 << 12)) == 0u);
       outBuf[baseOutputIdx+13]= select(NO_COLOR, color, (v & (1 << 13)) == 0u);
       outBuf[baseOutputIdx+14]= select(NO_COLOR, color, (v & (1 << 14)) == 0u);
       outBuf[baseOutputIdx+15]= select(NO_COLOR, color, (v & (1 << 15)) == 0u);
       outBuf[baseOutputIdx+16]= select(NO_COLOR, color, (v & (1 << 16)) == 0u);
       outBuf[baseOutputIdx+17]= select(NO_COLOR, color, (v & (1 << 17)) == 0u);
       outBuf[baseOutputIdx+18]= select(NO_COLOR, color, (v & (1 << 18)) == 0u);
       outBuf[baseOutputIdx+19]= select(NO_COLOR, color, (v & (1 << 19)) == 0u);
       outBuf[baseOutputIdx+20]= select(NO_COLOR, color, (v & (1 << 20)) == 0u);
       outBuf[baseOutputIdx+21]= select(NO_COLOR, color, (v & (1 << 21)) == 0u);
       outBuf[baseOutputIdx+22]= select(NO_COLOR, color, (v & (1 << 22)) == 0u);
       outBuf[baseOutputIdx+23]= select(NO_COLOR, color, (v & (1 << 23)) == 0u);
       outBuf[baseOutputIdx+24]= select(NO_COLOR, color, (v & (1 << 24)) == 0u);
       outBuf[baseOutputIdx+25]= select(NO_COLOR, color, (v & (1 << 25)) == 0u);
       outBuf[baseOutputIdx+26]= select(NO_COLOR, color, (v & (1 << 26)) == 0u);
       outBuf[baseOutputIdx+27]= select(NO_COLOR, color, (v & (1 << 27)) == 0u);
       outBuf[baseOutputIdx+28]= select(NO_COLOR, color, (v & (1 << 28)) == 0u);
       outBuf[baseOutputIdx+29]= select(NO_COLOR, color, (v & (1 << 29)) == 0u);
       outBuf[baseOutputIdx+30]= select(NO_COLOR, color, (v & (1 << 30)) == 0u);
       outBuf[baseOutputIdx+31]= select(NO_COLOR, color, (v & (1 << 31)) == 0u);
    }
    else {
       for (var j = 0u; (j < remainingPixels); j++) {
            outBuf[baseOutputIdx+j] = select(NO_COLOR, color, (v & (1u << j)) == 0u);
       }
    }

}