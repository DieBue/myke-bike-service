/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.common.api.reporting.KafkaMessage.TaskType;
import com.ibm.dx.publishing.common.api.reporting.MessageBusProducerClient;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public abstract class KafkaInvalidationManager implements Handler<RoutingContext> {

    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2016";

    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(KafkaInvalidationManager.class);

    public static final String INVALIDATION_TOPIC_PAGE = "prod-publishing-page-event";

    public static final String INVALIDATION_TOPIC = INVALIDATION_TOPIC_PAGE;
    public static final String INVALIDATION_TASK_TYPE = TaskType.PUBLISH_PAGE.toString();

    /** Message Format
     * {
     "key" : "20abb5c3-1e16-45d5-bb5b-7acdb8c3f6a9",
     "value" : {
     "tenantId" : "20abb5c3-1e16-45d5-bb5b-7acdb8c3f6a9",
     "jobId" : "current-job",
     "type" : "INFO",
     "created" : "2018-04-24T13:26:55.416Z",
     "task" : {
     "type" : "PUBLISH_CONTENT",
     "docs" : {
     "updated" : [ "6ba7991d-1857-4685-a65a-d3054fae8f45:revId:4-1a7d85f238a341aff12f3316c3a49fe0", "6ba7991d-1857-4685-a65a-d3054fae8f45" ]
     },
     "num" : 1
     }
     }
     }
     * @param deliveryId
     */
    public static CompletableFuture<?> sendInvalidationMessage(final RuntimeContext rc, MessageBusProducerClient<?> kafkaBusProducerClient, String deliveryId) {
        LOGGER.rcEntry(rc, deliveryId);
        String key = rc.getTenantId();
        String topic = INVALIDATION_TOPIC;
        JsonObject msg = buildMessage(rc, deliveryId);
        CompletableFuture<?> result = kafkaBusProducerClient.send(rc, topic, key, msg).toCompletableFuture();
        return LOGGER.rcExit(rc, result);
    }

    private static JsonObject buildMessage(RuntimeContext rc, String deliveryId) {
        LOGGER.rcEntry(rc, deliveryId);
        JsonObject task = new JsonObject();
        task.put("type", INVALIDATION_TASK_TYPE);
        task.put("docs", new JsonObject().put("updated", new JsonArray().add(deliveryId)));
        task.put("num", 1);

        JsonObject msg = new JsonObject();
        msg.put("tenantId", rc.getTenantId());
        msg.put("jobId", "current-job");
        msg.put("type", "INFO");
        msg.put("created", Instant.now().toString());
        msg.put("task", task);
        msg.put("metadata", new JsonObject().put("headers", new JsonObject(rc.encodeAsJson())));
        return LOGGER.rcExit(rc, msg);
    }
}
