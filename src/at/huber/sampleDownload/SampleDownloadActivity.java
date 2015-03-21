package at.huber.sampleDownload;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import at.huber.sampleDownload.R;
import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

public class SampleDownloadActivity extends Activity {

	LinearLayout mainLayout;
	ProgressBar mainProgressBar;
	Context activityContext;

	private static String youtubeLink;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_sample_download);
		colorTitle(Color.parseColor("#E62117"));
		mainLayout=(LinearLayout) findViewById(R.id.main_layout);
		mainProgressBar=(ProgressBar) findViewById(R.id.prgrBar);
		activityContext=this;

		// Check how it was started and if we can get the youtube link somehow
		if (savedInstanceState == null && Intent.ACTION_SEND.equals(getIntent().getAction())
				&& getIntent().getType() != null && "text/plain".equals(getIntent().getType())){

			String ytLink=getIntent().getStringExtra(Intent.EXTRA_TEXT);

			if (ytLink != null
					&& (ytLink.contains("://youtu.be/") || ytLink.contains("youtube.com/watch?v="))){
				youtubeLink=ytLink;
				// We have a valid link such as: http://youtu.be/xxxx or
				// http://youtube.com/watch?v=xxxx
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
					// Something went wrong we got no urls. Please always check
					// this.
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
		// Lets display some buttons to let the user choose the format he wants
		// to download
		String btnText=(ytfile.getMeta().getHeight() == -1) ? "Audio m4a" : ytfile.getMeta().getInfo();
		Button btn=new Button(activityContext);
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
				filename=filename.replaceAll("\\\\|>|<|\"|\\||\\*|\\?|%|:", "");
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
		// in order for this if to run, you must use the android 3.2 to compile
		// your app
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			request.allowScanningByMediaScanner();
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

		// get download service and enqueue file
		DownloadManager manager=(DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		manager.enqueue(request);
	}

	private void colorTitle(int color) {
		try{
			int dividerId=getResources().getIdentifier("android:id/titleDivider", null, null);
			View divider=findViewById(dividerId);
			divider.setBackgroundColor(color);
			int textViewId=getResources().getIdentifier("android:id/title", null, null);
			TextView tv=(TextView) findViewById(textViewId);
			tv.setTextColor(color);
		}catch (NullPointerException npe){
			// Ignore something has changed
		}
	}

}
