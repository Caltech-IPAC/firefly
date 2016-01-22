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
		}
	}
	
	enum WIN_BROW_TEST {
		
		Chrome(Browser.CHROME,"mozilla/5.0 (windows nt 10.0; win64; x64) applewebkit/537.36 (khtml, like gecko) chrome/44.0.2403.157 safari/537.36"),
		IE(Browser.IE,"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko"),
		Edge(Browser.IE,"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.10136"),
		Firefox(Browser.FIREFOX,"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1"),
		Safari(Browser.WEBKIT_GENERIC,"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/601.4.4 (KHTML, like Gecko) Version/9.0.3 Safari/601.4.4");
		
		private String useragent;
		private Browser b;

		WIN_BROW_TEST(Browser b, String useragent){
			this.useragent = useragent;
			this.b=b;
		}
		
		public String getUser(){
			return this.useragent;
		}

		public Browser getType() {
			return b;
		}	
		
	}
}
