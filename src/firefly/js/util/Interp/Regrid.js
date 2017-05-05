
import {LinearInterpolator} from './LinearInterpolator';
/**
 * The Regrid class shrinks or expands the size of an array by an  arbitrary amount.
 *
 * NOTE: this implementation is one dimension only.
 *
 *  The algorithm is the same as IDL CONGRID with MINUS_ONE is set to true and extrapolation=false by default
 *
 *        When MINUS_ONE is true, I enforced the new coordinate at newDim -1 is
 *          the same as oldDim-1.   Mathematically,
 *             RegridFact = (oldDim - 1) / (newDim - 1);
 *             new_coordinate(i) =   i * RegridFact
 *             when i = newDim -1, new_coordinate(newDim-1) = oldDim -1.
 *             But due to the numerically error, there may be some small detla value introduced.
 *             Therefore, I need to enforce it to oldDim -1
 *
 */

export function Regrid(inData, outSize, allowExtrapolation=false){

    const outDataCoord = generateNewCoordinates(inData, outSize, allowExtrapolation);

    const inCoords = Array.from(new Array(inData.length), (x,i) => i);
    const linearInterpolator = LinearInterpolator(inCoords, inData, allowExtrapolation);

    const outData= outDataCoord.map( (c) => linearInterpolator(c));
    return outData;
}

/**
 * For any input data, for example,
 *
 *   inData = [d0, d1, d2, d3], the coordinate corresponding to the data is coord=[0, 1, 2, 3].
 *   If we express it using a function expression,we have
 *     di = f(coord*i)
 *     d0=f(0)
 *     d1=f(1)
 *
 *     ...
 *
 *
 *
 *  When we make a new coordinate for the new out array, the coordinate needs to be satisfy that
 *
 *   d_dim = f(coord) when dim=inData.length-1, d_dim = the last element of inData.
 *   when dim=0, d_0 = the first element of the inData
 *
 * @param inData
 * @param outArraySize
 * @param allowExtrapolation
 * @returns {Array}
 */
function generateNewCoordinates (inData,outArraySize, allowExtrapolation) {

    var  RegridFact = allowExtrapolation?inData.length / outArraySize:(inData.length - 1) / (outArraySize - 1);

    var coordinate = Array.from(new Array(outArraySize), (x,i) => i * RegridFact);


    //At the right most point, coordinate(outArraySize-1) = (outArraySize-1) * RegridFact =
    // (outArraySize-1) * (double)(data.getSize() - 1) / (double)(outArraySize - 1).
    //Mathematically, coordinate(outArraySize-1) = data.get(size)-1.  But there is
    //a numerical issue, so coordinate(outArraySize-1)!=data.get(size)-1.
    //This line below is to enforce the right most data is the same as the original when MINUS_ONE is true
    if (!allowExtrapolation)  coordinate[outArraySize-1] = inData.length-1;

    return coordinate;

} // End of generateNewCoordinates().

