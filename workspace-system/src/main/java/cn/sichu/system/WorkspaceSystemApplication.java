package cn.sichu.system;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@EnableScheduling
@EnableFeignClients
@SpringBootApplication(scanBasePackages = "cn.sichu")
@MapperScan(basePackages = "cn.sichu", annotationClass = Mapper.class)
public class WorkspaceSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceSystemApplication.class, args);
    }
}
