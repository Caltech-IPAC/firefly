package edu.caltech.ipac.firefly.util;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.DownloadScript;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

import static edu.caltech.ipac.firefly.core.background.ScriptAttributes.*;
import static org.junit.Assert.*;

public class DownloadScriptTest {
    private File tempScript;

    @Before
    public void setUp() throws IOException {
        tempScript = File.createTempFile("test_script", ".sh");
    }

    @After
    public void tearDown() {
        tempScript.delete(); // Cleanup after test
    }

    @Test
    public void urlsOnly() throws Exception {
        List<FileInfo> fileInfoList = List.of(
                new FileInfo("https://example.com/file1.txt", "mydata/file1.txt", 0),
                new FileInfo("https://example.com/file2.txt", "mydata/file1.txt", 0)
        );

        DownloadScript.createScript(tempScript, "Test Data", fileInfoList, URLsOnly);

        List<String> lines = Files.readAllLines(tempScript.toPath());

        assertTrue("File1 URL should be present", lines.contains("https://example.com/file1.txt"));
        assertTrue("File2 URL should be present", lines.contains("https://example.com/file2.txt"));
        assertFalse("Script header should not exist in URLsOnly mode", lines.stream().anyMatch(line -> line.startsWith("#!")));
    }

    @Test
    public void wget() throws Exception {
        List<FileInfo> fileInfoList = List.of(
                new FileInfo("https://example.com/file1.zip", "dir1/file1.zip", 0),
                new FileInfo("https://example.com/file1.zip", null, 0),
                new FileInfo("https://example.com/file1.zip", "file1.zip", 0),
                new FileInfo("https://example.com/file1.zip", "dir2/", 0)
        );

        DownloadScript.createScript(tempScript, "Test Data", fileInfoList, Wget, Unzip);

        List<String> lines = Files.readAllLines(tempScript.toPath());

        assertTrue("Script should have header", lines.get(0).startsWith("#! /bin/sh"));
        assertTrue("Wget command should be present", lines.stream().anyMatch(line -> line.contains("wget ")));
        assertTrue("Unzip command should be present", lines.stream().anyMatch(line -> line.contains("unzip ")));
        assertFalse("Mkdir should NOT be present", lines.stream().anyMatch(line -> line.contains("mkdir ")));
    }

    @Test
    public void curl() throws Exception {
        List<FileInfo> fileInfoList = List.of(
                new FileInfo("https://example.com/file1.zip", "dir1/file1.zip", 0),
                new FileInfo("https://example.com/file1.zip", null, 0),
                new FileInfo("https://example.com/file1.zip", "file1.zip", 0),
                new FileInfo("https://example.com/file1.zip", "dir2/", 0)
        );

        DownloadScript.createScript(tempScript, "Test Data", fileInfoList, Curl, Unzip, MakeDirs);

        List<String> lines = Files.readAllLines(tempScript.toPath());

        assertTrue("Script should have header", lines.get(0).startsWith("#! /bin/sh"));
        assertTrue("Curl command should be present", lines.stream().anyMatch(line -> line.contains("curl ")));
        assertTrue("Should set output file", lines.stream().anyMatch(line -> line.contains(" -o dir1/file1.zip")));
        assertTrue("Mkdir should be present", lines.stream().anyMatch(line -> line.contains("mkdir ")));
    }

    @Test
    public void wgetUnzip() throws Exception {
        List<FileInfo> fileInfoList = List.of(
                new FileInfo("https://example.com/file1.txt", "mydata/file1.txt", 0),
                new FileInfo("https://example.com/file1.txt", "mydata/file2.zip", 0)
        );

        DownloadScript.createScript(tempScript, "Test Data", fileInfoList, Wget, Unzip, MakeDirs);

        List<String> lines = Files.readAllLines(tempScript.toPath());

        assertTrue("Script should have header", lines.get(0).startsWith("#! /bin/sh"));
        assertTrue("Wget command should be present", lines.stream().anyMatch(line -> line.contains("wget ")));
        assertTrue("Unzip command should be present", lines.stream().anyMatch(line -> line.contains("unzip -qq -d mydata mydata/file2.zip")));
    }
}
