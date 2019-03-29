package app.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Created by taesu at : 2019-03-29\
 *
 * @author taesu
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequiredArgsConstructor
public class ConfigController {

    private final @NotNull StaticConfigService staticConfigService;
    private final @NotNull DynamicConfigService dynamicConfigService;

    @GetMapping("config/static")
    public ResponseEntity<Map<String, String>> getStaticConfigPropertyMap() {
        return ResponseEntity.ok(staticConfigService.getPropertyMap());
    }

    @GetMapping("config/dynamic")
    public ResponseEntity<Map<String, String>> getDynamicConfigPropertyMap() {
        return ResponseEntity.ok(dynamicConfigService.getPropertyMap());
    }
}
