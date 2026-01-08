package edu.caltech.ipac.firefly.server.packagedata.obscorepackager;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.MappedData;
import org.json.JSONObject;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class ObsCoreUtil {

    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public record CenterCols(String lonCol, String latCol) {}
    public record CenterColValues(String ra, String dec) {}
    public record Position(CenterCols centerColNames, CenterColValues centerColValues) {}

    public static boolean testSem(String sem, String val) {
        return (sem != null && sem.toLowerCase().endsWith(val));
    }

    public static String getExtFromURL(String contentTypeOrUrl) {
        String ext = "unknown";
        try {
            if (contentTypeOrUrl != null) {
                for (String ct : ObsCorePackager.acceptableExtensionTypes) {
                    if (contentTypeOrUrl.contains(ct)) {
                        ext = ct;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return ext;
    }

    public static List<String> getPositionColVals(String pos, int idx, MappedData dgDataUrl) {
        Position position = getPosition(pos);
        String ra, dec;

        ra = ifNotNull(position.centerColValues().ra())
                .get(v -> v.equals("null") ? null : v);

        dec = ifNotNull(position.centerColValues().dec())
                .get(v -> v.equals("null") ? null : v);

        if (ra == null || dec == null) {
            String lonCol = position.centerColNames().lonCol();
            String latCol = position.centerColNames().latCol();
            ra = Objects.toString(dgDataUrl.get(idx, lonCol), null);
            dec = Objects.toString(dgDataUrl.get(idx, latCol), null);
        }
        return List.of(ra, dec);
    }

    private static Position getPosition(String pos) {
        JSONObject jsonObject = new JSONObject(pos);

        String lonCol = ObsCorePackager.RA;
        String latCol = ObsCorePackager.DEC;

        if (jsonObject.has(ObsCorePackager.CENTER_COL_NAMES)) {
            JSONObject centerColsJson = jsonObject.getJSONObject(ObsCorePackager.CENTER_COL_NAMES);
            if (centerColsJson.has(ObsCorePackager.LON_COL)) {
                lonCol = String.valueOf(centerColsJson.get(ObsCorePackager.LON_COL));
            }
            if (centerColsJson.has(ObsCorePackager.LAT_COL)) {
                latCol = String.valueOf(centerColsJson.get(ObsCorePackager.LAT_COL));
            }
        }

        CenterCols centerColNames = new CenterCols(lonCol, latCol);

        JSONObject centerColValuesJson = jsonObject.getJSONObject(ObsCorePackager.CENTER_COL_VALS);
        String raVal = String.valueOf(centerColValuesJson.opt(ObsCorePackager.RA));
        String decVal = String.valueOf(centerColValuesJson.opt(ObsCorePackager.DEC));

        CenterColValues centerColValues = new CenterColValues(raVal, decVal);

        return new Position(centerColNames, centerColValues);
    }

    public static String makeUniquePrefix(String basePrefix, Map<String, Integer> prefixCounter) {
        if (!prefixCounter.containsKey(basePrefix)) {
            prefixCounter.put(basePrefix, 1);
            return basePrefix;
        } else {
            int count = prefixCounter.get(basePrefix);
            String newPrefix = basePrefix + "-" + count;
            while (prefixCounter.containsKey(newPrefix)) {
                count++;
                newPrefix = basePrefix + "-" + count;
            }
            prefixCounter.put(basePrefix, count);
            prefixCounter.put(newPrefix, 1);
            return newPrefix;
        }
    }

    public static String makeValidString(String val) {
        return val.replaceAll("\\s", "").replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static String getFileNamePrefix(int idx, String[] colNames, MappedData dgDataUrl, URL url, List<String> positionColVals) {
        String obsCollection = (String) dgDataUrl.get(idx, "obs_collection");
        String instrumentName = (String) dgDataUrl.get(idx, "instrument_name");
        String obsId = (String) dgDataUrl.get(idx, "obs_id");
        String obsTitle = (String) dgDataUrl.get(idx, "obs_title");
        String ra = positionColVals.get(0);
        String dec = positionColVals.get(1);

        String file_name = "";

        if (colNames != null) {
            for (String col : colNames) {
                String value = (String) dgDataUrl.get(idx, col);
                if (value != null) {
                    file_name += makeValidString(value) + "-";
                }
            }
        }
        if (file_name.length() > 0) {
            return file_name.substring(0, file_name.length() - 1);
        } else if (obsTitle != null) {
            file_name = makeValidString(obsTitle);
        } else {
            file_name = (obsCollection != null ? makeValidString(obsCollection) + "-" : "") +
                    (instrumentName != null ? makeValidString(instrumentName) + "-" : "") +
                    (obsId != null ? makeValidString(obsId) : "");
        }

        if (file_name.length() == 0) {
            if (!isEmpty(ra) && !isEmpty(dec)) {
                file_name = ra + "_" + dec;
            } else {
                file_name = "file_" + idx;
            }
        }
        return file_name;
    }
}
