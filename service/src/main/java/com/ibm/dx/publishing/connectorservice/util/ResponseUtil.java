package com.ibm.dx.publishing.connectorservice.util;

import com.ibm.dx.publishing.connectorservice.idc.exceptions.ObjectNotFoundException;
import io.vertx.core.http.HttpClientResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ResponseUtil {

    private static final CompletableFuture<Void> VOID_FUTURE = CompletableFuture.completedFuture(null);

    private ResponseUtil() {
    }

    public static CompletableFuture<Void> validateVoidResponse(HttpClientResponse res, boolean tolerate404) {
        int status = res.statusCode();
        if (status == 404) {
            if (tolerate404) {
                return VOID_FUTURE;
            } else {
                CompletableFuture<Void> result = new CompletableFuture<>();
                result.completeExceptionally(new ObjectNotFoundException());
                return result;
            }
        } else {
            if (status >= 400) {
                CompletableFuture<Void> result = new CompletableFuture<>();
                result.completeExceptionally(new IOException(res.statusMessage()));
                return result;
            } else {
                return VOID_FUTURE;
            }
        }
    }
}
