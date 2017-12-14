package uk.gov.hmcts.reform.document;

import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.healthcheck.InternalHealth;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@FeignClient(name = "document-management-metadata-download-gateway-api", url = "${document_management.api_gateway.url}",
    configuration = DocumentMetadataDownloadClientApi.DownloadConfiguration.class)
public interface DocumentMetadataDownloadClientApi {

    @RequestMapping(method = RequestMethod.GET, value = "{document_metadata_uri}")
    Document getDocumentMetadata(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
                                 @PathVariable("document_metadata_uri") String documentMetadataUri);


    @RequestMapping(
        method = RequestMethod.GET,
        value = "/health",
        headers = CONTENT_TYPE + "=" + APPLICATION_JSON_UTF8_VALUE
    )
    InternalHealth health();

    class DownloadConfiguration {
        @Bean
        @Primary
        @Scope("prototype")
        Decoder feignDecoder() {
            return new JacksonDecoder();
        }
    }
}