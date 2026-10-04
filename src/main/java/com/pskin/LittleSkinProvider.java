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

    public LittleSkinProvider(PSkinConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    @Override
    public String getName() {
        return "LittleSkin";
    }

    @Override
    public CompletableFuture<SkinFetchResult> fetchSkin(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> sessionResp;
                JsonArray profilesArray;
                HttpResponse<String> profilesResp;
                String apiRoot = this.config.getLittleSkinApiRoot();
                if (apiRoot.endsWith("/")) {
                    apiRoot = apiRoot.substring(0, apiRoot.length() - 1);
                }
                String profilesUrl = apiRoot + "/api/profiles/minecraft";
                String body = "[\"" + username.replace("\"", "\\\"") + "\"]";
                HttpRequest profilesReq = HttpRequest.newBuilder().uri(URI.create(profilesUrl)).timeout(Duration.ofSeconds(this.config.getLittleSkinTimeout())).header("Content-Type", "application/json").header("User-Agent", this.config.getUserAgent()).POST(HttpRequest.BodyPublishers.ofString(body)).build();
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
                HttpRequest sessionReq = HttpRequest.newBuilder().uri(URI.create(sessionUrl)).timeout(Duration.ofSeconds(this.config.getLittleSkinTimeout())).header("User-Agent", this.config.getUserAgent()).GET().build();
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
                return MojangSkinProvider.extractTextures(sessionResp.body(), "LittleSkin");
            }
            catch (Exception e) {
                return SkinFetchResult.unknownError();
            }
        });
    }
}
