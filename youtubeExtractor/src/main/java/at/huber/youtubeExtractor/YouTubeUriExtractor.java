package at.huber.youtubeExtractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import com.evgenii.jsevaluator.JsEvaluator;
import com.evgenii.jsevaluator.interfaces.JsCallback;

public abstract class YouTubeUriExtractor extends AsyncTask<String, Void, SparseArray<YtFile>> {

    private final static boolean CACHING = true;
    private final static boolean LOGGING = false;
    private final static String LOG_TAG = "YouTubeUriExtractor";
    private final static String CACHE_FILE_NAME = "decipher_js_funct";
    private final static int DASH_PARSE_RETRIES = 5;

    private Context context;
    private String videoTitle;
    private String youtubeID;
    private boolean includeWebM = true;
    private boolean useHttp = false;
    private boolean parseDashManifest = false;

    private volatile String decipheredSignature;

    private static String decipherJsFileName;
    private static String decipherFunctions;
    private static String decipherFunctionName;

    private final Lock lock = new ReentrantLock();
    private final Condition jsExecuting = lock.newCondition();

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36";

    private static final Pattern patYouTubePageLink = Pattern.compile("(http|https)://(www\\.|m.|)youtube\\.com/watch\\?v=(.+?)( |\\z|&)");
    private static final Pattern patYouTubeShortLink = Pattern.compile("(http|https)://(www\\.|)youtu.be/(.+?)( |\\z|&)");

    private static final Pattern patDashManifest1 = Pattern.compile("dashmpd=(.+?)(&|\\z)");
    private static final Pattern patDashManifest2 = Pattern.compile("\"dashmpd\":\"(.+?)\"");
    private static final Pattern patDashManifestEncSig = Pattern.compile("/s/([0-9A-F|\\.]{10,}?)(/|\\z)");

    private static final Pattern patItag = Pattern.compile("itag=([0-9]+?)(&|,)");
    private static final Pattern patEncSig = Pattern.compile("s=([0-9A-F|\\.]{10,}?)(&|,|\")");
    private static final Pattern patUrl = Pattern.compile("url=(.+?)(&|,)");

    private static final Pattern patVariableFunction = Pattern.compile("(\\{|;| |=)([a-zA-Z$][a-zA-Z0-9$]{0,2})\\.([a-zA-Z$][a-zA-Z0-9$]{0,2})\\(");
    private static final Pattern patFunction = Pattern.compile("(\\{|;| |=)([a-zA-Z$_][a-zA-Z0-9$]{0,2})\\(");
    private static final Pattern patDecryptionJsFile = Pattern.compile("jsbin\\\\/(player-(.+?).js)");
    private static final Pattern patSignatureDecFunction = Pattern.compile("\\(\"signature\",(.+?)\\(");

    private static final SparseArray<Meta> META_MAP = new SparseArray<>();

