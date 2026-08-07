/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/** SearchProcessor ID of MultiSpectrumProcessor; transforms a MultiSpectrum table into an obs_core table with datalinks */
export const MULTI_SPECTRUM_PROC_ID = 'multi_spectrum';
export const MULTI_SPECTRUM_MIME_TYPE = 'application/x-votable+xml;content=multispectrum';  // mimeType for MultiSpectrum tables

/** Firefly-specific media type indicating mixed content in a FITS file */
export const MIXED_FITS_MIME_TYPE = 'application/x-fits;content=mixed';
