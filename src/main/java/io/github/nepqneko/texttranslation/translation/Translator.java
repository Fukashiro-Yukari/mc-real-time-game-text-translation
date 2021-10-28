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

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class Translator extends Thread {
    public static final Map<String, Map<String, Thread>> threads = Maps.newHashMap();
    private static final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Translator thread").build();
    private static final ExecutorService es = Executors.newSingleThreadExecutor(threadFactory);
    private static boolean IsFailed;
    private static int tasksCount;

    private final String key;
    private final String langTo;
    private final String text;

    private long nowTime;

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

    private long getNowTime() {
        return (ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - nowTime) / 1000000;
    }

    private String GoogleTranslate(String text) throws Exception {
        if (text.isEmpty()) return null;

        RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] FUCK");
        RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] " + new HttpUrl.Builder().scheme("https").host("fuckyou.com").build());

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

        if (ModConfig.ENABLE_DEBUG.getValue()) {
            JsonObject obj = new JsonObject();

            obj.addProperty("key", key);
            obj.addProperty("language", langTo);
            obj.addProperty("text", text);

            RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation send GET request: " + obj);
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = client.newCall(request).execute();

        if (ModConfig.ENABLE_DEBUG.getValue()) {
            JsonObject obj = new JsonObject();

            obj.addProperty("key", key);
            obj.addProperty("language", langTo);
            obj.addProperty("text", text);

            RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation Complete GET request: " + obj);
        }

        client.dispatcher().cancelAll();

        String result = Objects.requireNonNull(response.body()).string();

        if (response.code() != 200) {
            JsonObject obj = new JsonObject();
            String reason = getReason(response.code());

            if (reason == null) {
                reason = result;
            }

            obj.addProperty("key", key);
            obj.addProperty("runtime", getNowTime() + "ms");
            obj.addProperty("code", response.code());
            obj.addProperty("result", reason);

            RealTimeGameTextTranslation.LOGGER.warn("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation failed: " + obj);
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

                RealTimeGameTextTranslation.LOGGER.warn("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation pause: Wait for " + (random / 1000) + " seconds");

                Thread.sleep(random); //If google denies access, wait for a while and try
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            IsFailed = false;
        }

        if (ModConfig.ENABLE_DEBUG.getValue()) {
            JsonObject obj = new JsonObject();

            obj.addProperty("key", key);
            obj.addProperty("language", langTo);
            obj.addProperty("text", text);

            RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation start: " + obj);
        }

        nowTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
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
            obj.addProperty("runtime", getNowTime() + "ms");
            obj.addProperty("result", exception.getLocalizedMessage());

            RealTimeGameTextTranslation.LOGGER.warn("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation failed: " + obj);

            keyThreads.remove(key);
        }

        if (result != null) {
            byte[] byteStr = result.getBytes(StandardCharsets.UTF_8);
            String utf8Result = new String(byteStr, StandardCharsets.UTF_8);

            if (ModConfig.ENABLE_DEBUG.getValue()) {
                JsonObject obj = new JsonObject();

                obj.addProperty("key", key);
                obj.addProperty("runtime", getNowTime() + "ms");
                obj.addProperty("result", utf8Result);

                RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation result: " + obj);
            }

            if (!RealTimeGameTextTranslation.GoogleTranslationsMap.containsKey(langTo))
                RealTimeGameTextTranslation.GoogleTranslationsMap.put(langTo, new TreeMap<>());

            Map<String, String> newTranslations = RealTimeGameTextTranslation.GoogleTranslationsMap.get(langTo);

            newTranslations.put(key, utf8Result);
        }

        try {
            long random = RandomUtils.nextLong(1000, 30000);

            tasksCount--;

            if (ModConfig.ENABLE_DEBUG.getValue()) {
                RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation tasks remaining: " + tasksCount);
                RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation pause: Wait for " + (random / 1000) + " seconds");
            }

            Thread.sleep(random); //I do not have money...
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (!threads.containsKey(langTo))
            threads.put(langTo, Maps.newHashMap());

        Map<String, Thread> keyThreads = threads.get(langTo);

        if (!keyThreads.containsKey(key)) {
            tasksCount++;

            if (ModConfig.ENABLE_DEBUG.getValue()) {
                JsonObject obj = new JsonObject();

                obj.addProperty("key", key);
                obj.addProperty("language", langTo);
                obj.addProperty("text", text);

                RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation initialization: " + obj);
                RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] Translation tasks remaining: " + tasksCount);
            }

            Thread thread = new Thread(this, RealTimeGameTextTranslation.MOD_ID + "_" + key);

            es.submit(thread);

            keyThreads.put(key, thread);
        }
    }
}
