package cn.sichu.system.sync;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "workspace.internal-user-sync")
public class InternalUserSyncProperties {

    private List<Client> clients = new ArrayList<>();

    @Getter
    @Setter
    public static class Client {
        private String appCode;
        private String token;
    }
}
