package ${project.basePackage};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ${project.applicationClass}Application {

    public static void main(String[] args) {
        SpringApplication.run(${project.applicationClass}Application.class, args);
    }
}
