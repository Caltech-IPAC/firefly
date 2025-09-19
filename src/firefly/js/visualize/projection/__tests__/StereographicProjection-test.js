import {StereographicProjection} from '../StereographicProjection.js';

describe('StereographicProjection', () => {
    const cmpNumDigits = 8;

    // Base header for CDELT/CROTA2 tests
    const cdeltHeader = {
        crval1: 180.0,  // reference RA
        crval2: 0.0,    // reference Dec
        crpix1: 512.5,  // reference pixel X
        crpix2: 512.5,  // reference pixel Y
        cdelt1: 0.1,   // pixel scale X (degrees)
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
            const result = StereographicProjection.fwdProject(512.5-1, 512.5-1, cdeltHeader);
            expect(result.x).toBeCloseTo(180.0, cmpNumDigits);
            expect(result.y).toBeCloseTo(0.0, cmpNumDigits);
        });

        test('round-trip conversion without rotation', () => {
            const originalRA = 179.0;
            const originalDec = 1.0;

            const imageCoords = StereographicProjection.revProject(originalRA, originalDec, cdeltHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = StereographicProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
            expect(worldCoords.x).toBeCloseTo(originalRA, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(originalDec, cmpNumDigits);
        });

        test('round-trip conversion with CROTA2 rotation', () => {
            const originalRA = 179.0;
            const originalDec = 1.0;

            const imageCoords = StereographicProjection.revProject(originalRA, originalDec, rotatedHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = StereographicProjection.fwdProject(imageCoords.x, imageCoords.y, rotatedHeader);
            expect(worldCoords.x).toBeCloseTo(originalRA, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(originalDec, cmpNumDigits);
        });

        test('rotation effect on pixel coordinates', () => {
            const ra = 179.0;
            const dec = 1.0;

            const unrotated = StereographicProjection.revProject(ra, dec, cdeltHeader);
            const rotated = StereographicProjection.revProject(ra, dec, rotatedHeader);

            expect(unrotated).toBeTruthy();
            expect(rotated).toBeTruthy();

            // Coordinates should be different due to rotation
            expect(Math.abs(unrotated.x - rotated.x)).toBeGreaterThan(0.1);
            expect(Math.abs(unrotated.y - rotated.y)).toBeGreaterThan(0.1);
        });
    });

    describe('CD matrix transformation', () => {
        test('reference point handling with CD matrix', () => {
            const result = StereographicProjection.fwdProject(512.5-1, 512.5-1, cdMatrixHeader);
            expect(result.x).toBeCloseTo(180.0, cmpNumDigits);
            expect(result.y).toBeCloseTo(0.0, cmpNumDigits);
        });

        test('round-trip conversion with CD matrix', () => {
            const originalRA = 179.0;
            const originalDec = 1.0;

            const imageCoords = StereographicProjection.revProject(originalRA, originalDec, cdMatrixHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = StereographicProjection.fwdProject(imageCoords.x, imageCoords.y, cdMatrixHeader);
            expect(worldCoords.x).toBeCloseTo(originalRA, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(originalDec, cmpNumDigits);
        });

        test('CD matrix equivalent to CROTA2 transformation', () => {
            const ra = 179.5;
            const dec = 0.5;

            const rotatedCoords = StereographicProjection.revProject(ra, dec, rotatedHeader);
            const cdCoords = StereographicProjection.revProject(ra, dec, cdMatrixHeader);

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

            const imageCoords = StereographicProjection.revProject(originalRA, originalDec, sipHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = StereographicProjection.fwdProject(imageCoords.x, imageCoords.y, sipHeader);
            expect(worldCoords.x).toBeCloseTo(originalRA, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(originalDec, cmpNumDigits);
        });

        test('SIP distortion effect on coordinates', () => {
            const ra = 178.0;
            const dec = 2.0;

            const undistorted = StereographicProjection.revProject(ra, dec, cdeltHeader);
            const distorted = StereographicProjection.revProject(ra, dec, sipHeader);

            expect(undistorted).toBeTruthy();
            expect(distorted).toBeTruthy();

            // Coordinates should be slightly different due to distortion
            expect(Math.abs(undistorted.x - distorted.x)).toBeGreaterThan(0);
            expect(Math.abs(undistorted.y - distorted.y)).toBeGreaterThan(0);
        });
    });

    describe('edge cases and error handling', () => {
        test('points on opposite hemisphere', () => {
            // Point on opposite hemisphere should not be visible
            const result = StereographicProjection.revProject(0.0, 0.0, cdeltHeader);
            expect(result).toBeNull();
        });

        test('near-edge points', () => {
            // Point near the edge of visibility (89 degrees from center)
            const result = StereographicProjection.revProject(180.0, 89.0, cdeltHeader);
            expect(result).toBeTruthy();
        });

        test('large coordinate values', () => {
            // Test with coordinates far from reference point
            const originalRA = 170.0;
            const originalDec = 30.0;

            const imageCoords = StereographicProjection.revProject(originalRA, originalDec, cdeltHeader);
            expect(imageCoords).toBeTruthy();

            const worldCoords = StereographicProjection.fwdProject(imageCoords.x, imageCoords.y, cdeltHeader);
            expect(worldCoords.x).toBeCloseTo(originalRA, cmpNumDigits);
            expect(worldCoords.y).toBeCloseTo(originalDec, cmpNumDigits);
        });
    });
});