import {HpxProjection} from '../HpxProjection.js';

describe('HpxProjection comparison with Astropy', () => {
    const cmpNumDigits = 8;

    // FITS Header from the user
    const header = {
        naxis1: 192,
        naxis2: 192,
        ctype1: 'RA---HPX',
        crpix1: -248.217381441188,
        cdelt1: -0.0666666666666667,
        crval1: 0.0,
        ctype2: 'DEC--HPX',
        crpix2: -8.21754831338666,
        cdelt2: 0.0666666666666667,
        crval2: -90.0,
        lonpole: 180.0,
        latpole: 0.0,
        using_cd: false,
        crota2: 0.0
    };

    // Test cases: pixel coordinates and expected world coordinates from Astropy
    // console.log(`
    // To create the test cases, run this Python code:
    //
    // from astropy.wcs import WCS
    //
    // # Create WCS object
    // w = WCS(naxis=2)
    // w.wcs.ctype = ['RA---HPX', 'DEC--HPX']
    // w.wcs.crpix = [-248.217381441188, -8.21754831338666]
    // w.wcs.cdelt = [-0.0666666666666667, 0.0666666666666667]
    // w.wcs.crval = [0.0, -90.0]
    // w.wcs.lonpole = 180.0
    // w.wcs.latpole = 0.0
    //
    // # Test pixels (0-indexed), as in the test
    // pixels = [[0, 0], [96, 96], [191, 191], [50, 100], [150, 50]]
    // for px in pixels:
    //     world = w.pixel_to_world_values(px[0], px[1])
    //     print(f"{{ x: {px[0]}, y: {px[1]}, ra: {world[0]:.10f}, dec: {world[1]:.10f}, description: 'pixel [{px[0]}, {px[1]}]' }},")
    // `);

    const testCases = [
        // Format: { x: pixel_x (0-indexed), y: pixel_y (0-indexed), ra: expected_ra, dec: expected_dec }
        { x: 0, y: 0, ra: 271.8237002434, dec: -73.3775525505, description: 'bottom-left corner' },
        { x: 96, y: 96, ra: 284.9625000000, dec: -66.2658333333, description: 'center' },
        { x: 191, y: 191, ra: 292.3720498853, dec: -58.6988166522, description: 'top-right corner' },
        { x: 50, y: 100, ra: 287.6424997453, dec: -69.1505503841, description: 'random point 1' },
        { x: 150, y: 50, ra: 277.4507610386, dec: -63.1904350229, description: 'random point 2' },
    ];

    describe('Forward projection (pixel to world)', () => {
        testCases.forEach(({ x, y, ra, dec, description }) => {
            if (ra === null || dec === null) {
                test.skip(`${description} (x=${x}, y=${y}) - needs Astropy values`, () => {});
                return;
            }

            test(`${description} (x=${x}, y=${y})`, () => {
                // Firefly uses 0-indexed pixels internally
                const result = HpxProjection.fwdProject(x, y, header);

                expect(result).toBeTruthy();
                expect(result.x).toBeCloseTo(ra, cmpNumDigits);
                expect(result.y).toBeCloseTo(dec, cmpNumDigits);
            });
        });
    });

    describe('Reverse projection (world to pixel)', () => {
        testCases.forEach(({ x, y, ra, dec, description }) => {
            if (ra === null || dec === null) {
                test.skip(`${description} (RA=${ra}, Dec=${dec}) - needs Astropy values`, () => {});
                return;
            }

            test(`${description} (RA=${ra}, Dec=${dec})`, () => {
                const result = HpxProjection.revProject(ra, dec, header);

                expect(result).toBeTruthy();
                expect(result.x).toBeCloseTo(x, cmpNumDigits);
                expect(result.y).toBeCloseTo(y, cmpNumDigits);
            });
        });
    });
});

