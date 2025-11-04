package edu.caltech.ipac.util.download;


import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.HttpResultInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Trey Roby
 *
 */
public class ConcurrentDownload {

    private static final Logger.LoggerImpl log = Logger.getLogger();
    private static final Map<String, ForkJoinPool> exeMap= Collections.synchronizedMap(new HashMap<>());


    public static FileInfo getData(URL url, File outfile, Map<String, String> cookies,
                                   Map<String, String> requestHeaders, URLDownload.Options ops)  throws FailedRequestException{
        if (outfile==null) throw new FailedRequestException("outfile is null");

        var newHeaders = requestHeaders!=null ?  new HashMap<>(requestHeaders) : new HashMap<String,String>();

        if (outfile.canRead() && ops.onlyIfModified()) {
            newHeaders.put("If-Modified-Since", outfile.lastModified()+"");
        }

        HttpResultInfo result= URLDownload.getHeader(url,cookies,newHeaders,8);

        var status= result.getResponseCode();
        if (status>=400 && status!=HttpURLConnection.HTTP_BAD_METHOD) {
            logHeadFail(url,result);
            return new FileInfo(result.getResponseCode(), result.getResponseCodeMsg());
        }
        if (status == HttpURLConnection.HTTP_NOT_MODIFIED) return notModified(url,outfile,result);

        try {
            URL finalUrl= result.isRedirected() ? new URI(result.getLocation()).toURL() : url;
            if (partialDownloadQualified(result)) {
                return doMultiThreadedDownload(finalUrl,outfile,cookies,requestHeaders,ops,result);
            }
            else {
                return URLDownload.getDataToFile(finalUrl,outfile,cookies,requestHeaders,ops);
            }
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            throw new FailedRequestException("redirect URL could not be parsed");
        }
    }


    private static FileInfo doMultiThreadedDownload(URL url, File outfile, Map<String, String> cookies,
                                                    Map<String, String> requestHeaders, URLDownload.Options ops,
                                                    HttpResultInfo headResult) throws FailedRequestException{
        StopWatch.Tracker tracker = new StopWatch.Tracker("Multi-threaded Download", null);
        tracker.starts();
        var len= headResult.getContentLength();
        var workerCnt= getWorkerCnt(len);
        var partSize= (len % workerCnt== 0) ? len/workerCnt : (len/workerCnt)+1;
        var pdList = new ArrayList<PartialDownload>();
        var resultsList= new ArrayList<Future<Void>>();

        try {
            ForkJoinPool exeService= exeMap.computeIfAbsent(
                    ServerContext.getRequestOwner().getUserKey(),
                    k -> makeExecutorService());
            try (var outRaf = new RandomAccessFile(outfile, "rw") ) {
                outRaf.setLength(len);
            } catch (IOException e) {
                throw new FailedRequestException("could not create output file");
            }

            try (var outRaf = new RandomAccessFile(outfile, "rw") ) {
                var outChannel= outRaf.getChannel();
                
                for(var i=0;i<workerCnt;i++) {
                    var start = i * partSize;
                    var end = (i < workerCnt - 1) ? (i + 1) * partSize - 1 : len - 1;
                    var pd= new PartialDownload(i,
                            outChannel.map(FileChannel.MapMode.READ_WRITE, start, end-start+1),
                            url, cookies, requestHeaders, start, end);
                    pdList.add(pd);
                    resultsList.add( exeService.submit(pd::download));
                }

                DownloadListener dl = ops.dl();
                for (var i = 0; !isDone(resultsList); i++) {
                    if (dl != null && i % 8 == 0) {
                        callListener(dl, transferredBytes(pdList), len);
                    }
                    if (!isDone(resultsList)) Thread.sleep(100);
                }

                if (pdList.stream().allMatch( pd -> pd.getStatus()==HttpURLConnection.HTTP_PARTIAL)) {
                    outRaf.close(); //closing explicitly here - I want the tme to reflect any flushing
                    callListener(dl, len, 0);
                    tracker.stops();
                    double dlSeconds = tracker.getElapsedTime(StopWatch.Unit.SECONDS);
                    logSuccess(headResult,outfile,url,dlSeconds,pdList.size(),headResult);
                    return new FileInfo(outfile, headResult.getExternalName(), 200, headResult.getContentType());
                }
            } catch (IOException e) {
                tracker.stops();
                double seconds = tracker.getElapsedTime(StopWatch.Unit.SECONDS);
                logFail(e,outfile,url,0,"",seconds);
                throw new FailedRequestException("cannot write to file: " + outfile.toPath());
            }
            finally {
                clearIdleExecutors();
            }

            // if any not 206 something when wrong, delete the file and return failure
            tracker.stops();
            double seconds = tracker.getElapsedTime(StopWatch.Unit.SECONDS);
            var anyFailList= pdList.stream().filter( pd -> pd.getStatus()!=HttpURLConnection.HTTP_PARTIAL).toList();
            var ignore= outfile.delete();
            var stats= anyFailList.stream().map( pd -> pd.getStatus()+"").toList();
            String statusStr= "status: " + String.join(", ",  stats.toArray(new String[0]));
            logFail(null,outfile,url,409,statusStr,seconds);
            return new FileInfo(null,null,409,statusStr);
        } catch (InterruptedException e) {
            double seconds = tracker.getElapsedTime(StopWatch.Unit.SECONDS);
            logFail(e,null,url,0,"",seconds);
            throw new FailedRequestException(e.getMessage(),e);
        }
    }

