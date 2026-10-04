/*
 * Decompiled with CFR 0.152.
 */
package com.pskin;

public class SkinData {
    private final String value;
    private final String signature;
    private final String source;
    private final String originalSource;
    private final long timestamp;

    public SkinData(String value, String signature, String source) {
        this(value, signature, source, null);
    }

    public SkinData(String value, String signature, String source, String originalSource) {
        this.value = value;
        this.signature = signature;
        this.source = source;
        this.originalSource = originalSource;
        this.timestamp = System.currentTimeMillis();
    }

    public String getValue() {
        return this.value;
    }

    public String getSignature() {
        return this.signature;
    }

    public String getSource() {
        return this.source;
    }

    public String getOriginalSource() {
        return this.originalSource;
    }

    public String getDisplaySource() {
        return this.originalSource != null && !this.originalSource.isEmpty() ? this.originalSource : this.source;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public boolean hasSignature() {
        return this.signature != null && !this.signature.isEmpty();
    }
}
