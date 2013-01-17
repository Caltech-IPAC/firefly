package edu.caltech.ipac.voservices.server.tablemapper;

import com.thoughtworks.xstream.XStream;

import java.io.FileInputStream;

public class TestMapper {

    public TestMapper() {}

    public static void main(String [] args) {
	String xmlFileName = "/Users/tatianag/dev/heritage/config/voservices/shaToSiapMapper.xml";
	TableMapper obj;
	try {
	    XStream xstream = new XStream();

	    // since XML contains 'id', must alias the System's 'id'
	    xstream.aliasSystemAttribute("refid", "id");

	    // process annotations & register custom converters
	    xstream.processAnnotations(TableMapper.class);


	    obj = (TableMapper) xstream.fromXML(new FileInputStream(xmlFileName));

	    System.out.println(obj.getAsString());

	    System.out.println("Loaded top-level xml file: " + xmlFileName);
	    System.out.println("Marshalled object: \n" + xstream.toXML(obj));

	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error reading xml file: " + xmlFileName);
	}
	System.exit(0);
    }

}