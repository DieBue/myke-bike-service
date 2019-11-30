package com.ibm.dx.publishing.connectorservice.idc.uss;

import java.time.Instant;

import com.ibm.dx.publishing.common.api.http.MicroServiceHttpClient;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.common.api.reporting.MessageBusProducerClient;
import com.ibm.dx.publishing.connectorservice.controllers.DeliverySearchController;
import com.ibm.dx.publishing.connectorservice.idc.PageOverrides;
import com.ibm.dx.publishing.connectorservice.idc.SiteConfig;
import com.ibm.dx.publishing.connectorservice.idc.TRSConfig;

import io.vertx.core.json.JsonObject;

public class ProcessingContext {
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(ProcessingContext.class);
    protected final RuntimeContext rc;
    
    protected final MicroServiceHttpClient microServiceHttpClient;
    protected final MessageBusProducerClient messageBusProducerClient;
    protected final DeliverySearchController deliverySearchController;
    
    
    /**
     * The IDC backend configuration stored in TenantRegistry
     */
    private TRSConfig trsConfig;

    /**
     * The IDC configuration stored in the site record
     */
    private SiteConfig siteConfig;
    
    // the storeId
    protected String storeIdentifier;

    /** 
     * the SEO URL prefix added to all SEO URLs 
     */
    protected String urlBasePath;
    
    /**
     * The base URL of the site
     */
    protected String siteURL;
    private final String connectorId; 
    
    protected PageOverrides pageOverrides;
    private JsonObject catalogPage;
    private final Instant timeStamp = Instant.now();
    private String siteId;
    private boolean skipped = false;
    
    public static enum PARAMS {id, idcBaseURL, idcPreviewBaseURL, storeId, urlBasePath, siteURL, rootPageId, pageMappings};

    public ProcessingContext (RuntimeContext rc, MicroServiceHttpClient microServiceHttpClient, MessageBusProducerClient messageBusProducerClient, String siteId) {
        LOGGER.rcEntry(rc);
        this.rc = rc;

        this.microServiceHttpClient = microServiceHttpClient;
        this.messageBusProducerClient = messageBusProducerClient;
        this.deliverySearchController = new DeliverySearchController(rc, microServiceHttpClient);
        this.siteId = siteId;
        
        urlBasePath = "";
        siteURL = "/" + rc.getTenantId();
        connectorId = "IdcDefaultConnector";
        
        LOGGER.rcExit(rc, toString());
    }
    

    public void setPageOverrides(PageOverrides pageOverrides) {
        LOGGER.rcEntry(rc, pageOverrides);
        this.pageOverrides = pageOverrides;
        LOGGER.rcExit(rc);
    }

    public void setTRSConfig(TRSConfig trsConfig) {
        LOGGER.rcEntry(rc, trsConfig);
        this.trsConfig = trsConfig;
        LOGGER.rcExit(rc);
    }

    public TRSConfig getTRSConfig() {
        return trsConfig;
    }
    
    public void setSiteConfig(SiteConfig siteConfig) {
        LOGGER.rcEntry(rc, siteConfig);
        this.siteConfig = siteConfig;
        LOGGER.rcExit(rc);
    }

    public SiteConfig getSiteConfig() {
        return siteConfig;
    }

    public RuntimeContext getRc() {
        return rc;
    }

    public MicroServiceHttpClient getMicroServiceHttpClient() {
        return microServiceHttpClient;
    }

    public MessageBusProducerClient getMessageBusProducerClient() {
        return messageBusProducerClient;
    }

    public String getUrlBasePath() {
        return urlBasePath;
    }

    public String getSiteURL() {
        return siteURL;
    }
    
    public PageOverrides getPageOverrides() {
        return pageOverrides;
    }

    public DeliverySearchController getDeliverySearchController() {
        return deliverySearchController;
    }

    public JsonObject getCatalogPage() {
        return catalogPage;
    }

    public void setCatalogPage(JsonObject catalogPage) {
        this.catalogPage = catalogPage;
    }
    
	public void setSiteId(RuntimeContext rc, String siteId) {
        LOGGER.rcEntry(rc, siteId);
		this.siteId = siteId;
		LOGGER.rcExit(rc);
	}
    

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProcessingContext [trsConfig=");
        builder.append(trsConfig);
        builder.append(", siteConfig=");
        builder.append(siteConfig);
        builder.append(", storeIdentifier=");
        builder.append(storeIdentifier);
        builder.append(", urlBasePath=");
        builder.append(urlBasePath);
        builder.append(", siteURL=");
        builder.append(siteURL);
        builder.append(", siteId=");
        builder.append(siteId);
        builder.append(", catalogPage=");
        builder.append(catalogPage);
        builder.append(", skipped=");
        builder.append(skipped);
        builder.append(", timeStamp=");
        builder.append(timeStamp);
        builder.append("]");
        return builder.toString();
    }


    public String getConnectorId() {
        return connectorId;
    }


    public Instant getTimeStamp() {
        return timeStamp;
    }


	public String getSiteId() {
		return siteId;
	}


	public boolean isSkipped() {
		return skipped;
	}


	public void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}
}
