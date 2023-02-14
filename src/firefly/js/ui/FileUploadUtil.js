export const REGIONS = 'REGION';
export const TABLES = 'Table';
export const SPECTRUM_TABLES = 'spectrum-tables';
export const MOC_TABLES = 'moc-tables';
export const DATA_LINK_TABLES = 'data-link-tables';
export const IMAGES = 'Image';
export const UWS = 'UWS';

export const acceptImages = (acceptList) => acceptList.includes(IMAGES);
export const acceptRegions = (acceptList) => acceptList.includes(REGIONS);
export const acceptUWS = (acceptList) => acceptList.includes(UWS);
export const acceptOnlyTables = (acceptList) =>  acceptList.includes(TABLES) && acceptList.length === 1;
export const acceptMocTables = (acceptList) => acceptList.includes(MOC_TABLES);
export const acceptDataLinkTables = (acceptList) => acceptList.includes(DATA_LINK_TABLES);
export const acceptNonMocTables = (acceptList) =>
    [TABLES,SPECTRUM_TABLES,DATA_LINK_TABLES].some( (type) => acceptList.includes(type));
export const acceptNonDataLinkTables = (acceptList) =>
    [TABLES,SPECTRUM_TABLES,MOC_TABLES].some( (type) => acceptList.includes(type));
export const acceptAnyTables = (acceptList) =>
    [TABLES,SPECTRUM_TABLES,MOC_TABLES,DATA_LINK_TABLES].some( (type) => acceptList.includes(type));
export const acceptTableOrSpectrum = (acceptList) =>
    [TABLES,SPECTRUM_TABLES].some( (type) => acceptList.includes(type));