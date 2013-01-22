/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.jcr.md;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumStorageHelper;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.Checksums;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.MutableMetadataInfo;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.storage.StorageConstants;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import static org.artifactory.storage.StorageConstants.NODE_ARTIFACTORY_METADATA;
import static org.artifactory.storage.StorageConstants.NT_UNSTRUCTURED;

/**
 * @author freds
 */
public abstract class AbstractMetadataPersistenceHandler<T, MT> extends AbstractPersistenceHandler<T, MT> {
    private static final Logger log = LoggerFactory.getLogger(AbstractMetadataPersistenceHandler.class);

    protected AbstractMetadataPersistenceHandler(XmlMetadataProvider<T, MT> xmlProvider) {
        super(xmlProvider);
    }

    protected abstract String getXml(MetadataAware metadataAware);

    protected Node getOrCreateMetadataContainer(MetadataAware metadataAware) {
        return JcrHelper.getOrCreateNode(metadataAware.getNode(), NODE_ARTIFACTORY_METADATA, NT_UNSTRUCTURED);
    }

    protected Node getMetadataNode(MetadataAware metadataAware, boolean createIfEmpty) {
        String metadataName = getMetadataName();
        try {
            Node metadataContainer = JcrHelper.safeGetNode(metadataAware.getNode(), NODE_ARTIFACTORY_METADATA);
            if (metadataContainer == null) {
                if (createIfEmpty) {
                    //Get or create metadata container
                    metadataContainer = getOrCreateMetadataContainer(metadataAware);
                } else {
                    return null;
                }
            }
            Node metadataNode = null;
            if (!metadataContainer.hasNode(metadataName)) {
                if (createIfEmpty) {
                    //Add the metadata node and mark the create time
                    metadataNode = createMetadataNode(metadataContainer);
                    Calendar created = Calendar.getInstance();
                    metadataNode.setProperty(StorageConstants.PROP_ARTIFACTORY_CREATED, created);
                }
            } else {
                metadataNode = metadataContainer.getNode(metadataName);
            }
            return metadataNode;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to access metadata node '" + metadataName + "' of " + metadataAware, e);
        }
    }

    protected Node createMetadataNode(Node metadataContainer) throws RepositoryException {
        String metadataName = getMetadataName();
        Node metadataNode = JcrHelper.getOrCreateNode(metadataContainer, metadataName,
                StorageConstants.NT_ARTIFACTORY_METADATA,
                StorageConstants.MIX_ARTIFACTORY_BASE);
        metadataNode.setProperty(StorageConstants.PROP_ARTIFACTORY_METADATA_NAME, metadataName);
        return metadataNode;
    }

    @Override
    public boolean hasMetadata(MetadataAware metadataAware) {
        return getMetadataNode(metadataAware, false) != null;
    }

    public Set<ChecksumInfo> getChecksums(MetadataAware metadataAware)
            throws RepositoryException {
        String metadataName = getMetadataName();
        Set<ChecksumInfo> checksumInfos = new HashSet<ChecksumInfo>();
        Node metadataNode = getMetadataNode(metadataAware, true);
        ChecksumType[] checksumTypes = ChecksumType.values();
        for (ChecksumType checksumType : checksumTypes) {
            String propName = ChecksumStorageHelper.getActualPropName(checksumType);
            if (metadataNode.hasProperty(propName)) {
                Property property = metadataNode.getProperty(propName);
                String checksumValue = property.getString();
                checksumInfos.add(new ChecksumInfo(checksumType, checksumValue, checksumValue));
                log.debug("Found metadata checksum '{}' for '{}#{}'",
                        new Object[]{checksumType, metadataAware, metadataName});
            }
        }
        return checksumInfos;
    }

    protected Set<ChecksumInfo> createChecksums(MetadataAware metadataAware, byte[] xmlDataBytes)
            throws RepositoryException {
        String metadataName = getMetadataName();
        Set<ChecksumInfo> checksumInfos = new HashSet<ChecksumInfo>();
        Checksum[] checksums;
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(xmlDataBytes);
            checksums = Checksums.calculateAll(is);
        } catch (Exception e) {
            throw new RepositoryRuntimeException(String.format(
                    "Failed to compute metadata checksums for '%s$#%s$'", metadataAware, metadataName), e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        JcrService service = StorageContextHelper.get().beanForType(JcrService.class);
        service.saveChecksums((JcrFsItem) metadataAware, metadataName, checksums);
        for (Checksum checksum : checksums) {
            log.debug("Creating non-exitent metadata checksum '{}' for '{}#{}'",
                    new Object[]{checksum.getType(), metadataAware, metadataName});
            checksumInfos.add(new ChecksumInfo(checksum.getType(), checksum.getChecksum(), checksum.getChecksum()));
        }

        return checksumInfos;
    }
}