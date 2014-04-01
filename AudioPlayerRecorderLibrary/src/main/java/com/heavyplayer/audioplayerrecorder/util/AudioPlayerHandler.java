package com.heavyplayer.audioplayerrecorder.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import com.heavyplayer.audioplayerrecorder.widget.AudioPlayerLayout;
import com.heavyplayer.audioplayerrecorder.widget.PlayPauseImageButton;

import java.io.IOException;

public class AudioPlayerHandler implements
		MediaPlayer.OnBufferingUpdateListener,
		MediaPlayer.OnCompletionListener,
		SafeMediaPlayer.OnStartListener {
	public static final String TAG = AudioPlayerHandler.class.getSimpleName();

	private final static long PROGRESS_UPDATE_INTERVAL_MS = 200;

	private AudioManager mAudioManager;
	private AudioFocusChangeListener mAudioFocusChangeListener;

	private Uri mFileUri;

	private Handler mHandler;
	private ProgressUpdater mProgressUpdater;

	private SafeMediaPlayer mMediaPlayer;

	private AudioPlayerLayout mView;
	private PlayPauseImageButton mButton;
	private SeekBar mSeekBar;

	public AudioPlayerHandler(Context context, Uri fileUri, Handler handler) {
		mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

		mFileUri = fileUri;

		mMediaPlayer = new SafeMediaPlayer();
		mMediaPlayer.setOnStartListener(this);
		mMediaPlayer.setOnCompletionListener(this);
		mMediaPlayer.setOnBufferingUpdateListener(this);

		mHandler = handler;
		mProgressUpdater = new ProgressUpdater();
	}

	protected void start(boolean gainAudioFocus, boolean updateButton) {
		if(gainAudioFocus)
			gainAudioFocus();

		if(!mMediaPlayer.isPrepared()) {
			try {
				mMediaPlayer.setDataSource(mFileUri.getPath());
				mMediaPlayer.prepare();
			} catch (IOException e) {
				Log.w(TAG, e);
			}
		}

		mMediaPlayer.start();

		if(updateButton)
			updateButton(true);
	}

	protected void pause(boolean abandonAudioFocus, boolean updateButton) {
		mMediaPlayer.pause();

		if(updateButton)
			updateButton(false);

		if(abandonAudioFocus)
			abandonAudioFocus();

	}

	protected void seekTo(int msec) {
		mMediaPlayer.seekTo(msec);
	}

	protected void updateButton(boolean isPlaying) {
		if(mButton != null)
			mButton.setIsPlaying(isPlaying);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.i(TAG, "buffering percent: " + percent);
	}

	@Override
	public void onStart(MediaPlayer mp) {
		if(mSeekBar != null) {
			if(mSeekBar.getMax() != mp.getDuration()) {
				mSeekBar.setMax(mp.getDuration());
				mSeekBar.setProgress(mp.getCurrentPosition());
			}
			else if(mSeekBar.getProgress() != mp.getCurrentPosition()) {
				mSeekBar.setProgress(mp.getCurrentPosition());
			}
		}

		// Update seek bar.
		startSeekBarUpdate();
	}

	public void startSeekBarUpdate() {
		// Update seek bar.
		mHandler.removeCallbacks(mProgressUpdater);
		mHandler.post(mProgressUpdater);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// Updates seek bar.
		if(mSeekBar != null)
			mSeekBar.setProgress(mp.getDuration());

		updateButton(false);

		abandonAudioFocus();
	}

	public void registerView(AudioPlayerLayout view) {
		mView = view;
		mView.setOnDetachListener(new AudioPlayerLayout.OnDetachListener() {
			@Override
			public void onStartTemporaryDetach(View v) {
				clearView();
			}
		});

		registerButton(view.getButton());

		registerSeekBar(view.getSeekBar());

		// Resume updater.
		startSeekBarUpdate();
	}

	protected void registerButton(PlayPauseImageButton button) {
		mButton = button;

		mButton.setOnPlayPauseListener(new PlayPauseImageButton.OnPlayPauseListener() {
			@Override
			public void onPlay(View v) {
				start(true, false);
			}

			@Override
			public void onPause(View v) {
				pause(true, false);
			}
		});

		// Resume button state.
		mButton.setIsPlaying(mMediaPlayer.isGoingToPlay());
	}

	protected void registerSeekBar(SeekBar seekBar) {
		mSeekBar = seekBar;

		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mHandler.removeCallbacks(mProgressUpdater);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				seekTo(seekBar.getProgress());
				mHandler.post(mProgressUpdater);
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { /* Do nothing */ }
		});

		// Resume progress.
		mSeekBar.setMax(mMediaPlayer.getDuration());
		mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
	}

	protected void clearView() {
		mView.setOnDetachListener(null);
		mView = null;

		mButton.setOnClickListener(null);
		mButton = null;

		mSeekBar.setOnSeekBarChangeListener(null);
		mSeekBar = null;
	}

	public void onDestroy() {
		destroyMediaPlayer();
		abandonAudioFocus();
	}

	protected void destroyMediaPlayer() {
		try {
			mMediaPlayer.setOnBufferingUpdateListener(null);
			mMediaPlayer.setOnCompletionListener(null);
			mMediaPlayer.setOnStartListener(null);
			mMediaPlayer.stop();
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
		catch(Exception e) {
			Log.w(TAG, e);
		}
	}

	protected void gainAudioFocus() {
		if(mAudioFocusChangeListener == null)
			mAudioFocusChangeListener = new AudioFocusChangeListener();

		// Request audio focus for playback
		mAudioManager.requestAudioFocus(
				mAudioFocusChangeListener,
				AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
	}

	protected void abandonAudioFocus() {
		// Abandon audio focus when playback complete.
		if(mAudioFocusChangeListener != null)
			mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
	}

	private class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
		@Override
		public void onAudioFocusChange(int focusChange) {
			switch(focusChange) {
				case AudioManager.AUDIOFOCUS_LOSS:
					pause(true, true);
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					pause(false, true);
					break;
				case AudioManager.AUDIOFOCUS_GAIN:
					start(false, true);
					break;
			}
		}
	}

	protected class ProgressUpdater implements Runnable {
		@Override
		public void run() {
			if(mSeekBar != null && mMediaPlayer.isPlaying()) {
				mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
				mHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS);
			}
		}
	}
}
