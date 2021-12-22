package densowave.com.DensoScannerSDK_demo;

import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.TypedValue;

import java.util.EnumMap;
import java.util.Map;

/**
 * Class which gathers and manages the audio track of beep sound
 */
public class BeepAudioTracks {

    private final static EnumMap<AudioTrackName, BeepAudioTrack> audioTracks = new EnumMap<>(AudioTrackName.class);

    // Configuration information
    // These values ​​are acquired from Resources
    private static Integer samplingRate = null;
    private static Integer repeatCount = null;
    private static int[] frequencies = null;
    private static int[] soundDurations = null;
    private static int[] silentDurations = null;
    private static Float volume = null;

    /**
     * Set up audio tracks in advance
     * Since it takes a certain processing time to generate an audio track, make sure it has been set up at the timing of the user side.
     * @param resources Resource of setting information
     */
    public static void setupAudioTracks(Resources resources) {
        // Release in advance
        releaseAudioTracks();

        // Set up configuration information
        setupConfig(resources);

        // Generate each audio track
        AudioTrackName[] names = new AudioTrackName[] {
                AudioTrackName.TRACK_1,
                AudioTrackName.TRACK_2,
                AudioTrackName.TRACK_3,
                AudioTrackName.TRACK_4,
                AudioTrackName.TRACK_5};
        for (int i = 0; i < names.length; i++) {
            AudioTrackName name = names[i];
            BeepAudioTrack audioTrack = new BeepAudioTrack(frequencies[i], soundDurations[i], silentDurations[i]);
            audioTracks.put(name, audioTrack);
        }
    }

    /**
     * Set up the setting information
     * @param resources Resource of setting information
     */
    private static void setupConfig(Resources resources) {
        samplingRate = resources.getInteger(R.integer.beep_sampling_rate_hz);
        repeatCount = resources.getInteger(R.integer.beep_repeat_count);
        frequencies = resources.getIntArray(R.array.beep_frequencies_hz);
        soundDurations = resources.getIntArray(R.array.beep_sound_durations_ms);
        silentDurations = resources.getIntArray(R.array.beep_silent_durations_ms);

        TypedValue typedBeepVolume = new TypedValue();
        resources.getValue(R.dimen.beep_volume, typedBeepVolume, true);
        volume = typedBeepVolume.getFloat();
    }

    /**
     * Release all audio tracks.
     * It must be released before closing application.
     */
    public static void releaseAudioTracks() {
        for (Map.Entry<AudioTrackName, BeepAudioTrack> entry : audioTracks.entrySet()) {
            BeepAudioTrack audioTrack = entry.getValue();
            audioTrack.release();
        }
        audioTracks.clear();
    }

    /**
     * Play an audio track with the specified name
     * @param audioTrackName Audio Track name
     */
    public static void play(AudioTrackName audioTrackName) {
        audioTracks.get(audioTrackName).play();
    }

    /**
     * Stop the audio track with the specified name
     * @param audioTrackName Audio Track name
     */
    public static void stop(AudioTrackName audioTrackName) {
        audioTracks.get(audioTrackName).stop();
    }

    /**
     * Whether an audio track with the specified name is playing
     * @param audioTrackName Audio Track name
     * @return Return true if the specified audio track with the specified name is playing. And return false if it is stopped.
     */
    public static boolean isPlaying(AudioTrackName audioTrackName) {
        return audioTracks.get(audioTrackName).isPlaying();
    }

    /**
     * Stop all the audio tracks.
     */
    public static void stopAudioTracks() {
        for (Map.Entry<AudioTrackName, BeepAudioTrack> entry : audioTracks.entrySet()) {
            BeepAudioTrack audioTrack = entry.getValue();
            audioTrack.stop();
        }
    }

    /**
     * Audio track of beep sound
     * Express AudioTrack by wrapping in order to bring parameters for beep sound.
     */
    private static class BeepAudioTrack {

        private AudioTrack audioTrack;
        private int soundDuration;
        private int silentDuration;
        private Handler playHandler = new Handler();
        private Runnable playAction = null;

