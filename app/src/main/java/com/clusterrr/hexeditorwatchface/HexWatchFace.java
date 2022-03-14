package com.clusterrr.hexeditorwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;

import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn"t
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class HexWatchFace extends CanvasWatchFaceService {
    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    public static String TAG = "hex_watchface";
    private static final long INTERACTIVE_UPDATE_RATE = TimeUnit.SECONDS.toMillis(1);
    private static final long MAX_HEART_RATE_AGE = TimeUnit.SECONDS.toMillis(15);
    private static final long TOUCH_TIME = TimeUnit.SECONDS.toMillis(3);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<HexWatchFace.Engine> mWeakReference;

        public EngineHandler(HexWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            HexWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements SensorEventListener {
        private static final int NUMBER_WIDTH = 78;
        private static final int NUMBER_V_INTERVAL = 56;
        private static final int ENDIANNESS_LITTLE_ENDIAN = 0;
        private static final int ENDIANNESS_BIG_ENDIAN = 1;
        private static final int ENDIANNESS_FAKE_HEX = 2;

        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mAmbient;
        private boolean mRegisteredTimeZoneReceiver = false;
        private Bitmap mBackgroundBitmap;
        private Bitmap mVignettingBitmap;
        private Bitmap mBarsBitmap;
        private HexNumbers mNumbers;
        private int mHeartRate = 0;
        private long mHeartRateTS = 0;
        private int mStepCounter = 0;
        private long mTouchTS = 0;
        private SensorManager mSensorManager = null;
        private Sensor mHeartRateSensor = null;
        private Sensor mStepCountSensor = null;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(HexWatchFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();
            Resources res = getResources();
            mBackgroundBitmap = BitmapFactory.decodeResource(res, R.drawable.bg_empty);
            mVignettingBitmap = BitmapFactory.decodeResource(res, R.drawable.vignetting);
            mBarsBitmap = BitmapFactory.decodeResource(res, R.drawable.bars);
            mNumbers = new HexNumbers(res);
            mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
            mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            mStepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mStepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            mTouchTS = 0;
            updateTimer();
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    //Log.d(TAG, "Touched");
                    mTouchTS = System.currentTimeMillis();
                    invalidate();
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    //Log.d(TAG, "Touch cancel");
                    break;
                case TAP_TYPE_TAP:
                    //Log.d(TAG, "Tap");
                    break;
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            SharedPreferences preferences = getApplicationContext().getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            if ((mHeartRateTS + MAX_HEART_RATE_AGE < now) && (mHeartRate != 0)) {
                mHeartRate = 0;
                Log.d(TAG, "Heart rate is reset to 0");
            }
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryIntent = getApplicationContext().registerReceiver(null, filter);
            int battery = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

            int todayStepStart = preferences.getInt(getString(R.string.pref_today_step_start), 0);
            if (mCalendar.get(Calendar.DAY_OF_MONTH) != preferences.getInt(getString(R.string.pref_steps_day), 0)
                || mStepCounter < todayStepStart) {
                    preferences.edit()
                        .putInt(getString(R.string.pref_steps_day), mCalendar.get(Calendar.DAY_OF_MONTH))
                        .putInt(getString(R.string.pref_today_step_start), mStepCounter)
                        .commit();
                    todayStepStart = mStepCounter;
            }
            int todaySteps = mStepCounter - todayStepStart;

            boolean touched = mTouchTS + TOUCH_TIME >= now;

            if (!mAmbient) {
                canvas.drawBitmap(mBackgroundBitmap,
                        canvas.getWidth() / 2 - mBackgroundBitmap.getWidth() / 2,
                        canvas.getHeight() / 2 - mBackgroundBitmap.getHeight() / 2 - 54,
                        null);


                for (int i = -5; i < 5; i++) {
                    for (int j = -5; j < 5; j++) {
                        drawNumberAtPos(canvas, (int) (Math.random() * 256), HexNumbers.COLORS_CYAN, i, j);
                    }
                }

                drawNumber(canvas, mCalendar.get(Calendar.DAY_OF_MONTH), ENDIANNESS_FAKE_HEX, 1, HexNumbers.COLORS_CYAN, 2, -1);
                drawNumber(canvas, mCalendar.get(Calendar.DAY_OF_WEEK) - 1, ENDIANNESS_FAKE_HEX, 1, HexNumbers.COLORS_CYAN, 1, 1);
                drawNumber(canvas, mCalendar.get(Calendar.MONTH) + 1, ENDIANNESS_FAKE_HEX, 1, HexNumbers.COLORS_CYAN, 2, 1);
                drawNumber(canvas, mCalendar.get(Calendar.YEAR), ENDIANNESS_FAKE_HEX, 1, HexNumbers.COLORS_CYAN, 1, -1);

                drawNumber(canvas, mHeartRate, ENDIANNESS_FAKE_HEX, 2, HexNumbers.COLORS_CYAN, -1, -1);
                drawNumber(canvas, battery, ENDIANNESS_FAKE_HEX, 2, HexNumbers.COLORS_CYAN, -2, 0);
                drawNumber(canvas, todaySteps, ENDIANNESS_FAKE_HEX, 3, HexNumbers.COLORS_CYAN, -2, 1);

                drawNumber(canvas, mCalendar.get(Calendar.HOUR_OF_DAY), ENDIANNESS_FAKE_HEX, 1, HexNumbers.COLORS_WHITE, 0, 0);
//                drawNumber(canvas, 0x1E, ENDIANNESS_LITTLE_ENDIAN, 1, HexNumbers.COLORS_WHITE, 0, 0);
                drawNumber(canvas, mCalendar.get(Calendar.MINUTE), ENDIANNESS_FAKE_HEX, 1,HexNumbers.COLORS_WHITE, 1, 0);
//                drawNumber(canvas, 0xE7, ENDIANNESS_LITTLE_ENDIAN, 1,HexNumbers.COLORS_WHITE, 1, 0);
                drawNumber(canvas, mCalendar.get(Calendar.SECOND), ENDIANNESS_FAKE_HEX, 1,HexNumbers.COLORS_CYAN, 2, 0);

                canvas.drawBitmap(mBarsBitmap,
                        canvas.getWidth() / 2 - mBarsBitmap.getWidth() / 2,
                        canvas.getHeight() / 2 - mBarsBitmap.getHeight() / 2 - 54,
                        null);
                canvas.drawBitmap(mVignettingBitmap,
                        new Rect(0, 0, mVignettingBitmap.getWidth(), mVignettingBitmap.getHeight()),
                        new Rect(0, 0, canvas.getWidth(), canvas.getHeight()),
                        null);

            } else {
                canvas.drawColor(Color.BLACK);
                drawNumber(canvas, mCalendar.get(Calendar.HOUR_OF_DAY), ENDIANNESS_FAKE_HEX, 1, HexNumbers.COLORS_DARK, 0, 0);
                drawNumber(canvas, mCalendar.get(Calendar.MINUTE), ENDIANNESS_FAKE_HEX, 1,HexNumbers.COLORS_DARK, 1, 0);
            }
        }


        private void drawNumber(Canvas canvas, int number, int endianness, int size, int numberColor,
                                   float left, float top)
        {
            for (int i = 0; i < size; i++) {
                int digit, base;
                switch (endianness)
                {
                    case ENDIANNESS_BIG_ENDIAN:
                        digit = (number >> i) & 0xFF;
                        break;
                    case ENDIANNESS_LITTLE_ENDIAN:
                        base = (int)Math.pow(256, (size - i - 1));
                        digit = (number / base) % 256;
                        break;
                    default:
                    case ENDIANNESS_FAKE_HEX:
                        base = (int)Math.pow(100, (size - i - 1));
                        digit = (number / base) % 100;
                        digit = (digit % 10) | ((digit / 10) * 16);
                }
                drawNumberAtPos(canvas, digit, numberColor, left + i, top);
            }
        }

        private void drawNumberAtPos(Canvas canvas, int number, int numberColor,
                                     float left, float top)
        {
            drawDigitAtPoint(canvas, number, numberColor,
                    canvas.getWidth()/2 + (left - 1) * NUMBER_WIDTH,
                    canvas.getHeight()/2 + top * NUMBER_V_INTERVAL - 24);
        }

        private void drawDigitAtPoint(Canvas canvas, int number, int numberColor,
                                      float left, float top)
        {
            canvas.drawBitmap(mNumbers.GetNumber(number, numberColor), left, top, null);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren"t visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            HexWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            HexWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE
                        - (timeMs % INTERACTIVE_UPDATE_RATE);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            //Log.d(TAG, "New sensor data: " + event.sensor.getType());
            switch (event.sensor.getType()) {
                case Sensor.TYPE_HEART_RATE:
                    if ((int)event.values[0] != 0) {
                        mHeartRate = (int) event.values[0];
                        mHeartRateTS = System.currentTimeMillis();
                        //Log.d(TAG, "Heart rate: " + mHeartRate);
                    }
                    break;
                case Sensor.TYPE_STEP_COUNTER:
                    mStepCounter = (int)event.values[0];
                    Log.d(TAG, "Steps: " + mStepCounter);
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }
}