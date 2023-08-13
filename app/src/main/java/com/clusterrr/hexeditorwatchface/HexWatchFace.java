package com.clusterrr.hexeditorwatchface;

import static kotlin.jvm.internal.Reflection.createKotlinClass;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsResponse;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.HealthServicesClient;
import androidx.health.services.client.PassiveListenerCallback;
import androidx.health.services.client.PassiveMonitoringClient;
import androidx.health.services.client.data.DataPoint;
import androidx.health.services.client.data.DataPointContainer;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.DeltaDataType;
import androidx.health.services.client.data.ExerciseType;
import androidx.health.services.client.data.IntervalDataPoint;
import androidx.health.services.client.data.PassiveListenerConfig;
import androidx.health.services.client.data.PassiveMonitoringCapabilities;
import androidx.health.services.client.data.UserActivityInfo;
import androidx.health.services.client.data.UserActivityState;

import android.os.SystemClock;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class HexWatchFace extends CanvasWatchFaceService {
    public static String TAG = "hex_watchface";
    private static final long INTERACTIVE_UPDATE_RATE = TimeUnit.SECONDS.toMillis(1);
    private static final long MAX_HEART_RATE_AGE = TimeUnit.SECONDS.toMillis(10);
    private static final long TOUCH_DEC_DURATION = TimeUnit.SECONDS.toMillis(5);
    private static final long TOUCH_LEGEND_DURATION = 2500;
    private static final long TOUCH_INTERVAL = 500;
    private static final long ANTI_BURN_IN_TIME = TimeUnit.SECONDS.toMillis(3);
    private static final long ANTI_BURN_IN_TIME_MIN_PERIOD = TimeUnit.SECONDS.toMillis(58);
    private static final int NUMBER_WIDTH = 78;
    private static final int NUMBER_V_INTERVAL = 56;
    private static final int BACKGROUND_Y_OFFSET = -54;
    private static final int ENDIANNESS_LITTLE_ENDIAN = 0;
    private static final int ENDIANNESS_BIG_ENDIAN = 1;
    private static final int ENDIANNESS_FAKE_HEX = 2;
    private static final int STEPS_SAVE_INTERVAL = 10;

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

        @SuppressWarnings("deprecation")
        public EngineHandler(HexWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            HexWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) engine.handleUpdateTimeMessage();
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements SensorEventListener, PassiveListenerCallback {
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
        private Bitmap mLegendSecond;
        private Bitmap mLegendDate;
        private Bitmap mLegendDayOfWeek;
        private Bitmap mLegendSteps;
        private Bitmap mLegendStepsHex;
        private Bitmap mLegendBattery;
        private Bitmap mLegendBatteryHex;
        private Bitmap mLegendHeartRate;
        private HexNumbers mNumbers;
        private int mHeartRate = 0;
        private long mHeartRateTS = 0;
        private int mStepCounter = 0;
        private long mTouchTS = 0;
        private int mTouchCount = 0;
        private SensorManager mSensorManager = null;
        private Sensor mHeartRateSensor = null;
        private PassiveMonitoringClient mStepPassiveMonitoringClient = null;
        private int mBackgroundMinX = 0;
        private int mBackgroundMinY = 0;
        private int mBackgroundMaxX = 0;
        private int mBackgroundMaxY = 0;
        private int[] mBackground;
        private long mAoDStartTS = 0;
        private long mLastAmbientUpdateTS = 0;
        private int mLastAmbientUpdateX = 0;
        private int mLastAmbientUpdateY = 0;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            Resources res = getResources();
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);

            setWatchFaceStyle(new WatchFaceStyle.Builder(HexWatchFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();
            mBackgroundBitmap = BitmapFactory.decodeResource(res, R.drawable.bg_empty);
            mVignettingBitmap = BitmapFactory.decodeResource(res, R.drawable.vignetting);
            mBarsBitmap = BitmapFactory.decodeResource(res, R.drawable.bars);
            mLegendSecond = BitmapFactory.decodeResource(res, R.drawable.legend_second);
            mLegendDate = BitmapFactory.decodeResource(res, R.drawable.legend_date);
            mLegendDayOfWeek = BitmapFactory.decodeResource(res, R.drawable.legend_day_week);
            mLegendSteps = BitmapFactory.decodeResource(res, R.drawable.legend_steps);
            mLegendStepsHex = BitmapFactory.decodeResource(res, R.drawable.legend_steps_hex);
            mLegendBattery = BitmapFactory.decodeResource(res, R.drawable.legend_battery);
            mLegendBatteryHex = BitmapFactory.decodeResource(res, R.drawable.legend_battery_hex);
            mLegendHeartRate = BitmapFactory.decodeResource(res, R.drawable.legend_heart_rate);
            mNumbers = new HexNumbers(res);
            mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            //Log.d(TAG, "Tick");
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            Log.d(TAG, "mAmbient: " + mAmbient);
            mTouchTS = 0;
            updateSensors();
            updateTimer();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            if (tapType == TAP_TYPE_TOUCH) {
                // The user has started touching the screen.
                if (System.currentTimeMillis() - mTouchTS <= TOUCH_INTERVAL) {
                    mTouchCount++;
                    if (mTouchCount > 3) mTouchCount = 1;
                } else {
                    mTouchCount = 1;
                }
                Log.d(TAG, "Touches: " + mTouchCount);
                mTouchTS = System.currentTimeMillis();
                invalidate();
            }
        }

        @SuppressWarnings("IntegerDivisionInFloatingPointContext")
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Main variables
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
            Resources res = getApplicationContext().getResources();
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            // Calculate edges
            if (mBackgroundMinX == 0) mBackgroundMinX = -canvas.getWidth() / 2 / NUMBER_WIDTH;
            if (mBackgroundMaxX == 0) mBackgroundMaxX = canvas.getWidth() / 2 / NUMBER_WIDTH + 1;
            if (mBackgroundMinY == 0) mBackgroundMinY = -canvas.getWidth() / 2 / NUMBER_V_INTERVAL - 1;
            if (mBackgroundMaxY == 0) mBackgroundMaxY = canvas.getWidth() / 2 / NUMBER_V_INTERVAL + 2;

            // Read battery state
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryIntent = getApplicationContext().registerReceiver(null, filter);
            int battery = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

            // Check if screen was tapped recently
            boolean tappedDec = mTouchTS + TOUCH_DEC_DURATION >= now;
            boolean tappedLegend = (mTouchTS + TOUCH_LEGEND_DURATION >= now) && (mTouchCount == 3);

            // Calculate current hour - 12 or 24 format
            int hour;
            if (prefs.getInt(getString(R.string.pref_time_format), SettingsActivity.PREF_DEFAULT_TIME_FORMAT)
                    == SettingsActivity.PREF_TIME_FORMAT_12)
                hour = mCalendar.get(Calendar.HOUR);
            else
                hour = mCalendar.get(Calendar.HOUR_OF_DAY);
            int timeSystem = prefs.getInt(getString(R.string.pref_time_system), SettingsActivity.PREF_DEFAULT_TIME_SYSTEM);
            if (timeSystem == SettingsActivity.PREF_VALUE_TIME_DEC_ON_TAP)
                timeSystem = tappedDec
                        ? SettingsActivity.PREF_VALUE_TIME_DEC
                        : SettingsActivity.PREF_VALUE_TIME_HEX;
            switch (timeSystem) {
                default:
                case SettingsActivity.PREF_VALUE_TIME_DEC:
                    timeSystem = ENDIANNESS_FAKE_HEX;
                    break;
                case SettingsActivity.PREF_VALUE_TIME_HEX:
                    timeSystem = ENDIANNESS_LITTLE_ENDIAN;
                    break;
            }

            if (!mAmbient) {
                // Interactive mode

                // Endianness
                int endianness =
                        (prefs.getInt(getString(R.string.pref_endianness), SettingsActivity.PREF_DEFAULT_ENDIANNESS)
                                == SettingsActivity.PREF_VALUE_ENDIANNESS_LITTLE_ENDIAN)
                                ? ENDIANNESS_LITTLE_ENDIAN
                                : ENDIANNESS_BIG_ENDIAN;

                // Draw blue background
                canvas.drawBitmap(mBackgroundBitmap,
                        canvas.getWidth() / 2 - mBackgroundBitmap.getWidth() / 2,
                        canvas.getHeight() / 2 - mBackgroundBitmap.getHeight() / 2 + BACKGROUND_Y_OFFSET,
                        null);

                // Generate background digits
                int backgroundMode = prefs.getInt(getString(R.string.pref_background), SettingsActivity.PREF_DEFAULT_BACKGROUND);
                if (mBackground == null || prefs.getBoolean(getString(R.string.pref_background_redraw), false)) {
                    mBackground = new int[(mBackgroundMaxX - mBackgroundMinX + 1) * (mBackgroundMaxY - mBackgroundMinY + 1)];
                    switch (backgroundMode) {
                        case SettingsActivity.PREF_VALUE_BACKGROUND_RANDOM_ONCE:
                            for (int i = 0; i < mBackground.length; i++)
                                mBackground[i] = (int) (Math.random() * 256);
                            break;
                        case SettingsActivity.PREF_VALUE_BACKGROUND_ZEROS:
                            Arrays.fill(mBackground, 0);
                            break;
                    }
                    prefs.edit().putBoolean(getString(R.string.pref_background_redraw), false).apply();
                }
                if (backgroundMode == SettingsActivity.PREF_VALUE_BACKGROUND_RANDOM) {
                        for (int i = 0; i < mBackground.length; i++)
                            mBackground[i] = (int) (Math.random() * 256);
                }
                // Draw background digits
                int p = 0;
                for (int x = mBackgroundMinX; x <= mBackgroundMaxX; x++) {
                    for (int y = mBackgroundMinY; y < mBackgroundMaxY; y++) {
                        drawNumberAtPos(canvas, mBackground[p++], HexNumbers.COLORS_CYAN, x, y);
                    }
                }

                // Draw vertical bars if need
                if (tappedLegend) {
                    if (prefs.getInt(getString(R.string.pref_bars), SettingsActivity.PREF_DEFAULT_BARS)
                            == SettingsActivity.PREF_VALUE_BARS_SHOW) {
                        canvas.drawBitmap(mBarsBitmap,
                                canvas.getWidth() / 2 - mBarsBitmap.getWidth() / 2,
                                canvas.getHeight() / 2 - mBarsBitmap.getHeight() / 2 + BACKGROUND_Y_OFFSET,
                                null);
                    }
                }

                // Draw hours, minutes and seconds
                drawNumber(canvas, hour, timeSystem, 1, HexNumbers.COLORS_WHITE, 0, 0);
                drawNumber(canvas, mCalendar.get(Calendar.MINUTE), timeSystem, 1,HexNumbers.COLORS_WHITE, 1, 0);
                drawNumber(canvas, mCalendar.get(Calendar.SECOND), timeSystem, 1,HexNumbers.COLORS_CYAN, 2, 0);
                if (tappedLegend) {
                    canvas.drawBitmap(mLegendSecond,
                            canvas.getWidth() / 2 - mLegendSecond.getWidth() / 2,
                            canvas.getHeight() / 2 - mLegendSecond.getHeight() / 2,
                            null);
                }

                // Draw date if enabled
                int dateSystem = prefs.getInt(getString(R.string.pref_date), SettingsActivity.PREF_DEFAULT_DATE);
                if (dateSystem != SettingsActivity.PREF_VALUE_HIDE) {
                    // Check tap mode
                    if (dateSystem  == SettingsActivity.PREF_VALUE_COMMON_DEC_ON_TAP)
                        dateSystem = tappedDec
                                ? SettingsActivity.PREF_VALUE_COMMON_DEC
                                : SettingsActivity.PREF_VALUE_COMMON_HEX;
                    // Check system
                    switch (dateSystem) {
                        default:
                        case SettingsActivity.PREF_VALUE_COMMON_DEC:
                            dateSystem = ENDIANNESS_FAKE_HEX;
                            break;
                        case SettingsActivity.PREF_VALUE_COMMON_HEX:
                            dateSystem = ENDIANNESS_LITTLE_ENDIAN;
                            break;
                    }
                    drawNumber(canvas, mCalendar.get(Calendar.DAY_OF_MONTH), dateSystem, 1, HexNumbers.COLORS_CYAN, 2, 1);
                    drawNumber(canvas, mCalendar.get(Calendar.MONTH) + 1, dateSystem, 1, HexNumbers.COLORS_CYAN, 2, -1);
                    drawNumber(canvas, mCalendar.get(Calendar.YEAR), dateSystem, 1, HexNumbers.COLORS_CYAN, 1, -1);
                    if (tappedLegend) {
                        canvas.drawBitmap(mLegendDate,
                                canvas.getWidth() / 2 - mLegendDate.getWidth() / 2,
                                canvas.getHeight() / 2 - mLegendDate.getHeight() / 2,
                                null);
                    }
                }

                // Draw day of the week
                int dayOfTheWeekMode = prefs.getInt(getString(R.string.pref_day_week), SettingsActivity.PREF_DEFAULT_DAY_OF_THE_WEEK);
                if (dayOfTheWeekMode != SettingsActivity.PREF_VALUE_HIDE) {
                    int dayOfTheWeek = mCalendar.get(Calendar.DAY_OF_WEEK) - 1;
                            if ((dayOfTheWeek == 0) && (dayOfTheWeekMode == SettingsActivity.PREF_VALUE_DAY_SUNDAY_7)) dayOfTheWeek = 7;
                    drawNumber(canvas, dayOfTheWeek, ENDIANNESS_FAKE_HEX, 1, HexNumbers.COLORS_CYAN, 1, 1);
                    if (tappedLegend) {
                        canvas.drawBitmap(mLegendDayOfWeek,
                                canvas.getWidth() / 2 - mLegendDayOfWeek.getWidth() / 2,
                                canvas.getHeight() / 2 - mLegendDayOfWeek.getHeight() / 2,
                                null);
                    }
                }

                // Draw battery if enabled
                int batterySystem = prefs.getInt(getString(R.string.pref_battery), SettingsActivity.PREF_DEFAULT_BATTERY);
                if (batterySystem != SettingsActivity.PREF_VALUE_HIDE) {
                    // Check tap mode
                    switch(batterySystem) {
                        case SettingsActivity.PREF_VALUE_BATTERY_HEX_0_FF_TAP:
                            batterySystem = tappedDec
                                    ? SettingsActivity.PREF_VALUE_BATTERY_DEC_0_100
                                    : SettingsActivity.PREF_VALUE_BATTERY_HEX_0_FF;
                            break;
                        case SettingsActivity.PREF_VALUE_BATTERY_HEX_0_64_TAP:
                            batterySystem = tappedDec
                                    ? SettingsActivity.PREF_VALUE_BATTERY_DEC_0_100
                                    : SettingsActivity.PREF_VALUE_BATTERY_HEX_0_64;
                            break;
                    }
                    // Check system
                    switch (batterySystem) {
                        default:
                        case SettingsActivity.PREF_VALUE_BATTERY_DEC_0_100:
                            drawNumber(canvas, battery, ENDIANNESS_FAKE_HEX, 2, HexNumbers.COLORS_CYAN, -2, 0);
                            break;
                        case SettingsActivity.PREF_VALUE_BATTERY_HEX_0_64:
                            drawNumber(canvas, battery, ENDIANNESS_LITTLE_ENDIAN, 1, HexNumbers.COLORS_CYAN, -1, 0);
                            break;
                        case SettingsActivity.PREF_VALUE_BATTERY_HEX_0_FF:
                            drawNumber(canvas, battery * 255 / 100, ENDIANNESS_LITTLE_ENDIAN, 1, HexNumbers.COLORS_CYAN, -1, 0);
                            break;
                    }
                    if (tappedLegend) {
                        Bitmap batLegend = batterySystem == SettingsActivity.PREF_VALUE_BATTERY_DEC_0_100 ? mLegendBattery : mLegendBatteryHex;
                        canvas.drawBitmap(batLegend,
                                canvas.getWidth() / 2 - batLegend.getWidth() / 2,
                                canvas.getHeight() / 2 - batLegend.getHeight() / 2,
                                null);
                    }
                }

                // Draw heart rate if enabled
                int heartRateSystem = prefs.getInt(getString(R.string.pref_heart_rate), SettingsActivity.PREF_DEFAULT_HEART_RATE);
                if (heartRateSystem != SettingsActivity.PREF_VALUE_HIDE) {
                    if ((mHeartRateTS + MAX_HEART_RATE_AGE < now) && (mHeartRate != 0)) {
                        mHeartRate = 0;
                        Log.i(TAG, "Heart rate is reset to 0");
                    }
                    if (heartRateSystem == SettingsActivity.PREF_VALUE_COMMON_DEC_ON_TAP)
                        heartRateSystem = tappedDec
                                ? SettingsActivity.PREF_VALUE_COMMON_DEC
                                : SettingsActivity.PREF_VALUE_COMMON_HEX;
                    switch (heartRateSystem) {
                        default:
                        case SettingsActivity.PREF_VALUE_COMMON_DEC:
                            drawNumber(canvas, mHeartRate, ENDIANNESS_FAKE_HEX, 2, HexNumbers.COLORS_CYAN, -1, -1);
                            break;
                        case SettingsActivity.PREF_VALUE_COMMON_HEX:
                            drawNumber(canvas, mHeartRate, endianness, 2, HexNumbers.COLORS_CYAN, -1, -1);
                            break;
                    }
                    if (tappedLegend) {
                        canvas.drawBitmap(mLegendHeartRate,
                                canvas.getWidth() / 2 - mLegendHeartRate.getWidth() / 2,
                                canvas.getHeight() / 2 - mLegendHeartRate.getHeight() / 2,
                                null);
                    }
                }

                // Draw steps if enabled
                int stepsSystem = prefs.getInt(getString(R.string.pref_steps), SettingsActivity.PREF_DEFAULT_STEPS);
                if (stepsSystem != SettingsActivity.PREF_VALUE_HIDE) {
                    // Check tap mode
                    if (stepsSystem == SettingsActivity.PREF_VALUE_COMMON_DEC_ON_TAP)
                        stepsSystem = tappedDec
                                ? SettingsActivity.PREF_VALUE_COMMON_DEC
                                : SettingsActivity.PREF_VALUE_COMMON_HEX;
                    // Check system
                    switch (stepsSystem) {
                        default:
                        case SettingsActivity.PREF_VALUE_COMMON_DEC:
                            drawNumber(canvas, mStepCounter, ENDIANNESS_FAKE_HEX, 3, HexNumbers.COLORS_CYAN, -2, 1);
                            break;
                        case SettingsActivity.PREF_VALUE_COMMON_HEX:
                            drawNumber(canvas, mStepCounter, endianness, 2, HexNumbers.COLORS_CYAN, -1, 1);
                            break;
                    }
                    if (tappedLegend) {
                        Bitmap stepsLegend = stepsSystem == SettingsActivity.PREF_VALUE_COMMON_DEC ? mLegendSteps : mLegendStepsHex;
                        canvas.drawBitmap(stepsLegend,
                                canvas.getWidth() / 2 - stepsLegend.getWidth() / 2,
                                canvas.getHeight() / 2 - stepsLegend.getHeight() / 2,
                                null);
                    }
                }

                // Draw vertical bars if need
                if (!tappedLegend) {
                    if (prefs.getInt(getString(R.string.pref_bars), SettingsActivity.PREF_DEFAULT_BARS)
                            == SettingsActivity.PREF_VALUE_BARS_SHOW) {
                        canvas.drawBitmap(mBarsBitmap,
                                canvas.getWidth() / 2 - mBarsBitmap.getWidth() / 2,
                                canvas.getHeight() / 2 - mBarsBitmap.getHeight() / 2 + BACKGROUND_Y_OFFSET,
                                null);
                    }
                }

                // Draw vignetting if need
                if (prefs.getInt(getString(R.string.pref_vignetting), res.getInteger(R.integer.default_vignetting))
                    == SettingsActivity.PREF_VALUE_ENABLED) {
                    // Scale it to the screen size
                    canvas.drawBitmap(mVignettingBitmap,
                            new Rect(0, 0, mVignettingBitmap.getWidth(), mVignettingBitmap.getHeight()),
                            new Rect(0, 0, canvas.getWidth(), canvas.getHeight()),
                            null);
                }

                // Remember that AoD disabled
                mAoDStartTS = 0;
                mLastAmbientUpdateX = 0;
                mLastAmbientUpdateY = 0;
            } else {
                // Always-on-Display mode

                // Remember when switched to AoD mode
                if (mAoDStartTS == 0) mAoDStartTS = now;
                canvas.drawColor(Color.BLACK);
                // Always-on-Display anti-burn-in protection
                int x = 0;
                int y = 0;
                if ((prefs.getInt(getString(R.string.pref_anti_burn_in), SettingsActivity.PREF_DEFAULT_ANTI_BURN_IN)
                        == SettingsActivity.PREF_VALUE_ENABLED)
                        && (mAoDStartTS + ANTI_BURN_IN_TIME < now)) {
                    // Do not move time too often
                    if (mLastAmbientUpdateTS + ANTI_BURN_IN_TIME_MIN_PERIOD < now) {
                        // y always random from edge to edge
                        y = (int) (Math.round(Math.random() * Math.max(1,mBackgroundMaxY - 3)));
                        if (Math.round(Math.random()) == 0) y *= -1;
                        // x is from edge to edge only when y==0 or screen is rectangle
                        Log.d(TAG, "mBackgroundMaxY="+mBackgroundMaxY+", anti_burn_in_y=" + y);
                        if ((y == 0) || !res.getBoolean(R.bool.is_round))
                            x = (int) (Math.round(Math.random() * Math.max(1,mBackgroundMaxX - 2))); // from edge to edge
                        else if ((y > -(mBackgroundMaxY - 3) && (y < mBackgroundMaxY - 3)))
                            x = (int) (Math.round(Math.random())); // from -1 to 1
                        else
                            x = 0; // center only
                        if (Math.round(Math.random()) == 0) x *= -1;
                        Log.d(TAG, "mBackgroundMaxX="+mBackgroundMaxX+", anti_burn_in_x=" + x);
                        mLastAmbientUpdateX = x;
                        mLastAmbientUpdateY = y;
                        mLastAmbientUpdateTS = now;
                    } else {
                        x = mLastAmbientUpdateX;
                        y = mLastAmbientUpdateY;
                    }
                }
                // Draw hours and minutes
                drawNumber(canvas, hour, timeSystem, 1, HexNumbers.COLORS_DARK, 0 + x, 0 + y);
                drawNumber(canvas, mCalendar.get(Calendar.MINUTE), timeSystem, 1, HexNumbers.COLORS_DARK, 1 + x, 0 + y);
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
                        digit = (number >> ((size - i - 1) * 8)) & 0xFF;
                        break;
                    case ENDIANNESS_LITTLE_ENDIAN:
                        digit = (number >> i * 8) & 0xFF;
                        break;
                    default:
                    case ENDIANNESS_FAKE_HEX:
                        base = (int)Math.pow(100, (size - i - 1));
                        digit = (number / base) % 100;
                        digit = (digit % 10) | ((digit / 10) * 16);
                        break;
                }
                drawNumberAtPos(canvas, digit, numberColor, left + i, top);
            }
        }

        @SuppressWarnings("IntegerDivisionInFloatingPointContext")
        private void drawNumberAtPos(Canvas canvas, int number, int numberColor,
                                     float left, float top)
        {
            drawDigitAtPoint(canvas, number, numberColor,
                    canvas.getWidth() / 2 + (left - 1) * NUMBER_WIDTH,
                    canvas.getHeight() / 2 + top * NUMBER_V_INTERVAL - 24);
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
                updateSensors();
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


        private void updateSensors()
        {
            // Enable disable sensors
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);

            if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) &&
                    (prefs.getInt(getString(R.string.pref_heart_rate), SettingsActivity.PREF_DEFAULT_HEART_RATE) != SettingsActivity.PREF_VALUE_HIDE)
                    /* && !mAmbient && isVisible() */) {
                // Enable heart rate sensor if need
                if (mHeartRateSensor == null) {
                    mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
                    mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    Log.i(TAG, "Heart rate sensor enabled");
                }
            } else if (mHeartRateSensor != null) {
                mSensorManager.unregisterListener(this, mHeartRateSensor);
                mHeartRateSensor = null;
                mHeartRate = 0;
                Log.i(TAG, "Heart rate sensor disabled");
            }

            if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) &&
                    (prefs.getInt(getString(R.string.pref_steps), SettingsActivity.PREF_DEFAULT_STEPS) != SettingsActivity.PREF_VALUE_HIDE)
                    /* && !mAmbient && isVisible() */)
            {
                // Enable step sensor if need
                if (mStepPassiveMonitoringClient == null) {
                    HealthServicesClient healthServicesClient = HealthServices.getClient(getApplicationContext());
                    mStepPassiveMonitoringClient = healthServicesClient.getPassiveMonitoringClient();

                    Set<DataType<?,?>> dataTypes = new HashSet<>();
                    dataTypes.add(DataType.STEPS_DAILY);
                    PassiveListenerConfig passiveListenerConfig = PassiveListenerConfig.builder()
                            //.setShouldUserActivityInfoBeRequested(true)
                            .setDataTypes(dataTypes)
                            .build();
                    mStepPassiveMonitoringClient.setPassiveListenerCallback(passiveListenerConfig, this);
                    Log.i(TAG, "Step sensor enabled");
                }
            } else if (mStepPassiveMonitoringClient != null)
            {
                // Disable step sensor
                mStepPassiveMonitoringClient.clearPassiveListenerCallbackAsync();
                mStepPassiveMonitoringClient = null;
                Log.i(TAG, "Step sensor Disabled");
            }
        }

        // Heart rate receiver
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
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            // unused
        }

        // Steps receiver
        @Override
        public void onNewDataPointsReceived(@NonNull DataPointContainer dataPoints) {
            PassiveListenerCallback.super.onNewDataPointsReceived(dataPoints);

            List<IntervalDataPoint<Long>> dps = dataPoints.getData(DataType.STEPS_DAILY);
            Instant bootInstant = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime());

            long ts = 0;
            long steps = 0;

            if (!dps.isEmpty()) {
                for (IntervalDataPoint<Long> dp : dps)
                {
                    Instant endTime = dp.getEndInstant(bootInstant);
                    if (endTime.toEpochMilli() > ts)
                    {
                        ts = endTime.toEpochMilli();
                        steps = dp.getValue();
                    }
                }
            }

            mStepCounter = (int)steps;
            Log.d(TAG, "Today steps: " + mStepCounter);
        }

        @Override
        public void onRegistered() {
            PassiveListenerCallback.super.onRegistered();
            Log.d(TAG, "Step counter sensor registered");
        }

        @Override
        public void onPermissionLost() {
            PassiveListenerCallback.super.onPermissionLost();
            mStepPassiveMonitoringClient = null;
            Log.e(TAG, "Step counter permission lost");
        }

        @Override
        public void onRegistrationFailed(@NonNull Throwable throwable) {
            PassiveListenerCallback.super.onRegistrationFailed(throwable);
            mStepPassiveMonitoringClient = null;
            Log.d(TAG, "Step counter sensor unregistered");
        }
    }
}