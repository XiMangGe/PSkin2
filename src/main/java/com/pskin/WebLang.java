/*
 * Decompiled with CFR 0.152.
 */
package com.pskin;

import java.util.HashMap;
import java.util.Map;

public class WebLang {
    private final Map<String, String> texts = new HashMap<String, String>();

    public WebLang(String language) {
        String lang;
        String string = lang = language == null ? "zh_CN" : language.replace('-', '_').trim();
        if (lang.equalsIgnoreCase("en_US") || lang.equalsIgnoreCase("en") || lang.equalsIgnoreCase("english")) {
            this.loadEnglish();
        } else {
            this.loadChinese();
        }
    }

    public String get(String key) {
        return this.texts.getOrDefault(key, key);
    }

    public String get(String key, Object ... args) {
        String msg = this.texts.getOrDefault(key, key);
        try {
            return String.format(msg, args);
        }
        catch (Exception e) {
            return msg;
        }
    }

    private void loadChinese() {
        this.texts.put("upload-title", "\u4e0a\u4f20\u76ae\u80a4");
        this.texts.put("upload-subtitle", "\u4e0a\u4f20\u4f60\u7684\u4e13\u5c5e\u76ae\u80a4\uff0c\u5168\u670d\u53ef\u89c1");
        this.texts.put("form-name", "\u6e38\u620f\u540d");
        this.texts.put("form-name-placeholder", "\u8f93\u5165\u4f60\u7684\u6e38\u620f\u5185\u540d\u5b57");
        this.texts.put("form-model", "\u76ae\u80a4\u6a21\u578b");
        this.texts.put("model-classic", "\u7ecf\u5178\uff08Steve \u5bbd\u624b\u81c2\uff09");
        this.texts.put("model-slim", "\u7ea4\u7ec6\uff08Alex \u7ec6\u624b\u81c2\uff09");
        this.texts.put("form-file", "\u76ae\u80a4\u56fe\u7247");
        this.texts.put("form-file-hint", "PNG \u683c\u5f0f\uff0c\u652f\u6301 64x64 / 64x32 / 128x128 \u7b49\u6807\u51c6\u76ae\u80a4\u5c3a\u5bf8\uff0c\u6700\u5927 %d KB");
        this.texts.put("btn-upload", "\u4e0a\u4f20\u76ae\u80a4");
        this.texts.put("btn-uploading", "\u6b63\u5728\u4e0a\u4f20\uff0c\u8bf7\u7a0d\u5019...");
        this.texts.put("upload-success", "\u76ae\u80a4\u4e0a\u4f20\u6210\u529f\uff01\u5df2\u5728\u6e38\u620f\u4e2d\u751f\u6548");
        this.texts.put("upload-success-note", "\u5982\u679c\u6e38\u620f\u91cc\u6ca1\u7acb\u523b\u53d8\u5316\uff0c\u91cd\u65b0\u8fdb\u670d\u5373\u53ef");
        this.texts.put("gallery-link", "\u67e5\u770b\u5168\u670d\u76ae\u80a4");
        this.texts.put("upload-link", "\u4e0a\u4f20\u76ae\u80a4");
        this.texts.put("gallery-title", "\u5168\u670d\u76ae\u80a4");
        this.texts.put("gallery-subtitle", "\u5171 %d \u4f4d\u73a9\u5bb6");
        this.texts.put("gallery-empty", "\u8fd8\u6ca1\u6709\u73a9\u5bb6\u4e0a\u4f20\u76ae\u80a4");
        this.texts.put("badge-web", "\u7f51\u7ad9\u4e0a\u4f20");
        this.texts.put("badge-manual", "\u624b\u52a8\u8bbe\u7f6e");
        this.texts.put("badge-mojang", "\u6b63\u7248");
        this.texts.put("badge-littleskin", "LittleSkin");
        this.texts.put("badge-cache", "\u7f13\u5b58");
        this.texts.put("badge-bedrock", "\u57fa\u5ca9\u7248");
        this.texts.put("model-label", "\u6a21\u578b");
        this.texts.put("err-name", "\u8bf7\u8f93\u5165\u6e38\u620f\u540d\uff081-16 \u4e2a\u5b57\u7b26\uff0c\u5b57\u6bcd/\u6570\u5b57/\u4e0b\u5212\u7ebf\uff09");
        this.texts.put("err-file", "\u8bf7\u9009\u62e9 PNG \u76ae\u80a4\u56fe\u7247\u6587\u4ef6");
        this.texts.put("err-not-png", "\u8fd9\u4e0d\u662f\u6709\u6548\u7684 PNG \u56fe\u7247\u6587\u4ef6");
        this.texts.put("err-too-large", "\u56fe\u7247\u8fc7\u5927\uff01\u8bf7\u4f7f\u7528\u5c0f\u4e8e %d KB \u7684\u56fe\u7247");
        this.texts.put("err-size", "\u76ae\u80a4\u5c3a\u5bf8\u4e0d\u6b63\u786e\uff01\u8bf7\u4f7f\u7528 64x64\u300164x32 \u6216 128x128 \u7b49\u6807\u51c6\u76ae\u80a4\u5c3a\u5bf8");
        this.texts.put("err-rate", "\u76ae\u80a4\u751f\u6210\u670d\u52a1\u7e41\u5fd9\uff08\u5df2\u81ea\u52a8\u91cd\u8bd5\u4ecd\u53d7\u9650\uff09\uff0c\u8bf7 1 \u5206\u949f\u540e\u518d\u8bd5");
        this.texts.put("err-server", "\u670d\u52a1\u5668\u5185\u90e8\u9519\u8bef\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458");
        this.texts.put("err-network", "\u65e0\u6cd5\u8fde\u63a5\u76ae\u80a4\u751f\u6210\u670d\u52a1\uff08\u5df2\u81ea\u52a8\u91cd\u8bd5\u591a\u6b21\uff09\u3002\u8bf7\u7a0d\u540e\u518d\u8bd5\uff1b\u82e5\u6301\u7eed\u5931\u8d25\uff0c\u8bf7\u7ba1\u7406\u5458\u68c0\u67e5 config.yml \u4e2d mineskin.api-url \u7684\u8fde\u901a\u6027");
        this.texts.put("footer-note", "\u76ae\u80a4\u7531\u670d\u52a1\u5668\u7edf\u4e00\u5e94\u7528\uff0c\u6240\u6709\u73a9\u5bb6\u53ef\u89c1");
        this.texts.put("tab-skin", "\u4e0a\u4f20\u76ae\u80a4");
        this.texts.put("drop-hint", "\u70b9\u51fb\u9009\u62e9\u56fe\u7247\uff0c\u6216\u628a\u56fe\u7247\u62d6\u5230\u8fd9\u91cc");
        this.texts.put("search-placeholder", "\u641c\u7d22\u73a9\u5bb6\u540d...");
        this.texts.put("admin-link", "\u7ba1\u7406");
        this.texts.put("admin-title", "\u76ae\u80a4\u7ba1\u7406");
        this.texts.put("admin-subtitle", "\u67e5\u770b\u548c\u7ba1\u7406\u73a9\u5bb6\u4e0a\u4f20\u7684\u76ae\u80a4\u4e0e\u62ab\u98ce");
        this.texts.put("admin-password-placeholder", "\u8f93\u5165\u7ba1\u7406\u5bc6\u7801");
        this.texts.put("admin-login", "\u767b\u5f55");
        this.texts.put("admin-wrong-key", "\u5bc6\u7801\u9519\u8bef\uff01");
        this.texts.put("admin-disabled", "\u7ba1\u7406\u529f\u80fd\u672a\u542f\u7528");
        this.texts.put("admin-disabled-hint", "\u5728 config.yml \u4e2d\u8bbe\u7f6e web.admin-password \u540e\u91cd\u542f\u670d\u52a1\u5668\u5373\u53ef\u542f\u7528\u7ba1\u7406\u9875\u9762");
        this.texts.put("admin-logout", "\u9000\u51fa\u767b\u5f55");
        this.texts.put("admin-stats-skins", "\u7f51\u7ad9\u76ae\u80a4");
        this.texts.put("admin-skins-section", "\u76ae\u80a4\u5217\u8868");
        this.texts.put("admin-col-preview", "\u9884\u89c8");
        this.texts.put("admin-col-name", "\u73a9\u5bb6");
        this.texts.put("admin-col-model", "\u6a21\u578b");
        this.texts.put("admin-col-date", "\u4e0a\u4f20\u65f6\u95f4");
        this.texts.put("admin-col-actions", "\u64cd\u4f5c");
        this.texts.put("admin-empty", "\u6682\u65e0\u8bb0\u5f55");
        this.texts.put("admin-delete", "\u5220\u9664");
        this.texts.put("admin-confirm-delete", "\u786e\u5b9a\u8981\u5220\u9664 %s \u7684\u4e0a\u4f20\u8bb0\u5f55\u5417\uff1f\uff08\u5728\u7ebf\u73a9\u5bb6\u4f1a\u7acb\u5373\u751f\u6548\uff09");
        this.texts.put("admin-del-fail", "\u5220\u9664\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5");
        this.texts.put("admin-yes", "\u6709");
        this.texts.put("admin-no", "\u65e0");
    }

