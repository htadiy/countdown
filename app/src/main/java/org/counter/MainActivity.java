package org.counter;

import org.counter.R;
import org.counter.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText minuteInput, secondInput;
    private MaterialButton startButton, pauseButton, resetButton;
    private TextView countdownText;
    private ProgressBar progressBar;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private long initialTimeInMillis;
    private boolean isPaused = false;
    private Vibrator vibrator;
    private static final String CHANNEL_ID = "countdown_channel";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;
    private NotificationManagerCompat notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        minuteInput = findViewById(R.id.minuteInput);
        secondInput = findViewById(R.id.secondInput);
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        resetButton = findViewById(R.id.resetButton);
        countdownText = findViewById(R.id.countdownText);
        progressBar = findViewById(R.id.progressBar);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        notificationManager = NotificationManagerCompat.from(this);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPaused) {
                    resumeTimer();
                } else {
                    startTimer();
                }
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
            }
        });

        // 请求通知权限
        requestNotificationPermission();
    }

    private void createNotificationChannel() {
        CharSequence name = "倒计时";
        String description = "倒计时服务";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "通知权限尚未授权", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startTimer() {
        String minuteInputText = Objects.requireNonNull(minuteInput.getText()).toString();
        String secondInputText = Objects.requireNonNull(secondInput.getText()).toString();

        if (minuteInputText.isEmpty()) minuteInputText = "0";
        if (secondInputText.isEmpty()) secondInputText = "0";

        int minutes = Integer.parseInt(minuteInputText);
        int seconds = Integer.parseInt(secondInputText);

        timeLeftInMillis = (minutes * 60L + seconds) * 1000;
        initialTimeInMillis = timeLeftInMillis;

        if (timeLeftInMillis == 0) {
            minuteInput.setError("请输入一个正确的时间！");
            secondInput.setError("请输入一个正确的时间！");
            return;
        }

        minuteInput.setVisibility(View.GONE);
        secondInput.setVisibility(View.GONE);
        startButton.setText("暂停");
        pauseButton.setVisibility(View.VISIBLE);
        resetButton.setVisibility(View.VISIBLE);
        isPaused = false;

        sendProgressNotification();

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
                updateProgressBar();
                updateNotificationProgress();
            }

            @Override
            public void onFinish() {
                // 在倒计时结束时立即将进度条设置为100%
                progressBar.setProgress(100);
                updateNotificationProgress(100); // 更新通知栏进度条为100%
                updateCountdownText();

                // 振动1秒，强度为50
                vibrator.vibrate(VibrationEffect.createOneShot(1000, 50));

                // 发出完成通知
                sendCompletionNotification();

                // 延迟1秒后重置界面
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resetUI();
                    }
                }, 1000);
            }
        }.start();
    }

    private void pauseTimer() {
        countDownTimer.cancel();
        isPaused = true;
        startButton.setText("继续");
        startButton.setVisibility(View.VISIBLE);
        pauseButton.setVisibility(View.GONE);
    }

    private void resumeTimer() {
        startButton.setVisibility(View.GONE);
        pauseButton.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
                updateProgressBar();
                updateNotificationProgress();
            }

            @Override
            public void onFinish() {
                // 在倒计时结束时立即将进度条设置为100%
                progressBar.setProgress(100);
                updateNotificationProgress(100); // 更新通知栏进度条为100%
                updateCountdownText();

                // 振动1秒，强度为50
                vibrator.vibrate(VibrationEffect.createOneShot(1000, 50));

                // 发出完成通知
                sendCompletionNotification();

                // 延迟1秒后重置界面
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resetUI();
                    }
                }, 1000);
            }
        }.start();

        isPaused = false;
    }

    @SuppressLint("SetTextI18n")
    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timeLeftInMillis = 0;
        initialTimeInMillis = 0;
        isPaused = false;
        countdownText.setText("00:00");
        progressBar.setProgress(0);
        minuteInput.setText("");
        secondInput.setText("");
        startButton.setText("开始倒计时");
        startButton.setVisibility(View.VISIBLE);
        pauseButton.setVisibility(View.GONE);
        resetButton.setVisibility(View.GONE);

        // 重新显示控件
        minuteInput.setVisibility(View.VISIBLE);
        secondInput.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        countdownText.setVisibility(View.VISIBLE);
        // 移除通知
        notificationManager.cancel(1);
    }

    private void updateCountdownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        @SuppressLint("DefaultLocale") String timeFormatted = String.format("%02d:%02d", minutes, seconds);
        countdownText.setText(timeFormatted);
    }

    private void updateProgressBar() {
        int progress = (int) ((initialTimeInMillis - timeLeftInMillis) * 100 / initialTimeInMillis);
        progressBar.setProgress(progress);
    }

    private void sendProgressNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("倒计时")
                .setContentText("还剩 " + timeLeftInMillis / 1000 + " 秒")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, 0, false)
                .setOngoing(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private void updateNotificationProgress() {
        int progress = (int) ((initialTimeInMillis - timeLeftInMillis) * 100 / initialTimeInMillis);
        updateNotificationProgress(progress);
    }

    private void updateNotificationProgress(int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("倒计时")
                .setContentText("还剩 " + timeLeftInMillis / 1000 + " 秒")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, progress, false)
                .setOngoing(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private void sendCompletionNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("倒计时")
                .setContentText("时间到！")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{0, 1000})
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(2, builder.build());
    }

    private void resetUI() {
        minuteInput.setVisibility(View.VISIBLE);
        secondInput.setVisibility(View.VISIBLE);
        startButton.setVisibility(View.VISIBLE);
        startButton.setText("开始倒计时");
        pauseButton.setVisibility(View.GONE);
        resetButton.setVisibility(View.GONE);
        progressBar.setProgress(0);

        // 移除进度通知
        notificationManager.cancel(1);
    }
}
