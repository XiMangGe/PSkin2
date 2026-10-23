/*
 * Decompiled with CFR 0.152.
 */
package com.pskin;

import com.pskin.MojangSkinProvider;
import com.pskin.PSkinConfig;
import com.pskin.SkinFetchResult;
import com.pskin.SkinProvider;
import com.pskin.libs.gson.JsonArray;
import com.pskin.libs.gson.JsonObject;
import com.pskin.libs.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class LittleSkinProvider
implements SkinProvider {
    private final PSkinConfig config;
    private final HttpClient httpClient;
    private final String apiRoot;
    private final String displayName;
    private final int timeoutSeconds;

    public LittleSkinProvider(PSkinConfig config) {
        this(config, config.getLittleSkinApiRoot(), "LittleSkin", config.getLittleSkinTimeout());
    }

    public LittleSkinProvider(PSkinConfig config, String apiRoot, String displayName) {
        this(config, apiRoot, displayName, config.getLittleSkinTimeout());
    }

    /**
     * 自定义 Yggdrasil 皮肤站构造函数。
     * @param config 插件配置
     * @param apiRoot Yggdrasil API 根地址
     * @param displayName 显示名称（用于日志和 /pskin status）
     * @param timeoutSeconds 该皮肤站的请求超时（秒）
     */
    public LittleSkinProvider(PSkinConfig config, String apiRoot, String displayName, int timeoutSeconds) {
        this.config = config;
        String root = apiRoot;
        if (root != null && root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        this.apiRoot = root;
        this.displayName = displayName == null || displayName.isEmpty() ? "LittleSkin" : displayName;
        this.timeoutSeconds = Math.max(3, timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    @Override
    public String getName() {
        return this.displayName;
    }

    @Override
    public CompletableFuture<SkinFetchResult> fetchSkin(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> sessionResp;
                JsonArray profilesArray;
                HttpResponse<String> profilesResp;
                String apiRoot = this.apiRoot;
                if (apiRoot == null || apiRoot.isEmpty()) {
                    return SkinFetchResult.apiError();
                }
                String profilesUrl = apiRoot + "/api/profiles/minecraft";
                // 用 Gson 构造 JSON，避免用户名中的引号/反斜杠破坏请求体
                JsonArray nameArray = new JsonArray();
                nameArray.add(username);
                String body = nameArray.toString();
                HttpRequest profilesReq = HttpRequest.newBuilder().uri(URI.create(profilesUrl)).timeout(Duration.ofSeconds(this.timeoutSeconds)).header("Content-Type", "application/json").header("User-Agent", this.config.getUserAgent()).POST(HttpRequest.BodyPublishers.ofString(body)).build();
                try {
                    profilesResp = this.httpClient.send(profilesReq, HttpResponse.BodyHandlers.ofString());
                }
                catch (HttpTimeoutException e) {
                    return SkinFetchResult.timeout();
                }
                catch (IOException e) {
                    return SkinFetchResult.networkError();
                }
                if (profilesResp.statusCode() == 404 || profilesResp.statusCode() == 204) {
                    return SkinFetchResult.notFound();
                }
                if (profilesResp.statusCode() == 429) {
                    return SkinFetchResult.rateLimited();
                }
                if (profilesResp.statusCode() != 200) {
                    return SkinFetchResult.apiError();
                }
                try {
                    profilesArray = JsonParser.parseString(profilesResp.body()).getAsJsonArray();
                }
                catch (Exception e) {
                    return SkinFetchResult.invalidResponse();
                }
                if (profilesArray == null || profilesArray.isEmpty()) {
                    return SkinFetchResult.notFound();
                }
                JsonObject profile = profilesArray.get(0).getAsJsonObject();
                if (!profile.has("id")) {
                    return SkinFetchResult.invalidResponse();
                }
                String uuid = profile.get("id").getAsString();
                String sessionUrl = apiRoot + "/sessionserver/session/minecraft/profile/" + uuid + "?unsigned=false";
                HttpRequest sessionReq = HttpRequest.newBuilder().uri(URI.create(sessionUrl)).timeout(Duration.ofSeconds(this.timeoutSeconds)).header("User-Agent", this.config.getUserAgent()).GET().build();
                try {
                    sessionResp = this.httpClient.send(sessionReq, HttpResponse.BodyHandlers.ofString());
                }
                catch (HttpTimeoutException e) {
                    return SkinFetchResult.timeout();
                }
                catch (IOException e) {
                    return SkinFetchResult.networkError();
                }
                if (sessionResp.statusCode() == 429) {
                    return SkinFetchResult.rateLimited();
                }
                if (sessionResp.statusCode() == 404) {
                    return SkinFetchResult.notFound();
                }
                if (sessionResp.statusCode() != 200) {
                    return SkinFetchResult.apiError();
                }
                return MojangSkinProvider.extractTextures(sessionResp.body(), this.displayName);
            }
            catch (Exception e) {
                return SkinFetchResult.unknownError();
            }
        });
    }
}
