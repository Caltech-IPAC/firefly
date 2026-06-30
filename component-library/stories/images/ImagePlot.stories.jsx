import React from 'react';
import { ImagePlot } from '../../src/images/index.js';


const EXAMPLE_CODE = `\
import { ImagePlot } from '@ipac/firefly-components/images';

() => {
    return (
        <ImagePlot
            url="https://example.com/my-image.fits"
            sx={{ height: 600 }}
        />
    );
}`;

export default {
    title: 'Images/ImagePlot',
    component: ImagePlot,
    tags: ['autodocs'],
    parameters: {
        controls: { disable: true }, actions: { disable: true },
        docs: { source: { code: EXAMPLE_CODE } },
    },
};

// ─── Basic ────────────────────────────────────────────────────────────────────

export const Basic = () => (
    <ImagePlot url='https://web.ipac.caltech.edu/staff/roby/many-images/mips_IER_7735552_ier600_A24_P24_10s.cal.fits' sx={{ height: 600 }} />
);

Basic.storyName = 'Basic';
Basic.parameters = { storyDescription: 'Load a FITS image from a URL. The toolbar provides zoom, pan, stretch, and color controls.' };
