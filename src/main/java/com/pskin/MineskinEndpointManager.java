/*
 * Decompiled with CFR 0.152.
 */
package com.pskin;

import com.pskin.PSkinConfig;
import java.util.ArrayList;
import java.util.List;

public class MineskinEndpointManager {
    private final PSkinConfig config;
    private volatile String lastWorkingUrl;

    public MineskinEndpointManager(PSkinConfig config) {
        this.config = config;
    }

    public synchronized List<String> tryOrder() {
        List<String> endpoints = this.config.getMineskinEndpoints();
        String last = this.lastWorkingUrl;
        if (last != null && endpoints.contains(last)) {
            ArrayList<String> ordered = new ArrayList<String>(endpoints.size());
            ordered.add(last);
            for (String url : endpoints) {
                if (url.equals(last)) continue;
                ordered.add(url);
            }
            return ordered;
        }
        return endpoints;
    }

    public void markSuccess(String url) {
        this.lastWorkingUrl = url;
    }

    public void markFailure(String url) {
        if (url != null && url.equals(this.lastWorkingUrl)) {
            this.lastWorkingUrl = null;
        }
    }

    public String getCurrent() {
        return this.lastWorkingUrl != null ? this.lastWorkingUrl : this.config.getMineskinApiUrl();
    }

    public int getCount() {
        return this.config.getMineskinEndpoints().size();
    }
}
