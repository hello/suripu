package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

@Path("/v1/elastic_search")
public class ElasticSearchResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchResource.class);

    private final ElasticSearchTransportClient elasticSearchTransportClient;

    public ElasticSearchResource(final ElasticSearchTransportClient elasticSearchTransportClient) {
        this.elasticSearchTransportClient = elasticSearchTransportClient;
    }

    @POST
    @Path("/{index_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response createIndex(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                 @PathParam("index_name") final String indexName) {

        final TransportClient transportClient = elasticSearchTransportClient.generateClient();

        final CreateIndexRequestBuilder createIndexRequestBuilder = transportClient.admin().indices().prepareCreate(indexName);
        final Optional<String> settingsJSONOptional = ElasticSearchIndexSettings.createDefault().toJSON();
        if (settingsJSONOptional.isPresent()) {
            createIndexRequestBuilder.setSettings(ImmutableSettings.settingsBuilder().loadFromSource(settingsJSONOptional.get()));
        }

        // mapping (time to live per document type)
        final Optional<XContentBuilder> indexMappingOptional = ElasticSearchIndexMappings.createDefault().toJSON();
        if (indexMappingOptional.isPresent()) {
            createIndexRequestBuilder.addMapping("_default_", indexMappingOptional.get()); // all types share the same ttl
        }
        try {
            createIndexRequestBuilder.execute().actionGet();
        }
        catch (IndexAlreadyExistsException e) {
            throw new WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST)
                    .entity(new JsonError(400, String.format("Index %s already exists", indexName))).build());
        }
        finally {
            transportClient.close();
        }

        return javax.ws.rs.core.Response.noContent().build();
    }


    @DELETE
    @Path("/{index_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response deleteIndex(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                 @PathParam("index_name") final String indexName) {

        final TransportClient transportClient = elasticSearchTransportClient.generateClient();

        try {
            final DeleteIndexResponse deleteIndexResponse = transportClient.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet();
            if (!deleteIndexResponse.isAcknowledged()) {
                throw new WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new JsonError(500, String.format("Failed to delete index %s", indexName))).build());
            }
        }
        catch (IndexMissingException e) {
            throw new WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST)
                    .entity(new JsonError(400, String.format("Index %s does not exist", indexName))).build());
        }
        finally {
            transportClient.close();
        }
        return javax.ws.rs.core.Response.noContent().build();
    }


    @DELETE
    @Path("/{index_name}/{doc_type}/{doc_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response deleteDocument(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                    @PathParam("index_name") final String indexName,
                                                    @PathParam("doc_type") final String docType,
                                                    @PathParam("doc_id") final String docId) {

        final TransportClient transportClient = elasticSearchTransportClient.generateClient();

        final DeleteResponse response = transportClient.prepareDelete(indexName, docType, docId)
                .setOperationThreaded(false)
                .execute()
                .actionGet();
        if (!response.isFound()) {
            throw new WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, String.format("Document %s does not exist", docId))).build());
        }
        return javax.ws.rs.core.Response.noContent().build();
    }

}