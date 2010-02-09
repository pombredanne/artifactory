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

package org.artifactory.rest.resource.search;

import org.artifactory.api.rest.constant.SearchRestConstants;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.resource.search.types.ArchiveSearchResource;
import org.artifactory.rest.resource.search.types.ArtifactSearchResource;
import org.artifactory.rest.resource.search.types.CreatedInRangeResource;
import org.artifactory.rest.resource.search.types.GavcSearchResource;
import org.artifactory.rest.resource.search.types.PropertySearchResource;
import org.artifactory.rest.resource.search.types.UsageSinceResource;
import org.artifactory.rest.resource.search.types.XpathSearchResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

/**
 * Resource class that handles search actions
 *
 * @author Yoav Landman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(SearchRestConstants.PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_USER, AuthorizationService.ROLE_ADMIN})
public class SearchResource {

    @Context
    private HttpServletResponse response;

    @Context
    private HttpServletRequest request;

    @Autowired
    SearchService searchService;

    /**
     * Delegates the request to the artifact search resource
     *
     * @return Artifact search resource
     */
    @Path(SearchRestConstants.PATH_ARTIFACT)
    public ArtifactSearchResource artifactQuery() {
        return new ArtifactSearchResource(searchService, request, response);
    }

    /**
     * Delegates the request to the archive search resource
     *
     * @return Archive search resource
     */
    @Path(SearchRestConstants.PATH_ARCHIVE)
    public ArchiveSearchResource archiveQuery() {
        return new ArchiveSearchResource(searchService, request, response);
    }

    /**
     * Delegates the request to the GAVC search resource
     *
     * @return GAVC search resource
     */
    @Path(SearchRestConstants.PATH_GAVC)
    public GavcSearchResource gavcQuery() {
        return new GavcSearchResource(searchService, request, response);
    }

    /**
     * Delegates the request to the property search resource
     *
     * @return property search resource
     */
    @Path(SearchRestConstants.PATH_PROPERTY)
    public PropertySearchResource propertyQuery() {
        return new PropertySearchResource(searchService, request, response);
    }

    /**
     * Delegates the request to the metadata search resource
     *
     * @return Metadata search resource
     */
    @Path(SearchRestConstants.PATH_METADATA)
    public XpathSearchResource metadataQuery() {
        return new XpathSearchResource(searchService, request, response);
    }

    /**
     * Delegates the request to the usage since search resource
     *
     * @return Usage since resource
     */
    @Path(SearchRestConstants.PATH_USAGE_SINCE)
    public UsageSinceResource notDownloadedSinceQuery() {
        return new UsageSinceResource(searchService, request, response);
    }

    /**
     * Delegates the request to the in date range search resource
     *
     * @return create in range resource
     */
    @Path(SearchRestConstants.PATH_CREATED_IN_RANGE)
    public CreatedInRangeResource createdInDateRangeQuery() {
        return new CreatedInRangeResource(searchService, request, response);
    }

    /**
     * Searches the repository and returns a plain text result
     *
     * @param searchQuery The search query
     * @return String - Plain text response
     */
    /*@GET
    @Produces("text/plain")
    public String searchAsText(@QueryParam(QUERY_PREFIX) String searchQuery) {
        StringBuilder sb = new StringBuilder();
        sb.append("Artifactory REST API search\n");
        sb.append("============================\n");
        if ((searchQuery == null) || ("".equals(searchQuery))) {
            sb.append("'query' parameter is either empty or non existant");
            return sb.toString();
        }
        SearchHelper searchHelper = new SearchHelper(searchService, null);
        String searchResult = searchHelper.searchPlainText(sb, searchQuery);
        return searchResult;
    }*/
}