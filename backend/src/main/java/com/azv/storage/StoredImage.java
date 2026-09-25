package com.azv.storage;

/** record：不可变数据载体，自动生成构造器/getter/equals（省一堆样板代码） */
public record StoredImage(String url, String thumbnailUrl) {}