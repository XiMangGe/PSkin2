/*
 * Decompiled with CFR 0.152.
 */
package com.pskin;

import com.pskin.PSkinConfig;
import com.pskin.SkinData;
import com.pskin.SkinFetchResult;
import com.pskin.libs.gson.JsonObject;
import com.pskin.libs.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class UrlSkinProvider {
    private final PSkinConfig config;
    private final HttpClient httpClient;

    public UrlSkinProvider(PSkinConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15L)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    public SkinFetchResult fetchSkinFromUrl(String imageUrl) {
        try {
            JsonObject json;
            HttpResponse<String> resp;
            SkinFetchResult precheck = this.validateImageUrl(imageUrl);
            if (precheck != null) {
                return precheck;
            }
            String encodedUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
            String mineskinUrl = "https://api.mineskin.org/generate/url?url=" + encodedUrl + "&visibility=1";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(mineskinUrl)).timeout(Duration.ofSeconds(30L)).header("User-Agent", this.config.getUserAgent()).POST(HttpRequest.BodyPublishers.noBody()).build();
            try {
                resp = this.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            }
            catch (HttpTimeoutException e) {
                return SkinFetchResult.timeout();
            }
            catch (IOException e) {
                return SkinFetchResult.networkError();
            }
            int code = resp.statusCode();
            if (code == 429) {
                return SkinFetchResult.urlRateLimited();
            }
            if (code == 413) {
                return SkinFetchResult.urlImageTooLarge();
            }
            if (code == 400) {
                return SkinFetchResult.urlInvalidImage();
            }
            if (code != 200) {
                return SkinFetchResult.apiError();
            }
            try {
                json = JsonParser.parseString(resp.body()).getAsJsonObject();
            }
            catch (Exception e) {
                return SkinFetchResult.invalidResponse();
            }
            if (!json.has("data")) {
                if (json.has("error")) {
                    return SkinFetchResult.urlInvalidImage();
                }
                return SkinFetchResult.invalidResponse();
            }
            JsonObject data = json.getAsJsonObject("data");
            if (!data.has("texture")) {
                return SkinFetchResult.invalidResponse();
            }
            JsonObject texture = data.getAsJsonObject("texture");
            if (!texture.has("value")) {
                return SkinFetchResult.invalidResponse();
            }
            String value = texture.get("value").getAsString();
            String signature = texture.has("signature") ? texture.get("signature").getAsString() : null;
            return SkinFetchResult.success(new SkinData(value, signature, "URL"));
        }
        catch (Exception e) {
            return SkinFetchResult.unknownError();
        }
    }

    private SkinFetchResult validateImageUrl(String imageUrl) {
        try {
            boolean isJpeg;
            HttpResponse<byte[]> fullResp = this.httpClient.send(HttpRequest.newBuilder().uri(URI.create(imageUrl)).timeout(Duration.ofSeconds(10L)).header("User-Agent", this.config.getUserAgent()).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            int code = fullResp.statusCode();
            if (code == 429) {
                return SkinFetchResult.urlRateLimited();
            }
            if (code == 413) {
                return SkinFetchResult.urlImageTooLarge();
            }
            if (code != 200) {
                return SkinFetchResult.networkError();
            }
            byte[] data = fullResp.body();
            if (data == null || data.length == 0) {
                return SkinFetchResult.urlInvalidImage();
            }
            boolean isPng = data.length >= 8 && (data[0] & 0xFF) == 137 && data[1] == 80 && data[2] == 78 && data[3] == 71;
            boolean bl = isJpeg = data.length >= 3 && (data[0] & 0xFF) == 255 && (data[1] & 0xFF) == 216 && (data[2] & 0xFF) == 255;
            if (!isPng && !isJpeg) {
                return SkinFetchResult.urlInvalidImage();
            }
            return null;
        }
        catch (HttpTimeoutException e) {
            return SkinFetchResult.timeout();
        }
        catch (IOException e) {
            return SkinFetchResult.networkError();
        }
        catch (Exception e) {
            return SkinFetchResult.urlInvalidImage();
        }
    }
}
