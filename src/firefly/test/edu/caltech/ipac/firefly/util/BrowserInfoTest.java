package edu.caltech.ipac.firefly.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BrowserInfoTest {

	@Test
	public void testUserAgentCheck(){
		WIN_BROW_TEST[] wb = WIN_BROW_TEST.values();
		for (int i = 0; i < wb.length; i++) {
			
		String userAgent = wb[i].getUser();
		BrowserInfo b = new BrowserInfo(userAgent);
		
		assertTrue("Browser is not "+wb[i].name() + " but "+b.getBrowserType(),b.getBrowserType() == wb[i].getType());
		assertTrue("Browser version "+wb[i].name() + " is not correct "+b.getMajorVersion(),b.getMajorVersion() == wb[i].getVersion());

		}
	}
	
	enum WIN_BROW_TEST {
		
		Chrome(Browser.CHROME,44,"mozilla/5.0 (windows nt 10.0; win64; x64) applewebkit/537.36 (khtml, like gecko) chrome/44.0.2403.157 safari/537.36"),
		IE11(Browser.IE,11, "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; Touch; rv:11.0) like Gecko"),
		IE10(Browser.IE,10, "Mozilla/5.0 (MSIE 10.0; Windows NT 6.1; Trident/5.0)"),
		Edge(Browser.IE,12, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.10136"),
		Firefox(Browser.FIREFOX,40, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1"),
		Safari(Browser.SAFARI,9, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/601.4.4 (KHTML, like Gecko) Version/9.0.3 Safari/601.4.4"),
		IE_tabletpc(Browser.IE, 9, "mozilla/5.0 (compatible; msie 9.0; windows nt 10.0; wow64; trident/8.0; touch; .net4.0c; .net4.0e; .net clr 2.0.50727; .net clr 3.0.30729; .net clr 3.5.30729; infopath.3; tablet pc 2.0)");
		private String useragent;
		private Browser b;
		private int vers;

		WIN_BROW_TEST(Browser b, int ver, String useragent){
			this.useragent = useragent;
			this.b=b;
			this.vers=ver;
		}

		public int getVersion() {
			return vers;
		}

		public String getUser(){
			return this.useragent;
		}

		public Browser getType() {
			return b;
		}	
		
	}
}
