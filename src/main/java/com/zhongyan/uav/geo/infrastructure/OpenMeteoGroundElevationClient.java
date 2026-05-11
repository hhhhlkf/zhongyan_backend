package com.zhongyan.uav.geo.infrastructure;

import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;
import com.zhongyan.uav.geo.port.GroundElevationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "bms.geo.elevation", name = "provider", havingValue = "open-meteo")
public class OpenMeteoGroundElevationClient implements GroundElevationPort {
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public double elevationMeters(double latitude, double longitude) {
        String url = UriComponentsBuilder.fromUriString("https://api.open-meteo.com/v1/elevation")
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .toUriString();
        Map<?, ?> response = restTemplate.getForObject(url, Map.class);
        Object elevation = response == null ? null : response.get("elevation");
        if (elevation instanceof List<?> values && !values.isEmpty() && values.get(0) instanceof Number number) {
            return number.doubleValue();
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "ground elevation response is invalid");
    }
}
