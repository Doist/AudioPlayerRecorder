package com.heavyplayer.audioplayerrecorder.widget;

import com.heavyplayer.audioplayerrecorder.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;

public class PlayPauseImageButton extends AppCompatImageButton implements View.OnClickListener {
    private boolean mIsPlaying;

    private int mPlayResId;
    private int mPauseResId;

    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;

    private OnClickListener mOnClickListener;
    private OnPlayPauseListener mOnPlayPauseListener;

    public PlayPauseImageButton(Context context) {
        super(context);
        init(context, null);
    }

    public PlayPauseImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PlayPauseImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mIsPlaying = false;

        mPlayResId = R.drawable.ic_av_play;
        mPauseResId = R.drawable.ic_av_pause;

        if (attrs != null) {
            final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PlayPauseImageButton);
            if (ta != null) {
                try {
                    mPlayResId = ta.getResourceId(R.styleable.PlayPauseImageButton_playSrc, mPlayResId);
                    mPauseResId = ta.getResourceId(R.styleable.PlayPauseImageButton_pauseSrc, mPauseResId);
                } finally {
                    ta.recycle();
                }
            }
        }

        super.setOnClickListener(this);

        updateDrawable();
    }

    public void setPlayDrawableResource(int resId) {
        mPlayResId = resId;
        mPlayDrawable = null;
        updateDrawable();
    }

    public void setPauseDrawableResource(int resId) {
        mPauseResId = resId;
        mPauseDrawable = null;
        updateDrawable();
    }

    public void setIsPlaying(boolean isPlaying) {
        mIsPlaying = isPlaying;
        updateDrawable();
    }

    @Override
    public void onClick(View v) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(v);
        }

        if (mOnPlayPauseListener != null) {
            if (mIsPlaying) {
                mOnPlayPauseListener.onPause(this);
            } else {
                mOnPlayPauseListener.onPlay(this);
            }
        }

        mIsPlaying = !mIsPlaying;
        updateDrawable();
    }

    private void updateDrawable() {
        final Drawable drawable;

        if (mIsPlaying) {
            if (mPauseDrawable == null && getResources() != null) {
                mPauseDrawable = getResources().getDrawable(mPauseResId);
            }
            drawable = mPauseDrawable;
        } else {
            if (mPlayDrawable == null && getResources() != null) {
                mPlayDrawable = getResources().getDrawable(mPlayResId);
            }
            drawable = mPlayDrawable;
        }

        if (drawable != null && drawable != getDrawable()) {
            setImageDrawable(drawable);
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClickListener = l;
    }

    public void setOnPlayPauseListener(OnPlayPauseListener l) {
        mOnPlayPauseListener = l;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.isPlaying = mIsPlaying;
        ss.playResId = mPlayResId;
        ss.pauseResId = mPauseResId;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mIsPlaying = ss.isPlaying;
        mPlayResId = ss.playResId;
        mPauseResId = ss.pauseResId;

        updateDrawable();
    }

    static class SavedState extends BaseSavedState {
        boolean isPlaying;
        int playResId;
        int pauseResId;

        public SavedState(Parcel source) {
            super(source);
            isPlaying = source.readInt() == 1;
            playResId = source.readInt();
            pauseResId = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isPlaying ? 1 : 0);
            dest.writeInt(playResId);
            dest.writeInt(pauseResId);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public final static Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public interface OnPlayPauseListener {
        void onPlay(View v);

        void onPause(View v);
    }
}
