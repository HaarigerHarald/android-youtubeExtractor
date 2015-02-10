package at.huber.youtubeExtractor;

public class Meta {

	int itag;
	String ext;
	String info;
	int height;

	Meta(int itag, String ext, String info, int height) {
		this.itag=itag;
		this.info=info;
		this.ext=ext;
		this.height=height;
	}

	/**
	 * An identifier used by youtube for different formats.
	 */
	public int getItag() {
		return itag;
	}

	/**
	 * The file extension and conainer format like "mp4"
	 */
	public String getExt() {
		return ext;
	}

	/**
	 * General info about the meta data like "dash 1280x720" or "dash audio aac"
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * The height of the video stream or -1 for audio files.
	 */
	public int getHeight() {
		return height;
	}

}
