package com.scratch.community.module.social.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.idempotent.Idempotent;
import com.scratch.community.common.result.R;
import com.scratch.community.module.social.dto.AddCommentDTO;
import com.scratch.community.module.social.service.FeedService;
import com.scratch.community.module.social.service.RankService;
import com.scratch.community.module.social.service.SocialService;
import com.scratch.community.module.social.vo.CommentVO;
import com.scratch.community.module.social.vo.FeedVO;
import com.scratch.community.module.social.vo.RankVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 社区 API
 */
@Tag(name = "社区", description = "点赞/评论/Feed/排行榜")
@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final FeedService feedService;
    private final RankService rankService;

    // ==================== 点赞 ====================

    /**
     * 点赞项目
     * 仅在实际新增点赞时更新排行榜分数
     */
    @Operation(summary = "点赞项目")
    @Idempotent
    @PostMapping("/project/{id}/like")
    public R<Boolean> like(@PathVariable Long id) {
        Long userId = LoginUser.getUserId();
        boolean created = socialService.like(userId, id);
        if (created) {
            rankService.incrementLikeScore(userId, 1);
        }
        return R.ok(created);
    }

    /**
     * 取消点赞
     * 仅在实际取消时更新排行榜分数
     */
    @Operation(summary = "取消点赞")
    @DeleteMapping("/project/{id}/like")
    public R<Boolean> unlike(@PathVariable Long id) {
        Long userId = LoginUser.getUserId();
        boolean deleted = socialService.unlike(userId, id);
        if (deleted) {
            rankService.incrementLikeScore(userId, -1);
        }
        return R.ok(deleted);
    }

    /**
     * 检查是否已点赞
     */
    @Operation(summary = "检查是否已点赞")
    @GetMapping("/project/{id}/liked")
    public R<Boolean> isLiked(@PathVariable Long id) {
        return R.ok(socialService.isLiked(LoginUser.getUserId(), id));
    }

    // ==================== 评论 ====================

    /**
     * 添加评论
     */
    @Operation(summary = "添加评论")
    @Idempotent
    @PostMapping("/comment")
    public R<CommentVO> addComment(@Valid @RequestBody AddCommentDTO dto) {
        return R.ok(socialService.addComment(LoginUser.getUserId(), dto));
    }

    /**
     * 删除评论
     */
    @Operation(summary = "删除评论")
    @DeleteMapping("/comment/{id}")
    public R<Void> deleteComment(@PathVariable Long id) {
        LoginUser loginUser = LoginUser.get();
        String role = loginUser != null ? loginUser.getRole() : "STUDENT";
        socialService.deleteComment(LoginUser.getUserId(), id, role);
        return R.ok();
    }

    /**
     * 获取项目评论列表（分页）
     */
    @Operation(summary = "获取项目评论列表")
    @GetMapping("/project/{id}/comments")
    public R<?> getComments(@PathVariable Long id,
                            @RequestParam(defaultValue = "1") @Min(1) int page,
                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(socialService.getComments(id, new Page<>(page, size)));
    }

    // ==================== Feed 流 ====================

    /**
     * 社区项目列表（最新/最热）
     * 未登录用户也可浏览，isLiked 为 false
     */
    @Operation(summary = "社区项目列表")
    @GetMapping("/feed")
    public R<?> feed(@RequestParam(defaultValue = "latest") String sort,
                     @RequestParam(defaultValue = "1") @Min(1) int page,
                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Long userId = null;
        try {
            userId = LoginUser.getUserId();
        } catch (Exception e) {
            // 未登录用户，userId 为 null
        }
        return R.ok(feedService.getFeed(sort, userId, new Page<>(page, size)));
    }

    /**
     * 全文搜索项目（使用 MySQL FULLTEXT 索引）
     */
    @Operation(summary = "全文搜索项目")
    @GetMapping("/search")
    public R<?> search(@RequestParam String q,
                       @RequestParam(defaultValue = "1") @Min(1) int page,
                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        if (q == null || q.trim().isEmpty()) {
            return R.ok(new Page<FeedVO>(page, size));
        }
        Long userId = null;
        try {
            userId = LoginUser.getUserId();
        } catch (Exception e) {
            // 未登录用户
        }
        return R.ok(feedService.search(q.trim(), userId, new Page<>(page, size)));
    }

    // ==================== 排行榜 ====================

    /**
     * 周点赞排行榜
     */
    @Operation(summary = "周点赞排行榜")
    @GetMapping("/rank/like/weekly")
    public R<List<RankVO>> weeklyLikeRank(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int topN) {
        return R.ok(rankService.getWeeklyLikeRank(topN));
    }

    /**
     * 月点赞排行榜
     */
    @Operation(summary = "月点赞排行榜")
    @GetMapping("/rank/like/monthly")
    public R<List<RankVO>> monthlyLikeRank(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int topN) {
        return R.ok(rankService.getMonthlyLikeRank(topN));
    }

    /**
     * 周作品排行榜
     */
    @Operation(summary = "周作品排行榜")
    @GetMapping("/rank/project/weekly")
    public R<List<RankVO>> weeklyProjectRank(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int topN) {
        return R.ok(rankService.getWeeklyProjectRank(topN));
    }
}