    static {
        // http://en.wikipedia.org/wiki/YouTube#Quality_and_formats

        // Video and Audio
        META_MAP.put(17, new Meta(17, "3gp", 144, Meta.VCodec.MPEG4, Meta.ACodec.AAC, 24, false));
        META_MAP.put(36, new Meta(36, "3gp", 240, Meta.VCodec.MPEG4, Meta.ACodec.AAC, 32, false));
        META_MAP.put(5, new Meta(5, "flv", 240, Meta.VCodec.H263, Meta.ACodec.MP3, 64, false));
        META_MAP.put(43, new Meta(43, "webm", 360, Meta.VCodec.VP8, Meta.ACodec.VORBIS, 128, false));
        META_MAP.put(18, new Meta(18, "mp4", 360, Meta.VCodec.H264, Meta.ACodec.AAC, 96, false));
        META_MAP.put(22, new Meta(22, "mp4", 720, Meta.VCodec.H264, Meta.ACodec.AAC, 192, false));

        // Dash Video
        META_MAP.put(160, new Meta(160, "mp4", 144, Meta.VCodec.H264, Meta.ACodec.NONE, true));
        META_MAP.put(133, new Meta(133, "mp4", 240, Meta.VCodec.H264, Meta.ACodec.NONE, true));
        META_MAP.put(134, new Meta(134, "mp4", 360, Meta.VCodec.H264, Meta.ACodec.NONE, true));
        META_MAP.put(135, new Meta(135, "mp4", 480, Meta.VCodec.H264, Meta.ACodec.NONE, true));
        META_MAP.put(136, new Meta(136, "mp4", 720, Meta.VCodec.H264, Meta.ACodec.NONE, true));
        META_MAP.put(137, new Meta(137, "mp4", 1080, Meta.VCodec.H264, Meta.ACodec.NONE, true));
        META_MAP.put(264, new Meta(264, "mp4", 1440, Meta.VCodec.H264, Meta.ACodec.NONE, true));
        META_MAP.put(266, new Meta(266, "mp4", 2160, Meta.VCodec.H264, Meta.ACodec.NONE, true));

        META_MAP.put(298, new Meta(298, "mp4", 720, Meta.VCodec.H264, 60, Meta.ACodec.NONE, true));
        META_MAP.put(299, new Meta(299, "mp4", 1080, Meta.VCodec.H264, 60, Meta.ACodec.NONE, true));

        // Dash Audio
        META_MAP.put(140, new Meta(140, "m4a", Meta.VCodec.NONE, Meta.ACodec.AAC, 128, true));
        META_MAP.put(141, new Meta(141, "m4a", Meta.VCodec.NONE, Meta.ACodec.AAC, 256, true));

        // WEBM Dash Video
        META_MAP.put(278, new Meta(278, "webm", 144, Meta.VCodec.VP9, Meta.ACodec.NONE, true));
        META_MAP.put(242, new Meta(242, "webm", 240, Meta.VCodec.VP9, Meta.ACodec.NONE, true));
        META_MAP.put(243, new Meta(243, "webm", 360, Meta.VCodec.VP9, Meta.ACodec.NONE, true));
        META_MAP.put(244, new Meta(244, "webm", 480, Meta.VCodec.VP9, Meta.ACodec.NONE, true));
        META_MAP.put(247, new Meta(247, "webm", 720, Meta.VCodec.VP9, Meta.ACodec.NONE, true));
        META_MAP.put(248, new Meta(248, "webm", 1080, Meta.VCodec.VP9, Meta.ACodec.NONE, true));
        META_MAP.put(271, new Meta(271, "webm", 1440, Meta.VCodec.VP9, Meta.ACodec.NONE, true));
        META_MAP.put(313, new Meta(313, "webm", 2160, Meta.VCodec.VP9, Meta.ACodec.NONE, true));

        META_MAP.put(302, new Meta(302, "webm", 720, Meta.VCodec.VP9, 60, Meta.ACodec.NONE, true));
        META_MAP.put(308, new Meta(308, "webm", 1440, Meta.VCodec.VP9, 60, Meta.ACodec.NONE, true));
        META_MAP.put(303, new Meta(303, "webm", 1080, Meta.VCodec.VP9, 60, Meta.ACodec.NONE, true));
        META_MAP.put(315, new Meta(315, "webm", 2160, Meta.VCodec.VP9, 60, Meta.ACodec.NONE, true));

        // WEBM Dash Audio
        META_MAP.put(171, new Meta(171, "webm", Meta.VCodec.NONE, Meta.ACodec.VORBIS, 128, true));

        META_MAP.put(249, new Meta(249, "webm", Meta.VCodec.NONE, Meta.ACodec.OPUS, 48, true));
        META_MAP.put(250, new Meta(250, "webm", Meta.VCodec.NONE, Meta.ACodec.OPUS, 64, true));
        META_MAP.put(251, new Meta(251, "webm", Meta.VCodec.NONE, Meta.ACodec.OPUS, 160, true));
    }

    public YouTubeUriExtractor(Context con) {
        context = con;
    }

