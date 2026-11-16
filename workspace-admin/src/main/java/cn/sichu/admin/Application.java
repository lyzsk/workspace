package cn.sichu.admin;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "cn.sichu")
@MapperScan(basePackages = "cn.sichu", annotationClass = Mapper.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
