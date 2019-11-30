package com.ibm.dx.publishing.connectorservice.idc;

import io.vertx.core.json.JsonObject;

public class TemplatePageAssignment {
    public enum Mode {itemOverride, categoryOverride, inherited, inheritedFromDefault};
    
    private final JsonObject templatePage;
    private final Mode mode;
    
    public TemplatePageAssignment (JsonObject templatePage, Mode mode) {
        this.templatePage = templatePage;
        this.mode = mode;
    }

    public JsonObject getTemplatePage() {
        return templatePage;
    }

    public Mode getMode() {
        return mode;
    }
    
    public boolean isItemOverride() {
        return Mode.itemOverride == mode;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TemplatePageAssignement [templatePage=");
        builder.append(templatePage);
        builder.append(", mode=");
        builder.append(mode);
        builder.append("]");
        return builder.toString();
    }

    
    
}
