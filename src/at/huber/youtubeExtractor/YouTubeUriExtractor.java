package at.huber.youtubeExtractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import com.evgenii.jsevaluator.JsEvaluator;
import com.evgenii.jsevaluator.interfaces.JsCallback;

public abstract class YouTubeUriExtractor extends AsyncTask<String, String, SparseArray<YtFile>> {

	private final static boolean CACHING=true;
	private final static boolean LOGGING=false;
	private final static String CACHE_FILE_NAME="decipher_js_funct";

	private Context context;
	private String videoTitle="youtube";
	private String youtubeID="";
	private JsEvaluator js;
	private boolean includeWebM=true;

	private volatile String decipheredSignature;

	private static String decipherJsFileName;
	private static String decipherFunctions;
	private static String decipherFunctionName;

	private final static Lock lock=new ReentrantLock();
	private final static Condition jsExecuting=lock.newCondition();

	private static final Pattern patItag=Pattern.compile("itag=([0-9]+?)[&]");
	private static final Pattern patSig=Pattern.compile("signature=(.+?)[&|,|\\\\]");
	private static final Pattern patEncSig=Pattern.compile("s=([0-9A-F|\\.]{10,}?)[&|,|\"]");
	private static final Pattern patUrl=Pattern.compile("url=(.*?)[&|,|\\\\]");

	private static final Pattern patVariableFunction = Pattern.compile("(\\{|;| |=)(([a-zA-Z$]{1}[a-zA-Z0-9$]{0,2}))\\.([a-zA-Z$]{1}[a-zA-Z0-9$]{0,2})\\(");
	private static final Pattern patFunction = Pattern.compile("(\\{|;| |=)(([a-zA-Z$_]{1}[a-zA-Z0-9$]{0,2}))\\(");
	private static final Pattern patDecryptionJsFile=Pattern.compile("html5player-(.+?).js");
	private static final Pattern patSignatureDecFunction=Pattern.compile("\\(\"signature\",((.+?))\\(");

	public static final SparseArray<Meta> META_MAP=new SparseArray<Meta>();
	static{
		// Video and Audio
		META_MAP.put(17, new Meta(17, "3gp", "176x144", 144));
		META_MAP.put(36, new Meta(36, "3gp", "426x240", 240));
		META_MAP.put(5, new Meta(5, "flv", "426x240", 240));
		META_MAP.put(43, new Meta(43, "webm", "640x360", 360));
		META_MAP.put(18, new Meta(18, "mp4", "640x360", 360));
		META_MAP.put(22, new Meta(22, "mp4", "1280x720", 720));

		// Dash Video
		META_MAP.put(160, new Meta(160, "mp4", "dash 176x144", 144));
		META_MAP.put(133, new Meta(133, "mp4", "dash 426x240", 240));
		META_MAP.put(134, new Meta(134, "mp4", "dash 640x360", 360));
		META_MAP.put(135, new Meta(135, "mp4", "dash 854x480", 480));
		META_MAP.put(136, new Meta(136, "mp4", "dash 1280x720", 720));
		META_MAP.put(137, new Meta(137, "mp4", "dash 1920x1080", 1080));

		// Dash Audio
		META_MAP.put(140, new Meta(140, "m4a", "dash audio aac", -1));

		// WEBM Dash Video
		META_MAP.put(242, new Meta(242, "webm", "dash 426x240", 240));
		META_MAP.put(243, new Meta(243, "webm", "dash 640x360", 360));
		META_MAP.put(244, new Meta(244, "webm", "dash 854x480", 480));
		META_MAP.put(247, new Meta(247, "webm", "dash 1280x720", 720));
		META_MAP.put(248, new Meta(248, "webm", "dash 1920x1080", 1080));

		// WEBM Dash Audio
		META_MAP.put(171, new Meta(171, "webm", "dash audio aac", -1));
	}

	public YouTubeUriExtractor(Context con) {
		context=con;
	}

	@Override
	protected void onPostExecute(SparseArray<YtFile> ytFiles) {
		onUrisAvailable(youtubeID, videoTitle, ytFiles);
	}

