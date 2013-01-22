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

package org.artifactory.jcr.fs;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.FolderInfo;
import org.artifactory.fs.MutableFolderInfo;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.AbstractXmlContentPersistenceHandler;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataPersistenceHandler;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.MetadataInfo;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.schedule.TaskService;
import org.artifactory.storage.StorageConstants;
import org.artifactory.util.PathMatcher;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.jcr.Node;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Yoav Landman
 */
public class JcrFolder extends JcrFsItem<FolderInfo, MutableFolderInfo> implements VfsFolder {
    private static final Logger log = LoggerFactory.getLogger(JcrFolder.class);

    /**
     * Constructor used when reading JCR content and creating JCR file system item from it. Will not create anything in
     * JCR but will read the JCR content of the node.
     *
     * @param node     the JCR node this item represent
     * @param repo     the storing repo for the folder
     * @param original when coming from a mutable folder TODO: Check if really used!
     * @throws RepositoryRuntimeException if the node cannot be read
     */
    public JcrFolder(Node node, JcrFsItemFactory repo, JcrFolder original) {
        super(node, repo, original);
    }

    public JcrFolder(RepoPath repoPath, JcrFsItemFactory repo) {
        super(repoPath, repo);
    }

    public JcrFolder(JcrFolder copy, JcrFsItemFactory repo) {
        super(copy, repo);
    }

    @Override
    protected FolderInfo createInfo(RepoPath repoPath) {
        return InfoFactoryHolder.get().createFolderInfo(repoPath);
    }

    public List<JcrFsItem> getJcrItems() {
        return getJcrRepoService().getChildren(this, false);
    }

    @Override
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public List<VfsItem> getItems(boolean withWriteLock) {
        return (List<VfsItem>) ((List) getJcrRepoService().getChildren(this, withWriteLock));
    }

    @Override
    protected MetadataPersistenceHandler<FolderInfo, MutableFolderInfo> getInfoPersistenceHandler() {
        return getMdService().getFolderInfoMd().getPersistenceHandler();
    }

    /**
     * OVERIDDEN FROM FILE BEGIN
     */

    @Override
    public long length() {
        return 0;
    }

    @Override
    public String[] list() {
        return list(null);
    }

