A small android based YouTube url extractor.
=======================================================

These are the literal urls to the YouTube video or audio files, so you can stream or download them.
It features an age verification circumvention and a signature deciphering method (mainly for vevo videos).

I've made a little jar lib which should make integration super easy: [youtubeExtractor.jar](https://github.com/HaarigerHarald/android-youtubeExtractor/raw/master/bin/youtubeExtractor.jar)

#### How to Use:

It's basically build around an AsyncTask. Called from an Activity you can write something like that:
	
    String youtubeLink = "http://youtube.com/watch?v=xxxx";
    
    YouTubeUriExtractor ytEx = new YouTubeUriExtractor(this) {
        @Override
        public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
            if(ytFiles!=null){
                int itag = 22;
                // itag is a YouTube format identifier, 22 for example is "mp4 h264 1280x720"
                
                String downloadUrl = ytFiles.get(itag).getUrl();
                // TODO: Do something cool with the "downloadUrl"...
            }
        }
    };
    
    ytEx.execute(youtubeLink);

The important thing is the ytFiles SparseArray. Because YouTube videos are available in multiple formats we can choose one by
calling ytFiles.get(itag). One ytFile contains the url and its appropriate meta data like: "mp4 1280x720" or "m4a dash aac"

For further infos have a look at the supplied sample YoutubeDownloader app. It uses the "Share" function in the official YouTube
app to download YouTube videos. It doesn't have a launcher entry though so don't be irritated.

To try the app have a look at: [youtubeDownloader.apk](https://github.com/HaarigerHarald/android-youtubeExtractor/raw/master/bin/youtubeDownloader.apk)

<img src='https://github.com/HaarigerHarald/android-youtubeExtractor/raw/master/Screenshot_2015-02-10-15-12-45.png' width='250' alt='youtubeDownloader Screenshot 1'>

<img src='https://github.com/HaarigerHarald/android-youtubeExtractor/raw/master/Screenshot_2015-02-10-15-03-33.png' width='250' alt='youtubeDownloader Screenshot 2'>

I'm using a useful lib for JavaScript execution, if you want to build your own lib from the sources head 
over to: https://github.com/evgenyneu/js-evaluator-for-android

Just to make it clear I'm not in any way related to YouTube!

	
	
