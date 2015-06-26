package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.ElasticSearch.ElasticSearchIndexMappings;
import com.hello.suripu.core.models.ElasticSearch.ElasticSearchIndexSettings;
import com.hello.suripu.core.models.ElasticSearch.ElasticSearchTransportClient;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.JsonError;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.transport.ConnectTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v1/elastic_search")
public class ElasticSearchResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchResource.class);

    private final ElasticSearchTransportClient elasticSearchTransportClient;
    private final String indexPrefix;

    public ElasticSearchResource(final ElasticSearchTransportClient elasticSearchTransportClient, final String indexPrefix) {
        this.elasticSearchTransportClient = elasticSearchTransportClient;
        this.indexPrefix = indexPrefix;
    }

    @POST
    @Path("/{index_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIndex(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                                @QueryParam("with_prefix") final Boolean withPrefix,
                                @PathParam("index_name") String indexName) {

        final TransportClient transportClient = elasticSearchTransportClient.generateClient();
        try {

            buildIndex(transportClient, indexName, withPrefix).execute().actionGet();
        }
        catch (IndexAlreadyExistsException e) {
            LOGGER.error(e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), String.format("Index %s already exists", indexName))).build());
        }
        catch (ConnectTransportException e) {
            LOGGER.error(e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), String.format("Failed to connect because %s", e.getMessage()))).build());
        }
        finally {
            transportClient.close();
        }

        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIndices(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                                  @QueryParam("with_prefix") final Boolean withPrefix,
                                  @Valid @NotNull final String[] indexNames){

        final TransportClient transportClient = elasticSearchTransportClient.generateClient();
        final List<String> createdIndices = Lists.newArrayList();
        for (String indexName : indexNames) {
            try {
                buildIndex(transportClient, indexName, withPrefix).execute().actionGet();
                createdIndices.add(indexName);
            }
            catch (IndexAlreadyExistsException e) {
                LOGGER.error(e.getMessage());
                continue;
            }
            catch (ConnectTransportException e) {
                LOGGER.error(e.getMessage());
                continue;
            }
        }
        LOGGER.info("Successfully created {}", createdIndices);
        transportClient.close();
        return Response.noContent().build();
    }


    @DELETE
    @Path("/{index_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteIndex(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                                @PathParam("index_name") final String indexName) {

        final TransportClient transportClient = elasticSearchTransportClient.generateClient();

        try {
            final DeleteIndexResponse deleteIndexResponse = transportClient.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet();
            if (!deleteIndexResponse.isAcknowledged()) {
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), String.format("Failed to delete index %s", indexName))).build());
            }
        }
        catch (IndexMissingException e) {
            LOGGER.error(e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), String.format("Index %s does not exist", indexName))).build());
        }
        catch (ConnectTransportException e) {
            LOGGER.error(e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), String.format("Failed to connect because %s", e.getMessage()))).build());
        }
        finally {
            transportClient.close();
        }
        return Response.noContent().build();
    }


    @DELETE
    @Path("/{index_name}/{doc_type}/{doc_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDocument(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                                   @PathParam("index_name") final String indexName,
                                   @PathParam("doc_type") final String docType,
                                   @PathParam("doc_id") final String docId) {

        final TransportClient transportClient = elasticSearchTransportClient.generateClient();

        final DeleteResponse response = transportClient.prepareDelete(indexName, docType, docId)
                .setOperationThreaded(false)
                .execute()
                .actionGet();
        if (!response.isFound()) {
            LOGGER.error("Document {} does not exist", docId);
            throw new WebApplicationException(Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND)
                    .entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(), String.format("Document %s does not exist", docId))).build());
        }
        return Response.noContent().build();
    }


    private CreateIndexRequestBuilder buildIndex(final TransportClient transportClient, String indexName, final Boolean withPrefix) {
        if (withPrefix != null && withPrefix.equals(Boolean.TRUE)){
            indexName = indexPrefix + indexName;
        }
        final CreateIndexRequestBuilder createIndexRequestBuilder = transportClient.admin().indices().prepareCreate(indexName);
        final Optional<String> settingsJSONOptional = ElasticSearchIndexSettings.createDefault().toJSON();
        if (settingsJSONOptional.isPresent()) {
            createIndexRequestBuilder.setSettings(ImmutableSettings.settingsBuilder().loadFromSource(settingsJSONOptional.get()));
        }

        // mapping tokenizer, analyzer and ttl per document type, default mapping is used for "mortal" documents
        final Optional<XContentBuilder> indexMappingDefaultOptional = ElasticSearchIndexMappings.createDefault().toJSON();
        final Optional<XContentBuilder> indexMappingMortalOptional = ElasticSearchIndexMappings.createImmortal().toJSON();
        if (indexMappingDefaultOptional.isPresent()) {
            createIndexRequestBuilder.addMapping("_default_", indexMappingDefaultOptional.get());
        }
        if (indexMappingMortalOptional.isPresent()) {
            createIndexRequestBuilder.addMapping("immortal", indexMappingMortalOptional.get());
        }
        return createIndexRequestBuilder;
    }
}