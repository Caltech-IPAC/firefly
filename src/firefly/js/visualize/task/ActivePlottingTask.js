
const currentPlots= new Map();

export const setActiveRequestKey= (plotId, requestKey) => currentPlots.set(plotId,requestKey);
export const getActiveRequestKey= (plotId) => currentPlots.get(plotId);
export const hasActiveRequest= (plotId) => currentPlots.has(plotId);
export const clearActiveRequest= (plotId) => currentPlots.delete(plotId);

