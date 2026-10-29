package com.example.ungdungchiasecaycanh.ui.community;

import com.example.ungdungchiasecaycanh.model.Post;

public class CommunityPostItem {
    public final Post post;
    public final String authorName;
    public final boolean liked;
    public final int likeCount;
    public final boolean likeInProgress;
    /** Author's avatar ref ("" = initials). */
    public final String authorAvatar;
    /** Author's pick-up area from their profile ("" when unknown). */
    public final String authorArea;
    /** Comments on the post, replies included; 0 when the count could not be read. */
    public final int commentCount;

    public CommunityPostItem(Post post, String authorName, boolean liked, int likeCount, boolean progress) {
        this(post, authorName, liked, likeCount, progress, "");
    }

    public CommunityPostItem(Post post, String authorName, boolean liked, int likeCount, boolean progress,
                             String authorAvatar) {
        this(post, authorName, liked, likeCount, progress, authorAvatar, "", 0);
    }

    public CommunityPostItem(Post post, String authorName, boolean liked, int likeCount, boolean progress,
                             String authorAvatar, String authorArea, int commentCount) {
        this.authorAvatar = authorAvatar == null ? "" : authorAvatar;
        this.post = post;
        this.authorName = authorName;
        this.liked = liked;
        this.likeCount = likeCount;
        this.likeInProgress = progress;
        this.authorArea = authorArea == null ? "" : authorArea;
        this.commentCount = Math.max(0, commentCount);
    }

    CommunityPostItem withLike(boolean newLiked, int count, boolean progress) {
        return new CommunityPostItem(post, authorName, newLiked, count, progress, authorAvatar, authorArea, commentCount);
    }
}
