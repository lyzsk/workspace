package cn.sichu.system.apiclient.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Data
@AllArgsConstructor
public class ApiClientSecretVO {

    private String id;

    private String clientId;

    private String clientSecret;

    private String message;
}
