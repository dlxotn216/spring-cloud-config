package app.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by taesu at : 2019-03-29
 *
 * Refresh Scope를 적용하여
 * Spring actuator를 통해 config를 refresh 가능 함
 *
 * [POST] /actuator/refresh {}Content-Type : application/json}
 *
 * @author taesu
 * @version 1.0
 * @since 1.0
 */
@Service
@RefreshScope
public class DynamicConfigService {

    @Value("${taesu.profile}")
    private String profile;

    @Value("${taesu.said.first}")
    private String first;

    @Value("${taesu.said.second}")
    private String second;

    public Map<String, String> getPropertyMap() {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("profile", this.profile);
        propertyMap.put("first", this.first);
        propertyMap.put("second", this.second);
        return propertyMap;
    }


}
