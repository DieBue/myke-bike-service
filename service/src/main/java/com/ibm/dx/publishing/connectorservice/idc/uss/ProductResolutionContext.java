package com.ibm.dx.publishing.connectorservice.idc.uss;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;

public class ProductResolutionContext {
    
	private final SiteProcessingContext ctx;
	private final Category category;
	private int numFound;
	private int pageNumberZeroBased;
	
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(ProductResolutionContext.class);

    public ProductResolutionContext(SiteProcessingContext ctx, Category c, int pageSize) {
    	this.category = c;
    	this.ctx = ctx;
    	pageNumberZeroBased = 0;
    	setNumFound(-1);
    }

	public SiteProcessingContext getCtx() {
		return ctx;
	}

	public Category getCategory() {
		return category;
	}

	public int getNumFound() {
		return numFound;
	}

	public void setNumFound(int numFound) {
		this.numFound = numFound;
	}

	public int getPageNumberZeroBased() {
		return pageNumberZeroBased;
	}

	public void setPageNumberZeroBased(int pageNumber) {
		this.pageNumberZeroBased = pageNumber;
	}
	
	public RuntimeContext getRc() {
		return ctx.getRc();
	}
	
	public boolean hasMoreData(RuntimeContext rc) {
		LOGGER.rcEntry(rc, pageNumberZeroBased, ctx.getUssPageSize(), numFound, category.getName());
		return LOGGER.rcExit(rc, getFirstIndex() < numFound);
	}
	
	public int getFirstIndex() {
		return pageNumberZeroBased * ctx.getUssPageSize();
	}
	
	public int getPageSize() {
		return ctx.getUssPageSize();
	}
	
	public ProductResolutionContext nextPage() {
		pageNumberZeroBased++;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ProductResolutionContext [category=");
		builder.append(category);
		builder.append(", pageNumber=");
		builder.append(pageNumberZeroBased);
		builder.append(", numFound=");
		builder.append(numFound);
		builder.append("]");
		return builder.toString();
	}

	
	
}