    @Override
    protected void onPostExecute(SparseArray<YtFile> ytFiles) {
        onUrisAvailable(youtubeID, videoTitle, ytFiles);
    }

    public abstract void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles);

    @Override
    protected SparseArray<YtFile> doInBackground(String... params) {
        youtubeID = null;
        String ytUrl = params[0];
        if (ytUrl == null) {
            return null;
        }
        Matcher mat = patYouTubePageLink.matcher(ytUrl);
        if (mat.find()) {
            youtubeID = mat.group(3);
        } else {
            mat = patYouTubeShortLink.matcher(ytUrl);
            if (mat.find()) {
                youtubeID = mat.group(3);
            } else if (ytUrl.matches("\\p{Graph}+?")) {
                youtubeID = ytUrl;
            }
        }
        if (youtubeID != null) {
            try {
                return getStreamUrls();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(LOG_TAG, "Wrong YouTube link format");
        }
        return null;
    }

    private SparseArray<YtFile> getStreamUrls() throws IOException, InterruptedException {

        String ytInfoUrl = (useHttp) ? "http://" : "https://";
        ytInfoUrl += "www.youtube.com/get_video_info?video_id=" + youtubeID + "&eurl="
                + URLEncoder.encode("https://youtube.googleapis.com/v/" + youtubeID, "UTF-8");

        String dashMpdUrl = null;
        String streamMap = null;
        BufferedReader reader = null;
        URL getUrl = new URL(ytInfoUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) getUrl.openConnection();
        urlConnection.setRequestProperty("User-Agent", USER_AGENT);
        try {
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            streamMap = reader.readLine();

        } finally {
            if (reader != null) {
                reader.close();
            }
            urlConnection.disconnect();
        }
        Pattern patTitle;
        Matcher mat;
        String curJsFileName = null;
        String[] streams;
        SparseArray<String> encSignatures = null;

        // Some videos are using a ciphered signature we need to get the
        // deciphering js-file from the youtubepage.
        if (streamMap == null || !streamMap.contains("use_cipher_signature=False")) {
            // Get the video directly from the youtubepage
            if (CACHING
                    && (decipherJsFileName == null || decipherFunctions == null || decipherFunctionName == null)) {
                readDecipherFunctFromCache();
            }
            getUrl = new URL("https://youtube.com/watch?v=" + youtubeID);
            urlConnection = (HttpURLConnection) getUrl.openConnection();
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            try {
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Log.d("line", line);
                    if (line.contains("url_encoded_fmt_stream_map")) {
                        streamMap = line.replace("\\u0026", "&");
                        break;
                    }
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
                urlConnection.disconnect();
            }
            encSignatures = new SparseArray<>();

            patTitle = Pattern.compile("\"title\":( |)\"(.*?)(?<!\\\\)\"");
            mat = patTitle.matcher(streamMap);
            if (mat.find()) {
                videoTitle = mat.group(2);
                videoTitle = videoTitle.replace("\\\"", "\"").replace("\\/", "/");
            }
            mat = patDecryptionJsFile.matcher(streamMap);
            if (mat.find()) {
                curJsFileName = mat.group(1).replace("\\/", "/");
                if (decipherJsFileName == null || !decipherJsFileName.equals(curJsFileName)) {
                    decipherFunctions = null;
                    decipherFunctionName = null;
                }
                decipherJsFileName = curJsFileName;
            }

            if (parseDashManifest) {
                mat = patDashManifest2.matcher(streamMap);
                if (mat.find()) {
                    dashMpdUrl = mat.group(1).replace("\\/", "/");
                    mat = patDashManifestEncSig.matcher(dashMpdUrl);
                    if (mat.find()) {
                        encSignatures.append(0, mat.group(1));
                    } else {
                        dashMpdUrl = null;
                    }
                }
            }
        } else {
            if (parseDashManifest) {
                mat = patDashManifest1.matcher(streamMap);
                if (mat.find()) {
                    dashMpdUrl = URLDecoder.decode(mat.group(1), "UTF-8");
                }
            }
            patTitle = Pattern.compile("title=(.*?)(&|\\z)");
            mat = patTitle.matcher(streamMap);
            if (mat.find()) {
                videoTitle = URLDecoder.decode(mat.group(1), "UTF-8");
            }
            streamMap = URLDecoder.decode(streamMap, "UTF-8");
        }

        streams = streamMap.split(",|url_encoded_fmt_stream_map|&adaptive_fmts=");
        SparseArray<YtFile> ytFiles = new SparseArray<>();
        for (String encStream : streams) {
            encStream = encStream + ",";
            if (!encStream.contains("itag%3D")) {
                continue;
            }
            String stream;
            stream = URLDecoder.decode(encStream, "UTF-8");

            mat = patItag.matcher(stream);
            int itag;
            if (mat.find()) {
                itag = Integer.parseInt(mat.group(1));
                if (LOGGING)
                    Log.d(LOG_TAG, "Itag found:" + itag);
                if (META_MAP.get(itag) == null) {
                    if (LOGGING)
                        Log.d(LOG_TAG, "Itag not in list:" + itag);
                    continue;
                } else if (!includeWebM && META_MAP.get(itag).getExt().equals("webm")) {
                    continue;
                }
            } else {
                continue;
            }

            if (curJsFileName != null) {
                mat = patEncSig.matcher(stream);
                if (mat.find()) {
                    encSignatures.append(itag, mat.group(1));
                }
            }
            mat = patUrl.matcher(encStream);
            String url = null;
            if (mat.find()) {
                url = mat.group(1);
            }

            if (url != null) {
                Meta meta = META_MAP.get(itag);
                String finalUrl = URLDecoder.decode(url, "UTF-8");
                YtFile newVideo = new YtFile(meta, finalUrl);
                ytFiles.put(itag, newVideo);
            }
        }

        if (encSignatures != null) {
            if (LOGGING)
                Log.d(LOG_TAG, "Decipher signatures");
            String signature;
            decipheredSignature = null;
            if (decipherSignature(encSignatures)) {
                lock.lock();
                try {
                    jsExecuting.await(7, TimeUnit.SECONDS);
                } finally {
                    lock.unlock();
                }
            }
            signature = decipheredSignature;
            if (signature == null) {
                return null;
            } else {
                String[] sigs = signature.split("\n");
                for (int i = 0; i < encSignatures.size() && i < sigs.length; i++) {
                    int key = encSignatures.keyAt(i);
                    if (key == 0) {
                        dashMpdUrl = dashMpdUrl.replace("/s/" + encSignatures.get(key), "/signature/" + sigs[i]);
                    } else {
                        String url = ytFiles.get(key).getUrl();
                        url += "&signature=" + sigs[i];
                        YtFile newFile = new YtFile(META_MAP.get(key), url);
                        ytFiles.put(key, newFile);
                    }
                }
            }
        }

        if (parseDashManifest && dashMpdUrl != null) {
            for (int i = 0; i < DASH_PARSE_RETRIES; i++) {
                try {
                    // It sometimes failes to connect for no apparent reason. We just retry.
                    parseDashManifest(dashMpdUrl, ytFiles);
                    break;
                } catch (IOException io) {
                    Thread.sleep(5);
                    if (LOGGING)
                        Log.d(LOG_TAG, "Failed to parse dash manifest " + (i + 1));
                }
            }
        }

        if (ytFiles.size() == 0) {
            if (LOGGING)
                Log.d(LOG_TAG, streamMap);
            return null;
        }
        return ytFiles;
    }

    private boolean decipherSignature(final SparseArray<String> encSignatures) throws IOException {
        // Assume the functions don't change that much
        if (decipherFunctionName == null || decipherFunctions == null) {
            String decipherFunctUrl = "https://s.ytimg.com/yts/jsbin/" + decipherJsFileName;

            BufferedReader reader = null;
            String javascriptFile = null;
            URL url = new URL(decipherFunctUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            try {
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder sb = new StringBuilder("");
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append(" ");
                }
                javascriptFile = sb.toString();
            } finally {
                if (reader != null)
                    reader.close();
                urlConnection.disconnect();
            }

            if (LOGGING)
                Log.d(LOG_TAG, "Decipher FunctURL: " + decipherFunctUrl);
            Matcher mat = patSignatureDecFunction.matcher(javascriptFile);
            if (mat.find()) {
                decipherFunctionName = mat.group(1);
                if (LOGGING)
                    Log.d(LOG_TAG, "Decipher Functname: " + decipherFunctionName);

                Pattern patMainVariable = Pattern.compile("(var |\\s|,|;)" + decipherFunctionName.replace("$", "\\$") +
                        "(=function\\((.{1,3})\\)\\{)");

                String mainDecipherFunct;

                mat = patMainVariable.matcher(javascriptFile);
                if (mat.find()) {
                    mainDecipherFunct = "var " + decipherFunctionName + mat.group(2);
                } else {
                    Pattern patMainFunction = Pattern.compile("function " + decipherFunctionName.replace("$", "\\$") +
                            "(\\((.{1,3})\\)\\{)");
                    mat = patMainFunction.matcher(javascriptFile);
                    if (!mat.find())
                        return false;
                    mainDecipherFunct = "function " + decipherFunctionName + mat.group(2);
                }

                int startIndex = mat.end();

                for (int braces = 1, i = startIndex; i < javascriptFile.length(); i++) {
                    if (braces == 0 && startIndex + 5 < i) {
                        mainDecipherFunct += javascriptFile.substring(startIndex, i) + ";";
                        break;
                    }
                    if (javascriptFile.charAt(i) == '{')
                        braces++;
                    else if (javascriptFile.charAt(i) == '}')
                        braces--;
                }
                decipherFunctions = mainDecipherFunct;
                // Search the main function for extra functions and variables
                // needed for deciphering
                // Search for variables
                mat = patVariableFunction.matcher(mainDecipherFunct);
                while (mat.find()) {
                    String variableDef = "var " + mat.group(2) + "={";
                    if (decipherFunctions.contains(variableDef)) {
                        continue;
                    }
                    startIndex = javascriptFile.indexOf(variableDef) + variableDef.length();
                    for (int braces = 1, i = startIndex; i < javascriptFile.length(); i++) {
                        if (braces == 0) {
                            decipherFunctions += variableDef + javascriptFile.substring(startIndex, i) + ";";
                            break;
                        }
                        if (javascriptFile.charAt(i) == '{')
                            braces++;
                        else if (javascriptFile.charAt(i) == '}')
                            braces--;
                    }
                }
                // Search for functions
                mat = patFunction.matcher(mainDecipherFunct);
                while (mat.find()) {
                    String functionDef = "function " + mat.group(2) + "(";
                    if (decipherFunctions.contains(functionDef)) {
                        continue;
                    }
                    startIndex = javascriptFile.indexOf(functionDef) + functionDef.length();
                    for (int braces = 0, i = startIndex; i < javascriptFile.length(); i++) {
                        if (braces == 0 && startIndex + 5 < i) {
                            decipherFunctions += functionDef + javascriptFile.substring(startIndex, i) + ";";
                            break;
                        }
                        if (javascriptFile.charAt(i) == '{')
                            braces++;
                        else if (javascriptFile.charAt(i) == '}')
                            braces--;
                    }
                }

                if (LOGGING)
                    Log.d(LOG_TAG, "Decipher Function: " + decipherFunctions);
                decipherViaWebView(encSignatures);
                if (CACHING) {
                    writeDeciperFunctToChache();
                }
            } else {
                return false;
            }
        } else {
            decipherViaWebView(encSignatures);
        }
        return true;
    }

    private void parseDashManifest(String dashMpdUrl, SparseArray<YtFile> ytFiles) throws IOException {
        Pattern patBaseUrl = Pattern.compile("<BaseURL yt:contentLength=\"[0-9]+?\">(.+?)</BaseURL>");
        String dashManifest;
        BufferedReader reader = null;
        URL getUrl = new URL(dashMpdUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) getUrl.openConnection();
        urlConnection.setRequestProperty("User-Agent", USER_AGENT);
        try {
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            reader.readLine();
            dashManifest = reader.readLine();

        } finally {
            if (reader != null) {
                reader.close();
            }
            urlConnection.disconnect();
        }
        if (dashManifest == null)
            return;
        Matcher mat = patBaseUrl.matcher(dashManifest);
        while (mat.find()) {
            int itag;
            String url = mat.group(1);
            Matcher mat2 = patItag.matcher(url);
            if (mat2.find()) {
                itag = Integer.parseInt(mat2.group(1));
                if (META_MAP.get(itag) == null)
                    continue;
                if (!includeWebM && META_MAP.get(itag).getExt().equals("webm"))
                    continue;
            } else {
                continue;
            }
            url = url.replace("&amp;", "&").replace(",", "%2C").
                    replace("mime=audio/", "mime=audio%2F").
                    replace("mime=video/", "mime=video%2F");
            YtFile yf = new YtFile(META_MAP.get(itag), url);
            ytFiles.append(itag, yf);
        }

    }

    private void readDecipherFunctFromCache() {
        if (context != null) {
            File cacheFile = new File(context.getCacheDir().getAbsolutePath() + "/" + CACHE_FILE_NAME);
            // The cached functions are valid for 2 weeks
            if (cacheFile.exists() && (System.currentTimeMillis() - cacheFile.lastModified()) < 1209600000) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile), "UTF-8"));
                    decipherJsFileName = reader.readLine();
                    decipherFunctionName = reader.readLine();
                    decipherFunctions = reader.readLine();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * Parse the dash manifest for different dash streams and high quality audio. Default: false
     */
    public void setParseDashManifest(boolean parseDashManifest) {
        this.parseDashManifest = parseDashManifest;
    }


    /**
     * Include the webm format files into the result. Default: true
     */
    public void setIncludeWebM(boolean includeWebM) {
        this.includeWebM = includeWebM;
    }


    /**
     * Set default protocol of the returned urls to HTTP instead of HTTPS.
     * HTTP may be blocked in some regions so HTTPS is the default value.
     * <p/>
     * Note: Enciphered videos require HTTPS so they are not affected by
     * this.
     */
    public void setDefaultHttpProtocol(boolean useHttp) {
        this.useHttp = useHttp;
    }

    private void writeDeciperFunctToChache() {
        if (context != null) {
            File cacheFile = new File(context.getCacheDir().getAbsolutePath() + "/" + CACHE_FILE_NAME);
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cacheFile), "UTF-8"));
                writer.write(decipherJsFileName + "\n");
                writer.write(decipherFunctionName + "\n");
                writer.write(decipherFunctions);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void decipherViaWebView(final SparseArray<String> encSignatures) {
        if (context == null) {
            return;
        }
        final StringBuilder stb = new StringBuilder(decipherFunctions + " function decipher(");
        stb.append("){return ");
        for (int i = 0; i < encSignatures.size(); i++) {
            int key = encSignatures.keyAt(i);
            if (i < encSignatures.size() - 1)
                stb.append(decipherFunctionName).append("('").append(encSignatures.get(key)).append("')+\"\\n\"+");
            else
                stb.append(decipherFunctionName).append("('").append(encSignatures.get(key)).append("')");
        }
        stb.append("};decipher();");

        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                JsEvaluator js = new JsEvaluator(context);
                js.evaluate(stb.toString(),
                        new JsCallback() {
                            @Override
                            public void onResult(final String result) {
                                lock.lock();
                                try {
                                    decipheredSignature = result;
                                    jsExecuting.signal();
                                } finally {
                                    lock.unlock();
                                }
                            }
                        });
            }
        });
    }

}
