package at.huber.youtubeExtractor;

public class YtFile {

	public Meta meta;
	/**
	 * The url to download the file.
	 */
	public String url = "";

    YtFile(Meta meta, String url) {
    	this.meta=meta;
        this.url = url;
    }
}