	public abstract void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles);

	@Override
	protected SparseArray<YtFile> doInBackground(String... params) {
		String ytUrl=params[0];
		if (ytUrl == null){
			return null;
		}

		if (ytUrl.contains("://youtu.be/")){
			youtubeID=ytUrl.substring(ytUrl.lastIndexOf("/") + 1);
			ytUrl="http://youtube.com/watch?v=" + youtubeID;
		}else if (ytUrl.contains("watch?v=")){
			if (ytUrl.contains("&")){
				ytUrl=ytUrl.substring(0, ytUrl.indexOf('&'));
			}
			if (ytUrl.contains("https")){
				ytUrl.replace("https", "http");
			}
			youtubeID=ytUrl.substring(ytUrl.indexOf("watch?v=") + 8);
		}
		try{
			return getStreamUrls(ytUrl);
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

	private SparseArray<YtFile> getStreamUrls(String ytUrl) throws IOException, InterruptedException {

		String ytInfoUrl="http://www.youtube.com/get_video_info?video_id=" + youtubeID + "&eurl="
				+ URLEncoder.encode("https://youtube.googleapis.com/v/" + youtubeID, "UTF-8");

		HttpClient client=new DefaultHttpClient();
		HttpGet request=new HttpGet(ytInfoUrl);
		HttpResponse response=client.execute(request);
		InputStream in=response.getEntity().getContent();
		BufferedReader reader=new BufferedReader(new InputStreamReader(in));
		String streamMap=reader.readLine();
		in.close();
		reader.close();
		Pattern patTitle;
		Matcher mat;
		String curJsFileName=null;
		String[] streams;

		// Some videos are using a ciphered signature we need to get the
		// deciphering js-file from the youtubepage.
		if (streamMap == null || !streamMap.contains("use_cipher_signature=False")){
			// Get the video directly from the youtubepage
			if (CACHING
					&& (decipherJsFileName == null || decipherFunctions == null || decipherFunctionName == null)){
				readDecipherFunctFromCache();
			}

			request=new HttpGet(ytUrl);
			response=client.execute(request);
			in=response.getEntity().getContent();
			reader=new BufferedReader(new InputStreamReader(in));
			String line=null;
			while ((line=reader.readLine()) != null){
				if (line.contains("stream_map")){
					streamMap=line.replace("\\u0026", "&");
					break;
				}
			}
			in.close();
			reader.close();

			patTitle= Pattern.compile("\"title\":( |)\"((.*?))(?<!\\\\)\"");
			mat= patTitle.matcher(streamMap);
			if(mat.find()){
				videoTitle=mat.group(2);
				videoTitle=videoTitle.replace("\\\"", "\"");
			}
			mat=patDecryptionJsFile.matcher(streamMap);
			if (mat.find()){
				curJsFileName=mat.group(0).replace("\\/", "/");
				if (decipherJsFileName == null || !decipherJsFileName.equals(curJsFileName)){
					decipherFunctions=null;
					decipherFunctionName=null;
				}
				decipherJsFileName=curJsFileName;
			}
			streams=streamMap.split(",");

		}else{
			patTitle= Pattern.compile("title=((.*?))(&|\\z)");
			mat=patTitle.matcher(streamMap);
			if (mat.find()){
				videoTitle=URLDecoder.decode(mat.group(2), "UTF-8");
			}

			streamMap=URLDecoder.decode(streamMap, "UTF-8");
			streams=streamMap.split(",|%3B");
		}

		SparseArray<YtFile> ytFiles=new SparseArray<YtFile>();
		for(String encStream : streams){
			encStream=encStream + ",";
			String stream;
			try{
				stream=URLDecoder.decode(encStream, "UTF-8");
			}catch (IllegalArgumentException iae){
				continue;
			}

			mat=patItag.matcher(stream);
			int itag=-1;
			if (mat.find()){
				itag=Integer.parseInt(mat.group(1));
				if(LOGGING)
					Log.d(getClass().getSimpleName(), "Itag found:" + itag);
				if (META_MAP.get(itag) == null){
					continue;
				}else if(!includeWebM && META_MAP.get(itag).ext.equals("webm")){
					continue;
				}
			}else{
				continue;
			}

			mat=patSig.matcher(stream);
			String sig=null;
			if (mat.find()){
				sig=mat.group(1);
			}

			if (sig == null && curJsFileName != null){
				mat=patEncSig.matcher(stream);
				if (mat.find()){
					if(LOGGING)
						Log.d(getClass().getSimpleName(), "Decypher signature: " + mat.group(1));
					decipheredSignature=null;
					if (decipherSignature(mat.group(1), client)){
						lock.lock();
						try{
							jsExecuting.await(3, TimeUnit.SECONDS);
						}finally{
							lock.unlock();
						}
					}
					if (decipheredSignature == null){
						return null;
					}else{
						sig=decipheredSignature;
					}
				}
			}
			mat=patUrl.matcher(encStream);
			String url=null;
			if (mat.find()){
				url=mat.group(1);
			}

			if (sig != null && url != null){
				Meta meta=META_MAP.get(itag);
				String finalUrl=URLDecoder.decode(url, "UTF-8");
				if (!finalUrl.contains("signature=")){
					finalUrl+="&signature=" + sig;
				}
				YtFile newVideo=new YtFile(meta, finalUrl);
				ytFiles.put(itag, newVideo);
			}
		}
		if (ytFiles.size() == 0){
			Log.d(getClass().getSimpleName(), streamMap);
			return null;
		}
		return ytFiles;
	}

	private boolean decipherSignature(final String sig, HttpClient client) throws IOException {
		// Assume the functions don't change that much
		if (decipherFunctionName == null || decipherFunctions == null){
			String decipherFunctUrl="http://s.ytimg.com/yts/jsbin/" + decipherJsFileName;

			HttpGet request=new HttpGet(decipherFunctUrl);
			HttpResponse response=client.execute(request);
			InputStream in=response.getEntity().getContent();
			BufferedReader reader=new BufferedReader(new InputStreamReader(in));
			String javascriptFile;
			StringBuilder sb=new StringBuilder("");
			String line;
			while ((line=reader.readLine()) != null){
				sb.append(line);
			}
			javascriptFile=sb.toString();
			reader.close();
			in.close();
			if(LOGGING)
				Log.d(getClass().getSimpleName(), "Decipher FunctURL: " + decipherFunctUrl);
			Matcher mat=patSignatureDecFunction.matcher(javascriptFile);
			if (mat.find()){
				decipherFunctionName=mat.group(2);
				if(LOGGING)
					Log.d(getClass().getSimpleName(), "Decipher Functname: " + decipherFunctionName);
				// Get the main function.
				String mainDecipherFunct="function " + decipherFunctionName + "(";
				int startIndex=javascriptFile.indexOf(mainDecipherFunct) + mainDecipherFunct.length();
				if (startIndex < mainDecipherFunct.length()){
					return false;
				}
				for(int braces=0, i=startIndex; i < javascriptFile.length(); i++){
					if (braces == 0 && startIndex + 5 < i){
						mainDecipherFunct+=javascriptFile.substring(startIndex, i) + ";";
						break;
					}
					if (javascriptFile.charAt(i) == '{')
						braces++;
					else if (javascriptFile.charAt(i) == '}')
						braces--;
				}
				decipherFunctions=mainDecipherFunct;
				// Search the main function for extra functions and variables
				// needed for deciphering
				// Search for variables
				mat=patVariableFunction.matcher(mainDecipherFunct);
				while (mat.find()){
					String variableDef="var " + mat.group(2) + "={";
					if (decipherFunctions.contains(variableDef)){
						continue;
					}
					startIndex=javascriptFile.indexOf(variableDef) + variableDef.length();
					for(int braces=1, i=startIndex; i < javascriptFile.length(); i++){
						if (braces == 0){
							decipherFunctions+=variableDef + javascriptFile.substring(startIndex, i) + ";";
							break;
						}
						if (javascriptFile.charAt(i) == '{')
							braces++;
						else if (javascriptFile.charAt(i) == '}')
							braces--;
					}
				}
				// Search for functions
				mat=patFunction.matcher(mainDecipherFunct);
				while (mat.find()){
					String functionDef="function " + mat.group(2) + "(";
					if (decipherFunctions.contains(functionDef)){
						continue;
					}
					startIndex=javascriptFile.indexOf(functionDef) + functionDef.length();
					for(int braces=0, i=startIndex; i < javascriptFile.length(); i++){
						if (braces == 0 && startIndex + 5 < i){
							decipherFunctions+=functionDef + javascriptFile.substring(startIndex, i) + ";";
							break;
						}
						if (javascriptFile.charAt(i) == '{')
							braces++;
						else if (javascriptFile.charAt(i) == '}')
							braces--;
					}
				}
				if(LOGGING)
					Log.d(getClass().getSimpleName(), "Decipher Function: " + decipherFunctions);
				decipherViaWebView(sig);
				if (CACHING){
					writeDeciperFunctToChache();
				}
			}
		}else{
			decipherViaWebView(sig);
		}
		return true;
	}

	private void readDecipherFunctFromCache() {
		if (context != null){
			File cacheFile=new File(context.getCacheDir().getAbsolutePath() + "/" + CACHE_FILE_NAME);
			// The cached functions are valid for 2 weeks
			if (cacheFile.exists() && (System.currentTimeMillis() - cacheFile.lastModified()) < 1209600000){
				BufferedReader reader=null;
				try{
					reader=new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile), "UTF-8"));
					decipherJsFileName=reader.readLine();
					decipherFunctionName=reader.readLine();
					decipherFunctions=reader.readLine();
				}catch (Exception e){
					e.printStackTrace();
				}finally{
					if (reader != null){
						try{
							reader.close();
						}catch (IOException e){
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Include the webm format urls into the result. Default: true
	 */
	public void setIncludeWebM(boolean includeWebM){
		this.includeWebM=includeWebM;
	}

	private void writeDeciperFunctToChache() {
		if (context != null ){
			File cacheFile=new File(context.getCacheDir().getAbsolutePath() + "/" + CACHE_FILE_NAME);
			BufferedWriter writer=null;
			try{
				writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cacheFile), "UTF-8"));
				writer.write(decipherJsFileName + "\n");
				writer.write(decipherFunctionName + "\n");
				writer.write(decipherFunctions);
			}catch (Exception e){
				e.printStackTrace();
			}finally{
				if (writer != null){
					try{
						writer.close();
					}catch (IOException e){
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void decipherViaWebView(final String sig) {
		if (context == null){
			return;
		}
		new Handler(Looper.getMainLooper()).post(new Runnable() {

			@Override
			public void run() {
				if (js == null){
					js=new JsEvaluator(context);
				}
				js.evaluate(decipherFunctions + " " + decipherFunctionName + "('" + sig + "');",
						new JsCallback() {
							@Override
							public void onResult(final String result) {
								lock.lock();
								try{
									decipheredSignature=result;
									jsExecuting.signal();
								}finally{
									lock.unlock();
								}
							}
						});
			}
		});
	}

}
