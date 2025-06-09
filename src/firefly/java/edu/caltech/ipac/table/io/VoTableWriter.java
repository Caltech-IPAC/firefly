/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;
import edu.caltech.ipac.table.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.caltech.ipac.util.FormatUtil;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.votable.*;
import nom.tam.fits.FitsFactory;

import static edu.caltech.ipac.table.TableMeta.*;
import static edu.caltech.ipac.table.TableMeta.DESC;
import static edu.caltech.ipac.table.io.VoTableUtil.*;
import static edu.caltech.ipac.firefly.core.Util.Try;

/**
 * This class handles an action to save a catalog in VOTable (metadata, binary, binary2, fits) format to local file.
 *
 * @author  Cindy Wang
 * @see     DataGroup
 * @see     DataType
 */
public class VoTableWriter {

    /**
     * save the catalogs to a file
     *
     * @param file the file to be saved
     * @param dataGroup data group
     * @param outputFormat votable output format
     * @throws IOException on error
     */
    public static void save(File file, DataGroup dataGroup, FormatUtil.Format outputFormat) throws IOException {

        OutputStream ostream = new BufferedOutputStream( new FileOutputStream(file) );
        save(ostream, dataGroup, outputFormat);
    }


    /**
     * save the catalogs to a stream, stream is not closed
     *
     * @param stream the output stream to write to
     * @param dataGroup data group
     * @param outputFormat votable output format
     * @throws IOException on error
     */
    public static void save(OutputStream stream, DataGroup dataGroup, FormatUtil.Format outputFormat) throws IOException {

        FitsFactory.useThreadLocalSettings(true);  // consistent with FitsTableReader
        FitsFactory.setLongStringsEnabled(false);

        VoTableWriterImpl voWriter = new VoTableWriterImpl(outputFormat, dataGroup);
        voWriter.write(stream);

        FitsFactory.useThreadLocalSettings(false);
    }

    /**
     * A {@link VOTableWriter} implementation that outputs extra metadata from a {@link DataGroup} in VOTable format.
     * This writer is designed for a single DataGroup and does not support multiple tables.
     * It can, however, write multiple RESOURCE tags with empty TABLE elements as metadata.
     */
    public static class VoTableWriterImpl extends VOTableWriter {
        private final DataGroup dataGroup;

        public VoTableWriterImpl(FormatUtil.Format outputFormat, DataGroup dataGroup) {
            super(getDataFormat(outputFormat), true, getVotVersion(outputFormat));
            this.dataGroup = dataGroup;
        }

        public void write(OutputStream stream) throws IOException {
            // this should return only visible columns
            List<String> columns = Arrays.stream(dataGroup.getDataDefinitions())
                    .filter(dt -> IpacTableUtil.isVisible(dataGroup, dt))
                    .map(DataType::getKeyName)
                    .collect(Collectors.toList());

            StarTable st = new DataGroupStarTable(dataGroup, columns);
            writeStarTable(st, stream);
        }

        /**
         * Override to include all RESOURCE tags, placing the RESOURCE tag containing the TABLE last.
         */
        @Override
        protected String createResourceStartTag() {
            List<ResourceInfo> resources = new ArrayList<>(dataGroup.getResourceInfos());

            // When there are no resources or a single META resource,
            // create a "results" resource to represent the main table.
            if (resources.isEmpty() ||
                    (resources.size() == 1 && "META".equalsIgnoreCase(String.valueOf(resources.getFirst().getType())))) {
                resources.addFirst(new ResourceInfo(null, null, "results", dataGroup.getAttribute(UTYPE), null));
            }
            // output other resources first
            StringBuilder rval = new StringBuilder();
            for (int i = 1; i < resources.size(); i++) {
                rval.append(xmlResource(resources.get(i)).toXml());
            }
            // output the main table resource without ending tag
            rval.append(xmlResource(resources.getFirst()).toXmlNoFooter());
            return rval.toString();
        }

        /**
         * Override to inject additional info into the table.
         * This also forces inline writing only. and ignore optimization and File writing.
         */
        @Override
        public void writeStarTables(TableSequence tableSeq, OutputStream out, File file) throws IOException {

            OutputStreamWriter osw;
            osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            BufferedWriter writer = new BufferedWriter(osw);

            /* Output preamble. */
            writePreTableXML(writer);

            /* There should be only 1 table in tableSep */
            StarTable starTbl = tableSeq.nextTable();
            if (starTbl != null) {
                VOSerializer serializer = VOSerializer.makeSerializer(getDataFormat(), getVotableVersion(), starTbl);

                //note: in accordance with the IVOA standard for VOTable, the order of the tags below needs to be maintained
                writer.write(xmlTable(dataGroup).toXmlNoFooter() + "\n");      // TABLE tag; without closing tag
                serializer.writeFields(writer);                         // FIELDS tags
                dataGroup.getParamInfos().forEach(p -> {
                    Try.it(() -> writer.write(xmlParam(p).toXml()));    // PARAM tags
                });
                dataGroup.getGroupInfos().forEach(g -> {
                    Try.it(() -> writer.write(xmlGroup(g).toXml()));    // GROUP tags
                });
                dataGroup.getLinkInfos().forEach(l -> {
                    Try.it(() -> writer.write(xmlLink(l).toXml()));     // LINK tags
                });

                /* Now write the DATA element. ALWAYS write inline. */
                serializer.writeInlineDataElement(writer);                  // DATA tag

                getInfosFromMeta(dataGroup.getAttributeList()).stream()     // INFO tags
                        .map( a -> xmlInfo(a.getKey(), a.getValue()))
                        .forEach(info -> Try.it(() -> writer.write(info.toXml())));

                serializer.writePostDataXML(writer);                        // close TABLE tag
            }
            writePostTableXML(writer);

            writer.flush();
        }

        private static VOTableVersion getVotVersion(FormatUtil.Format outputFormat) {
            return outputFormat.equals(FormatUtil.Format.VO_TABLE_BINARY2) ? VOTableVersion.V13 : VOTableVersion.V12;
        }

        private static DataFormat getDataFormat(FormatUtil.Format outputFormat) {
            if (outputFormat.equals(FormatUtil.Format.VO_TABLE_BINARY2)) {
                return DataFormat.BINARY2;
            } else if (outputFormat.equals(FormatUtil.Format.VO_TABLE_BINARY)) {
                return DataFormat.BINARY;
            } else if (outputFormat.equals(FormatUtil.Format.VO_TABLE_FITS)) {
                return DataFormat.FITS;
            } else {
                return DataFormat.TABLEDATA;
            }
        }
    }

    private static List<DataGroup.Attribute> getInfosFromMeta(List<DataGroup.Attribute> meta) {
        List<String> ignore = Arrays.asList(ID, REF,UCD, UTYPE,NAME, DESC);     // these are table attributes, not infos
        return meta.stream()
                .filter(a -> (!a.isComment()) && !ignore.contains(a.getKey()))
                .collect(Collectors.toList());
    }


}

