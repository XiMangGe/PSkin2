/*
 * Decompiled with CFR 0.152.
 */
package com.pskin;

import com.pskin.SkinData;

public class SkinFetchResult {
    private final SkinData skinData;
    private final Status status;

    private SkinFetchResult(SkinData skinData, Status status) {
        this.skinData = skinData;
        this.status = status;
    }

    public static SkinFetchResult success(SkinData data) {
        return new SkinFetchResult(data, Status.SUCCESS);
    }

    public static SkinFetchResult notFound() {
        return new SkinFetchResult(null, Status.NOT_FOUND);
    }

    public static SkinFetchResult networkError() {
        return new SkinFetchResult(null, Status.NETWORK_ERROR);
    }

    public static SkinFetchResult timeout() {
        return new SkinFetchResult(null, Status.TIMEOUT);
    }

    public static SkinFetchResult rateLimited() {
        return new SkinFetchResult(null, Status.RATE_LIMITED);
    }

    public static SkinFetchResult apiError() {
        return new SkinFetchResult(null, Status.API_ERROR);
    }

    public static SkinFetchResult invalidResponse() {
        return new SkinFetchResult(null, Status.INVALID_RESPONSE);
    }

    public static SkinFetchResult urlInvalidImage() {
        return new SkinFetchResult(null, Status.URL_INVALID_IMAGE);
    }

    public static SkinFetchResult urlImageTooLarge() {
        return new SkinFetchResult(null, Status.URL_IMAGE_TOO_LARGE);
    }

    public static SkinFetchResult urlRateLimited() {
        return new SkinFetchResult(null, Status.URL_RATE_LIMITED);
    }

    public static SkinFetchResult unknownError() {
        return new SkinFetchResult(null, Status.UNKNOWN_ERROR);
    }

    public boolean isSuccess() {
        return this.status == Status.SUCCESS && this.skinData != null;
    }

    public SkinData getSkinData() {
        return this.skinData;
    }

    public Status getStatus() {
        return this.status;
    }

    public String getSource() {
        return this.skinData != null ? this.skinData.getDisplaySource() : null;
    }

    public static enum Status {
        SUCCESS,
        NOT_FOUND,
        NETWORK_ERROR,
        TIMEOUT,
        RATE_LIMITED,
        API_ERROR,
        INVALID_RESPONSE,
        URL_INVALID_IMAGE,
        URL_IMAGE_TOO_LARGE,
        URL_RATE_LIMITED,
        UNKNOWN_ERROR;

    }
}
