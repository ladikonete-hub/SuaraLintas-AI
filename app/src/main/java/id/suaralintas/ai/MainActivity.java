package id.suaralintas.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_AUDIO = 41;
    private Spinner languageSpinner;
    private TextView statusText, heardText, translatedText;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private String latestTranslation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("id", "ID"));
            }
        });

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("SuaraLintas AI");
        title.setTextSize(30);
        title.setTypeface(null, 1);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Penerjemah suara Indonesia ↔ Mandarin\nPrototipe awal — belum menerjemahkan otomatis.");
        subtitle.setTextSize(16);
        subtitle.setPadding(0, dp(8), 0, dp(18));
        root.addView(subtitle);

        TextView direction = new TextView(this);
        direction.setText("Pilih bahasa yang sedang diucapkan:");
        root.addView(direction);

        languageSpinner = new Spinner(this);
        String[] langs = {"Bahasa Indonesia", "Mandarin standar"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, langs);
        languageSpinner.setAdapter(adapter);
        root.addView(languageSpinner);

        Button listen = new Button(this);
        listen.setText("🎙 DENGARKAN UCAPAN");
        root.addView(listen);
        listen.setOnClickListener(v -> startListening());

        statusText = new TextView(this);
        statusText.setText("Status: siap. Pengenalan suara offline bergantung pada dukungan HP dan paket bahasa.");
        statusText.setTextSize(14);
        statusText.setPadding(0, dp(12), 0, dp(12));
        root.addView(statusText);

        addSection(root, "Teks yang terdengar", false);
        heardText = new TextView(this);
        heardText.setText("Ucapan akan tampil di sini.");
        heardText.setTextSize(18);
        heardText.setPadding(0, 0, 0, dp(14));
        root.addView(heardText);

        addSection(root, "Hasil terjemahan", false);
        translatedText = new TextView(this);
        translatedText.setText("Mesin terjemahan offline belum dipasang pada versi awal.");
        translatedText.setTextSize(18);
        translatedText.setPadding(0, 0, 0, dp(12));
        root.addView(translatedText);

        Button speak = new Button(this);
        speak.setText("🔊 BACA TEKS");
        root.addView(speak);
        speak.setOnClickListener(v -> {
            String text = latestTranslation.isEmpty() ? heardText.getText().toString() : latestTranslation;
            if (tts != null && !text.isEmpty()) {
                Locale locale = languageSpinner.getSelectedItemPosition() == 0
                        ? new Locale("id", "ID") : Locale.SIMPLIFIED_CHINESE;
                int result = tts.setLanguage(locale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Paket suara untuk bahasa ini belum tersedia di HP.", Toast.LENGTH_LONG).show();
                } else {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "suaralintas");
                }
            }
        });

        TextView note = new TextView(this);
        note.setText("\nCatatan: versi ini baru menguji antarmuka, pengenalan ucapan bawaan Android, dan pembacaan teks. Belum ada penerjemah offline atau deteksi dialek.");
        note.setTextSize(13);
        root.addView(note);
        setContentView(scroll);
    }

    private void addSection(LinearLayout root, String text, boolean bold) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(19);
        if (bold) label.setTypeface(null, 1);
        label.setPadding(0, dp(8), 0, dp(6));
        root.addView(label);
    }

    private void startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.setText("Layanan pengenalan suara tidak tersedia di HP ini.");
            return;
        }
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { statusText.setText("Mendengarkan..."); }
            @Override public void onBeginningOfSpeech() { statusText.setText("Suara terdeteksi..."); }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { statusText.setText("Memproses ucapan..."); }
            @Override public void onError(int error) { statusText.setText("Pengenalan gagal (kode " + error + "). Coba lagi atau periksa paket bahasa di HP."); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    heardText.setText(matches.get(0));
                    statusText.setText("Ucapan dikenali. Penerjemahan otomatis belum tersedia pada prototipe ini.");
                } else statusText.setText("Tidak ada teks yang dikenali.");
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        String lang = languageSpinner.getSelectedItemPosition() == 0 ? "id-ID" : "zh-CN";
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        if (Build.VERSION.SDK_INT >= 31) {
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        }
        recognizer.startListening(intent);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
