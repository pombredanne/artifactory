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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoBuilder;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.model.LocalRepoActionableItem;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels.ActionsPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels.ChecksumsPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels.DependencyDeclarationPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels.DistributionManagementPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels.GeneralInfoPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels.VirtualRepoListPanel;

/**
 * @author Yoav Landman
 */
public class GeneralTabPanel extends Panel {

    @SpringBean
    private RepositoryService repositoryService;

    private RepoAwareActionableItem repoItem;

    public GeneralTabPanel(String id, RepoAwareActionableItem repoItem) {
        super(id);
        setOutputMarkupId(true);
        this.repoItem = repoItem;

        add(new GeneralInfoPanel("generalInfoPanel").init(repoItem));

        if (shouldDisplayDistributionManagement()) {
            add(new DistributionManagementPanel("distributionManagement", repoItem));
        } else {
            add(new WebMarkupContainer("distributionManagement"));
        }

        add(checksumInfo());

        add(new ActionsPanel("actionsPanel", repoItem));

        WebMarkupContainer markupContainer = new WebMarkupContainer("dependencyDeclarationPanel");
        add(markupContainer);

        org.artifactory.fs.ItemInfo itemInfo = repoItem.getItemInfo();
        if (!itemInfo.isFolder()) {
            ModuleInfo moduleInfo = null;
            if (repoItem.getRepo().isMavenRepoLayout()) {
                MavenArtifactInfo mavenArtifactInfo = MavenArtifactInfo.fromRepoPath(itemInfo.getRepoPath());
                if (mavenArtifactInfo.isValid()) {
                    moduleInfo = new ModuleInfoBuilder()
                            .organization(mavenArtifactInfo.getGroupId())
                            .module(mavenArtifactInfo.getArtifactId())
                            .baseRevision(mavenArtifactInfo.getVersion())
                            .classifier(mavenArtifactInfo.getClassifier())
                            .ext(mavenArtifactInfo.getType())
                            .build();
                }
            }


            if (moduleInfo == null) {
                moduleInfo = repositoryService.getItemModuleInfo(itemInfo.getRepoPath());
            }

            if (moduleInfo.isValid()) {
                this.replace(new DependencyDeclarationPanel(markupContainer.getId(), moduleInfo,
                        repoItem.getRepo().getRepoLayout()));
            }
        }

        add(new VirtualRepoListPanel("virtualRepoList", repoItem));
        addMessage();
    }

    private void addMessage() {
        RepeatingView messages = new RepeatingView("message");
        add(messages);

        // don't check folders
        if (repoItem.getItemInfo().isFolder()) {
            return;
        }

        RepoPath repoPath = repoItem.getRepoPath();

        // display warning for unaccepted file
        boolean accepted = repositoryService.isRepoPathAccepted(repoPath);
        if (!accepted) {
            Label message = new Label(messages.newChildId(), getString("repo.unaccepted", null));
            message.add(new CssClass("warn"));
            messages.add(message);
        }

        // display warning for unhandled file
        boolean handled = repositoryService.isRepoPathHandled(repoPath);
        if (!handled) {
            Label message = new Label(messages.newChildId(), getString("repo.unhandled", null));
            message.add(new CssClass("warn"));
            messages.add(message);
        }
    }

    private boolean shouldDisplayDistributionManagement() {
        if (repoItem instanceof LocalRepoActionableItem) {
            // display distribution mgmt only if the repo handles releases or snapshots
            LocalRepoDescriptor localRepo = repoItem.getRepo();
            return (localRepo.isHandleReleases() || localRepo.isHandleSnapshots() || !localRepo.isCache());
        }
        return false;
    }

    private Component checksumInfo() {
        if (repoItem.getItemInfo().isFolder()) {
            return new WebMarkupContainer("checksums");
        } else {
            return new ChecksumsPanel("checksums", (org.artifactory.fs.FileInfo) repoItem.getItemInfo());
        }
    }
}
