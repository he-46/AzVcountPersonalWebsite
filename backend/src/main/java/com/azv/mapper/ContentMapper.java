package com.azv.mapper;

import com.azv.entity.Content;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ContentMapper extends BaseMapper<Content> {

    /**
     * 原子增加公开帖子的浏览数。状态和类型条件同时放在 UPDATE 中，避免校验后
     * 内容被下架的竞态窗口。
     */
    @Update("""
            UPDATE content
            SET view_count = COALESCE(view_count, 0) + 1
            WHERE id = #{id} AND status = 'APPROVED' AND type <> 'COMMENT'
            """)
    int incrementViewCount(@Param("id") Long id);

    /** 原子增加已审核内容的点赞数。 */
    @Update("""
            UPDATE content
            SET like_count = COALESCE(like_count, 0) + 1
            WHERE id = #{id} AND status = 'APPROVED' AND type <> 'COMMENT'
            """)
    int incrementLikeCount(@Param("id") Long id);

    @Select("SELECT like_count FROM content WHERE id = #{id}")
    Integer selectLikeCount(@Param("id") Long id);
}
