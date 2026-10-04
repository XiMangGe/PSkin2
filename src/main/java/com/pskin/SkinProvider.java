/*
 * Decompiled with CFR 0.152.
 */
package com.pskin;

import com.pskin.SkinFetchResult;
import java.util.concurrent.CompletableFuture;

public interface SkinProvider {
    public String getName();

    public CompletableFuture<SkinFetchResult> fetchSkin(String var1);
}