    private static boolean isFileLarge(long length) { return getWorkerCnt(length) > 1; }

    private static int getWorkerCnt(long contentLen) {
        long lenMeg= contentLen/FileUtil.MEG;
        if (lenMeg<50) return 1;
        if (lenMeg<200) return 2;
        if (lenMeg<400) return 3;
        if (lenMeg<600) return 4;
        if (lenMeg<800) return 5;
        return 6;
    }

    private static boolean isDone(List<Future<Void>> fList) {
        return fList.stream().allMatch(Future::isDone);
    }

    private static FileInfo notModified(URL url, File outfile, HttpResultInfo result) {
        logNotModified(outfile, url);
        return new FileInfo(outfile, "",
                HttpURLConnection.HTTP_NOT_MODIFIED,
                ResponseMessage.getHttpResponseMessage(HttpURLConnection.HTTP_NOT_MODIFIED),
                result.getContentType());
    }

    private static boolean partialDownloadQualified(HttpResultInfo result) {
        return result.isOK() &&
                isFileLarge(result.getContentLength()) &&
                "bytes".equalsIgnoreCase(result.getAttribute("Accept-Ranges"));
    }

    private static long transferredBytes(ArrayList<PartialDownload> pdList) {
        var total=0L;
        for (var pd : pdList) {total+= pd.getTransferredBytes();}
        return total;
    }

    private static void callListener(DownloadListener dl, long transferredBytes, long length) {
        String msg;
        if (length==0) {
            msg= FileUtil.getSizeAsString(transferredBytes);
        }
        else {
            msg= (transferredBytes==length)
                    ? FileUtil.getSizeAsString(length)
                    : String.format("%s of %s", FileUtil.getSizeAsString(transferredBytes), FileUtil.getSizeAsString(length));
        }
        var ev = new DownloadEvent(ConcurrentDownload.class, transferredBytes, length, 0, 0, "", "", msg);
        dl.dataDownloading(ev);
    }

    private static void logPartError(int index, URL url, String range, int responseCode, String contentRange) {
        var s= String.format("part error: %d: %s, %s, %d, %s",
                index, url.toString(), range, responseCode,contentRange);
        log.info(s);
    }

    private static void logNotModified(File outfile, URL url) {
        String send= String.format( "URL Download (Not Modified): %s\n", url.toString() );
        String file= "        File: "+ outfile.toPath();
        log.info(send+file);
    }

    private static void logSuccess(HttpResultInfo r, File outfile, URL url,  double dSeconds, int parts, HttpResultInfo headerR) {
        String formatedSize= FileUtil.getSizeAsString(r.getContentLength());
        String lastMod= r.getAttribute("Last-Modified")!=null ? ", Last-Modified: " +r.getAttribute("Last-Modified") : "";
        log.info(
                String.format( "DOWNLOAD (%.1f sec, %s, %d parts): Content-Type: %s, Content-Length: %s%s",
                        dSeconds, formatedSize, parts, r.getContentType(), r.getContentLength(), lastMod),
                "url:  "+ url.toString(),
                "file: "+ outfile.toPath(),
                "headers sent: " + sendHeadersToStr(headerR),
                "more headers: "+otherHeadersToStr(r)
        );
    }

    private static String otherHeadersToStr(HttpResultInfo r) {
        StringBuilder out= new StringBuilder();
        int cnt=0;
        for(var a : r.getAttributes().entrySet()) {
            var k = a.getKey();;
            if (!r.isReservedKey(k) && !k.equalsIgnoreCase("<none>") &&
                    !k.equalsIgnoreCase("content-type") && !k.equalsIgnoreCase("content-length") &&
                    !k.equalsIgnoreCase("Last-Modified") ) {
                if (cnt>0) out.append(", ");
                out.append(String.format("%s: %s", k, a.getValue()));
                cnt++;
            }
        }
        return out.toString();
    }

