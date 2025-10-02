package edu.caltech.ipac.util.download;


import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Trey Roby
 *
 */
public class UriRefTest {

    @Test
    public void testSimpleCases(){

        try {
            var ref= UriRef.make("s3://abc/xxx/yyy/a.fits");
            assertEquals(UriRef.ResourceType.S3Cloud, ref.getType());
            assertEquals("abc", ref.getS3Ref().bucket());
            assertEquals("xxx/yyy/a.fits", ref.getS3Ref().key());

            URL url = new URI("https://abc.com/xxx/yyy/a.fits").toURL();
            assertNull( UriRef.make(url).getS3Ref());
            assertNotNull( UriRef.make(url).getURL());
            assertEquals(UriRef.ResourceType.OnPrimUrl, UriRef.make(url).getType());


            var srRef= UriRef.make("https://s3.us-west-2.amazonaws.com/amzn-s3-demo-bucket1/puppy.jpg");
            assertEquals(UriRef.ResourceType.S3Cloud, srRef.getType());
            assertEquals("amzn-s3-demo-bucket1", srRef.getS3Ref().bucket());

            var srRefwithQ= UriRef.make("https://s3.us-west-2.amazonaws.com/amzn-s3-demo-bucket1/puppy.jpg?a=2");
            assertEquals(UriRef.ResourceType.OnPrimUrl, srRefwithQ.getType());
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

//=======================================================================
//-------------- Method from xxx Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

}
