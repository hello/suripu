package com.hello.suripu.admin.resources.v1;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.search.ElasticSearchIndexMappings;
import com.hello.suripu.search.ElasticSearchIndexSettings;
import com.hello.suripu.search.ElasticSearchTransportClient;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

@Path("/v1/elastic_search")
public class ElasticSearchResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchResource.class);
    private static final String DEFAULT_ES_SORT_BY = "timestamp";
    private static final String DEFAULT_ES_SORT_ORDER = "desc";
    private static final String DEFAULT_ES_QUERY_SIZE = "50";
    private static final String DEFAULT_DOCUMENT_TYPE = "_default_";
    private static final String IMMORTAL_DOCUMENT_TYPE = "immortal";
    private static final com.squareup.okhttp.MediaType JSON = com.squareup.okhttp.MediaType.parse("application/json; charset=utf-8");


    private final ElasticSearchTransportClient elasticSearchTransportClient;
    private final String indexPrefix;
    private final String elasticSearchHttpEndpoint;

    public ElasticSearchResource(final ElasticSearchTransportClient elasticSearchTransportClient, final String indexPrefix, final String elasticSearchHttpEndpoint) {
        this.elasticSearchTransportClient = elasticSearchTransportClient;
        this.indexPrefix = indexPrefix;
        this.elasticSearchHttpEndpoint = elasticSearchHttpEndpoint;
    }

    @POST
    @Path("/{index_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIndexByTCP(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                                     @QueryParam("with_prefix") final Boolean withPrefix,
                                     @PathParam("index_name") String indexName) {

        final TransportClient transportClient = elasticSearchTransportClient.generateClient();
        String errorMessage=null;
        try {

            buildIndex(transportClient, indexName, withPrefix).execute().actionGet();
        }
        catch (IndexAlreadyExistsException e) {
            errorMessage = e.getMessage();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), String.format("Index %s already exists", indexName))).build());
        }
        catch (ConnectTransportException e) {
            errorMessage = e.getMessage();
            LOGGER.error(e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), String.format("Failed to connect because %s", e.getMessage()))).build());
        }
        finally {
            if (errorMessage != null) {
                LOGGER.error(errorMessage);
            }
            transportClient.close();
        }

        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIndicesByTCP(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
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
    public Response deleteIndexByTCP(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
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

    @POST
    @Path("/via_http/{index_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer createIndexByHttp(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                     @PathParam("index_name") final String indexName,
                                     @DefaultValue("true") @QueryParam("with_prefix") final Boolean withPrefix){
        final String esDeleteIndexURL = withPrefix.equals(Boolean.FALSE) ? (elasticSearchHttpEndpoint + indexName) : (elasticSearchHttpEndpoint + indexPrefix + indexName);
        final OkHttpClient client = new OkHttpClient();
        try {
            final RequestBody body = RequestBody.create(JSON, new JSONObject()
                    .put("settings", ElasticSearchIndexSettings.createDefault().toJSONObject())
                    .put("mappings", new JSONObject()
                        .put(DEFAULT_DOCUMENT_TYPE, ElasticSearchIndexMappings.createDefault().toJSONObject())
                        .put(IMMORTAL_DOCUMENT_TYPE, ElasticSearchIndexMappings.createImmortal().toJSONObject())
                    )
                    .toString());

            final Request request = new Request.Builder().url(esDeleteIndexURL).put(body).build();
            final com.squareup.okhttp.Response response = client.newCall(request).execute();
            return response.code();
        }
        catch (IOException e) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage())).build());
        }
        catch (JSONException e) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage())).build());
        }
    }

    @DELETE
    @Path("/via_http/{index_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer deleteIndexByHttp(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                     @PathParam("index_name") final String indexName){
        final String esDeleteIndexURL = elasticSearchHttpEndpoint + indexName;
        final OkHttpClient client = new OkHttpClient();

        try {
            final Request request = new Request.Builder().url(esDeleteIndexURL).delete().build();
            final com.squareup.okhttp.Response response = client.newCall(request).execute();
            return response.code();
        }
        catch (IOException e) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage())).build());
        }

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String query(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                        @QueryParam("es_query") final String esQuery,
                        @DefaultValue(DEFAULT_ES_SORT_BY) @QueryParam("sort_by") final String sortBy,
                        @DefaultValue(DEFAULT_ES_SORT_ORDER) @QueryParam("sort_order") final String sortOrder,
                        @DefaultValue(DEFAULT_ES_QUERY_SIZE) @QueryParam("size") final Integer size){
        final String esQueryURL = String.format("%s%s&size=%s", elasticSearchHttpEndpoint, esQuery, size);
        LOGGER.info("Raw es query url {}, ORDER by {} {}", esQueryURL, sortBy, sortOrder);

        final OkHttpClient client = new OkHttpClient();
        String errorMessage;

        try {
            final RequestBody body = RequestBody.create(JSON, new JSONObject().put("sort", new JSONObject().put(sortBy, sortOrder)).toString());
            final Request request = new Request.Builder().url(esQueryURL).post(body).build();
            final com.squareup.okhttp.Response response = client.newCall(request).execute();
            return new JSONObject(response.body().string()).toString();
        }
        catch (IOException e) {
            errorMessage = e.getMessage();
        }
        catch (JSONException e) {
            errorMessage = e.getMessage();
        }
        catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }
        throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), String.format(errorMessage))).build());
    }


    private CreateIndexRequestBuilder buildIndex(final TransportClient transportClient, String indexName, final Boolean withPrefix) {
        if (withPrefix != null && withPrefix.equals(Boolean.TRUE)){
            indexName = indexPrefix + indexName;
        }
        final CreateIndexRequestBuilder createIndexRequestBuilder = transportClient.admin().indices().prepareCreate(indexName);
        final Optional<String> settingsJSONOptional = ElasticSearchIndexSettings.createDefault().toJSONString();
        if (settingsJSONOptional.isPresent()) {
            createIndexRequestBuilder.setSettings(ImmutableSettings.settingsBuilder().loadFromSource(settingsJSONOptional.get()));
        }

        // mapping tokenizer, analyzer and ttl per document type, default mapping is used for "mortal" documents
        final Optional<XContentBuilder> indexMappingDefaultOptional = ElasticSearchIndexMappings.createDefault().toJSON();
        final Optional<XContentBuilder> indexMappingImmortalOptional = ElasticSearchIndexMappings.createImmortal().toJSON();
        if (indexMappingDefaultOptional.isPresent()) {
            createIndexRequestBuilder.addMapping(DEFAULT_DOCUMENT_TYPE, indexMappingDefaultOptional.get());
        }
        if (indexMappingImmortalOptional.isPresent()) {
            createIndexRequestBuilder.addMapping(IMMORTAL_DOCUMENT_TYPE, indexMappingImmortalOptional.get());
        }
        return createIndexRequestBuilder;
    }
}