describe('HpxProjection', () => {
    const cmpNumDigits = 8;

    // Base header for CDELT/CROTA2 tests with default H=4, K=3
    const cdeltHeader = {
        crval1: 180.0,  // reference RA
        crval2: 0.0,    // reference Dec
        crpix1: 512.5,  // reference pixel X
        crpix2: 512.5,  // reference pixel Y
        cdelt1: 0.1,    // pixel scale X (degrees)
        cdelt2: 0.1,    // pixel scale Y (degrees)
        crota2: 0.0,    // rotation angle
        using_cd: false
    };

    // Header with CROTA2 rotation
    const rotatedHeader = {
        ...cdeltHeader,
        crota2: 30.0    // 30 degree rotation
    };

    // Header using CD matrix (equivalent to rotatedHeader)
    const cdMatrixHeader = (() => {
        const cos30 = Math.cos(30 * Math.PI / 180);
        const sin30 = Math.sin(30 * Math.PI / 180);

        // CD matrix for 30 degree rotation with scale [0.1, 0.1]
        // CD = [cdelt1*cos(θ)  -cdelt2*sin(θ)]
        //      [cdelt1*sin(θ)   cdelt2*cos(θ)]
        const cd1_1 = 0.1 * cos30;  // cdelt1 * cos(θ)
        const cd1_2 = -0.1 * sin30;  // -cdelt2 * sin(θ)
        const cd2_1 = 0.1 * sin30;  // cdelt1 * sin(θ)
        const cd2_2 = 0.1 * cos30;   // cdelt2 * cos(θ)

        // Calculate inverse DC matrix
        const det = cd1_1 * cd2_2 - cd1_2 * cd2_1;
        const dc1_1 = cd2_2 / det;
        const dc1_2 = -cd1_2 / det;
        const dc2_1 = -cd2_1 / det;
        const dc2_2 = cd1_1 / det;

        return {
            crval1: 180.0,
            crval2: 0.0,
            crpix1: 512.5,
            crpix2: 512.5,
            using_cd: true,
            cd1_1, cd1_2, cd2_1, cd2_2,
            dc1_1, dc1_2, dc2_1, dc2_2
        };
    })();

    // Header with SIP distortion
    const sipHeader = {
        ...cdeltHeader,
        map_distortion: true,
        a_order: 2,
        b_order: 2,
        ap_order: 2,
        bp_order: 2,
        a: [[0, 0, 1e-6], [0, 0, 0], [1e-6, 0, 0]],
        b: [[0, 0, 1e-6], [0, 0, 0], [1e-6, 0, 0]],
        ap: [[0, 0, -1e-6], [0, 0, 0], [-1e-6, 0, 0]],
        bp: [[0, 0, -1e-6], [0, 0, 0], [-1e-6, 0, 0]]
    };

    describe('CDELT/CROTA2 transformation', () => {
        test('reference point handling', () => {
            const result = HpxProjection.fwdProject(512.5-1, 512.5-1, cdeltHeader);
            expect(result.x).toBeCloseTo(180.0, cmpNumDigits);
            expect(result.y).toBeCloseTo(0.0, cmpNumDigits);
        });

        test('round-trip conversion without rotation', () => {
            const originalRA = 179.0;
            const originalDec = 1.0;

            const imageCoords = HpxProjection.revProject(originalRA, originalDec, cdeltHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
            expect(worldCoords.x).toBeCloseTo(originalRA, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(originalDec, cmpNumDigits);
        });

        test('round-trip conversion with CROTA2 rotation', () => {
            const originalRA = 179.0;
            const originalDec = 1.0;

            const imageCoords = HpxProjection.revProject(originalRA, originalDec, rotatedHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, rotatedHeader);
            expect(worldCoords.x).toBeCloseTo(originalRA, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(originalDec, cmpNumDigits);
        });

        test('rotation effect on pixel coordinates', () => {
            const ra = 179.0;
            const dec = 1.0;

            const unrotated = HpxProjection.revProject(ra, dec, cdeltHeader);
            const rotated = HpxProjection.revProject(ra, dec, rotatedHeader);

            expect(unrotated).toBeTruthy();
            expect(rotated).toBeTruthy();

            // Coordinates should be different due to rotation
            expect(Math.abs(unrotated.x - rotated.x)).toBeGreaterThan(0.1);
            expect(Math.abs(unrotated.y - rotated.y)).toBeGreaterThan(0.1);
        });
    });

    describe('CD matrix transformation', () => {
        test('reference point handling with CD matrix', () => {
            const result = HpxProjection.fwdProject(512.5-1, 512.5-1, cdMatrixHeader);
            expect(result.x).toBeCloseTo(180.0, cmpNumDigits);
            expect(result.y).toBeCloseTo(0.0, cmpNumDigits);
        });

        test('round-trip conversion with CD matrix', () => {
            const originalRA = 179.0;
            const originalDec = 1.0;

            const imageCoords = HpxProjection.revProject(originalRA, originalDec, cdMatrixHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdMatrixHeader);
            expect(worldCoords.x).toBeCloseTo(originalRA, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(originalDec, cmpNumDigits);
        });

        test('CD matrix equivalent to CROTA2 transformation', () => {
            const ra = 179.5;
            const dec = 0.5;

            const rotatedCoords = HpxProjection.revProject(ra, dec, rotatedHeader);
            const cdCoords = HpxProjection.revProject(ra, dec, cdMatrixHeader);

            expect(rotatedCoords).toBeTruthy();
            expect(cdCoords).toBeTruthy();

            // Results should be very close (within numerical precision)
            expect(rotatedCoords.x).toBeCloseTo(cdCoords.x, cmpNumDigits);
            expect(rotatedCoords.y).toBeCloseTo(cdCoords.y, cmpNumDigits);
        });
    });

    describe('SIP distortion correction', () => {
        test('round-trip conversion with SIP distortion', () => {
            const originalRA = 179.0;
            const originalDec = 1.0;

            const imageCoords = HpxProjection.revProject(originalRA, originalDec, sipHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, sipHeader);
            expect(worldCoords.x).toBeCloseTo(originalRA, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(originalDec, cmpNumDigits);
        });

        test('SIP distortion effect on coordinates', () => {
            const ra = 178.0;
            const dec = 2.0;

            const undistorted = HpxProjection.revProject(ra, dec, cdeltHeader);
            const distorted = HpxProjection.revProject(ra, dec, sipHeader);

            expect(undistorted).toBeTruthy();
            expect(distorted).toBeTruthy();

            // Coordinates should be slightly different due to distortion
            expect(Math.abs(undistorted.x - distorted.x)).toBeGreaterThan(0);
            expect(Math.abs(undistorted.y - distorted.y)).toBeGreaterThan(0);
        });
    });

    describe('H and K parameter handling', () => {
        test('default H=4, K=3 when pv2 not provided', () => {
            // cdeltHeader doesn't have pv2, so defaults should be used
            const ra = 180.0;
            const dec = 45.0;

            const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
            expect(imageCoords).toBeTruthy();

            // Verify round-trip works with defaults
            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('custom H=6, K=5 from header.pv2', () => {
            const customHeader = {
                ...cdeltHeader,
                pv2: [6.0, 5.0]  // H=6, K=5
            };

            const ra = 180.0;
            const dec = 30.0;

            const imageCoords = HpxProjection.revProject(ra, dec, customHeader);
            expect(imageCoords).toBeTruthy();

            // Verify round-trip works with custom parameters
            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, customHeader);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('different H/K combinations in equatorial region affect Y coordinate', () => {
            const header1 = { ...cdeltHeader, pv2: [4.0, 3.0] };  // K/H = 0.75
            const header2 = { ...cdeltHeader, pv2: [6.0, 5.0] };  // K/H = 0.833

            const ra = 180.0;
            const dec = 30.0;  // In equatorial region for both (transition at ~41.81° and ~53.13°)

            const coords1 = HpxProjection.revProject(ra, dec, header1);
            const coords2 = HpxProjection.revProject(ra, dec, header2);

            expect(coords1).toBeTruthy();
            expect(coords2).toBeTruthy();

            // In equatorial region: fline = (90*K/H)*sin(theta), so different K/H ratios affect Y
            // fsamp = phi (in degrees), which is the same for both since RA and reference are the same
            // X coordinate should be the same (or within numerical precision)
            expect(Math.abs(coords1.x - coords2.x)).toBeLessThan(1e-10);
            // Y coordinate should differ due to different K/H ratios
            expect(Math.abs(coords1.y - coords2.y)).toBeGreaterThan(0.01);
        });

        test('different H/K combinations in polar region affect both X and Y coordinates', () => {
            const header1 = { ...cdeltHeader, pv2: [4.0, 3.0] };
            const header2 = { ...cdeltHeader, pv2: [6.0, 5.0] };

            const ra = 90.0;  // Away from reference RA to see X differences
            const dec = 60.0;  // In polar region for both

            const coords1 = HpxProjection.revProject(ra, dec, header1);
            const coords2 = HpxProjection.revProject(ra, dec, header2);

            expect(coords1).toBeTruthy();
            expect(coords2).toBeTruthy();

            // In polar region: both H and K affect sigma, facet centers, and both fsamp and fline
            // So both X and Y coordinates should differ
            expect(Math.abs(coords1.x - coords2.x)).toBeGreaterThan(0.01);
            expect(Math.abs(coords1.y - coords2.y)).toBeGreaterThan(0.01);
        });

        test('transition latitude changes with K', () => {
            // For K=3, theta_transition = asin((3-1)/3) = asin(2/3) ≈ 41.81°
            // For K=5, theta_transition = asin((5-1)/5) = asin(4/5) ≈ 53.13°
            const headerK3 = { ...cdeltHeader, pv2: [4.0, 3.0] };
            const headerK5 = { ...cdeltHeader, pv2: [4.0, 5.0] };

            // Test at dec=45°, which is in polar region for K=3 but closer to transition for K=5
            const ra = 180.0;
            const dec = 45.0;

            const coordsK3 = HpxProjection.revProject(ra, dec, headerK3);
            const coordsK5 = HpxProjection.revProject(ra, dec, headerK5);

            expect(coordsK3).toBeTruthy();
            expect(coordsK5).toBeTruthy();

            // Verify both round-trip correctly
            const worldK3 = HpxProjection.fwdProject(coordsK3.x, coordsK3.y, headerK3);
            const worldK5 = HpxProjection.fwdProject(coordsK5.x, coordsK5.y, headerK5);

            expect(worldK3.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldK3.y).toBeCloseTo(dec, cmpNumDigits);
            expect(worldK5.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldK5.y).toBeCloseTo(dec, cmpNumDigits);
        });
    });

    describe('equatorial region (|theta| <= theta_transition)', () => {
        test('equatorial coordinates near reference point', () => {
            // For default K=3, H=4: theta_transition ≈ 41.81°
            // Test at dec=20°, which is well within equatorial region
            const ra = 180.0;
            const dec = 20.0;

            const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('equatorial round-trip at various RAs', () => {
            const dec = 15.0;  // In equatorial region
            const testRAs = [0.0, 90.0, 180.0, 270.0, 350.0];

            testRAs.forEach((ra) => {
                const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
                expect(imageCoords).toBeTruthy();

                const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
                expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
                expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
            });
        });

        test('equatorial region spanning both sides of reference RA', () => {
            const dec = 10.0;

            // Test points on both sides of reference RA=180°
            const ra1 = 170.0;
            const ra2 = 190.0;

            const coords1 = HpxProjection.revProject(ra1, dec, cdeltHeader);
            const coords2 = HpxProjection.revProject(ra2, dec, cdeltHeader);

            expect(coords1).toBeTruthy();
            expect(coords2).toBeTruthy();

            // Verify round-trips
            const world1 = HpxProjection.fwdProject(coords1.x, coords1.y, cdeltHeader);
            const world2 = HpxProjection.fwdProject(coords2.x, coords2.y, cdeltHeader);

            expect(world1.x).toBeCloseTo(ra1, cmpNumDigits);
            expect(world1.y).toBeCloseTo(dec, cmpNumDigits);
            expect(world2.x).toBeCloseTo(ra2, cmpNumDigits);
            expect(world2.y).toBeCloseTo(dec, cmpNumDigits);
        });
    });

    describe('polar regions (|theta| > theta_transition)', () => {
        test('northern polar region projection', () => {
            // For default K=3: theta_transition ≈ 41.81°
            // Test at dec=60°, which is in northern polar region
            const ra = 180.0;
            const dec = 60.0;

            const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('southern polar region projection', () => {
            // Test at dec=-60°, which is in southern polar region
            const ra = 180.0;
            const dec = -60.0;

            const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('near north pole', () => {
            const ra = 180.0;
            const dec = 85.0;

            const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('near south pole', () => {
            const ra = 180.0;
            const dec = -85.0;

            const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('polar region at various longitudes', () => {
            const dec = 70.0;  // In northern polar region
            const testRAs = [0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0];

            testRAs.forEach((ra) => {
                const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
                expect(imageCoords).toBeTruthy();

                const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
                expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
                expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
            });
        });
    });

    describe('facet boundaries and transitions', () => {
        test('points near theta transition latitude', () => {
            // For K=3, theta_transition ≈ 41.81°
            // Test just below, at, and just above transition
            const ra = 180.0;
            const decTransition = Math.asin(2.0/3.0) * 180.0 / Math.PI;  // ≈ 41.81°

            const testDecs = [
                decTransition - 1.0,
                decTransition,
                decTransition + 1.0
            ];

            testDecs.forEach((dec) => {
                const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
                expect(imageCoords).toBeTruthy();

                const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
                expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
                expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
            });
        });

        test('phi normalization across 180° meridian', () => {
            // Test points near phi = ±180°
            const dec = 50.0;  // In polar region
            const testRAs = [179.0, 180.0, 181.0, 359.0, 0.0, 1.0];

            testRAs.forEach((ra) => {
                const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
                expect(imageCoords).toBeTruthy();

                const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);

                // Handle RA wrap-around (0 and 360 are equivalent)
                const raDiff = Math.abs(worldCoords.x - ra);
                const wrappedDiff = Math.min(raDiff, Math.abs(raDiff - 360.0));
                expect(wrappedDiff).toBeLessThan(Math.pow(10, -cmpNumDigits));
                expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
            });
        });

        test('facet boundaries with default H=4', () => {
            // For H=4, facets are centered at -180°, -90°, 0°, 90°
            // Facet width is 360°/4 = 90°
            const dec = 60.0;  // In polar region

            // Test at facet centers and boundaries
            const testRAs = [
                -180.0 + 360.0,  // -180° wrapped to 180°
                -135.0 + 360.0,  // Boundary
                -90.0 + 360.0,   // Center
                -45.0 + 360.0,   // Boundary
                0.0,             // Center
                45.0,            // Boundary
                90.0,            // Center
                135.0,           // Boundary
                180.0            // Center
            ];

            testRAs.forEach((ra) => {
                const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
                expect(imageCoords).toBeTruthy();

                const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);

                const raDiff = Math.abs(worldCoords.x - ra);
                const wrappedDiff = Math.min(raDiff, Math.abs(raDiff - 360.0));
                expect(wrappedDiff).toBeLessThan(Math.pow(10, -cmpNumDigits));
                expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
            });
        });
    });

    describe('odd/even K and H combinations', () => {
        test('odd K (K=3) behavior', () => {
            const headerOddK = { ...cdeltHeader, pv2: [4.0, 3.0] };

            // Test in southern polar region where offset logic applies
            const ra = 180.0;
            const dec = -50.0;

            const imageCoords = HpxProjection.revProject(ra, dec, headerOddK);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, headerOddK);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('even K (K=4) behavior', () => {
            const headerEvenK = { ...cdeltHeader, pv2: [4.0, 4.0] };

            // Test in southern polar region where offset logic differs
            // Use ra=170 instead of 180 to avoid facet center edge case
            const ra = 170.0;
            const dec = -50.0;

            const imageCoords = HpxProjection.revProject(ra, dec, headerEvenK);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, headerEvenK);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('even K at facet boundary - pixel consistency despite RA ambiguity', () => {
            const headerEvenK = { ...cdeltHeader, pv2: [4.0, 4.0] };

            // Test at facet boundary where RA may not round-trip exactly
            // but pixel coordinates should remain consistent
            const ra = 180.0;  // At facet center with crval1=180
            const dec = -50.0;

            // Forward: RA/Dec -> Pixel
            const imageCoords1 = HpxProjection.revProject(ra, dec, headerEvenK);
            expect(imageCoords1).toBeTruthy();

            // Inverse: Pixel -> RA/Dec (may return different RA due to facet boundary)
            const worldCoords = HpxProjection.fwdProject(imageCoords1.x, imageCoords1.y, headerEvenK);
            expect(worldCoords).toBeTruthy();

            // The returned RA might be different (e.g., 183° instead of 180°)
            // but Dec should be the same
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);

            // Forward again with the returned RA: should give same pixels
            const imageCoords2 = HpxProjection.revProject(worldCoords.x, worldCoords.y, headerEvenK);
            expect(imageCoords2).toBeTruthy();

            // Pixel coordinates should be consistent (same pixels for both RAs)
            expect(imageCoords2.x).toBeCloseTo(imageCoords1.x, cmpNumDigits);
            expect(imageCoords2.y).toBeCloseTo(imageCoords1.y, cmpNumDigits);
        });

        test('odd K vs even K produces different results in southern polar region', () => {
            const headerOddK = { ...cdeltHeader, pv2: [4.0, 3.0] };
            const headerEvenK = { ...cdeltHeader, pv2: [4.0, 4.0] };

            const ra = 90.0;  // Different from facet center
            const dec = -50.0;

            const coordsOddK = HpxProjection.revProject(ra, dec, headerOddK);
            const coordsEvenK = HpxProjection.revProject(ra, dec, headerEvenK);

            expect(coordsOddK).toBeTruthy();
            expect(coordsEvenK).toBeTruthy();

            // Different K parity can produce different pixel coordinates
            // due to offset adjustment in southern polar half-facets
            const diffX = Math.abs(coordsOddK.x - coordsEvenK.x);
            const diffY = Math.abs(coordsOddK.y - coordsEvenK.y);

            // At least one coordinate should differ
            expect(diffX + diffY).toBeGreaterThan(0);
        });

        test('odd H (H=5) behavior', () => {
            const headerOddH = { ...cdeltHeader, pv2: [5.0, 3.0] };

            const ra = 180.0;
            const dec = 60.0;

            const imageCoords = HpxProjection.revProject(ra, dec, headerOddH);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, headerOddH);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('even H (H=6) behavior', () => {
            const headerEvenH = { ...cdeltHeader, pv2: [6.0, 3.0] };

            const ra = 180.0;
            const dec = 60.0;

            const imageCoords = HpxProjection.revProject(ra, dec, headerEvenH);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, headerEvenH);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('H parameter affects facet count and boundaries', () => {
            const headerH4 = { ...cdeltHeader, pv2: [4.0, 3.0] };
            const headerH6 = { ...cdeltHeader, pv2: [6.0, 3.0] };

            const ra = 180.0;
            const dec = 60.0;

            const coordsH4 = HpxProjection.revProject(ra, dec, headerH4);
            const coordsH6 = HpxProjection.revProject(ra, dec, headerH6);

            expect(coordsH4).toBeTruthy();
            expect(coordsH6).toBeTruthy();

            // Different H should produce different results
            expect(Math.abs(coordsH4.x - coordsH6.x)).toBeGreaterThan(0.01);
            expect(Math.abs(coordsH4.y - coordsH6.y)).toBeGreaterThan(0.01);
        });
    });

    describe('edge cases and comprehensive coverage', () => {
        test('all four quadrants in equatorial region', () => {
            const dec = 20.0;  // In equatorial region
            const quadrantRAs = [45.0, 135.0, 225.0, 315.0];

            quadrantRAs.forEach((ra) => {
                const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
                expect(imageCoords).toBeTruthy();

                const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
                expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
                expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
            });
        });

        test('all four quadrants in polar region', () => {
            const dec = 60.0;  // In northern polar region
            const quadrantRAs = [45.0, 135.0, 225.0, 315.0];

            quadrantRAs.forEach((ra) => {
                const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
                expect(imageCoords).toBeTruthy();

                const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
                expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
                expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
            });
        });

        test('large coordinate values far from reference point', () => {
            // Test with coordinates far from reference point
            const originalRA = 0.0;   // Far from crval1=180°
            const originalDec = 70.0;

            const imageCoords = HpxProjection.revProject(originalRA, originalDec, cdeltHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
            expect(worldCoords.x).toBeCloseTo(originalRA, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(originalDec, cmpNumDigits);
        });

        test('multiple round-trips maintain precision', () => {
            let ra = 170.0;
            let dec = 45.0;

            // Perform 5 round-trips
            for (let i = 0; i < 5; i++) {
                const imageCoords = HpxProjection.revProject(ra, dec, cdeltHeader);
                expect(imageCoords).toBeTruthy();

                const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
                ra = worldCoords.x;
                dec = worldCoords.y;
            }

            // Should still be close to original values
            expect(ra).toBeCloseTo(170.0, cmpNumDigits - 1);  // Allow slight accumulation
            expect(dec).toBeCloseTo(45.0, cmpNumDigits - 1);
        });

        test('consistency across RA wrap-around boundary', () => {
            const dec = 30.0;

            // Test points near 0°/360° boundary
            const ra1 = 359.5;
            const ra2 = 0.5;

            const coords1 = HpxProjection.revProject(ra1, dec, cdeltHeader);
            const coords2 = HpxProjection.revProject(ra2, dec, cdeltHeader);

            expect(coords1).toBeTruthy();
            expect(coords2).toBeTruthy();

            // Both should round-trip correctly
            const world1 = HpxProjection.fwdProject(coords1.x, coords1.y, cdeltHeader);
            const world2 = HpxProjection.fwdProject(coords2.x, coords2.y, cdeltHeader);

            expect(world1.x).toBeCloseTo(ra1, cmpNumDigits);
            expect(world1.y).toBeCloseTo(dec, cmpNumDigits);
            expect(world2.x).toBeCloseTo(ra2, cmpNumDigits);
            expect(world2.y).toBeCloseTo(dec, cmpNumDigits);
        });

        test('reference point at different sky position', () => {
            // Test with reference point at RA=0°, Dec=45°
            const altHeader = {
                ...cdeltHeader,
                crval1: 0.0,
                crval2: 45.0
            };

            const result = HpxProjection.fwdProject(512.5-1, 512.5-1, altHeader);
            expect(result.x).toBeCloseTo(0.0, cmpNumDigits);
            expect(result.y).toBeCloseTo(45.0, cmpNumDigits);

            // Test round-trip near this reference point
            const ra = 10.0;
            const dec = 50.0;

            const imageCoords = HpxProjection.revProject(ra, dec, altHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = HpxProjection.fwdProject(imageCoords.x, imageCoords.y, altHeader);
            expect(worldCoords.x).toBeCloseTo(ra, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(dec, cmpNumDigits);
        });
    });
});
