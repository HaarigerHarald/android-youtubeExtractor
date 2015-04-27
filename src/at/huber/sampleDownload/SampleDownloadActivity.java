package at.huber.sampleDownload;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

public class SampleDownloadActivity extends Activity {

	private static String youtubeLink;
	
	private LinearLayout mainLayout;
	private ProgressBar mainProgressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_sample_download);
		mainLayout=(LinearLayout) findViewById(R.id.main_layout);
		mainProgressBar=(ProgressBar) findViewById(R.id.prgrBar);

		// Check how it was started and if we can get the youtube link
		if (savedInstanceState == null && Intent.ACTION_SEND.equals(getIntent().getAction())
				&& getIntent().getType() != null && "text/plain".equals(getIntent().getType())){

			String ytLink=getIntent().getStringExtra(Intent.EXTRA_TEXT);

			if (ytLink != null
					&& (ytLink.contains("://youtu.be/") || ytLink.contains("youtube.com/watch?v="))){
				youtubeLink=ytLink;
				// We have a valid link
				getYoutubeDownloadUrl(youtubeLink);
			}else{
				Toast.makeText(this, R.string.error_no_yt_link, Toast.LENGTH_LONG).show();
				finish();
			}
		}else if (savedInstanceState != null && youtubeLink != null){
			getYoutubeDownloadUrl(youtubeLink);
		}else{
			finish();
		}
	}

	private void getYoutubeDownloadUrl(String youtubeLink) {
		YouTubeUriExtractor ytEx=new YouTubeUriExtractor(this) {

			@Override
			public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
				mainProgressBar.setVisibility(View.GONE);

				if (ytFiles == null){
					// Something went wrong we got no urls. Always check this.
					finish();
					return;
				}
				// Iterate over itags
				for(int i=0, itag=0; i < ytFiles.size(); i++){
					itag=ytFiles.keyAt(i);
					// ytFile represents one file with its url and meta data
					YtFile ytFile=ytFiles.get(itag);

					// Just add videos in a decent format => height -1 = audio
					if (ytFile.getMeta().getHeight() == -1 || ytFile.getMeta().getHeight() >= 360){
						addButtonToMainLayout(videoTitle, ytFile);
					}
				}
			}
		};
		// Ignore the google proprietary webm format
		ytEx.setIncludeWebM(false);
		// Lets execute the request
		ytEx.execute(youtubeLink);

	}

	private void addButtonToMainLayout(final String videoTitle, final YtFile ytfile) {
		// Display some buttons and let the user choose the format
		String btnText=(ytfile.getMeta().getHeight() == -1) ? "Audio m4a" : ytfile.getMeta().getInfo();
		Button btn=new Button(this);
		btn.setText(btnText);
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String filename;
				if (videoTitle.length() > 55){
					filename=videoTitle.substring(0, 55) + "." + ytfile.getMeta().getExt();
				}else{
					filename=videoTitle + "." + ytfile.getMeta().getExt();
				}
				filename=filename.replaceAll("\\\\|>|<|\"|\\||\\*|\\?|%|:|#|/", "");
				downloadFromUrl(ytfile.getUrl(), videoTitle, filename);
				finish();
			}
		});
		mainLayout.addView(btn);
	}

	private void downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName) {
		Uri uri=Uri.parse(youtubeDlUrl);
		DownloadManager.Request request=new DownloadManager.Request(uri);
		request.setTitle(downloadTitle);
		
		request.allowScanningByMediaScanner();
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

		// get download service and enqueue file
		DownloadManager manager=(DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		manager.enqueue(request);
	}

}
