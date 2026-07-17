/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import step.attachments.FileResolver;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.resources.AttachmentStorage;
import step.resources.InMemoryResourceAccessor;
import step.resources.InMemoryResourceRevisionAccessor;
import step.resources.LayeredResourceManager;
import step.resources.LocalAttachmentStorage;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class AbstractStepContext extends AbstractContext {

    private final UUID contextId = UUID.randomUUID();
    private ExpressionHandler expressionHandler;
    private DynamicBeanResolver dynamicBeanResolver;
    private ResourceManager resourceManager;
    private FileResolver fileResolver;
    private LoadingCache<String, File> fileResolverCache;
    // Keep track of the default resource manager created at initialization of the context
    private LocalResourceManagerImpl localResourceManager;
    // Similar to the resource manager, attachmentStorage can be shared by multiple context, but only one of them owns it.
    private AttachmentStorage attachmentStorage;
    private AttachmentStorage ownedAttachmentStorage;

    protected void setDefaultAttributes() {
        expressionHandler = new ExpressionHandler();
        dynamicBeanResolver = new DynamicBeanResolver(new DynamicValueResolver(expressionHandler));
        // Create a local resource manager in a dedicated folder per default
        localResourceManager = new LocalResourceManagerImpl(getContextFolderAsFile("resources"), new InMemoryResourceAccessor(), new InMemoryResourceRevisionAccessor());
        setResourceManager(localResourceManager);
        ownedAttachmentStorage = new LocalAttachmentStorage(getContextFolderAsFile("attachments"));
        setAttachmentStorage(ownedAttachmentStorage);
    }

    private File getContextFolderAsFile(String suffix) {
        String tmpPath = System.getProperty("java.io.tmpdir");
        tmpPath = (tmpPath.endsWith(File.separator)) ? tmpPath : tmpPath + File.separator;
        String dirName = tmpPath + "stepContext_" + getClass().getSimpleName() + "_" + contextId + "_" + suffix;
        return new File(dirName);
    }

    protected void useAllAttributesFromParentContext(AbstractStepContext parentContext) {
        ResourceManager resourceManager = new LayeredResourceManager(parentContext.getResourceManager(), true);
        setResourceManager(resourceManager);
        setAttachmentStorage(parentContext.getAttachmentStorage());
        expressionHandler = parentContext.getExpressionHandler();
        dynamicBeanResolver = parentContext.getDynamicBeanResolver();
    }

    public ExpressionHandler getExpressionHandler() {
        return expressionHandler;
    }

    public void setExpressionHandler(ExpressionHandler expressionHandler) {
        this.expressionHandler = expressionHandler;
    }

    public DynamicBeanResolver getDynamicBeanResolver() {
        return dynamicBeanResolver;
    }

    public void setDynamicBeanResolver(DynamicBeanResolver dynamicBeanResolver) {
        this.dynamicBeanResolver = dynamicBeanResolver;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        updateFileResolver();
    }

    public AttachmentStorage getAttachmentStorage() {
        return attachmentStorage;
    }

    public void setAttachmentStorage(AttachmentStorage attachmentStorage) {
        if (attachmentStorage == this.attachmentStorage) {
            System.err.println("FIXME: redundant setting of attachment storage in " + this.getClass().getSimpleName());
            return;
        }
        if (this.attachmentStorage != null) {
            throw new IllegalStateException("BUG: attachmentStorage must not be set more than once");
        }
        this.attachmentStorage = attachmentStorage;
    }

    public FileResolver getFileResolver() {
        return fileResolver;
    }

    private void updateFileResolver() {
        this.fileResolver = new FileResolver(resourceManager);
        this.fileResolverCache = CacheBuilder.newBuilder().concurrencyLevel(4)
            .maximumSize(1000)
            .expireAfterWrite(500, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<>() {
                public File load(String filepath) {
                    return fileResolver.resolve(filepath);
                }
            });
    }

    public LoadingCache<String, File> getFileResolverCache() {
        return fileResolverCache;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            // Cleanup the default resource manager, and attachment storage
            if (localResourceManager != null) {
                localResourceManager.cleanup();
            }
            if (ownedAttachmentStorage != null) {
                ownedAttachmentStorage.cleanupIfNeeded();
            }
        }
    }
}
