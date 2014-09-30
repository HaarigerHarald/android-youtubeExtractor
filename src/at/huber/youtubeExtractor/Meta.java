package at.huber.youtubeExtractor;

public class Meta {

	/**
	 * An identifier used by youtube for different formats.
	 */
	public int itag;
	/**
	 * The file extension and conainer format.
	 */
	public String ext;
	/**
	 * General info about the meta data like dash 1280x720 or dash audio aac
	 */
	public String info;
	/**
	 * The height of the video stream or -1 for audio files.
	 */
	public int height;

    Meta(int itag, String ext, String info, int height) {
    	this.itag=itag;
        this.info = info;
        this.ext = ext;
        this.height=height;
    }
}
