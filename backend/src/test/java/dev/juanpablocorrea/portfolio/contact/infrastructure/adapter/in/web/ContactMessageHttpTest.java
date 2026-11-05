package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web;

import dev.juanpablocorrea.portfolio.contact.application.service.ReceiveContactMessageService;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter.CorsPolicyFilter;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter.RateLimitFilter;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter.SecurityHeadersFilter;
import dev.juanpablocorrea.portfolio.contact.support.ContactFixtures;
import dev.juanpablocorrea.portfolio.contact.support.InMemoryContactMessageRepository;
import dev.juanpablocorrea.portfolio.contact.support.RecordingEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The HTTP contract end to end (filters, controller, error handler and the real use case),
 * with in-memory fakes instead of a database. No Spring context is started, so it runs in milliseconds.
 */
class ContactMessageHttpTest {

    private static final String URL = "/api/v1/contact-messages";
    private static final String ORIGIN = "https://juanpablo.github.io";

    private MockMvc mvc;
    private InMemoryContactMessageRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryContactMessageRepository();
        ReceiveContactMessageService useCase = new ReceiveContactMessageService(
                repository, new RecordingEventPublisher(), ContactFixtures.fixedClock());
        mvc = MockMvcBuilders.standaloneSetup(new ContactMessageController(useCase))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(
                        new SecurityHeadersFilter(),
                        new CorsPolicyFilter(List.of(ORIGIN)),
                        new RateLimitFilter(2, 100, Duration.ofMinutes(1), 0))
                .build();
    }

    private MockHttpServletResponse postJson(String json) throws Exception {
        return mvc.perform(post(URL)
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn().getResponse();
    }

    @Test
    void acceptsAValidMessageWith201AndAReceipt() throws Exception {
        MockHttpServletResponse response = postJson(ContactFixtures.validJson(ContactFixtures.ID));

        assertEquals(201, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"id\":\"" + ContactFixtures.ID + "\""));
        assertEquals(1, repository.size());
        assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
    }

    @Test
    void resendingTheSameSubmissionDoesNotDuplicateIt() throws Exception {
        postJson(ContactFixtures.validJson(ContactFixtures.ID));
        MockHttpServletResponse retry = postJson(ContactFixtures.validJson(ContactFixtures.ID));

        assertEquals(201, retry.getStatus());
        assertEquals(1, repository.size());
    }

    @Test
    void answersInvalidDataWith422AndEveryFieldToFix() throws Exception {
        MockHttpServletResponse response = postJson(
                "{\"submissionId\":\"" + ContactFixtures.ID + "\",\"name\":\"A\",\"email\":\"mal\","
                        + "\"reason\":\"oferta\",\"message\":\"corto\",\"consent\":false}");

        assertEquals(422, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"field\":\"name\""));
        assertTrue(body.contains("\"field\":\"email\""));
        assertTrue(body.contains("\"field\":\"message\""));
        assertTrue(body.contains("\"field\":\"consent\""));
        assertEquals(0, repository.size());
    }

    @Test
    void answers400WhenTheBodyIsNotJson() throws Exception {
        MockHttpServletResponse response = postJson("esto no es json");

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
    }

    @Test
    void otherMethodsGetA405WithoutInternalDetails() throws Exception {
        MockHttpServletResponse response = mvc.perform(get(URL)).andReturn().getResponse();

        assertEquals(405, response.getStatus());
        assertTrue(!response.getContentAsString().contains("Exception"));
    }

    @Test
    void thePreflightFromThePortfolioIsAnswered() throws Exception {
        MockHttpServletResponse response = mvc.perform(options(URL)
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andReturn().getResponse();

        assertEquals(204, response.getStatus());
        assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
        assertNotNull(response.getHeader("Access-Control-Allow-Methods"));
    }

    @Test
    void thePreflightFromAnotherSiteIsRefused() throws Exception {
        MockHttpServletResponse response = mvc.perform(options(URL)
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andReturn().getResponse();

        assertEquals(403, response.getStatus());
        assertEquals(null, response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void tooManyRequestsFromOneClientGetA429WithRetryAfterAndCorsHeaders() throws Exception {
        postJson(ContactFixtures.validJson("11111111-1111-4111-8111-111111111111"));
        postJson(ContactFixtures.validJson("22222222-2222-4222-8222-222222222222"));

        MockHttpServletResponse blocked = postJson(ContactFixtures.validJson("33333333-3333-4333-8333-333333333333"));

        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
        assertEquals(ORIGIN, blocked.getHeader("Access-Control-Allow-Origin"));
        assertEquals(2, repository.size());
    }
}
