package com.azv.controller;

import com.azv.entity.Content;
import com.azv.entity.enums.ContentStatus;
import com.azv.entity.enums.ContentType;
import com.azv.mapper.ContentImageMapper;
import com.azv.mapper.ContentMapper;
import com.azv.mapper.ReportMapper;
import com.azv.security.ClientIpResolver;
import com.azv.service.SensitiveWordService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicInteractionControllerTest {
    private final ContentMapper contents = mock(ContentMapper.class);
    private final ClientIpResolver ips = mock(ClientIpResolver.class);

    @SuppressWarnings("unchecked")
    private StringRedisTemplate rateLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(any(String.class))).thenReturn(1L);
        return redis;
    }

    private Content approvedPost() {
        Content content = new Content();
        content.setId(7L);
        content.setType(ContentType.POST);
        content.setStatus(ContentStatus.APPROVED);
        return content;
    }

    @Test
    void commentAcceptsJsonAndRemainsPending() throws Exception {
        when(contents.selectById(7L)).thenReturn(approvedPost());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CommentController(
                contents, mock(SensitiveWordService.class), rateLimit(), ips)).build();

        mvc.perform(post("/api/comment").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentId\":7,\"text\":\"hello\",\"nickname\":\"guest\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<Content> saved = ArgumentCaptor.forClass(Content.class);
        verify(contents).insert(saved.capture());
        assertEquals(ContentStatus.PENDING, saved.getValue().getStatus());
        assertEquals("hello", saved.getValue().getBody());
    }

    @Test
    void reportAcceptsJson() throws Exception {
        when(contents.selectById(7L)).thenReturn(approvedPost());
        ReportMapper reports = mock(ReportMapper.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ReportController(
                contents, reports, rateLimit(), ips)).build();

        mvc.perform(post("/api/report").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentId\":7,\"reason\":\"needs review\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<com.azv.entity.Report> saved = ArgumentCaptor.forClass(com.azv.entity.Report.class);
        verify(reports).insert(saved.capture());
        assertEquals("needs review", saved.getValue().getReason());
    }

    @Test
    void commentsArePagedAndPageSizeIsCapped() throws Exception {
        when(contents.selectById(7L)).thenReturn(approvedPost());
        Content comment = new Content();
        comment.setId(11L);
        comment.setBody("approved");
        Page<Content> page = new Page<>(2, 50, 101);
        page.setRecords(List.of(comment));
        when(contents.selectPage(any(Page.class), any())).thenReturn(page);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ContentController(
                contents, mock(ContentImageMapper.class))).build();

        mvc.perform(get("/api/content/7/comments").param("page", "2").param("size", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].body").value("approved"))
                .andExpect(jsonPath("$.data.pages").value(3));

        verify(contents).selectPage(argThat(requested -> requested.getSize() == 50
                && requested.getCurrent() == 2), any());
    }
}
