package com.scratch.community.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.user.entity.UserFollow;
import com.scratch.community.module.user.vo.UserVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户关注 Mapper
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /**
     * INSERT IGNORE 关注关系（原子操作，避免竞态）
     *
     * @param followerId 关注者 ID
     * @param followingId 被关注者 ID
     * @return 实际插入行数（0=已关注，1=新关注）
     */
    @Insert("INSERT IGNORE INTO user_follow (follower_id, following_id) VALUES (#{followerId}, #{followingId})")
    int insertIgnore(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    /**
     * 查询关注列表（我关注的人）
     */
    @Select("SELECT u.id, u.username, u.nickname, u.avatar_url, u.bio, u.role " +
            "FROM user_follow f JOIN user u ON f.following_id = u.id " +
            "WHERE f.follower_id = #{userId} AND u.deleted = 0 " +
            "ORDER BY f.created_at DESC")
    List<UserVO> selectFollowing(@Param("userId") Long userId);

    /**
     * 查询粉丝列表（关注我的人）
     */
    @Select("SELECT u.id, u.username, u.nickname, u.avatar_url, u.bio, u.role " +
            "FROM user_follow f JOIN user u ON f.follower_id = u.id " +
            "WHERE f.following_id = #{userId} AND u.deleted = 0 " +
            "ORDER BY f.created_at DESC")
    List<UserVO> selectFollowers(@Param("userId") Long userId);
}
