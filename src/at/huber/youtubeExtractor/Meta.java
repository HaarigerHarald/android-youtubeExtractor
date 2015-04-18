package at.huber.youtubeExtractor;

public class Meta {

	private int itag;
	private String ext;
	private String info;
	private int height;
	private int fps;
	private boolean isDashContainer;

	Meta(int itag, String ext, String info, int height, boolean isDashContainer) {
		this.itag=itag;
		this.info=info;
		this.ext=ext;
		this.height=height;
		this.fps=30;
		this.isDashContainer=isDashContainer;
	}

	Meta(int itag, String ext, String info, int height, int fps, boolean isDashContainer) {
		this.itag=itag;
		this.info=info;
		this.ext=ext;
		this.height=height;
		this.fps=fps;
		this.isDashContainer=isDashContainer;
	}

	/**
	 * Get the frames per second
	 */
	public int getFps() {
		return fps;
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

	public boolean isDashContainer() {
		return isDashContainer;
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
