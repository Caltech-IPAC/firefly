-- Convert deg2pix from JAVA to SQL
-- https://sourceforge.net/p/healpix/code/HEAD/tree/trunk/src/java/src/healpix/essentials/

CREATE OR REPLACE FUNCTION utab(i)
AS
	(1 & i)      	|  -- Check bit 0
	((2 & i) << 1) 	|  -- Check bit 1 and shift
	((4 & i) << 2) 	|  -- Check bit 2 and shift
	((8 & i) << 3) 	|  -- Check bit 3 and shift
	((16 & i) << 4) | -- Check bit 4 and shift
	((32 & i) << 5) | -- Check bit 5 and shift
	((64 & i) << 6) | -- Check bit 6 and shift
	((128 & i) << 7)  -- Check bit 7 and shift
;

-- java's >>> bitwise shift for int64
CREATE OR REPLACE FUNCTION rightShift(v, n) AS (v >> n) & ((1::LONG << (64 - n)) - 1);

-- Return remainder of the division v1/v2; positive and smaller than v2
-- v1 dividend; can be positive or negative
-- v2 divisor; must be positive
-- Converted from Java, but it seems the same to DuckDB's fmod. Using fmod instead.
-- CREATE OR REPLACE FUNCTION fmodulo(v1, v2)
-- AS (
--     CASE
--         WHEN v1 >= 0 THEN
--             CASE
--                 WHEN v1 < v2 THEN v1
--                 ELSE v1 % v2
--             END
--         ELSE
--             CASE
--                 WHEN v1 % v2 + v2 = v2 THEN 0.0
--                 ELSE v1 % v2 + v2
--             END
--     END
-- );

-- original code uses >>>, but our rightShift() replacement is slow.
-- will use >> instead, assuming v is never negative.
CREATE OR REPLACE FUNCTION spread_bits(v)
AS (
    SELECT (
	   utab(255 & v)                       |
	   (utab(255 & (v >>  8)) << 16)|
	   (utab(255 & (v >> 16)) << 32)|
	   (utab(255 & (v >> 24)) << 48)
    )
);

CREATE OR REPLACE FUNCTION xyf2nest(ix, iy, face_num, n_order)
AS (    (face_num << (2 * n_order)) +
        spread_bits(ix) +
        (spread_bits(iy) << 1)
);

CREATE OR REPLACE FUNCTION adj_phi(phi)
AS (
    WITH const AS (
        SELECT
            2 * PI() AS TWOPI,
    )
    SELECT
        CASE
            WHEN phi >= TWOPI THEN phi - TWOPI
            WHEN phi < 0 THEN phi + TWOPI
            ELSE phi
            END
    FROM const
);

CREATE OR REPLACE FUNCTION pix_equatorial(n_order, nside, tt, z)
AS (
    WITH equ_1 AS (
        SELECT
            nside * (0.5 + tt) AS temp1,
            nside * (z * 0.75) AS temp2
    ),
    equ_2 AS (
        SELECT
            TRUNC(temp1 - temp2)::LONG AS jp,  -- ascending edge line
            TRUNC(temp1 + temp2)::LONG AS jm   -- descending edge line
        FROM equ_1
    ),
    equ_3 AS (
        SELECT
            jp,
            jm,
            jp >> n_order AS ifp,                   -- in {0,4}
            jm >> n_order AS ifm
        FROM equ_2
    ),
    equ_f AS (
        SELECT
            CASE
                WHEN ifp = ifm THEN (ifp | 4)
                WHEN ifp < ifm THEN ifp
                ELSE ifm + 8
            END AS face_num,                      -- calculates face_num based on ifp and ifm
            jm & (nside - 1) AS ix,               -- calculates ix
            nside - (jp & (nside - 1)) - 1 AS iy  -- calculates iy
        FROM equ_3
    )
    SELECT
       xyf2nest(TRUNC(ix)::LONG, TRUNC(iy)::LONG, TRUNC(face_num)::LONG, n_order)
   FROM equ_f
);

CREATE OR REPLACE FUNCTION pix_polar(n_order, nside, have_sth, sth, tt, ntt, z, za)
AS (
    WITH p_1 AS (
        SELECT
            tt - ntt  AS tp,
            CASE WHEN za < 0.99 OR NOT have_sth
                THEN nside * SQRT(3 * (1 - za))
                ELSE nside * sth / SQRT((1 + za) / 3)
            END AS tmp
    ),
    p_f AS (
        SELECT
            LEAST(TRUNC(tp * tmp)::LONG, nside - 1) AS jp,
            LEAST(TRUNC((1.0 - tp) * tmp)::LONG, nside - 1) AS jm,
        FROM p_1
    )
    SELECT
        CASE
            WHEN z >= 0 THEN
                xyf2nest( nside - jm - 1, nside - jp - 1, ntt, n_order)
            ELSE
                xyf2nest(jp, jm, ntt + 8, n_order)
        END AS pixel
    FROM p_f
);

CREATE OR REPLACE FUNCTION deg2pix(n_order, a_ra, a_dec)
AS (
    WITH step_1 AS (
        SELECT
            radians(90 - a_dec)     AS theta,
            adj_phi(radians(a_ra))  AS phi,
    ),
    step_2 AS (
        SELECT
            theta,
            phi,
            COS(theta)  AS z
        FROM step_1
    ),
    step_3 AS (
        SELECT
            theta,
            phi,
            z,
            ABS(z)          AS za,
        FROM step_2
    ),
	step_f AS (
		SELECT
            z,
            za,
            za > 0.99       AS have_sth,
            IF(za > 0.99, SIN(theta),0)    AS sth,
 			(theta > PI() OR theta < 0) AS bad_theta,
            phi < 0 	    AS bad_phi,
            fmod((phi *(2./PI())),4.0) AS tt,           -- use duckdb fmod instead of fmodulo from java code
            TRUNC(POW(2, n_order))::LONG AS nside
        FROM step_3
    )
    SELECT
        CASE
            WHEN ((2.0/3) >= za) THEN
                pix_equatorial(n_order, nside, tt, z)
            ELSE
                pix_polar(n_order, nside, have_sth, sth, tt, LEAST(3, TRUNC(tt)::LONG), z, za)
        END AS pixel
    FROM step_f
);

