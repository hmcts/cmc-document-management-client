package uk.gov.hmcts.reform.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.healthcheck.InternalHealth;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@FeignClient(name = "document-management-metadata-download-api", url = "${document_management.url}",
    configuration = DocumentMetadataDownloadClientApi.DownloadConfiguration.class)
public interface DocumentMetadataDownloadClientApi {

    @RequestMapping(method = RequestMethod.GET, value = "{document_metadata_uri}")
    Document getDocumentMetadata(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuth,
        @RequestHeader("user-roles") String userRoles,
        @PathVariable("document_metadata_uri") String documentMetadataUri
    );

    @RequestMapping(method = RequestMethod.GET, value = "{document_metadata_uri}")
    Document getDocumentMetadata(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuth,
        @RequestHeader("user-roles") String userRoles,
        @RequestHeader("user-id") String userId,
        @PathVariable("document_metadata_uri") String documentMetadataUri
    );


    @RequestMapping(
        method = RequestMethod.GET,
        value = "/health",
        headers = CONTENT_TYPE + "=" + APPLICATION_JSON_UTF8_VALUE
    )
    InternalHealth health();

    class DownloadConfiguration {
        @Bean
        @Primary
        Decoder feignDecoder(ObjectMapper objectMapper) {
            return new JacksonDecoder(objectMapper);
        }
    }
}