    private static String sendHeadersToStr(HttpResultInfo r) {
        if (r.getSendHeaders()==null) return "";
        StringBuilder out= new StringBuilder();
        int cnt=0;
        for(var a : r.getSendHeaders().entrySet()) {
            var k = a.getKey();
            if (cnt>0) out.append(", ");
            out.append(String.format("%s: %s", k, a.getValue()));
            cnt++;
        }
        return out.toString();
    }

    private static void logHeadFail(URL url, HttpResultInfo r) {
        String send= String.format( "FAIL: Concurrent Download Header (%d, %s): %s",
                r.getResponseCode(), r.getResponseCodeMsg(), url);
        log.info(send,"headers sent: " + sendHeadersToStr(r));
    }

    private static void logFail(Exception e, File outfile, URL url, int statusCode, String statusMessage, double seconds) {
        if (e!=null) log.error(e);
        String send= String.format( "Concurrent Download Failed (%d, %s, %.1f sec): %s\n",
                statusCode, statusMessage, seconds, url);
        String file= outfile!=null ? "        File: "+ outfile.toPath() : "";
        log.info(send+file);
    }

    private static ForkJoinPool makeExecutorService () { return (ForkJoinPool)Executors.newWorkStealingPool(); }

    private static boolean isExecutorIdle(ExecutorService executor) {
        if (executor instanceof ForkJoinPool forkJoin) {
            long active= forkJoin.getQueuedTaskCount();
            int queued= forkJoin.getQueuedSubmissionCount();
            return (active==0 && queued==0);
        }
        else if (executor instanceof ThreadPoolExecutor tpExe){
            int active= tpExe.getActiveCount();
            int queued= tpExe.getQueue().size();
            return (active==0 && queued==0);
        }
        return false;

    }

    private static void clearIdleExecutors() {
        var toRemoveKeyList = new ArrayList<String>();
        for(var entry : exeMap.entrySet()) {
            var executor= entry.getValue();
            if (isExecutorIdle(executor)) {
                executor.shutdown();
                toRemoveKeyList.add(entry.getKey());
            }
        }
        if (!toRemoveKeyList.isEmpty()) {
            synchronized(exeMap) {
                for(var key : toRemoveKeyList) {
                    var exe= exeMap.get(key);
                    exeMap.remove(key);
                    if (exe!=null) exe.shutdown();
                }
            }
        }
    }

//    static boolean sameDomain(URL url) {
//        var urlHost= url.getHost();
//        var idx= urlHost.indexOf(".");
//        var urlDomain= (idx==-1) ? urlHost : urlHost.substring(idx);
//        if (StringUtils.isEmpty(urlDomain)) return true;
//        var sUrl= Util.Try.it(() -> new URI(ServerContext.getRequestOwner().getBaseUrl()).toURL()).getOrElse((URL)null);
//        if (sUrl==null) return false;
//        idx= sUrl.getHost().indexOf(".");
//        var serverDomain= (idx==-1) ? urlHost : urlHost.substring(idx);
//        return (serverDomain.toLowerCase().contains(urlDomain.toLowerCase()));
//    }

    private static class PartialDownload {
        private final URL url;
        private final Map<String, String> cookies;
        private final Map<String, String> requestHeaders;
        private final long start;
        private final long end;
        private int status;
        private final int index;
        private long transferredBytes = 0;
        private final MappedByteBuffer outBuf;

        public PartialDownload(int index, MappedByteBuffer outBuf, URL url, Map<String, String> cookies,
                               Map<String, String> requestHeaders, long start, long end) {
            this.url= url;
            this.cookies= cookies;
            this.requestHeaders= requestHeaders;
            this.start= start;
            this.end= end;
            this.index= index;
            this.outBuf= outBuf;
        }

        public int getStatus() { return status; }
        public long getTransferredBytes() { return transferredBytes; }

        Void download() {
            URLDownload.Options localOps= URLDownload.Options.def();
            localOps.setDl(ev -> transferredBytes = ev.getCurrent());
            localOps.setLogErrorsOnly(true);
            var newHeaders = requestHeaders!=null ?  new HashMap<>(requestHeaders) : new HashMap<String,String>();
            var range= "bytes=" + start + "-" + end;
            newHeaders.put("Range", range);
            String contentRange;
            try {
                var out= URLDownload.getDataFromURL(url,null,cookies,newHeaders,outBuf,localOps);
                this.status= out.getResponseCode();
                contentRange= out.getAttribute("Content-Range");
                if (this.status!=HttpURLConnection.HTTP_PARTIAL) logPartError(index, url,range,this.status,contentRange);
            } catch (Throwable e) {
                log.error(e,"part error exception: " +index+ " range: "+range);
            }
            return null;
        }
    }

}
