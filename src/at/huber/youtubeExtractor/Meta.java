package at.huber.youtubeExtractor;

public class Meta {
	
	public enum VCodec{
		 H263, H264, MPEG4, VP8, VP9, NONE
	}
	
	public enum ACodec{
		MP3, AAC, VORBIS, NONE
	}

	private int itag;
	private String ext;
	private int height;
	private int fps;
	private VCodec vCodec;
	private ACodec aCodec;
	private boolean isDashContainer;

	Meta(int itag, String ext, int height, VCodec vCodec, ACodec aCodec, boolean isDashContainer) {
		this.itag=itag;
		this.ext=ext;
		this.height=height;
		this.fps=30;
		this.isDashContainer=isDashContainer;
	}

	Meta(int itag, String ext,int height, VCodec vCodec, ACodec aCodec, int fps, boolean isDashContainer) {
		this.itag=itag;
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
	
	public ACodec getAudioCodec(){
		return aCodec;
	}
	
	public VCodec getVideoCodec(){
		return vCodec;
	}

	/**
	 * The height of the video stream or -1 for audio files.
	 */
	public int getHeight() {
		return height;
	}

}