    @Override
    public String[] list(@Nullable FilenameFilter filter) {
        File[] files = listFiles(filter);
        String[] paths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            paths[i] = file.getAbsolutePath();
        }
        return paths;
    }

    @Override
    public File[] listFiles() {
        return listFiles((FilenameFilter) null);
    }

    @Override
    public File[] listFiles(FilenameFilter filter) {
        ArrayList<File> files = new ArrayList<File>();
        List<JcrFsItem> children = getJcrItems();
        for (JcrFsItem child : children) {
            if (filter == null || filter.accept(this, child.getPath())) {
                files.add(child);
            }
        }
        return files.toArray(new File[files.size()]);
    }

    @Override
    public File[] listFiles(FileFilter filter) {
        ArrayList<File> files = new ArrayList<File>();
        List<JcrFsItem> children = getJcrItems();
        for (JcrFsItem child : children) {
            if (filter != null && filter.accept(child)) {
                files.add(child);
            }
        }
        return files.toArray(new File[files.size()]);
    }

    @Override
    public boolean mkdir() {
        String absPath = getAbsolutePath();
        JcrService jcr = StorageContextHelper.get().getJcrService();
        JcrSession session = jcr.getManagedSession();

        boolean created = false;
        if (session.itemExists(absPath)) {
            log.debug("Folder node already exists: {}.", absPath);
        } else {
            checkMutable("mkdir");
            String parentPath = PathUtils.getParent(absPath);
            Node parentNode = (Node) session.getItem(parentPath);
            String dir = getRepoPath().getName();
            if (!PathUtils.hasText(dir)) {
                dir = getRepoKey();
            }
            //Add our node
            JcrHelper.getOrCreateNode(parentNode, dir, StorageConstants.NT_ARTIFACTORY_FOLDER,
                    StorageConstants.MIX_ARTIFACTORY_BASE);
            setModifiedInfoFields(System.currentTimeMillis(), System.currentTimeMillis());
            getInfoPersistenceHandler().update(this, getMutableInfo());
            log.debug("Created folder node: {}.", absPath);
            created = true;
        }
        return created;
    }

    @Override
    public VfsItem save(VfsItem originalFsItem) {
        if (isDeleted()) {
            throw new IllegalStateException("Cannot save item " + getRepoPath() + " it is scheduled for deletion");
        }
        // Save the main info only if new (original null, or main info non identical)
        if (originalFsItem == null || !originalFsItem.getInfo().isIdentical(getInfo())) {
            getInfoPersistenceHandler().update(this, getMutableInfo());
        }
        saveDirtyState();
        return new JcrFolder(getNode(), getRepo(), this);
    }

    @Override
    public boolean isIdentical(VfsItem item) {
        return item instanceof JcrFolder && super.isIdentical(item);
    }

    @Override
    public void unexpire() {
        super.unexpire();
        updateMavenMetadataLastModifiedIfExists(System.currentTimeMillis());
    }

    /**
     * Maven metadata is an expirable resource, so we should change its last modified date whenever
     * expiring(zapping)/un-expiring.
     *
     * @param lastModified The last modified date
     */
    public void updateMavenMetadataLastModifiedIfExists(long lastModified) {
        try {
            if (hasMetadata(MavenNaming.MAVEN_METADATA_NAME)) {
                MetadataDefinition definition =
                        getMdService().getMetadataDefinition(MavenNaming.MAVEN_METADATA_NAME, false);
                MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
                if (mdph instanceof AbstractXmlContentPersistenceHandler) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(lastModified);
                    ((AbstractXmlContentPersistenceHandler) mdph).markModified(this, cal);
                }
            }
        } catch (RepositoryRuntimeException e) {
            log.error("Failed to set last modified on maven metadata: {}", e.getLocalizedMessage());
        }
    }

    @Override
    public boolean mkdirs() {

        //Init repo interceptors
        StorageInterceptors interceptors = StorageContextHelper.get().beanForType(StorageInterceptors.class);

        //Split the path and create each subdir in turn starting from the root
        String path = getRelativePath();
        int from = 1;
        boolean created = false;
        int to;
        do {
            to = path.indexOf('/', from);
            String subPath = to > 0 ? path.substring(0, to) : path;
            if (created || !getRepo().itemExists(subPath)) {
                RepoPath subRepoPath = InternalRepoPathFactory.create(getRepoKey(), subPath);
                JcrFolder subFolder = getRepo().getLockedJcrFolder(subRepoPath, true);
                BasicStatusHolder statusHolder = new BasicStatusHolder();
                statusHolder.setFastFail(true);
                interceptors.beforeCreate(subFolder, statusHolder);
                created = subFolder.mkdir();
                if (!created) {
                    // Not created release write lock early
                    LockingHelper.removeLockEntry(subFolder.getRepoPath());
                } else {
                    //If the folder was created successfully, invoke onCreate
                    interceptors.afterCreate(subFolder, new MultiStatusHolder());
                }
            } else {
                created = false;
            }
            from = to + 1;
        } while (to > 0);
        return created;
    }

    /**
     * OVERRIDDEN FROM FILE END
     */

    @Override
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void exportTo(ExportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        try {
            TaskService taskService = StorageContextHelper.get().getTaskService();
            //Check if we need to break/pause
            boolean stop = taskService.pauseOrBreak();
            if (stop) {
                status.setError("Export was stopped on " + this, log);
                return;
            }
            File targetDir = new File(settings.getBaseDir(), getRelativePath());
            status.setDebug("Exporting directory '" + getAbsolutePath() + "'...", log);
            FileUtils.forceMkdir(targetDir);

            FolderInfo folderInfo = getInfo();
            long modified = folderInfo.getLastModified();
            if (modified <= 0) {
                modified = folderInfo.getCreated();
            }
            targetDir.setLastModified(modified);

            if (settings.isIncludeMetadata()) {
                exportMetadata(targetDir, status, settings.isIncremental());
            }

            if (settings.isM2Compatible()) {
                exportMavenFiles(status, targetDir);
            }

            //The children we get have read locks we do not want to keep
            List<JcrFsItem> list = getJcrItems();

            //Release the folder read lock immediately - no need to hold for self or children
            LockingHelper.releaseReadLock(getRepoPath());
            for (JcrFsItem item : list) {
                LockingHelper.releaseReadLock(item.getRepoPath());
            }

            if (exportChildren(settings, status, taskService, list)) {
                // task should stop
                return;
            }

            if (settings.isIncremental()) {
                cleanupIncrementalBackupDirectory(list, targetDir, settings);
            }
        } catch (Exception e) {
            //If a child export fails, we collect the error but not fail the whole export
            File exportDir = settings.getBaseDir();
            String msg;
            if (exportDir != null) {
                msg = "Failed to export '" + getAbsolutePath() + "' to dir '" + exportDir.getPath() + "'.";
            } else {
                msg = "Failed to export '" + getAbsolutePath() + "' to a null dir";
            }
            status.setError(msg, e, log);
        }
    }

    private boolean exportChildren(ExportSettings settings, MutableStatusHolder status, TaskService taskService,
            List<JcrFsItem> list) {
        boolean shouldStop = false;
        for (JcrFsItem item : list) {
            //Check if we need to break/pause
            shouldStop = taskService.pauseOrBreak();
            if (shouldStop) {
                status.setError("Export was stopped on " + this, log);
                return true;
            }
            String itemName = item.getName();
            if (item.isDirectory()) {
                if (isStorable(itemName)) {
                    JcrFolder jcrFolder = ((JcrFolder) item);
                    jcrFolder.exportTo(settings);
                }
            } else {
                //Do not export checksums
                if (JcrFile.isStorable(itemName)) {
                    JcrFile jcrFile = ((JcrFile) item);
                    getJcrRepoService().exportFile(jcrFile, settings);
                }
            }
        }
        return shouldStop;  // will be false here
    }

    private void exportMavenFiles(MutableStatusHolder status, File targetDir) {
        String mavenMetadataName = MavenNaming.MAVEN_METADATA_NAME;
        try {
            RepositoryService repoService = ContextHelper.get().getRepositoryService();
            String xmlData = repoService.getXmlMetadata(getRepoPath(), mavenMetadataName);
            if (StringUtils.isNotBlank(xmlData)) {
                // get the metadata inside a transaction
                MetadataInfo metadataInfo = repoService.getMetadataInfo(getRepoPath(), mavenMetadataName);
                assert metadataInfo != null;
                long lastModified = metadataInfo.getLastModified();
                File metadataFile = new File(targetDir, mavenMetadataName);
                writeFile(status, metadataFile, xmlData, lastModified);
                // create checksum files for the maven-metadata.xml
                writeChecksums(metadataFile, metadataInfo.getChecksumsInfo(), lastModified);
            }
        } catch (Exception e) {
            status.setError("Failed to export maven info for '" + getAbsolutePath() + "'.", e, log);
        }
    }

    // remove files and folders from the incremental backup dir if they were deleted from the repository

    private void cleanupIncrementalBackupDirectory(List<JcrFsItem> currentJcrFolderItems, File targetDir,
            ExportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        //Metadata File filter
        IOFileFilter metadataFilter = new MetadataFileFilter();

        //List all artifacts
        Collection<File> artifacts = Sets.newHashSet(
                targetDir.listFiles((FileFilter) new NotFileFilter(metadataFilter)));
        cleanArtifacts(currentJcrFolderItems, artifacts, settings);

        //List all sub-target metadata
        Collection<File> subTargetMetadataFiles = FileUtils.listFiles(targetDir, metadataFilter,
                DirectoryFileFilter.INSTANCE);
        cleanMetadata(currentJcrFolderItems, subTargetMetadataFiles, status);

        //List all target metadata
        File targetDirMetadataContainerFolder = getMetadataContainerFolder(targetDir);
        Collection<File> targetMetadataFiles = FileUtils.listFiles(targetDirMetadataContainerFolder, metadataFilter,
                DirectoryFileFilter.INSTANCE);
        cleanTargetMetadata(targetMetadataFiles, status);
    }

    /**
     * Indicates if the given file is located inside a metadata folder
     *
     * @param file File to query
     * @return True if the file is located in a metadata folder. False if not
     */
    private boolean isFileInMetadataFolder(File file) {
        return file.getAbsolutePath().contains(METADATA_FOLDER);
    }

    /**
     * Locates the artifacts that were removed from the repo since last backup, but still remain in the backup folder
     * and clean them out.
     *
     * @param currentJcrFolderItems List of jcr items in the current jcr folder
     * @param artifacts             List of artifact files in the current target folder
     * @param settings
     */
    private void cleanArtifacts(List<JcrFsItem> currentJcrFolderItems, Collection<File> artifacts,
            ExportSettings settings) {
        for (File artifact : artifacts) {
            if (artifact != null) {
                String jcrFileName = artifact.getName();
                JcrFsItem jcrFsItem = getFsItem(currentJcrFolderItems, jcrFileName);
                if (jcrFsItem == null) {
                    if (artifact.isDirectory()) {
                        // If a directory does not exist in data store - we need to recursively handle all of his children as well
                        Collection<File> childArtifacts = Sets.newHashSet(
                                artifact.listFiles((FileFilter) new NotFileFilter(new MetadataFileFilter())));
                        cleanArtifacts(Collections.<JcrFsItem>emptyList(), childArtifacts, settings);
                    }
                    log.debug("Deleting {} from the incremental backup dir since it was " +
                            "deleted from the repository", artifact.getAbsolutePath());
                    boolean deleted = FileUtils.deleteQuietly(artifact);
                    if (!deleted) {
                        log.warn("Failed to delete {}", artifact.getAbsolutePath());
                    }
                    // now delete the metadata folder of the file/folder is it exists
                    File metadataFolder = getMetadataContainerFolder(artifact);
                    if (metadataFolder.exists()) {
                        deleted = FileUtils.deleteQuietly(metadataFolder);
                        if (!deleted) {
                            log.warn("Failed to delete metadata folder {}", metadataFolder.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    /**
     * Locates metadata that was removed from different artifacts since last backup, but still remain in the backup
     * folder and clean them out.
     *
     * @param currentJcrFolderItems List of jcr items in the current jcr folder
     * @param metadataFiles         List of metadata files in the current target's metadata folder
     * @param status                Status holder
     */
    private void cleanMetadata(List<JcrFsItem> currentJcrFolderItems, Collection<File> metadataFiles,
            MutableStatusHolder status) {
        for (File metadataFile : metadataFiles) {
            if ((metadataFile != null) && (metadataFile.isFile())) {
                String metadataFolderPath = metadataFile.getParent();
                //Extract the metadata container name from the parent path
                String metadataContainerName = getMetadataContainerName(metadataFolderPath);
                //Extract the metadata name from the metadata file name
                String metadataName = PathUtils.stripExtension(metadataFile.getName());

                //If metadata and container names returned valid
                if ((metadataName != null) && (metadataContainerName != null)) {
                    JcrFsItem jcrFsItem = getFsItem(currentJcrFolderItems, metadataContainerName);
                    if (jcrFsItem != null) {
                        //If the metadata container does not contain this metadata anymore
                        boolean hasMetadata = false;
                        try {
                            hasMetadata = jcrFsItem.hasMetadata(metadataName);
                        } catch (RepositoryRuntimeException e) {
                            String message = String.format("Unable to determine whether %s is annotated by metadata " +
                                    "of type %s. Metadata was not cleaned.", jcrFsItem.getAbsolutePath(), metadataName);
                            status.setError(message, e, log);
                        }
                        if (!hasMetadata) {
                            boolean deleted = FileUtils.deleteQuietly(metadataFile);
                            if (!deleted) {
                                log.warn("Failed to delete {}", metadataFile.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Locates metadata that was removed from the current target since last backup, but still remain in the backup
     * folder and clean them out.
     *
     * @param targetMetadataFiles List of metadata files in the current target's metadata folder
     * @param status              Status holder
     */
    private void cleanTargetMetadata(Collection<File> targetMetadataFiles, MutableStatusHolder status) {
        for (File metadataFile : targetMetadataFiles) {
            if ((metadataFile != null) && metadataFile.isFile()) {
                //Extract the metadata name from the metadata file name
                String metadataName = PathUtils.stripExtension(metadataFile.getName());
                boolean hasMetadata = false;
                try {
                    hasMetadata = hasMetadata(metadataName);
                } catch (RepositoryRuntimeException e) {
                    // File may be deleted in the meantime, so this is just a warning
                    String message = String.format("Unable to determine whether %s is annotated by metadata of type " +
                            "%s. Metadata entry not present!", getAbsolutePath(), metadataName);
                    status.setWarning(message, e, log);
                }
                //If the metadata container does not contain this metadata anymore
                if (!hasMetadata) {
                    boolean deleted = FileUtils.deleteQuietly(metadataFile);
                    if (!deleted) {
                        log.warn("Failed to delete {}", metadataFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * Extracts the metadata container name from the metadata folder path
     *
     * @param metadataFolderPath Metadata folder path
     * @return Metadata container name extracted from metadata folder path
     */
    private String getMetadataContainerName(String metadataFolderPath) {
        //Get last index of slash
        int indexOfLastSlash = metadataFolderPath.lastIndexOf('/') + 1;
        //Get index of metadata folder suffix
        int indexOfFolderName = metadataFolderPath.indexOf(METADATA_FOLDER);
        if ((indexOfLastSlash == -1) || (indexOfFolderName == -1)) {
            return null;
        }
        return metadataFolderPath.substring(indexOfLastSlash,
                indexOfFolderName);
    }

    private JcrFsItem getFsItem(List<JcrFsItem> currentJcrFolderItems, String jcrFileName) {
        for (JcrFsItem jcrFsItem : currentJcrFolderItems) {
            if (jcrFileName.equals(jcrFsItem.getName())) {
                return jcrFsItem;
            }
        }
        return null;
    }

    /**
     * Shallow folder import, creating all dirs and settings the folder metadata
     *
     * @param settings import parameters
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void importFrom(ImportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        File baseDir = settings.getBaseDir();
        File folder = new File(baseDir, getRelativePath());
        if (PathMatcher.isInGlobalExcludes(folder)) {
            //Nothing to do
            return;
        }
        //Create the folder and import its the metadata
        try {
            //First create the folder in jcr
            mkdir();
            //Read metadata into the node
            if (settings.isIncludeMetadata()) {
                importMetadata(folder, status, settings);
            }
        } catch (Exception e) {
            //Just log an error and continue
            String msg =
                    "Failed to import folder " + folder.getAbsolutePath() + " into '" + getRepoPath() + "'.";
            status.setError(msg, e, log);
        }
    }

    public void importChildren(ImportSettings settings, LinkedList<RepoPath> foldersToScan) {
        MutableStatusHolder status = settings.getStatusHolder();
        File folder = new File(settings.getBaseDir(), getRelativePath());
        try {
            File[] dirEntries = folder.listFiles();

            if (dirEntries == null) {
                status.setWarning("Unable to list the children of the folder '" + folder.getAbsolutePath() +
                        "' for import: perhaps a lack of permissions on the folder.", log);
                return;
            }
            for (File dirEntry : dirEntries) {
                if (PathMatcher.isInGlobalExcludes(dirEntry)) {
                    continue;
                }
                String fileName = dirEntry.getName();
                String repoKey = getRepoKey();
                if (dirEntry.isDirectory()) {
                    if (JcrFolder.isStorable(fileName)) {
                        status.setDebug("Importing folder '" + dirEntry.getAbsolutePath() + "' into '"
                                + repoKey + "'...", log);
                        foldersToScan.add(InternalRepoPathFactory.create(getRepoPath(), fileName));
                    }
                } else if (JcrFile.isStorable(fileName)) {
                    final String msg = "Importing file '" + dirEntry.getAbsolutePath() + "' into '" + repoKey + "'";
                    status.setDebug(msg + "...", log);
                    try {
                        if (!MavenNaming.isMavenMetadataFileName(fileName)) {
                            JcrFile jcrFile = getJcrRepoService().importFile(this, dirEntry, settings);
                            if (jcrFile != null) {
                                // Created successfully, release lock
                                LockingHelper.removeLockEntry(jcrFile.getRepoPath());
                            }
                        } else if (getRepo().isCache()) {
                            //We are out of transaction scope - check metadata via service
                            RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
                            if (!repositoryService.hasMetadata(getRepoPath(), MavenNaming.MAVEN_METADATA_NAME)) {
                                //Special fondling for maven-metadata.xml - store it as real metadata if importing
                                //to local cache repository (non cache repositories recalculate the maven metadata)
                                String xmlData = FileUtils.readFileToString(dirEntry, "utf-8");
                                repositoryService.setXmlMetadata(getRepoPath(), MavenNaming.MAVEN_METADATA_NAME,
                                        xmlData);
                            }
                        }
                    } catch (RepoRejectException rre) {
                        status.setError("Artifact rejected: " + rre.getMessage(), log);
                    } catch (Exception e) {
                        //Just log an error and continue
                        status.setWarning("Error importing file: " + msg, e, log);
                    }
                }
            }
        } catch (Exception e) {
            String msg = "Failed to import folder children from '" + folder.getAbsolutePath() + "'.";
            status.setError(msg, e, log);
        }
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Override
    public boolean delete() {
        setDeleted(true);
        deleteChildren();
        return super.delete();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public boolean deleteChildren() {
        List<JcrFsItem> children = getJcrRepoService().getChildren(this, true);
        for (JcrFsItem child : children) {
            try {
                child.delete();
            } catch (Exception e) {
                log.error("Could not directly delete child node '{}'.", child.getName(), e);
            }
        }
        return true;
    }

    public static boolean isStorable(String name) {
        return !name.endsWith(METADATA_FOLDER) && !name.startsWith(".svn") &&
                !MavenNaming.NEXUS_INDEX_DIR.equals(name);
    }

    private class MetadataFileFilter extends AbstractFileFilter {
        @Override
        public boolean accept(File file) {
            //Accept only files within the metadata folder which are not part of the file info ystem
            boolean isArtifactoryFile = file.getName().contains(FileInfo.ROOT);
            boolean isArtifactoryFolder = file.getName().contains(FolderInfo.ROOT);
            return isFileInMetadataFolder(file) && !isArtifactoryFile && !isArtifactoryFolder;
        }
    }

    ;
}