package com.zhongyan.uav.geo.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;
import com.zhongyan.uav.common.resilience.ExternalCallGuard;
import com.zhongyan.uav.geo.domain.LayerPublishRecord;
import com.zhongyan.uav.geo.domain.LayerStatus;
import com.zhongyan.uav.geo.port.GeoServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "bms.geo.geoserver", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GeoServerProperties.class)
public class GeoServerRestClient implements GeoServerPort {
    private final GeoServerProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ExternalCallGuard restGuard;

    @Autowired
    public GeoServerRestClient(GeoServerProperties properties,
                               JdbcTemplate jdbcTemplate,
                               ObjectMapper objectMapper) {
        this(properties, jdbcTemplate, objectMapper, restTemplate(properties));
    }

    GeoServerRestClient(GeoServerProperties properties,
                        JdbcTemplate jdbcTemplate,
                        ObjectMapper objectMapper,
                        RestTemplate restTemplate) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate must not be null");
        this.restGuard = new ExternalCallGuard("geoserver", properties.retryMaxAttempts(), properties.retryBackoffMs(),
                properties.circuitFailureThreshold(), properties.circuitOpenDurationMs(), ResourceAccessException.class);
    }

    @Override
    public LayerPublishRecord publishLayer(Asset asset, Map<String, Object> parameters) {
        Map<String, Object> safeParameters = parameters == null ? Map.of() : parameters;
        String layerId = safeParameters.getOrDefault("layerId", properties.layerName()).toString();
        upsertAssetGeometry(asset, safeParameters);
        ensureWorkspace();
        ensurePostgisDataStore();
        ensureFeatureType(layerId);
        ensureStyle();
        assignDefaultStyle(layerId);
        Object layerUrl = safeParameters.get("layerUrl");
        String publishedLayerUrl = layerUrl == null || layerUrl.toString().isBlank()
                ? defaultWmsLayerUrl(layerId, asset.assetId())
                : layerUrl.toString();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("adapter", "geoserver-rest");
        metadata.put("workspace", properties.workspace());
        metadata.put("datastore", properties.datastore());
        metadata.put("tableName", properties.tableName());
        metadata.put("styleName", properties.styleName());
        metadata.put("restUrl", properties.normalizedUrl());
        metadata.put("cqlFilter", "asset_id='" + asset.assetId() + "'");
        return new LayerPublishRecord(asset.assetId(), layerId, publishedLayerUrl,
                LayerStatus.PUBLISHED, metadata, Instant.now());
    }

    private void upsertAssetGeometry(Asset asset, Map<String, Object> parameters) {
        try {
            String geometryJson = geometryJson(asset, parameters);
            String propertiesJson = objectMapper.writeValueAsString(asset.metadata());
            jdbcTemplate.update("""
                    INSERT INTO asset_geometry (asset_id, mission_id, task_id, geom, properties, updated_at)
                    VALUES (?, ?, ?, ST_SetSRID(ST_GeomFromGeoJSON(?), 4326), ?::jsonb, now())
                    ON CONFLICT (asset_id) DO UPDATE
                    SET mission_id = EXCLUDED.mission_id,
                        task_id = EXCLUDED.task_id,
                        geom = EXCLUDED.geom,
                        properties = EXCLUDED.properties,
                        updated_at = EXCLUDED.updated_at
                    """, asset.assetId(), asset.missionId(), asset.taskId(), geometryJson, propertiesJson);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "failed to upsert asset geometry for GeoServer publishing: " + asset.assetId(), exception);
        }
    }

    private String geometryJson(Asset asset, Map<String, Object> parameters) {
        Object source = parameters.containsKey("geometry") ? parameters.get("geometry") : asset.metadata().get("geometry");
        if (source == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "asset geometry must be calculated before publishing layer: " + asset.assetId());
        }
        JsonNode node = objectMapper.valueToTree(source);
        JsonNode geometry = "Feature".equalsIgnoreCase(node.path("type").asText()) ? node.path("geometry") : node;
        if (geometry.isMissingNode() || geometry.isNull() || geometry.path("type").asText().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "asset geometry is not valid GeoJSON");
        }
        try {
            return objectMapper.writeValueAsString(geometry);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "asset geometry is not valid GeoJSON", exception);
        }
    }

    private void ensureWorkspace() {
        String workspace = properties.workspace();
        String url = restUrl("/workspaces/" + workspace + ".xml");
        if (exists(url)) {
            return;
        }
        String body = "<workspace><name>" + xml(workspace) + "</name></workspace>";
        postXml(restUrl("/workspaces"), body);
    }

    private void ensurePostgisDataStore() {
        String url = restUrl("/workspaces/" + properties.workspace() + "/datastores/" + properties.datastore() + ".xml");
        if (exists(url)) {
            return;
        }
        String body = """
                <dataStore>
                  <name>%s</name>
                  <enabled>true</enabled>
                  <connectionParameters>
                    <entry key="dbtype">postgis</entry>
                    <entry key="host">%s</entry>
                    <entry key="port">%d</entry>
                    <entry key="database">%s</entry>
                    <entry key="schema">%s</entry>
                    <entry key="user">%s</entry>
                    <entry key="passwd">%s</entry>
                    <entry key="Expose primary keys">true</entry>
                  </connectionParameters>
                </dataStore>
                """.formatted(
                xml(properties.datastore()),
                xml(properties.postgisHost()),
                properties.postgisPort(),
                xml(properties.postgisDatabase()),
                xml(properties.postgisSchema()),
                xml(properties.postgisUsername()),
                xml(properties.postgisPassword()));
        postXml(restUrl("/workspaces/" + properties.workspace() + "/datastores"), body);
    }

    private void ensureFeatureType(String layerId) {
        if (layerId == null || layerId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "layerId must not be blank");
        }
        String url = restUrl("/workspaces/" + properties.workspace()
                + "/datastores/" + properties.datastore()
                + "/featuretypes/" + layerId + ".xml");
        if (exists(url)) {
            return;
        }
        String body = """
                <featureType>
                  <name>%s</name>
                  <nativeName>%s</nativeName>
                  <title>%s</title>
                  <srs>EPSG:4326</srs>
                  <enabled>true</enabled>
                </featureType>
                """.formatted(xml(layerId), xml(properties.tableName()), xml(layerId));
        postXml(restUrl("/workspaces/" + properties.workspace()
                + "/datastores/" + properties.datastore()
                + "/featuretypes"), body);
    }

    private void ensureStyle() {
        String url = restUrl("/workspaces/" + properties.workspace() + "/styles/" + properties.styleName() + ".xml");
        if (exists(url)) {
            return;
        }
        postSld(restUrl("/workspaces/" + properties.workspace() + "/styles?name=" + properties.styleName()), defaultStyleSld());
    }

    private void assignDefaultStyle(String layerId) {
        String body = """
                <layer>
                  <defaultStyle>
                    <name>%s:%s</name>
                  </defaultStyle>
                </layer>
                """.formatted(xml(properties.workspace()), xml(properties.styleName()));
        putXml(restUrl("/layers/" + properties.workspace() + ":" + layerId + ".xml"), body);
    }

    private boolean exists(String url) {
        try {
            exchange("exists", url, HttpMethod.GET, new HttpEntity<>(headers()));
            return true;
        } catch (HttpClientErrorException.NotFound exception) {
            return false;
        } catch (HttpClientErrorException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "GeoServer REST request failed: " + exception.getStatusCode(), exception);
        } catch (ResourceAccessException | IllegalStateException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "GeoServer REST request unavailable: " + exception.getMessage(), exception);
        }
    }

    private void postXml(String url, String body) {
        HttpHeaders headers = headers();
        headers.setContentType(MediaType.APPLICATION_XML);
        try {
            exchange("postXml", url, HttpMethod.POST, new HttpEntity<>(body, headers));
        } catch (HttpClientErrorException.Conflict ignored) {
            // GeoServer returns 409 when another caller already created the same resource.
        } catch (HttpClientErrorException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "GeoServer REST create request failed: " + exception.getStatusCode(), exception);
        } catch (ResourceAccessException | IllegalStateException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "GeoServer REST create request unavailable: " + exception.getMessage(), exception);
        }
    }

    private void postSld(String url, String body) {
        HttpHeaders headers = headers();
        headers.setContentType(MediaType.parseMediaType("application/vnd.ogc.sld+xml"));
        try {
            exchange("postSld", url, HttpMethod.POST, new HttpEntity<>(body, headers));
        } catch (HttpClientErrorException.Conflict ignored) {
            // GeoServer returns 409 when another caller already created the same resource.
        } catch (HttpClientErrorException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "GeoServer REST style create request failed: " + exception.getStatusCode(), exception);
        } catch (ResourceAccessException | IllegalStateException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "GeoServer REST style create request unavailable: " + exception.getMessage(), exception);
        }
    }

    private void putXml(String url, String body) {
        HttpHeaders headers = headers();
        headers.setContentType(MediaType.APPLICATION_XML);
        try {
            exchange("putXml", url, HttpMethod.PUT, new HttpEntity<>(body, headers));
        } catch (HttpClientErrorException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "GeoServer REST update request failed: " + exception.getStatusCode(), exception);
        } catch (ResourceAccessException | IllegalStateException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "GeoServer REST update request unavailable: " + exception.getMessage(), exception);
        }
    }

    private void exchange(String operation, String url, HttpMethod method, HttpEntity<String> entity) {
        restGuard.execute(operation, () -> restTemplate.exchange(url, method, entity, String.class));
    }

    private String defaultStyleSld() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <StyledLayerDescriptor version="1.0.0"
                  xmlns="http://www.opengis.net/sld"
                  xmlns:ogc="http://www.opengis.net/ogc"
                  xmlns:xlink="http://www.w3.org/1999/xlink"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd">
                  <NamedLayer>
                    <Name>%s</Name>
                    <UserStyle>
                      <Title>Asset boundary</Title>
                      <FeatureTypeStyle>
                        <Rule>
                          <PolygonSymbolizer>
                            <Fill>
                              <CssParameter name="fill">#2F80ED</CssParameter>
                              <CssParameter name="fill-opacity">0.18</CssParameter>
                            </Fill>
                            <Stroke>
                              <CssParameter name="stroke">#EB5757</CssParameter>
                              <CssParameter name="stroke-width">2</CssParameter>
                            </Stroke>
                          </PolygonSymbolizer>
                        </Rule>
                      </FeatureTypeStyle>
                    </UserStyle>
                  </NamedLayer>
                </StyledLayerDescriptor>
                """.formatted(xml(properties.styleName()));
    }

    private String defaultWmsLayerUrl(String layerId, String assetId) {
        String qualifiedLayer = properties.workspace() + ":" + layerId;
        return UriComponentsBuilder.fromUriString(properties.normalizedUrl() + "/" + properties.workspace() + "/wms")
                .queryParam("service", "WMS")
                .queryParam("version", "1.1.0")
                .queryParam("request", "GetMap")
                .queryParam("layers", qualifiedLayer)
                .queryParam("styles", "")
                .queryParam("bbox", "-180,-90,180,90")
                .queryParam("width", "768")
                .queryParam("height", "384")
                .queryParam("srs", "EPSG:4326")
                .queryParam("format", "image/png")
                .queryParam("CQL_FILTER", "asset_id='" + assetId + "'")
                .build()
                .encode()
                .toUriString();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = properties.username() + ":" + properties.password();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        return headers;
    }

    private String restUrl(String path) {
        return properties.normalizedUrl() + "/rest" + path;
    }

    private String xml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static RestTemplate restTemplate(GeoServerProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.readTimeoutMs());
        return new RestTemplate(requestFactory);
    }
}
