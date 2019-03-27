package com.example.gukov.slyrecord;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity implements View.OnClickListener {

    ImageButton btnStart, btnStop, btnPlayback; //кнопки
    TextView tvInfo; // отображаемый текст
    Animation mAnimation; // анимация
    Boolean recording; // для переключения записи

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // находим элементы
        tvInfo = (TextView) findViewById(R.id.tvInfo);
        btnStart = (ImageButton) findViewById(R.id.btnStart);
        btnStop = (ImageButton) findViewById(R.id.btnStop);
        btnPlayback = (ImageButton) findViewById(R.id.btnPlayback);

        // подключаем файл анимации
        mAnimation = AnimationUtils.loadAnimation(this, R.anim.flashing);

        // устанавливаем слушатели
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnPlayback.setOnClickListener(this);
    }


    // действия по нажатию кнопок
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnStart: // кнопка "начать запись"
                btnStart.setVisibility(View.GONE);
                btnStop.setVisibility(View.VISIBLE);
                tvInfo.setText(getResources().getString(R.string.recording));
                tvInfo.startAnimation(mAnimation);
                btnPlayback.setClickable(false);

                Thread recordThread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        recording = true;
                        startRecord();
                    }
                });
                recordThread.start();
                break;
            case R.id.btnStop: // кнопка "остановить запись"
                btnStart.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.GONE);
                tvInfo.setText(getResources().getString(R.string.after));
                tvInfo.clearAnimation();
                btnPlayback.setClickable(true);

                recording = false;
                break;
            case R.id.btnPlayback: // кнопка "воспроизвести"
                tvInfo.setText(getResources().getString(R.string.playing));
                tvInfo.startAnimation(mAnimation);
                btnPlayback.setClickable(false);
                btnStart.setClickable(false);

                Thread playThread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        playRecord();
                    }
                });
                playThread.start();
                break;
            default:
                break;
        }
    }


    // функция записи
    private void startRecord() {

        File file = new File(Environment.getExternalStorageDirectory(), "test.pcm");  // имя файла

        try {
            file.createNewFile();

            OutputStream outputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            int minBufferSize = AudioRecord.getMinBufferSize(11025,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            short[] audioData = new short[minBufferSize];

            // создаем запись аудиорекорд
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    11025,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize);

            audioRecord.startRecording();

            while (recording) {
                int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
                for (int i = 0; i < numberOfShort; i++) {
                    dataOutputStream.writeShort(audioData[i]);
                }
            }

            audioRecord.stop();
            dataOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    // функция воспроизведения
    void playRecord() {

        File file = new File(Environment.getExternalStorageDirectory(), "test.pcm"); // имя файла

        int shortSizeInBytes = Short.SIZE/Byte.SIZE;

        int bufferSizeInBytes = (int)(file.length()/shortSizeInBytes);
        short[] audioData = new short[bufferSizeInBytes];

        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int i = 0;
            while(dataInputStream.available() > 0) {
                audioData[i] = dataInputStream.readShort();
                i++;
            }

            dataInputStream.close();

            // создаем аудиотрек
            AudioTrack audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    11025,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes,
                    AudioTrack.MODE_STREAM);


            // начинаем воспроизводить
            audioTrack.play();
            audioTrack.write(audioData, 0, bufferSizeInBytes);

            // конец воспроизведения
            audioTrack.setNotificationMarkerPosition(bufferSizeInBytes);
            audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onPeriodicNotification(AudioTrack track) { }
                @Override
                public void onMarkerReached(AudioTrack track) {
                    tvInfo.setText(getResources().getString(R.string.playing_end));
                    tvInfo.clearAnimation();
                    btnPlayback.setClickable(true);
                    btnStart.setClickable(true);
                }
            });




        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        tvInfo.clearAnimation();
    }

}