/*
 * Decompiled with CFR 0.152.
 */
package com.pskin;

import com.pskin.PSkinConfig;
import com.pskin.SkinData;
import com.pskin.SkinFetchResult;
import com.pskin.SkinProvider;
import com.pskin.libs.gson.JsonArray;
import com.pskin.libs.gson.JsonElement;
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

public class MojangSkinProvider
implements SkinProvider {
    private final PSkinConfig config;
    private final HttpClient httpClient;

    public MojangSkinProvider(PSkinConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    @Override
    public String getName() {
        return "Mojang";
    }

    @Override
    public CompletableFuture<SkinFetchResult> fetchSkin(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> sessionResp;
                JsonObject profileJson;
                HttpResponse<String> profileResp;
                String profileUrl = String.format(this.config.getMojangProfileUrl(), username);
                HttpRequest profileReq = HttpRequest.newBuilder().uri(URI.create(profileUrl)).timeout(Duration.ofSeconds(this.config.getMojangTimeout())).header("User-Agent", this.config.getUserAgent()).GET().build();
                try {
                    profileResp = this.httpClient.send(profileReq, HttpResponse.BodyHandlers.ofString());
                }
                catch (HttpTimeoutException e) {
                    return SkinFetchResult.timeout();
                }
                catch (IOException e) {
                    return SkinFetchResult.networkError();
                }
                if (profileResp.statusCode() == 404 || profileResp.statusCode() == 204) {
                    return SkinFetchResult.notFound();
                }
                if (profileResp.statusCode() == 429) {
                    return SkinFetchResult.rateLimited();
                }
                if (profileResp.statusCode() != 200) {
                    return SkinFetchResult.apiError();
                }
                try {
                    profileJson = JsonParser.parseString(profileResp.body()).getAsJsonObject();
                }
                catch (Exception e) {
                    return SkinFetchResult.invalidResponse();
                }
                if (profileJson == null || !profileJson.has("id")) {
                    return SkinFetchResult.notFound();
                }
                String uuid = profileJson.get("id").getAsString();
                String sessionUrl = String.format(this.config.getMojangSessionUrl(), uuid);
                HttpRequest sessionReq = HttpRequest.newBuilder().uri(URI.create(sessionUrl)).timeout(Duration.ofSeconds(this.config.getMojangTimeout())).header("User-Agent", this.config.getUserAgent()).GET().build();
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
                if (sessionResp.statusCode() != 200) {
                    return SkinFetchResult.apiError();
                }
                return MojangSkinProvider.extractTextures(sessionResp.body(), "Mojang");
            }
            catch (Exception e) {
                return SkinFetchResult.unknownError();
            }
        });
    }

    static SkinFetchResult extractTextures(String body, String source) {
        try {
            JsonObject sessionJson = JsonParser.parseString(body).getAsJsonObject();
            if (!sessionJson.has("properties")) {
                return SkinFetchResult.invalidResponse();
            }
            JsonArray properties = sessionJson.getAsJsonArray("properties");
            String value = null;
            String signature = null;
            for (JsonElement prop : properties) {
                JsonObject p = prop.getAsJsonObject();
                if (!p.has("name") || !"textures".equals(p.get("name").getAsString())) continue;
                value = p.get("value").getAsString();
                if (!p.has("signature")) break;
                signature = p.get("signature").getAsString();
                break;
            }
            if (value == null) {
                return SkinFetchResult.invalidResponse();
            }
            return SkinFetchResult.success(new SkinData(value, signature, source));
        }
        catch (Exception e) {
            return SkinFetchResult.invalidResponse();
        }
    }
}
