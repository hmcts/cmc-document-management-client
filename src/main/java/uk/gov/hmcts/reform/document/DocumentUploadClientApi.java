package uk.gov.hmcts.reform.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.Classification;
import uk.gov.hmcts.reform.document.domain.UploadResponse;

import java.io.IOException;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Service
public class DocumentUploadClientApi {

    private static final String CLASSIFICATION = "classification";
    private static final String FILES = "files";
    private static final String DOCUMENTS_PATH = "/documents";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String USER_ID = "user-id";
    public static final String USER_ROLES = "user-roles";
    public static final String ROLES = "roles";
    private final String dmUri;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public DocumentUploadClientApi(
        @Value("${document_management.url}") final String dmUri,
        final RestTemplate restTemplate,
        final ObjectMapper objectMapper
    ) {
        this.dmUri = dmUri;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public UploadResponse upload(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuth,
        @RequestHeader(USER_ID) String userId,
        @RequestPart List<MultipartFile> files
    ) {
        try {
            MultiValueMap<String, Object> parameters = prepareRequest(files);
            HttpHeaders httpHeaders = setHttpHeaders(authorisation, serviceAuth, userId);

            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(
                parameters, httpHeaders
            );

            final String t = restTemplate.postForObject(dmUri + DOCUMENTS_PATH, httpEntity, String.class);
            return objectMapper.readValue(t, UploadResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public UploadResponse upload(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuth,
        @RequestHeader(USER_ID) String userId,
        @RequestPart List<MultipartFile> files,
        @RequestPart List<String> roles,
        @RequestHeader("user-roles") String userRoles
    ) {
        try {
            MultiValueMap<String, Object> parameters = prepareRequest(files);
            parameters.add(ROLES,roles);

            HttpHeaders httpHeaders = setHttpHeaders(authorisation, serviceAuth, userId);
            httpHeaders.add(USER_ROLES, userRoles);

            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(
                parameters, httpHeaders
            );

            final String t = restTemplate.postForObject(dmUri + DOCUMENTS_PATH, httpEntity, String.class);
            return objectMapper.readValue(t, UploadResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpHeaders setHttpHeaders(String authorizationToken,
                                       String serviceAuth,
                                       String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, authorizationToken);
        headers.add(SERVICE_AUTHORIZATION, serviceAuth);
        headers.add(USER_ID, userId);

        headers.set(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA_VALUE);

        return headers;
    }



    private static MultiValueMap<String, Object> prepareRequest(List<MultipartFile> files) {
        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        files.stream()
            .map(DocumentUploadClientApi::buildPartFromFile)
            .forEach(file -> parameters.add(FILES, file));
        parameters.add(CLASSIFICATION, Classification.RESTRICTED.name());
        return parameters;
    }

    private static HttpEntity<Resource> buildPartFromFile(MultipartFile file) {
        return new HttpEntity<>(buildByteArrayResource(file), buildPartHeaders(file));
    }

    private static HttpHeaders buildPartHeaders(MultipartFile file) {
        requireNonNull(file.getContentType());
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(file.getContentType()));
        return headers;
    }

    private static ByteArrayResource buildByteArrayResource(MultipartFile file) {
        try {
            return new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
        } catch (IOException ioException) {
            throw new IllegalStateException(ioException);
        }
    }
}
