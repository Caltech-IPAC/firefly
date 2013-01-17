VOServices Objective: convert data in IPAC Table format into VoTable output, using mapping configuration file in XML format.

Most projects are required to support IVOA standard services: Simple Image Access and Simple Spectra Access. The standards require that the output is VOTable with certain required and suggested fields that must have a particular ucd (Unified Column Descriptor) and be in particular format. Another requirement is that each service must support FORMAT parameter. If the value of this parameter is METADATA, the service must output service  metadata in VOTable format. The metadata include the list of possible input parameters and field definitions. 

This service was introduced to facilitate conversion IPAC table output of a project service outputting SIAP or SIA data into VOTable format. It allows to
1. define service metadata, using static configuration file
2. produce VOTable metadata just from the information, contained in the mapping configuration file (without calling the data producing service) 
3. convert IPAC table columns into VO table column with predefined attributes
4. convert several IPAC table columns into a single VO table column with arraysize more than 1. 
5. define default values for the columns not present in an IPAC table, but required by the standard


The project must provide:

1. Http GET service producing the data, required by a particular VO standard. The service must support the input parameters required by this VO Standard (and possibly other parameters) and produce the output in IPAC table format. Errors should be reported via IPAC table attribute with the key "ERROR". Http errors will also be reported, but the description will only include HTTP response description (like "Bad Request" for HTTP status code 400), which is not very descriptive.
2. XML configuration file, which contains URL to the above service, defines service metadata and mapping from IPAC Table fields into VO fields. The XML must follow DTD defined in xml/tablemapper.dtd 

To enable automatic service discovery, the configuration entry with mapping file reference should be added into [vo_services_config]/xml/services.xml file.


Publishing VO service

Very good introduction to VO services and steps to publish (make them available) can be found here:
http://www.us-vo.org/pubs/files/PublishHowTo.html


Questions & Answers


Should the configuration DTD allow to define coordinate system?

Q: Simple Image Specifications require to specify coordinate reference frame, unless it is the default "International Celestial Reference System" (ICRS)

Possible values for CoordRefFrame field are "ICRS", "FK5", "FK4", "ECL", "GAL", and "SGAL"

Is the difference between ICRS and FK5 only the precision? Is Spitzer using ICRS? (http://www.iers.org/IERS/EN/Science/ICRS/ICRS.html)

A: (from Dave Shupe) The short answer is that yes, Spitzer uses ICRS.  This came down from JPL as a requirement.

For practical purposes, to the positional accuracy we do anything on Spitzer, ICRS and J2000 are the same.  In the link you sent, the difference is just a few (or at least <100) milliarcsec.

My understanding is that ICRS will be used from now on, to avoid making e.g. J2050 in a few years.  So the celestial coordinate system will no longer be "precessed" along with the precession of the Earth's axis. 
 

Q: How to validate that Simple Image Access service is standard compliant?

A: SIA service validator (for public URLs) is available at http://nvo.ncsa.uiuc.edu/dalvalidate/siavalidate.html





