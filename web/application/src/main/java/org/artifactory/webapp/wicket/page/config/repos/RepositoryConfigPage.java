/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.ajax.CancelDefaultDecorator;
import org.artifactory.common.wicket.ajax.ConfirmationAjaxCallDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.links.ModalShowLink;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.panel.sortedlist.OrderedListPanel;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.SchemaHelpModel;
import org.artifactory.webapp.wicket.page.config.repos.remote.RemoteRepoImportPanel;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.artifactory.common.wicket.component.CreateUpdateAction.CREATE;
import static org.artifactory.common.wicket.component.CreateUpdateAction.UPDATE;

/**
 * Repositories configuration page.
 *
 * @author Yossi Shaul
 */
@SuppressWarnings({"OverlyComplexAnonymousInnerClass"})
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class RepositoryConfigPage extends AuthenticatedPage {
    private static final Logger log = LoggerFactory.getLogger(RepositoryConfigPage.class);

    @SpringBean
    private CentralConfigService centralConfigService;

    private MutableCentralConfigDescriptor mutableDescriptor;
    private WebMarkupContainer reposListContainer;

    public RepositoryConfigPage() {
        mutableDescriptor = refreshDescriptor();

        reposListContainer = new WebMarkupContainer("reposList");
        reposListContainer.setOutputMarkupId(true);
        add(reposListContainer);

        addLocalReposList();
        addRemoteReposList();
        addVirtualReposList();
    }

    private MutableCentralConfigDescriptor refreshDescriptor() {
        return centralConfigService.getMutableDescriptor();
    }

    @Override
    public String getPageName() {
        return "Configure Repositories";
    }

    private void addLocalReposList() {
        IModel repoListModel = new RepoListModel<LocalRepoDescriptor>() {
            @Override
            protected Collection<LocalRepoDescriptor> getRepos() {
                return mutableDescriptor.getLocalRepositoriesMap().values();
            }

            @Override
            protected void saveRepos(List<LocalRepoDescriptor> repos) {
                OrderedMap<String, LocalRepoDescriptor> currentLocalReposMap =
                        mutableDescriptor.getLocalRepositoriesMap();
                assertLegalReorderedList(repos, currentLocalReposMap.values());

                ListOrderedMap<String, LocalRepoDescriptor> localReposMap =
                        new ListOrderedMap<String, LocalRepoDescriptor>();
                for (LocalRepoDescriptor localRepo : repos) {
                    localReposMap.put(localRepo.getKey(), localRepo);
                }
                mutableDescriptor.setLocalRepositoriesMap(localReposMap);
                centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
            }
        };

        reposListContainer.add(new RepoListPanel<LocalRepoDescriptor>("localRepos", repoListModel) {
            @Override
            protected String getItemDisplayValue(LocalRepoDescriptor itemObject) {
                return itemObject.getKey();
            }

            @Override
            protected BaseModalPanel newCreateItemPanel() {
                return new LocalRepoPanel(CREATE, new LocalRepoDescriptor(), mutableDescriptor);
            }

            @Override
            protected BaseModalPanel newUpdateItemPanel(LocalRepoDescriptor itemObject) {
                return new LocalRepoPanel(UPDATE, itemObject, mutableDescriptor);
            }


            @Override
            protected Component newToolbar(String id) {
                return new SchemaHelpBubble(id, getHelpModel("localRepositoriesMap"));
            }

            @Override
            protected List<AbstractLink> getItemActions(LocalRepoDescriptor itemObject, String linkId) {
                List<AbstractLink> links = super.getItemActions(itemObject, linkId);
                if (mutableDescriptor.getLocalRepositoriesMap().size() == 1) {
                    // don't allow deletion of the last local repository
                    removeDeleteLink(links);
                }
                return links;
            }

            private void removeDeleteLink(List<AbstractLink> links) {
                Iterator<AbstractLink> linkIterator = links.iterator();
                while (linkIterator.hasNext()) {
                    AbstractLink link = linkIterator.next();
                    if (link instanceof DeleteRepoLink) {
                        linkIterator.remove();
                    }
                }
            }
        });

    }

    private void addRemoteReposList() {
        IModel repoListModel = new RepoListModel<RemoteRepoDescriptor>() {
            @Override
            protected Collection<RemoteRepoDescriptor> getRepos() {
                return mutableDescriptor.getRemoteRepositoriesMap().values();
            }

            @Override
            protected void saveRepos(List<RemoteRepoDescriptor> repos) {
                OrderedMap<String, RemoteRepoDescriptor> currentReposMap =
                        mutableDescriptor.getRemoteRepositoriesMap();
                assertLegalReorderedList(repos, currentReposMap.values());

                ListOrderedMap<String, RemoteRepoDescriptor> remoteReposMap =
                        new ListOrderedMap<String, RemoteRepoDescriptor>();
                for (RemoteRepoDescriptor remoteRepo : repos) {
                    remoteReposMap.put(remoteRepo.getKey(), remoteRepo);
                }
                mutableDescriptor.setRemoteRepositoriesMap(remoteReposMap);
                centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
            }
        };

        reposListContainer.add(new RepoListPanel<RemoteRepoDescriptor>("remoteRepos", repoListModel) {
            @Override
            protected String getItemDisplayValue(RemoteRepoDescriptor itemObject) {
                return itemObject.getKey();
            }

            @Override
            protected BaseModalPanel newCreateItemPanel() {
                return new HttpRepoPanel(CREATE, new HttpRepoDescriptor(), mutableDescriptor);
            }

            @Override
            protected HttpRepoPanel newUpdateItemPanel(RemoteRepoDescriptor itemObject) {
                return new HttpRepoPanel(UPDATE, (HttpRepoDescriptor) itemObject, mutableDescriptor);
            }

            @Override
            protected Component newToolbar(String id) {
                return new SchemaHelpBubble(id, getHelpModel("remoteRepositoriesMap"));
            }

            @Override
            protected Component getImportLink(String id) {
                return new ModalShowLink(id, "Import") {
                    @Override
                    protected BaseModalPanel getModelPanel() {
                        return new RemoteRepoImportPanel(mutableDescriptor);
                    }
                };
            }
        });
    }

    private void addVirtualReposList() {
        IModel repoListModel = new RepoListModel<VirtualRepoDescriptor>() {
            @Override
            protected Collection<VirtualRepoDescriptor> getRepos() {
                return mutableDescriptor.getVirtualRepositoriesMap().values();
            }

            @Override
            protected void saveRepos(List<VirtualRepoDescriptor> repos) {
                OrderedMap<String, VirtualRepoDescriptor> currentReposMap =
                        mutableDescriptor.getVirtualRepositoriesMap();
                assertLegalReorderedList(repos, currentReposMap.values());

                ListOrderedMap<String, VirtualRepoDescriptor> virtualReposMap =
                        new ListOrderedMap<String, VirtualRepoDescriptor>();
                for (VirtualRepoDescriptor virtualRepo : repos) {
                    virtualReposMap.put(virtualRepo.getKey(), virtualRepo);
                }
                mutableDescriptor.setVirtualRepositoriesMap(virtualReposMap);
                centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
            }
        };

        reposListContainer.add(new RepoListPanel<VirtualRepoDescriptor>("virtualRepos", repoListModel) {
            @Override
            protected String getItemDisplayValue(VirtualRepoDescriptor itemObject) {
                return itemObject.getKey();
            }

            @Override
            protected BaseModalPanel newCreateItemPanel() {
                return new VirtualRepoPanel(CREATE, new VirtualRepoDescriptor(), mutableDescriptor);
            }

            @Override
            protected BaseModalPanel newUpdateItemPanel(VirtualRepoDescriptor itemObject) {
                return new VirtualRepoPanel(UPDATE, itemObject, mutableDescriptor);
            }

            @Override
            protected Component newToolbar(String id) {
                return new SchemaHelpBubble(id, getHelpModel("virtualRepositoriesMap"));
            }
        });
    }

    private SchemaHelpModel getHelpModel(String property) {
        return new SchemaHelpModel(mutableDescriptor, property);
    }

    public void refresh(AjaxRequestTarget target) {
        target.addComponent(reposListContainer);
    }

    private class DeleteRepoLink extends TitledAjaxLink {
        private final RepoDescriptor repoDescriptor;

        private DeleteRepoLink(String linkId, RepoDescriptor repoDescriptor) {
            super(linkId, "Delete");
            add(new CssClass("icon-link DeleteAction"));
            this.repoDescriptor = repoDescriptor;
        }

        public void onClick(AjaxRequestTarget target) {
            mutableDescriptor.removeRepository(repoDescriptor.getKey());
            centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
            // reload the whole page to refresh all components (and to use the new descriptors)
            ((RepositoryConfigPage) getPage()).refresh(target);
        }

        @Override
        protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new ConfirmationAjaxCallDecorator("Are you sure you wish to delete the repository "
                    + repoDescriptor.getKey() + "?");
        }
    }

    private abstract static class EditLink extends ModalShowLink {
        private EditLink(String linkId) {
            super(linkId, "Edit");
            add(new CssClass("icon-link UpdateAction"));
        }
    }

    private abstract class RepoListPanel<T extends RepoDescriptor> extends OrderedListPanel<T> {
        protected RepoListPanel(String id, IModel listModel) {
            super(id, listModel);
        }

        protected abstract BaseModalPanel newUpdateItemPanel(T itemObject);

        @Override
        protected void populateItem(final ListItem item) {
            super.populateItem(item);
            item.add(new AjaxEventBehavior("ondblclick") {
                @SuppressWarnings({"unchecked"})
                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    ModalHandler modalHandler = ModalHandler.getInstanceFor(RepoListPanel.this);
                    mutableDescriptor = refreshDescriptor();
                    T itemObject = (T) item.getModelObject();
                    modalHandler.setModalPanel(newUpdateItemPanel(itemObject));
                    modalHandler.show(target);
                }

                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    return new CancelDefaultDecorator();
                }
            });
        }

        @Override
        protected List<AbstractLink> getItemActions(final T itemObject, String linkId) {
            List<AbstractLink> links = new ArrayList<AbstractLink>();
            links.add(new EditLink(linkId) {
                @Override
                protected BaseModalPanel getModelPanel() {
                    return newUpdateItemPanel(itemObject);
                }
            });
            links.add(new DeleteRepoLink(linkId, itemObject));
            return links;
        }

        @Override
        protected void onOrderChanged(AjaxRequestTarget target) {
            super.onOrderChanged(target);
            ((RepositoryConfigPage) getPage()).refresh(target);
        }
    }

    private abstract class RepoListModel<T extends RepoDescriptor> implements IModel {
        public ArrayList<T> getObject() {
            return new ArrayList<T>(getRepos());
        }

        @SuppressWarnings({"unchecked"})
        public void setObject(Object object) {
            saveRepos((List<T>) object);
        }

        public void detach() {
        }

        protected abstract Collection<T> getRepos();

        protected abstract void saveRepos(List<T> repos);

        protected void assertLegalReorderedList(List<? extends RepoDescriptor> newList,
                Collection<? extends RepoDescriptor> original) {
            log.debug("Original ordered list: {}", original);
            log.debug("New ordered list: {}", newList);
            // make sure that the new reordered list contains the same repositories count and the
            // same repository keys
            if (newList.size() != original.size()) {
                throw new IllegalArgumentException(
                        "Invalid reordered repositories list: size doesn't match. " +
                                "Expected " + original.size() +
                                " but received " + newList.size());
            }
            if (!original.containsAll(newList)) {
                throw new IllegalArgumentException("Invalid reordered repositories list: " +
                        "the new list contains repository not in the original list");
            }
        }
    }
}