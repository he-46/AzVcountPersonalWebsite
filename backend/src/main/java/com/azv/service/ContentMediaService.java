package com.azv.service;

import com.azv.entity.Content;
import com.azv.entity.ContentImage;
import com.azv.mapper.ContentImageMapper;
import com.azv.mapper.ContentMapper;
import com.azv.storage.FileStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 统一清理内容封面、多图文件、图片记录和失效 URL 引用。 */
@Service
@RequiredArgsConstructor
public class ContentMediaService {

    private final ContentMapper contentMapper;
    private final ContentImageMapper contentImageMapper;
    private final FileStorageService storageService;

    @Transactional
    public void deleteAll(Content content) {
        if (content == null || content.getId() == null) return;

        List<ContentImage> images = contentImageMapper.selectList(
                new LambdaQueryWrapper<ContentImage>()
                        .eq(ContentImage::getContentId, content.getId()));

        Set<String> urls = new LinkedHashSet<>();
        add(urls, content.getMediaUrl());
        add(urls, content.getThumbnailUrl());
        for (ContentImage image : images) {
            add(urls, image.getUrl());
            add(urls, image.getThumbnailUrl());
        }
        contentImageMapper.delete(new LambdaQueryWrapper<ContentImage>()
                .eq(ContentImage::getContentId, content.getId()));
        contentMapper.update(null, new LambdaUpdateWrapper<Content>()
                .eq(Content::getId, content.getId())
                .set(Content::getMediaUrl, null)
                .set(Content::getThumbnailUrl, null));

        // 文件系统不能随数据库事务回滚，必须等数据库提交成功后再物理删除。
        Runnable deleteFiles = () -> urls.forEach(storageService::delete);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFiles.run();
                }
            });
        } else {
            deleteFiles.run();
        }
    }

    private void add(Set<String> urls, String url) {
        if (url != null && !url.isBlank()) urls.add(url);
    }
}
