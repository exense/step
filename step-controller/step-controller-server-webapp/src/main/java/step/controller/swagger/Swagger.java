package step.controller.swagger;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.glassfish.jersey.server.ResourceConfig;
import step.core.Version;

import java.util.List;

public class Swagger {

    public static void setup(ResourceConfig resourceConfig) {
        OpenAPI oas = new OpenAPI();
        Info info = new Info()
                .title("step Controller REST API")
                .description("")
                .version(Version.getCurrentVersion().toString())
                .contact(new Contact()
                        .email("support@exense.ch"))
                .license(new License()
                        .name("GNU Affero General Public License")
                        .url("http://www.gnu.org/licenses/agpl-3.0.de.html"));

        oas.info(info);

        // SecurityScheme api-key
        SecurityScheme securitySchemeApiKey = new SecurityScheme();
        securitySchemeApiKey.setName("Api key");
        securitySchemeApiKey.setScheme("bearer");
        securitySchemeApiKey.setType(SecurityScheme.Type.HTTP);
        securitySchemeApiKey.setIn(SecurityScheme.In.HEADER);

        OpenApiResource openApiResource = new OpenApiResource();

        oas.schemaRequirement(securitySchemeApiKey.getName(), securitySchemeApiKey);
        oas.servers(List.of(new io.swagger.v3.oas.models.servers.Server().url("/rest")));
        oas.security(List.of(new SecurityRequirement().addList("Api key")));

        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true);

        openApiResource.setOpenApiConfiguration(oasConfig);
        resourceConfig.register(openApiResource);

        ModelConverters.getInstance().addConverter(new ObjectIdAwareConverter());
    }
}
