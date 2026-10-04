/*
 * Decompiled with CFR 0.152.
 */
package com.pskin;

import com.pskin.MineskinEndpointManager;
import com.pskin.PSkinConfig;
import com.pskin.SkinData;
import com.pskin.libs.gson.JsonObject;
import com.pskin.libs.gson.JsonParser;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MineskinResigner {
    private static final String MINESKIN_API_PATH = "/v2/generate";
    private final Logger logger;
    private final PSkinConfig config;
    private final MineskinEndpointManager endpoints;

    public MineskinResigner(Logger logger, PSkinConfig config, MineskinEndpointManager endpoints) {
        this.logger = logger;
        this.config = config;
        this.endpoints = endpoints;
    }

    public SkinData resign(SkinData skinData) {
        if (skinData == null) {
            return null;
        }
        if ("mojang".equalsIgnoreCase(skinData.getSource())) {
            return skinData;
        }
        if (!this.config.isMineskinResignEnabled()) {
            return skinData;
        }
        String skinUrl = this.extractSkinUrl(skinData);
        if (skinUrl == null || skinUrl.isEmpty()) {
            if (this.config.isDebug()) {
                this.logger.warning("\u65e0\u6cd5\u4ece\u76ae\u80a4\u6570\u636e\u4e2d\u63d0\u53d6\u56fe\u7247 URL\uff0c\u8df3\u8fc7 Mineskin \u91cd\u7b7e");
            }
            return skinData;
        }
        try {
            String variant = this.detectVariant(skinData);
            SkinData resigned = this.resignFromUrl(skinUrl, variant);
            if (resigned != null) {
                SkinData withOrigin = new SkinData(resigned.getValue(), resigned.getSignature(), resigned.getSource(), skinData.getDisplaySource());
                if (this.config.isDebug()) {
                    this.logger.info("Mineskin \u91cd\u7b7e\u6210\u529f: \u539f\u59cb\u6765\u6e90 " + skinData.getDisplaySource() + "\uff0c\u7b7e\u540d\u7c7b\u578b " + resigned.getSource());
                }
                return withOrigin;
            }
        }
        catch (Exception e) {
            this.logger.log(Level.WARNING, "Mineskin \u91cd\u7b7e\u5931\u8d25: " + e.getMessage());
        }
        return skinData;
    }

    private String extractSkinUrl(SkinData skinData) {
        block3: {
            try {
                String decoded = new String(Base64.getDecoder().decode(skinData.getValue()), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
                if (json.has("textures") && json.getAsJsonObject("textures").has("SKIN")) {
                    return json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                }
            }
            catch (Exception e) {
                if (!this.config.isDebug()) break block3;
                this.logger.warning("\u89e3\u6790\u76ae\u80a4\u7eb9\u7406\u5931\u8d25: " + e.getMessage());
            }
        }
        return null;
    }

    private String detectVariant(SkinData skinData) {
        try {
            String model;
            JsonObject skinObj;
            String decoded = new String(Base64.getDecoder().decode(skinData.getValue()), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
            if (json.has("textures") && json.getAsJsonObject("textures").has("SKIN") && (skinObj = json.getAsJsonObject("textures").getAsJsonObject("SKIN")).has("metadata") && skinObj.getAsJsonObject("metadata").has("model") && "slim".equalsIgnoreCase(model = skinObj.getAsJsonObject("metadata").get("model").getAsString())) {
                return "slim";
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return "classic";
    }

    private SkinData resignFromUrl(String imageUrl, String variant) throws Exception {
        Exception lastError = null;
        for (String endpoint : this.endpoints.tryOrder()) {
            try {
                SkinData result = this.resignFromUrlOnce(endpoint, imageUrl, variant);
                this.endpoints.markSuccess(endpoint);
                return result;
            }
            catch (Exception e) {
                lastError = e;
                this.endpoints.markFailure(endpoint);
                if (!this.config.isDebug()) continue;
                this.logger.warning("Mineskin \u7aef\u70b9 " + endpoint + " \u91cd\u7b7e\u5931\u8d25: " + e.getMessage() + "\uff0c\u5c1d\u8bd5\u4e0b\u4e00\u4e2a\u7aef\u70b9");
            }
        }
        throw lastError != null ? lastError : new RuntimeException("\u65e0\u53ef\u7528 Mineskin \u7aef\u70b9");
    }

    private SkinData resignFromUrlOnce(String endpoint, String imageUrl, String variant) throws Exception {
        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("url", imageUrl);
        bodyJson.addProperty("variant", variant);
        byte[] body = bodyJson.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection)URI.create(endpoint + MINESKIN_API_PATH).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", this.config.getUserAgent() + " Mineskin");
        if (this.config.hasMineskinApiKey()) {
            conn.setRequestProperty("Authorization", "Bearer " + this.config.getMineskinApiKey());
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(this.config.getMineskinTimeoutSeconds() * 1000);
        conn.setReadTimeout(this.config.getMineskinTimeoutSeconds() * 1000);
        try {
        try (OutputStream os = conn.getOutputStream();){
            os.write(body);
            os.flush();
        }
        int code = conn.getResponseCode();
        if (code == 200 || code == 201) {
            try (InputStream is = conn.getInputStream();){
                    String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return this.parseMineskinResponse(response);
            }
        }
        if (code == 429) {
            throw new RuntimeException("Mineskin \u9650\u6d41 (HTTP 429)");
        }
        String error = "";
        try (InputStream es = conn.getErrorStream();){
            if (es != null) {
                error = new String(es.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new RuntimeException("Mineskin API \u8fd4\u56de " + code + ": " + error);
        } finally {
            conn.disconnect();
        }
    }

    private SkinData parseMineskinResponse(String response) {
        try {
            JsonObject data;
            String string;
            String signature;
            String value;
            JsonObject data2;
            JsonObject texture;
            JsonObject skin;
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (json.has("skin") && (skin = json.getAsJsonObject("skin")).has("texture") && (texture = skin.getAsJsonObject("texture")).has("data") && (data2 = texture.getAsJsonObject("data")).has("value")) {
                value = data2.get("value").getAsString();
                string = signature = data2.has("signature") ? data2.get("signature").getAsString() : null;
                if (value != null && !value.isEmpty()) {
                    return new SkinData(value, signature, "mojang");
                }
            }
            if (json.has("data") && (data = json.getAsJsonObject("data")).has("texture")) {
                texture = data.getAsJsonObject("texture");
                value = texture.get("value").getAsString();
                string = signature = texture.has("signature") ? texture.get("signature").getAsString() : null;
                if (value != null && !value.isEmpty()) {
                    return new SkinData(value, signature, "mojang");
                }
            }
        }
        catch (Exception e) {
            this.logger.warning("\u89e3\u6790 Mineskin \u54cd\u5e94\u5931\u8d25: " + e.getMessage());
        }
        return null;
    }
}