        /**
         * Initialize
         * @param frequency Frequency (Hertz)
         * @param soundDuration  Time to sound (milliseconds)
         * @param silentDuration Time to stop sound (milliseconds)
         */
        BeepAudioTrack(int frequency, int soundDuration, int silentDuration) {
            this.soundDuration = soundDuration;
            this.silentDuration = silentDuration;

            int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

            // Secure the buffer for the sounding time
            // In this configuration, it will be (buffer for the sounding time) = (number of seconds) / 4 * (sampling frequency)
            // TODO: for now I have no idea why it can be improved by dividing by 4. Will add the reason when I get to know about it
            // Set to a multiple of sampling frequency otherwise it may not be possible to generate AudioTrack
            // Round the “(number of seconds) / 4" result to an integer value. This will be the multiple of the sampling frequency
            // TODO: I want to understand why it is required to set to a multiple, and whether there are any cases we can generate it without having to set to a multiple
            // Secure the buffer for a bit longer than the sounding time so that the ending sound of the sound is not interrupted
            // Currently securing the buffer longer than the sounding time by adding 1 to the “(number of seconds) / 4" result
            // If it is less than the minimum buffer size, you can not generate AudioTrack. Make sure that it does not fall below the minimum buffer size
            // TODO: I would like to look into this since I believe it would be OK when the sampling frequency is exceeded
            float soundDurationSecond = (float)(soundDuration) / 1000.0f;
            int soundBufferSize = (int)(soundDurationSecond / 4.0f + 1.0f) * samplingRate;

            int minBufferSize = AudioTrack.getMinBufferSize(samplingRate, channelConfig, audioFormat);
            int bufferSize = Math.max(soundBufferSize, minBufferSize);

            // Specify STREAM_SYSTEM since the beep sound corresponds to system sound
            // Specify MODE_STATIC since it is assumed that it will not be a long sound
            audioTrack = new AudioTrack(AudioManager.STREAM_SYSTEM,
                    samplingRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STATIC);

            // Create a waveform of the specified frequency and write to the audio track
            short[] audioData = new short[bufferSize];
            for (int i = 0; i < audioData.length; i++) {
                double sound = Math.sin(2.0 * Math.PI * i / ((double)samplingRate / frequency));

                audioData[i] = (short) (sound * Short.MAX_VALUE);

            }
            audioTrack.write(audioData, 0, audioData.length);

            // Volume setting
            audioTrack.setVolume(volume);
        }

        /**
         * Release the audio track
         */
        void release() {
            // Remember to also terminate the sound processing
            if (playAction != null) {
                playHandler.removeCallbacks(playAction);
                playAction = null;
            }

            audioTrack.release();
        }

        /**
         * Play
         * Stop the sound of the specified frequency for a certain period of time after sounding for a certain period of time and repeat this for a certain number of times.
         * Execute the processing to play in a separate thread
         * While playing back, the playing time is not reset and the sound continues to be played continuously
         * If you want to play from the beginning, execute stop once and then execute playback again
         */
        void play() {
            // Do nothing since the sound is being played
            if (playAction != null) {
                return;
            }

            // Sound asynchronously
            playAction = new Runnable() {
                int remainingCount = repeatCount;
                boolean nextModeIsSound = true;

                @Override
                public void run() {
                    // Switch the “Sound → Do not sound" mode and execute
                    if (nextModeIsSound) {
                        // Stop when it sounds for a certain number of times
                        if (remainingCount <= 0) {
                            stop();
                            return;
                        }
                        --remainingCount;

                        sound();
                    } else {
                        silent();
                    }
                }

                /**
                 * The sound is sounded
                 * Put it into the state that the sound is not emitted when sounding it during the fixed time
                 */
                private void sound() {
                    audioTrack.reloadStaticData();
                    audioTrack.play();

                    nextModeIsSound = false;
                    playHandler.postDelayed(this, (long) soundDuration);
                }

                /**
                 * The sound is not emitted.
                 * When the fixed time passes, put it into the state to sound the sound
                 * When the loop of "Sounded → is not sounded" becomes a constant frequency, it ends.
                 */
                private void silent() {
                    // Use pause() and flush() instead of stop() to stop immediately
                    audioTrack.pause();
                    audioTrack.flush();

                    nextModeIsSound = true;
                    playHandler.postDelayed(this, (long) silentDuration);
                }
            };
            playHandler.post(playAction);
        }

        /**
         * Stop
         */
        void stop() {
            // Remember to also terminate the sound processing
            if (playAction != null) {
                playHandler.removeCallbacks(playAction);
                playAction = null;

                audioTrack.stop();
            }
        }

        /**
         * Whether it is playing back or not?
         * @return Return true if it is playing back, false if it is stopped
         */
        boolean isPlaying() {
            return playAction != null;
        }
    }

    /**
     * Name of audio track
     */
    public enum AudioTrackName {
        TRACK_1
        , TRACK_2
        , TRACK_3
        , TRACK_4
        , TRACK_5
    }
}
