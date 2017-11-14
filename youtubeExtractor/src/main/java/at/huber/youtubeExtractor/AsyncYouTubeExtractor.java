package at.huber.youtubeExtractor;

import android.content.Context;
import android.os.AsyncTask;
import android.util.SparseArray;

public abstract class AsyncYouTubeExtractor extends AsyncTask<String, Void, SparseArray<YtFile>> {
    private YouTubeExtractor youTubeExtractor;
    private boolean parseDashManifest;
    private boolean includeWebM;

    public AsyncYouTubeExtractor(Context con) {
        youTubeExtractor = new YouTubeExtractor(con);
    }

    @Override
    protected void onPostExecute(SparseArray<YtFile> ytFiles) {
        onExtractionComplete(ytFiles, youTubeExtractor.getVideoMeta());
    }

    /**
     * Start the extraction.
     *
     * @param youtubeLink       the youtube page link or video id
     * @param parseDashManifest true if the dash manifest should be downloaded and parsed
     * @param includeWebM       true if WebM streams should be extracted
     */
    public void extract(String youtubeLink, boolean parseDashManifest, boolean includeWebM) {
        this.parseDashManifest = parseDashManifest;
        this.includeWebM = includeWebM;
        this.execute(youtubeLink);
    }

    @Override
    protected SparseArray<YtFile> doInBackground(String... params) {
        if (params.length != 1) {
            return null;
        }

        String youtubeUrl = params[0];

        return youTubeExtractor.extract(youtubeUrl, parseDashManifest, includeWebM);
    }

    protected abstract void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta);
}
