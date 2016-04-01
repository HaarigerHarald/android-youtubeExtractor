package at.huber.youtubeExtractor;

public class VideoMeta {

    private static final String IMAGE_BASE_URL = "http://i.ytimg.com/vi/";

    private String videoId;
    private String title;

    private String author;
    private String channelId;

    private long videoLength;
    private long viewCount;

    protected VideoMeta(String videoId, String title, String author, String channelId, long videoLength, long viewCount) {
        this.videoId = videoId;
        this.title = title;
        this.author = author;
        this.channelId = channelId;
        this.videoLength = videoLength;
        this.viewCount = viewCount;
    }

    // 120 x 90
    public String getThumbUrl() {
        return IMAGE_BASE_URL + videoId + "/default.jpg";
    }

    // 320 x 180
    public String getMqImageUrl() {
        return IMAGE_BASE_URL + videoId + "/mqdefault.jpg";
    }

    // 480 x 360
    public String getHqImageUrl() {
        return IMAGE_BASE_URL + videoId + "/hqdefault.jpg";
    }

    // 640 x 480
    public String getSdImageUrl() {
        return IMAGE_BASE_URL + videoId + "/sddefault.jpg";
    }

    // Max Res
    public String getMaxResImageUrl() {
        return IMAGE_BASE_URL + videoId + "/maxresdefault.jpg";
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getChannelId() {
        return channelId;
    }

    /**
     * The video length in seconds.
     */
    public long getVideoLength() {
        return videoLength;
    }

    public long getViewCount() {
        return viewCount;
    }

}
