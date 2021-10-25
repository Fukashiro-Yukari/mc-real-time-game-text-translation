package io.github.nepqneko.texttranslation.translation;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import io.github.nepqneko.texttranslation.RealTimeGameTextTranslation;
import io.github.nepqneko.texttranslation.config.ModConfig;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.RandomUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class Translator extends Thread {
    public static final Map<String, Map<String, Thread>> threads = Maps.newHashMap();
    private static final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Translator thread").build();
    private static final ExecutorService es = Executors.newSingleThreadExecutor(threadFactory);
    private static boolean IsFailed;
    private final String key;
    private final String langTo;
    private final String text;

    public Translator(String key, String langTo, String text) {
        this.key = key;
        this.langTo = langTo;
        this.text = text;
    }

    private String getReason(int code) {
        return switch (code) {
            case 429 -> "Too Many Requests";
            case 404 -> "Not Found"; // This will never happen unless google really changes the link
            default -> null;
        };
    }

    private String GoogleTranslate(String text) throws Exception {
        if (text.isEmpty()) return null;

        OkHttpClient client = new OkHttpClient();

        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("translate.googleapis.com")
                .addPathSegments("translate_a/single")
                .addQueryParameter("client", "gtx")
                .addQueryParameter("sl", "en")
                .addQueryParameter("tl", langTo)
                .addQueryParameter("dt", "t")
                .addQueryParameter("q", text)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = client.newCall(request).execute();

        client.dispatcher().cancelAll();

        String result = Objects.requireNonNull(response.body()).string();

        if (response.code() != 200) {
            JsonObject obj = new JsonObject();
            String reason = getReason(response.code());

            if (reason == null) {
                reason = result;
            }

            obj.addProperty("key", key);
            obj.addProperty("code", response.code());
            obj.addProperty("result", reason);

            RealTimeGameTextTranslation.LOGGER.warn("Translation failed: " + obj);
            IsFailed = true;

            return null;
        }

        return result.substring(4, result.indexOf(',') - 1);
    }

    @Override
    public void run() {
        if (IsFailed) {
            try {
                long random = RandomUtils.nextLong(1200000, 2400000);

                RealTimeGameTextTranslation.LOGGER.warn("Translation pause: Wait for " + (random / 1000) + " seconds");

                Thread.sleep(random); //If google denies access, wait for a while and try
            } catch (InterruptedException ignored) {
            }

            IsFailed = false;
        }

        String result = null;
        Map<String, Thread> keyThreads = threads.get(langTo);

        try {
            String ret = GoogleTranslate(text);

            if (ret != null) {
                result = ret;
            } else {
                keyThreads.remove(key);
            }
        } catch (Exception exception) {
            JsonObject obj = new JsonObject();

            obj.addProperty("key", key);
            obj.addProperty("result", exception.toString());

            RealTimeGameTextTranslation.LOGGER.warn("Translation failed: " + obj);

            keyThreads.remove(key);
        }

        if (result != null) {
            byte[] byteStr = result.getBytes(StandardCharsets.UTF_8);
            String utf8Result = new String(byteStr, StandardCharsets.UTF_8);

            if (ModConfig.ENABLE_DEBUG.getValue()) {
                JsonObject obj = new JsonObject();

                obj.addProperty("key", key);
                obj.addProperty("result", utf8Result);

                RealTimeGameTextTranslation.LOGGER.info("Translation result: " + obj);
            }

            if (!RealTimeGameTextTranslation.TranslationsMap.containsKey(langTo))
                RealTimeGameTextTranslation.TranslationsMap.put(langTo, Maps.newHashMap());

            Map<String, String> newTranslations = RealTimeGameTextTranslation.TranslationsMap.get(langTo);

            newTranslations.put(key, utf8Result);
        }

        try {
            long random = RandomUtils.nextLong(1000, 10000);

            Thread.sleep(random); //I do not have money...
        } catch (InterruptedException ignored) {
        }
    }

    public void start() {
        if (!threads.containsKey(langTo))
            threads.put(langTo, Maps.newHashMap());

        Map<String, Thread> keyThreads = threads.get(langTo);

        if (!keyThreads.containsKey(key)) {
            Thread thread = new Thread(this, RealTimeGameTextTranslation.MOD_ID + "_" + key);

            es.submit(thread);

            keyThreads.put(key, thread);
        }
    }
}
