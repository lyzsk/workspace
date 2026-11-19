package cn.sichu.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@EnableFeignClients
@SpringBootApplication(scanBasePackages = "cn.sichu")
public class WorkspaceAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceAuthApplication.class, args);
    }
}
