export default class ColValuesStatistics{
    constructor(name, descr, unit, min, max, numpoints) {
        this.name = name;
        this.descr = descr;
        this.unit = unit;
        this.min = Number(min);
        this.max = Number(max);
        this.numpoints = Number(numpoints);
    }
};