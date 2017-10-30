package uk.gov.hmcts.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.document.exception.TemporaryStoreFailureException;

import java.io.IOException;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UploadServiceTest {

    @InjectMocks
    private UploadService uploadService = spy(new UploadService());

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MockMultipartFile textFileMultipart;

    @Before
    public void before() {
        ReflectionTestUtils.setField(uploadService, "documentManagementServiceURL", "givenUrl");
    }

    @Test
    public void shouldUploadFileAndReturnFileUrlWithMetadataWhenValidInputsArePassed() throws Exception {
        MockMultipartFile textFileMultipart = mockMultipartFile();

        String hateoasResponse = new String(readAllBytes(get("src/test/resources/fileuploadresponse.txt")));

        ObjectNode objectNode = (ObjectNode) new ObjectMapper().readTree(hateoasResponse);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), any())).thenReturn(objectNode);

        assertThat(uploadService.uploadFiles(asList(mockMultipartFile()), "AAAAA", "123333"),
            contains(allOf(hasProperty("fileUrl", containsString("http://localhost:8080/documents/6")),
                hasProperty("fileName", containsString("JDP.pdf")),
                hasProperty("createdBy", containsString("testuser")),
                hasProperty("createdOn", containsString("2017-09-01T13:12:36.862+0000")),
                hasProperty("lastModifiedBy", containsString("testuser")),
                hasProperty("modifiedOn", containsString("2017-09-01T13:12:36.860+0000")),
                hasProperty("mimeType", containsString(MediaType.APPLICATION_PDF_VALUE)),
                hasProperty("status", is(HttpStatus.OK)))));

        verifyInteractionForFileUpload(textFileMultipart);

    }

    @Test(expected = HttpClientErrorException.class)
    public void shouldNotUploadFileAndThrowExceptionWhenAuthorizationTokenIsInvalid() throws Exception {
        MockMultipartFile textFileMultipart = mockMultipartFile();

        when(restTemplate.postForObject(anyString(), any(), any()))
            .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        uploadService.uploadFiles(asList(textFileMultipart), "AAAAA", "123333");

        verifyInteractionForFileUpload(textFileMultipart);
    }

    @Test(expected = TemporaryStoreFailureException.class)
    public void shouldNotUploadFileAndThrowExceptionWhenExeptionIsThrownWhileRetrievingFileBytes() throws Exception {
        when(textFileMultipart.getBytes()).thenThrow(new IOException());

        uploadService.uploadFiles(asList(textFileMultipart), "AAAAA", "123333");

        verify(textFileMultipart).getBytes();

        verifyNoMoreInteractions(textFileMultipart);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldInvokeCircuitBreakerFallbackAndReturnServiceNotAvailableStatus() throws Exception {
        MockMultipartFile textFileMultipart = mockMultipartFile();

        UploadService uploadServiceImpl = new UploadService();

        assertThat(uploadServiceImpl.serviceUnavailable(asList(textFileMultipart),
            "AAAAA", "123333"),
            contains(allOf(hasProperty("status", is(HttpStatus.SERVICE_UNAVAILABLE)))));
    }

    private void verifyInteractionForFileUpload(MockMultipartFile textFileMultipart) {
        verify(restTemplate).postForObject(anyString(),
            Mockito.<HttpEntity<MultiValueMap<String, Object>>>any(), any());

        verifyNoMoreInteractions(restTemplate);
    }

    private MockMultipartFile mockMultipartFile() {
        return new MockMultipartFile("file", "JDP.pdf", "application/pdf",
            "This is a test pdf file".getBytes());
    }

}