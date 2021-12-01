package edu.caltech.ipac.firefly.data;
/**
 * User: roby
 * Date: 12/21/20
 * Time: 10:10 AM
 */


import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.ResponseMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class HttpResultInfo {
    public static final String RESPONSE_CODE = "responseCode";
    public static final String RESPONSE_CODE_MSG = "responseCodeMsg";
    public static final String CONTENT_TYPE= "contentType";
    public static final String SIZE_IN_BYTES= "sizeInBytes";
    public static final String EXTERNAL_NAME= "externalName";

    private final byte[] result;

    private final Map<String,String> attributes= new HashMap<>(7);

    public HttpResultInfo(byte[] result,int responseCode, String contentType, String suggestedFileName) {

        this.result= result;
        putAttribute(RESPONSE_CODE, responseCode + "");
        putAttribute(RESPONSE_CODE_MSG, ResponseMessage.getHttpResponseMessage(responseCode));
        putAttribute(EXTERNAL_NAME,suggestedFileName);
        if (contentType!=null) putAttribute(CONTENT_TYPE, contentType);
        if (result!=null) putAttribute(SIZE_IN_BYTES, result.length+"");
    }

    public void putAttribute(String key, String value) { attributes.put(key,value); }
    public String getAttribute(String key) {return attributes.get(key); }

    public int getResponseCode() { return StringUtils.getInt(attributes.get(RESPONSE_CODE), 200); }
    public String getResponseCodeMsg() { return attributes.get(RESPONSE_CODE_MSG); }
    public long getSizeInBytes() { return StringUtils.getInt(getAttribute(SIZE_IN_BYTES),0); }
    public String getContentType() { return getAttribute(CONTENT_TYPE); }
    public String getExternalName() { return getAttribute(EXTERNAL_NAME); }

    public byte[] getResult() { return result;}
   public String getResultAsString() { return new String(result);}
}
