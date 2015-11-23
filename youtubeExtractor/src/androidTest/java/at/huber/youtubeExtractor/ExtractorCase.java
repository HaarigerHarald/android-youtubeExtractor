package at.huber.youtubeExtractor;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.test.InstrumentationTestCase;
import android.util.Log;
import android.util.SparseArray;

import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

public class ExtractorCase extends InstrumentationTestCase {

    private static final String EXTRACTOR_TEST_TAG = "Extractor Test";

    private String testUrl;

    public void testUsualVideo() throws Throwable {
        extractorTest("http://youtube.com/watch?v=YE7VzlLtp-4", "YE7VzlLtp-4", "Big Buck Bunny");
        extractorTestDashManifest("http://youtube.com/watch?v=YE7VzlLtp-4");
    }


    public void testEncipheredVideo() throws Throwable {
        extractorTest("https://www.youtube.com/watch?v=e8X3ACToii0", "e8X3ACToii0",
                "Rise Against - Savior");
        extractorTestDashManifest("https://www.youtube.com/watch?v=e8X3ACToii0");
    }

    public void testAgeRestrictVideo() throws Throwable {
        extractorTest("http://www.youtube.com/watch?v=61Ev-YvBw2c", "61Ev-YvBw2c",
                "Test video for age-restriction");
        extractorTestDashManifest("http://www.youtube.com/watch?v=61Ev-YvBw2c");
    }

    private void extractorTestDashManifest(final String youtubeLink)
            throws Throwable {
        final CountDownLatch signal = new CountDownLatch(1);
        testUrl = null;

        runTestOnUiThread(new Runnable() {

            @Override
            public void run() {
                final YouTubeUriExtractor ytEx = new YouTubeUriExtractor(getInstrumentation()
                        .getTargetContext()) {
                    @Override
                    public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
                        assertNotNull(ytFiles);
                        int numNotDash = 0;
                        int itag;
                        for (int i = 0; i < ytFiles.size(); i++) {
                            itag = ytFiles.keyAt(i);
                            if (ytFiles.get(itag).getMeta().isDashContainer()) {
                                numNotDash = i;
                                break;
                            }
                        }
                        itag = ytFiles.keyAt(new Random().nextInt(ytFiles.size() - numNotDash) + numNotDash);
                        Log.d(EXTRACTOR_TEST_TAG, "Testing itag:" + itag);
                        testUrl = ytFiles.get(itag).getUrl();
                        signal.countDown();
                    }
                };
                ytEx.setParseDashManifest(true);
                ytEx.execute(youtubeLink);
            }
        });

        signal.await(10, TimeUnit.SECONDS);

        assertNotNull(testUrl);

        final URL url = new URL(testUrl);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int code = con.getResponseCode();
        assertEquals(code, 200);
        con.disconnect();
    }


    private void extractorTest(final String youtubeLink, final String expVideoId, final String expVideoTitle)
            throws Throwable {
        final CountDownLatch signal = new CountDownLatch(1);
        testUrl = null;

        runTestOnUiThread(new Runnable() {

            @Override
            public void run() {
                final YouTubeUriExtractor ytEx = new YouTubeUriExtractor(getInstrumentation()
                        .getTargetContext()) {
                    @Override
                    public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
                        assertEquals(videoId, expVideoId);
                        assertEquals(videoTitle, expVideoTitle);
                        assertNotNull(ytFiles);
                        int itag = ytFiles.keyAt(new Random().nextInt(ytFiles.size()));
                        Log.d(EXTRACTOR_TEST_TAG, "Testing itag:" + itag);
                        testUrl = ytFiles.get(itag).getUrl();
                        signal.countDown();
                    }
                };
                ytEx.execute(youtubeLink);
            }
        });

        signal.await(10, TimeUnit.SECONDS);

        assertNotNull(testUrl);

        final URL url = new URL(testUrl);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int code = con.getResponseCode();
        assertEquals(code, 200);
        con.disconnect();
    }

}
