// -------------
// WebGPU Shading Language - processing mask
// optimzation: done, very optimized
// -------------
@group(0) @binding(0) var<storage, read> pixelData : array<u32>; // each 32 bit entry contains 32 output colors, clear or set
@group(0) @binding(1) var<storage, read_write> outBuf : array<u32>; //each output 32 big contains a r,g,b,a
@group(0) @binding(2) var<uniform> color : u32;
@group(0) @binding(3) var<uniform> u32Count : u32;      // number of u32s to iterate over
@group(0) @binding(4) var<uniform> totalPixels : u32;   // actual pixel count for bounds checking


const NO_COLOR= 0u;
const ALPHA_SHIFT = 255u << 24u;

@compute @workgroup_size(256)
fn main(@builtin(global_invocation_id) gid: vec3<u32>) {
    let idx = gid.x;
    if (idx >= u32Count) { return; }
    let v = pixelData[idx];
    let i= idx*32;

    // Early exit if this entire chunk is beyond pixel bounds
    if (i >= totalPixels) { return; }

    // Calculate how many pixels to process in this chunk (max 32)
    let remaining = min(32u, totalPixels - i);

    // Unrolled loop with bounds checking to avoid out-of-bounds writes
    // For large images where totalPixels is a multiple of 32, all conditionals pass
    if (remaining >  0u) { outBuf[i+ 0]= select(NO_COLOR, color, (v & (1u <<  0u)) == 0u); }
    if (remaining >  1u) { outBuf[i+ 1]= select(NO_COLOR, color, (v & (1u <<  1u)) == 0u); }
    if (remaining >  2u) { outBuf[i+ 2]= select(NO_COLOR, color, (v & (1u <<  2u)) == 0u); }
    if (remaining >  3u) { outBuf[i+ 3]= select(NO_COLOR, color, (v & (1u <<  3u)) == 0u); }
    if (remaining >  4u) { outBuf[i+ 4]= select(NO_COLOR, color, (v & (1u <<  4u)) == 0u); }
    if (remaining >  5u) { outBuf[i+ 5]= select(NO_COLOR, color, (v & (1u <<  5u)) == 0u); }
    if (remaining >  6u) { outBuf[i+ 6]= select(NO_COLOR, color, (v & (1u <<  6u)) == 0u); }
    if (remaining >  7u) { outBuf[i+ 7]= select(NO_COLOR, color, (v & (1u <<  7u)) == 0u); }
    if (remaining >  8u) { outBuf[i+ 8]= select(NO_COLOR, color, (v & (1u <<  8u)) == 0u); }
    if (remaining >  9u) { outBuf[i+ 9]= select(NO_COLOR, color, (v & (1u <<  9u)) == 0u); }
    if (remaining > 10u) { outBuf[i+10]= select(NO_COLOR, color, (v & (1u << 10u)) == 0u); }
    if (remaining > 11u) { outBuf[i+11]= select(NO_COLOR, color, (v & (1u << 11u)) == 0u); }
    if (remaining > 12u) { outBuf[i+12]= select(NO_COLOR, color, (v & (1u << 12u)) == 0u); }
    if (remaining > 13u) { outBuf[i+13]= select(NO_COLOR, color, (v & (1u << 13u)) == 0u); }
    if (remaining > 14u) { outBuf[i+14]= select(NO_COLOR, color, (v & (1u << 14u)) == 0u); }
    if (remaining > 15u) { outBuf[i+15]= select(NO_COLOR, color, (v & (1u << 15u)) == 0u); }
    if (remaining > 16u) { outBuf[i+16]= select(NO_COLOR, color, (v & (1u << 16u)) == 0u); }
    if (remaining > 17u) { outBuf[i+17]= select(NO_COLOR, color, (v & (1u << 17u)) == 0u); }
    if (remaining > 18u) { outBuf[i+18]= select(NO_COLOR, color, (v & (1u << 18u)) == 0u); }
    if (remaining > 19u) { outBuf[i+19]= select(NO_COLOR, color, (v & (1u << 19u)) == 0u); }
    if (remaining > 20u) { outBuf[i+20]= select(NO_COLOR, color, (v & (1u << 20u)) == 0u); }
    if (remaining > 21u) { outBuf[i+21]= select(NO_COLOR, color, (v & (1u << 21u)) == 0u); }
    if (remaining > 22u) { outBuf[i+22]= select(NO_COLOR, color, (v & (1u << 22u)) == 0u); }
    if (remaining > 23u) { outBuf[i+23]= select(NO_COLOR, color, (v & (1u << 23u)) == 0u); }
    if (remaining > 24u) { outBuf[i+24]= select(NO_COLOR, color, (v & (1u << 24u)) == 0u); }
    if (remaining > 25u) { outBuf[i+25]= select(NO_COLOR, color, (v & (1u << 25u)) == 0u); }
    if (remaining > 26u) { outBuf[i+26]= select(NO_COLOR, color, (v & (1u << 26u)) == 0u); }
    if (remaining > 27u) { outBuf[i+27]= select(NO_COLOR, color, (v & (1u << 27u)) == 0u); }
    if (remaining > 28u) { outBuf[i+28]= select(NO_COLOR, color, (v & (1u << 28u)) == 0u); }
    if (remaining > 29u) { outBuf[i+29]= select(NO_COLOR, color, (v & (1u << 29u)) == 0u); }
    if (remaining > 30u) { outBuf[i+30]= select(NO_COLOR, color, (v & (1u << 30u)) == 0u); }
    if (remaining > 31u) { outBuf[i+31]= select(NO_COLOR, color, (v & (1u << 31u)) == 0u); }
}
