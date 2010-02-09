/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.build.tabs.list;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.build.api.Artifact;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleArtifactActionableItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The base module artifacts list panel
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseModuleArtifactsListPanel extends TitledPanel {

    /**
     * Main constructor
     *
     * @param id ID to assign to the panel
     */
    public BaseModuleArtifactsListPanel(String id) {
        super(id);
    }

    @Override
    public String getTitle() {
        return "Artifacts";
    }

    /**
     * Returns the list of artifacts to be displayed
     *
     * @return Artifact list to display
     */
    protected abstract List<Artifact> getArtifacts();

    /**
     * Returns a list of artifact actionable items
     *
     * @param artifacts Artifacts to create actionable items from
     * @return Artifact actionable item list
     */
    protected abstract List<ModuleArtifactActionableItem> getModuleArtifactActionableItems(List<Artifact> artifacts);

    /**
     * Adds the artifacts table
     */
    protected void addTable() {
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new ActionsColumn(""));
        columns.add(new PropertyColumn(new Model("Name"), "name", "artifact.name"));
        columns.add(new PropertyColumn(new Model("Type"), "type", "artifact.type"));
        columns.add(new PropertyColumn(new Model("Repo Path"), "repoPath"));

        add(new SortableTable("artifacts", columns, new ModuleArtifactsDataProvider(), 10));
    }

    /**
     * The published module's artifacts table data provider
     */
    private class ModuleArtifactsDataProvider extends SortableDataProvider {

        private List<Artifact> artifactsList;

        /**
         * Default constructor
         */
        public ModuleArtifactsDataProvider() {
            setSort("artifactName", true);
            this.artifactsList = getArtifacts();
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(artifactsList, getSort());
            List<ModuleArtifactActionableItem> listToReturn =
                    getModuleArtifactActionableItems(artifactsList.subList(first, first + count));
            return listToReturn.iterator();
        }

        public int size() {
            return artifactsList.size();
        }

        public IModel model(Object object) {
            return new Model((ModuleArtifactActionableItem) object);
        }
    }
}