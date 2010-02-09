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

package org.artifactory.api.rest.constant;

/**
 * @author yoavl
 */
public interface SearchRestConstants {
    String PATH_ROOT = "search";

    //Query path
    String PATH_ARTIFACT = "artifact";
    String PATH_ARCHIVE = "archive";
    String PATH_GAVC = "gavc";
    String PATH_PROPERTY = "prop";
    String PATH_METADATA = "xpath";
    String PATH_USAGE_SINCE = "usage";
    String PATH_CREATED_IN_RANGE = "creation";

    //Common query params
    String PARAM_REPO_TO_SEARCH = "repos";
    String PARAM_SEARCH_NAME = "name";

    //Gavc query params
    String PARAM_GAVC_GROUP_ID = "g";
    String PARAM_GAVC_ARTIFACT_ID = "a";
    String PARAM_GAVC_VERSION = "v";
    String PARAM_GAVC_CLASSIFIER = "c";

    //Xpath query params
    String PARAM_METADATA_NAME_SEARCH = "name";
    String PARAM_METADATA_SEARCH_TYPE = "metadata";
    String PARAM_METADATA_PATH = "xpath";
    String PARAM_METADATA_VALUE = "val";

    //Downloaded Since query param
    String PARAM_SEARCH_SINCE = "since";

    //Modified in range query params
    String PARAM_IN_RANGE_FROM = "from";
    String PARAM_IN_RANGE_TO = "to";

    //Media types
    String MT_ARTIFACT_SEARCH_RESULT = RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".ArtifactSearchResult+json";
    String MT_ARCHIVE_ENTRY_SEARCH_RESULT =
            RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".ArchiveEntrySearchResult+json";
    String MT_GAVC_SEARCH_RESULT = RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".GavcSearchResult+json";
    String MT_PROPERTY_SEARCH_RESULT = RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".MetadataSearchResult+json";
    String MT_XPATH_SEARCH_RESULT = RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".XpathSearchResult+json";
    String MT_USAGE_SINCE_SEARCH_RESULT = RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".ArtifactUsageResult+json";
    String MT_CREATED_IN_RANGE_SEARCH_RESULT =
            RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".ArtifactCreationResult+json";

    String NOT_FOUND = "No results found.";
}