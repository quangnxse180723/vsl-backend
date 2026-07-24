package com.vslbackend;

import com.vslbackend.dto.request.user.UserCreateBlogRequest;
import com.vslbackend.entity.Blog;
import com.vslbackend.entity.BlogStatus;
import com.vslbackend.entity.User;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.*;
import com.vslbackend.service.GeminiModerationService;
import com.vslbackend.service.MinioService;
import com.vslbackend.service.impl.BlogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Kiem thu don vi cho luong chan dang TRUNG LAP noi dung khi PUBLISHED.
 * Tap trung vao logic moi (checkNotDuplicatePublishedOrThrow) - mock toan bo repository.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlogDuplicateContentTest {

    @Mock private BlogRepository blogRepository;
    @Mock private UserRepository userRepository;
    @Mock private MinioService minioService;
    @Mock private GeminiModerationService geminiModerationService;
    @Mock private BlogLikeRepository blogLikeRepository;
    @Mock private BlogCommentRepository blogCommentRepository;
    @Mock private BlogReportRepository blogReportRepository;
    @Mock private BlogShareRepository blogShareRepository;
    @Mock private UserFollowRepository userFollowRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private ReplyLikeRepository replyLikeRepository;
    @Mock private CommentReplyRepository commentReplyRepository;
    @Mock private BlogNotificationRepository blogNotificationRepository;

    @InjectMocks private BlogServiceImpl blogService;

    private static final Long AUTHOR_ID = 1L;

    private UserCreateBlogRequest publishRequest(String title, String content) {
        UserCreateBlogRequest req = new UserCreateBlogRequest();
        req.setTitle(title);
        req.setContent(content);
        req.setStatus("PUBLISHED");
        return req;
    }

    private void stubAuthorAndEmptyReports(List<Blog> published) {
        when(userRepository.findById(AUTHOR_ID))
                .thenReturn(Optional.of(User.builder().userId(AUTHOR_ID).username("alice").build()));
        when(blogReportRepository.findReportedBlogs()).thenReturn(List.of());
        when(blogRepository.findAllPublished()).thenReturn(published);
    }

    @Test
    void publish_whenExactDuplicateOfExistingPublished_shouldThrowDuplicate() {
        Blog existing = Blog.builder().id(99L)
                .title("Con chuot").content("Noi dung bai viet mau").status(BlogStatus.PUBLISHED).build();
        stubAuthorAndEmptyReports(List.of(existing));

        assertThatThrownBy(() ->
                blogService.createUserBlog(publishRequest("Con chuot", "Noi dung bai viet mau"), AUTHOR_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_DUPLICATE_CONTENT);
    }

    @Test
    void publish_whenDiffersOnlyByAccentsCaseWhitespace_shouldStillThrowDuplicate() {
        // Bai da co: co dau + hoa thuong khac + thua khoang trang -> van tinh la trung sau khi fold.
        Blog existing = Blog.builder().id(99L)
                .title("Con Chuột").content("Nội  dung   bài viết mẫu").status(BlogStatus.PUBLISHED).build();
        stubAuthorAndEmptyReports(List.of(existing));

        assertThatThrownBy(() ->
                blogService.createUserBlog(publishRequest("con chuot", "noi dung bai viet mau"), AUTHOR_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_DUPLICATE_CONTENT);
    }

    @Test
    void publish_whenContentDiffers_shouldNotThrowDuplicate() {
        // Cung tieu de nhung noi dung khac han -> KHONG bi chan trung; di tiep den kiem duyet AI.
        Blog existing = Blog.builder().id(99L)
                .title("Con chuot").content("Mot noi dung hoan toan khac").status(BlogStatus.PUBLISHED).build();
        stubAuthorAndEmptyReports(List.of(existing));
        // Cho AI tu choi de chung minh da vuot qua buoc kiem trung (khong nem BLOG_DUPLICATE_CONTENT).
        when(geminiModerationService.moderate(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new GeminiModerationService.ModerationResult(false, "khong dat"));

        assertThatThrownBy(() ->
                blogService.createUserBlog(publishRequest("Con chuot", "Noi dung bai viet mau"), AUTHOR_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_REJECTED);
    }

    @Test
    void publishAsDraft_shouldSkipDuplicateCheckEntirely() {
        // Nhap (DRAFT) trung noi dung voi bai da cong khai -> van luu binh thuong, khong kiem trung.
        when(userRepository.findById(AUTHOR_ID))
                .thenReturn(Optional.of(User.builder().userId(AUTHOR_ID).username("alice").build()));
        Blog saved = Blog.builder().id(5L).title("Con chuot").content("Noi dung bai viet mau")
                .status(BlogStatus.DRAFT).author(User.builder().userId(AUTHOR_ID).username("alice").build()).build();
        when(blogRepository.save(org.mockito.ArgumentMatchers.any(Blog.class))).thenReturn(saved);

        UserCreateBlogRequest draft = new UserCreateBlogRequest();
        draft.setTitle("Con chuot");
        draft.setContent("Noi dung bai viet mau");
        draft.setStatus("DRAFT");

        var response = blogService.createUserBlog(draft, AUTHOR_ID);

        assertThat(response.getStatus()).isEqualTo(BlogStatus.DRAFT);
    }
}
