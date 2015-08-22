Android based YouTube url extractor
=======================================================

These are the urls to the YouTube video or audio files, so you can stream or download them.
It features an age verification circumvention and a signature deciphering method (mainly for vevo videos).

## Integration

* Import the jar library [youtubeExtractor.jar](https://github.com/HaarigerHarald/android-youtubeExtractor/releases/latest)

* Or include the sources (you also need: [js-evaluator-for-android](https://github.com/evgenyneu/js-evaluator-for-android))

## Usage

It's build around an AsyncTask. Called from an Activity you can write:
	
    String youtubeLink = "http://youtube.com/watch?v=xxxx";
    
    YouTubeUriExtractor ytEx = new YouTubeUriExtractor(this) {
        @Override
        public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
            if(ytFiles!=null){
                int itag = 22; // a YouTube format identifier
                String downloadUrl = ytFiles.get(itag).getUrl();
            }
        }
    };
    
    ytEx.execute(youtubeLink);

The ytFiles SparseArray is a map of available media files for one YouTube video, accessible by their itag 
value. For further infos about itags and their associated formats refer to: [Wikipedia - YouTube Quality and formats](http://en.wikipedia.org/wiki/YouTube#Quality_and_formats).

The format data like: codec, container, height and audio bitrate can be accessed through getMeta() of the YtFile class.  

There is a very simple example YouTube Downloader app in the src directory, 
that uses the "Share" function in the official YouTube app (no launcher entry).

## Requirements

Android 3.0 and up for Webview Javascript execution see [js-evaluator-for-android](https://github.com/evgenyneu/js-evaluator-for-android)

Not signature enciphered Videos may work on lower Android versions (untested).

## Limitations

Those videos aren't working:

* Everything private (private videos, bought movies, ...)
* Live streams
* RTMPE urls (very rare)


## Advanced YouTube Downloader App

There is also now an advanced App that addresses the following issues:

1. Some resolutions are only available separated in two files, one for audio and one for video. We need to merge them after the download is completed.
There is a great Java library for doing this with mp4 files: [mp4parser](https://github.com/sannies/mp4parser)

1. Some media players aren't able to read the dash container of the m4a audio files. This is also fixable via the library mentioned above.

To download, "Share" a video from the YouTube App or from your browser: [youtubeDownloader.apk](https://github.com/HaarigerHarald/android-youtubeExtractor/releases/latest)

<img src='Screenshot_2015-04-26-17-04-382.png' width='30%' alt='youtubeDownloader Screenshot 1'>
<img height="0" width="10%">
<img src='Screenshot_2015-04-27-17-05-50.png' width='30%' alt='youtubeDownloader Screenshot 2'>
<img height="0" width="15%">

## License

Modified BSD license see LICENSE and 3rd party licenses depending on what you need