    private void loadEnglish() {
        this.texts.put("upload-title", "Upload Skin");
        this.texts.put("upload-subtitle", "Upload your custom skin, visible to everyone");
        this.texts.put("form-name", "Game Name");
        this.texts.put("form-name-placeholder", "Enter your in-game name");
        this.texts.put("form-model", "Skin Model");
        this.texts.put("model-classic", "Classic (Steve, wide arms)");
        this.texts.put("model-slim", "Slim (Alex, thin arms)");
        this.texts.put("form-file", "Skin Image");
        this.texts.put("form-file-hint", "PNG format, 64x64 / 64x32 / 128x128 skins supported, max %d KB");
        this.texts.put("btn-upload", "Upload Skin");
        this.texts.put("btn-uploading", "Uploading, please wait...");
        this.texts.put("upload-success", "Skin uploaded! Applied in game");
        this.texts.put("upload-success-note", "If not changed immediately, rejoin the server");
        this.texts.put("gallery-link", "View All Skins");
        this.texts.put("upload-link", "Upload Skin");
        this.texts.put("gallery-title", "Server Skins");
        this.texts.put("gallery-subtitle", "%d players");
        this.texts.put("gallery-empty", "No skins uploaded yet");
        this.texts.put("badge-web", "Website");
        this.texts.put("badge-manual", "Manual");
        this.texts.put("badge-mojang", "Premium");
        this.texts.put("badge-littleskin", "LittleSkin");
        this.texts.put("badge-cache", "Cached");
        this.texts.put("badge-bedrock", "Bedrock");
        this.texts.put("model-label", "Model");
        this.texts.put("err-name", "Enter a valid game name (1-16 chars, letters/digits/underscore)");
        this.texts.put("err-file", "Please select a PNG skin file");
        this.texts.put("err-not-png", "Not a valid PNG file");
        this.texts.put("err-too-large", "Image too large! Use one smaller than %d KB");
        this.texts.put("err-size", "Invalid skin dimensions! Use 64x64, 64x32 or 128x128");
        this.texts.put("err-rate", "Skin service is busy (auto-retried, still limited). Try again in 1 minute");
        this.texts.put("err-server", "Internal server error, contact admin");
        this.texts.put("err-network", "Cannot reach skin service (auto-retried). Try later; if it keeps failing, ask admin to check mineskin.api-url connectivity");
        this.texts.put("footer-note", "Skins are applied server-side, visible to all players");
        this.texts.put("tab-skin", "Upload Skin");
        this.texts.put("drop-hint", "Click to choose an image, or drag & drop it here");
        this.texts.put("search-placeholder", "Search player...");
        this.texts.put("admin-link", "Admin");
        this.texts.put("admin-title", "Skin Management");
        this.texts.put("admin-subtitle", "View and manage uploaded skins");
        this.texts.put("admin-password-placeholder", "Enter admin password");
        this.texts.put("admin-login", "Login");
        this.texts.put("admin-wrong-key", "Wrong password!");
        this.texts.put("admin-disabled", "Admin panel disabled");
        this.texts.put("admin-disabled-hint", "Set web.admin-password in config.yml and restart to enable the admin page");
        this.texts.put("admin-logout", "Logout");
        this.texts.put("admin-stats-skins", "Web Skins");
        this.texts.put("admin-skins-section", "Skins");
        this.texts.put("admin-col-preview", "Preview");
        this.texts.put("admin-col-name", "Player");
        this.texts.put("admin-col-model", "Model");
        this.texts.put("admin-col-date", "Uploaded");
        this.texts.put("admin-col-actions", "Actions");
        this.texts.put("admin-empty", "No records");
        this.texts.put("admin-delete", "Delete");
        this.texts.put("admin-confirm-delete", "Delete %s's upload? (takes effect immediately if online)");
        this.texts.put("admin-del-fail", "Delete failed, try again");
        this.texts.put("admin-yes", "Yes");
        this.texts.put("admin-no", "No");
    }
}
