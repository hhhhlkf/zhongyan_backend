package com.zhongyan.uav.geo.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.geo.domain.LayerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeoServerRestClientTests {
    @Test
    void publishesAssetGeometryThroughPostgisBackedGeoServerLayer() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        GeoServerProperties properties = new GeoServerProperties(true,
                "http://geoserver.test/geoserver",
                "admin",
                "geoserver",
                "zhongyan",
                "postgis",
                "postgres",
                5432,
                "zhongyan_uav",
                "public",
                "zhongyan",
                "zhongyan_local",
                "asset_geometry",
                "asset_geometry",
                "asset-boundary",
                3000,
                10000,
                3,
                0,
                3,
                30000);
        GeoServerRestClient client = new GeoServerRestClient(properties, jdbcTemplate, new ObjectMapper(), restTemplate);
        Asset asset = Asset.created("asset-geo-1", "mission-geo-1", "task-geo-1",
                AssetType.IMAGE, AssetRole.OUTPUT, "geo.jpg", "raw/geo.jpg",
                "image/jpeg", 10, "sha256:geo", Map.of("geometry", geoJson()),
                "tester", Instant.parse("2026-05-17T00:00:00Z")).markAvailable(Instant.now());

        server.expect(requestTo("http://geoserver.test/geoserver/rest/workspaces/zhongyan.xml"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://geoserver.test/geoserver/rest/workspaces"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());
        server.expect(requestTo("http://geoserver.test/geoserver/rest/workspaces/zhongyan/datastores/postgis.xml"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://geoserver.test/geoserver/rest/workspaces/zhongyan/datastores"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());
        server.expect(requestTo("http://geoserver.test/geoserver/rest/workspaces/zhongyan/datastores/postgis/featuretypes/asset_geometry.xml"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://geoserver.test/geoserver/rest/workspaces/zhongyan/datastores/postgis/featuretypes"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());
        server.expect(requestTo("http://geoserver.test/geoserver/rest/workspaces/zhongyan/styles/asset-boundary.xml"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://geoserver.test/geoserver/rest/workspaces/zhongyan/styles?name=asset-boundary"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());
        server.expect(requestTo("http://geoserver.test/geoserver/rest/layers/zhongyan:asset_geometry.xml"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess());

        var record = client.publishLayer(asset, Map.of("approved", true));

        assertThat(record.status()).isEqualTo(LayerStatus.PUBLISHED);
        assertThat(record.layerId()).isEqualTo("asset_geometry");
        assertThat(record.layerUrl()).contains("service=WMS", "asset_geometry", "asset-geo-1");
        assertThat(record.metadata()).containsEntry("workspace", "zhongyan")
                .containsEntry("datastore", "postgis")
                .containsEntry("tableName", "asset_geometry")
                .containsEntry("styleName", "asset-boundary");
        verify(jdbcTemplate).update(contains("INSERT INTO asset_geometry"), any(), any(), any(), any(), any());
        server.verify();
    }

    private Map<String, Object> geoJson() {
        return Map.of(
                "type", "Feature",
                "geometry", Map.of(
                        "type", "Polygon",
                        "coordinates", List.of(List.of(
                                List.of(120.0, 30.0),
                                List.of(120.1, 30.0),
                                List.of(120.1, 30.1),
                                List.of(120.0, 30.1),
                                List.of(120.0, 30.0)))),
                "properties", Map.of("assetId", "asset-geo-1"));
    }
}
