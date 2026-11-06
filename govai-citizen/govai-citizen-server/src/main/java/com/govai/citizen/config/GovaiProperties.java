package com.govai.citizen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "govai")
public class GovaiProperties {

    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();
    private boolean mockLogin = true;
    private Feign feign = new Feign();

    @Data
    public static class Jwt {
        private String secret;
        private long expireSeconds = 604800;
        private String header = "Authorization";
        private String prefix = "Bearer ";
    }

    @Data
    public static class Upload {
        private String dir;
        private String urlPrefix = "/uploads/";
    }

    @Data
    public static class Feign {
        private String approvalUrl;
        private String supervisionUrl;
    }
}
