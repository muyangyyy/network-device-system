package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.network.device.entity.NotificationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMessageMapper extends BaseMapper<NotificationMessage> {

    // 已移除 selectByReceiverId（不分页的「取全部」版本）：
    // 与下面的 selectPageByReceiverId 语义重叠、易混用，且已无调用方。

    /**
     * 分页查询某用户的通知。
     *
     * <p>{@code IPage} 必须是方法的第一个参数，且 SQL 里**不能**写 LIMIT ——
     * 分页由 {@code PaginationInnerInterceptor}（已在 MybatisPlusConfig 中注册）自动改写 SQL 并回填 total。
     */
    @Select("SELECT * FROM notification_message WHERE receiver_id = #{receiverId} AND deleted = 0 ORDER BY created_at DESC")
    IPage<NotificationMessage> selectPageByReceiverId(IPage<NotificationMessage> page, @Param("receiverId") Long receiverId);

    @Select("SELECT COUNT(*) FROM notification_message WHERE receiver_id = #{receiverId} AND read_status = 0 AND deleted = 0")
    Long countUnread(Long receiverId);

    /**
     * 全部标记为已读。
     *
     * <p><b>必须用 {@code @Update}，不能用 {@code @Select}。</b>MyBatis 按注解决定
     * {@code SqlCommandType} 并据此选择执行路径：写成 {@code @Select} 时该语句会被当成查询
     * 走 {@code Executor.query()}，导致两个后果：
     * <ol>
     *   <li>返回的是空结果集，而方法声明为基本类型 {@code int}，
     *       映射阶段会因「试图从空结果返回 null 给基本类型」而抛异常；</li>
     *   <li>查询路径不会清理一级（SqlSession 级）缓存，同一事务内先调用的
     *       {@code countUnread()} 结果会一直保持旧值——即「标记已读后未读数不变」。</li>
     * </ol>
     */
    @Update("UPDATE notification_message SET read_status = 1, read_time = NOW() WHERE receiver_id = #{receiverId} AND read_status = 0 AND deleted = 0")
    int markAllAsRead(Long receiverId);
}
