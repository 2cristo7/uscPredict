package usc.uscPredict.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("USC Predict API")
                        .version("0.1.1")
                        .description("Prediction market platform for USC students. " +
                                "Create and participate in prediction events about future outcomes, " +
                                "similar to Polymarket but without blockchain.")
                        .contact(new Contact()
                                .name("USC Predict Team")
                                .email("support@uscpredict.com")
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("https://api.uscpredict.com")
                                .description("Production server")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Events")
                                .description("Endpoints para gestionar eventos de predicción. " +
                                        "Los eventos representan mercados de predicción con resultados YES/NO."),
                        new Tag()
                                .name("Users")
                                .description("Endpoints para gestionar usuarios de la plataforma."),
                        new Tag()
                                .name("Comments")
                                .description("Endpoints para gestionar comentarios en eventos."),
                        new Tag()
                                .name("Orders")
                                .description("Endpoints para gestionar órdenes de compra/venta en eventos.")
                ));
    }
}
