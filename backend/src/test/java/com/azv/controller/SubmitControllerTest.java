package com.azv.controller;

import com.azv.common.BizException;
import com.azv.mapper.ContentImageMapper;
import com.azv.mapper.ContentMapper;
import com.azv.security.ClientIpResolver;
import com.azv.service.SensitiveWordService;
import com.azv.storage.FileStorageService;
import com.azv.storage.StoredImage;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubmitControllerTest {
    private final ContentMapper contents = mock(ContentMapper.class);
    private final ContentImageMapper images = mock(ContentImageMapper.class);
    private final SensitiveWordService words = mock(SensitiveWordService.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ClientIpResolver ips = mock(ClientIpResolver.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @SuppressWarnings("unchecked")
    private SubmitController controller() {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(any(String.class))).thenReturn(1L);
        when(ips.resolve(request)).thenReturn("127.0.0.1");
        return new SubmitController(contents, words, storage, redis, images, ips);
    }

    @Test
    void cleansEarlierImageWhenLaterStorageFails() throws Exception {
        SubmitController controller = controller();
        MockMultipartFile file = new MockMultipartFile("files", "image.png", "image/png", new byte[]{1});
        when(storage.store(file)).thenReturn(new StoredImage("/uploads/original.png", "/uploads/thumb.jpg"))
                .thenThrow(new BizException("broken image"));

        assertThrows(BizException.class, () -> controller.submit("title", "guest", "body", List.of(file, file), request));

        verify(storage).delete("/uploads/original.png");
        verify(storage).delete("/uploads/thumb.jpg");
        verifyNoInteractions(contents);
    }

    @Test
    void cleansImageWhenTransactionRollsBackAfterMethodReturns() throws Exception {
        SubmitController controller = controller();
        MockMultipartFile file = new MockMultipartFile("files", "image.png", "image/png", new byte[]{1});
        when(storage.store(file)).thenReturn(new StoredImage("/uploads/original.png", "/uploads/thumb.jpg"));
        TransactionSynchronizationManager.initSynchronization();
        try {
            controller.submit("title", "guest", "body", List.of(file), request);
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(storage).delete("/uploads/original.png");
        verify(storage).delete("/uploads/thumb.jpg");
    }
}
