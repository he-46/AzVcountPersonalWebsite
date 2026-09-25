package com.azv.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /** 存储图片，返回原图+缩略图的访问 URL */
    StoredImage store(MultipartFile file) throws Exception;

    /** 物理删除（审核驳回时调用，URL 如 /uploads/2026/08/xxx.jpg） */
    void delete(String url);
}