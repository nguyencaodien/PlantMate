package com.example.ungdungchiasecaycanh.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Comment;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.example.ungdungchiasecaycanh.util.DiffList;
import java.util.Arrays;

/** Admin: edit a post's text and category, delete it, or delete individual comments. */
public class AdminPostEditActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "postId";

    private AdminPostEditViewModel viewModel;
    private String postId;
    private boolean formBound;
    private int shownSaved, shownDeleted, shownError;
    private TextInputLayout textLayout, noteLayout, imageLayout;
    private TextInputEditText text, note, image;
    private ChipGroup categoryGroup;
    private final CommentAdapter comments = new CommentAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_post_edit);
        SecondaryChrome.apply(this, findViewById(R.id.adminPost), findViewById(R.id.adminPostHero));
        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        ((TextView) findViewById(R.id.adminPostTitle)).setText(R.string.admin_edit_post);
        textLayout = findViewById(R.id.adminPostTextLayout);
        noteLayout = findViewById(R.id.adminPostNoteLayout);
        text = findViewById(R.id.adminPostText);
        note = findViewById(R.id.adminPostNote);
        categoryGroup = findViewById(R.id.adminPostCategory);
        imageLayout = findViewById(R.id.adminPostImageLayout);
        image = findViewById(R.id.adminPostImage);
        // Preview the link when the field loses focus; "Remove image" clears it.
        image.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) refreshPreview(); });
        View remove = findViewById(R.id.adminPostImageRemove);
        remove.setOnClickListener(v -> {
            image.setText("");
            refreshPreview();
        });
        // "Remove image" only makes sense while there is a link to remove.
        image.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                remove.setVisibility(s.toString().trim().isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        remove.setVisibility(AdminDialogs.text(image).isEmpty() ? View.GONE : View.VISIBLE);

        RecyclerView list = findViewById(R.id.adminPostComments);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(comments);

        MaterialButton save = findViewById(R.id.adminPostSave);
        MaterialButton delete = findViewById(R.id.adminPostDelete);
        findViewById(R.id.adminPostBack).setOnClickListener(v -> finish());
        findViewById(R.id.adminPostRetry).setOnClickListener(v -> viewModel.load(postId));
        save.setOnClickListener(v -> {
            boolean invalid = AdminDialogs.error(this, textLayout, AdminValidation.postText(AdminDialogs.text(text)));
            invalid |= AdminDialogs.error(this, imageLayout, AdminValidation.imageUrl(AdminDialogs.text(image)));
            String n = AdminDialogs.note(this, noteLayout, note);
            if (!invalid && n != null) {
                viewModel.save(AdminDialogs.text(text), selectedCategory(), AdminDialogs.text(image), n);
            }
        });
        delete.setOnClickListener(v -> AdminDialogs.confirm(this, R.string.admin_delete_confirm_title,
                R.string.admin_delete_post_body, R.string.admin_delete, () -> {
                    String n = AdminDialogs.note(this, noteLayout, note);
                    if (n != null) viewModel.deletePost(n);
                }));

        View scroll = findViewById(R.id.adminPostScroll);
        View loading = findViewById(R.id.adminPostLoading);
        View error = findViewById(R.id.adminPostError);
        viewModel = new ViewModelProvider(this).get(AdminPostEditViewModel.class);
        viewModel.getState().observe(this, state -> {
            boolean content = state.status == AdminEditState.Status.CONTENT;
            loading.setVisibility(state.status == AdminEditState.Status.LOADING ? View.VISIBLE : View.GONE);
            error.setVisibility(state.status == AdminEditState.Status.ERROR
                    || state.status == AdminEditState.Status.NOT_FOUND ? View.VISIBLE : View.GONE);
            scroll.setVisibility(content ? View.VISIBLE : View.GONE);
            save.setEnabled(!state.busy);
            delete.setEnabled(!state.busy);
            if (content && state.data != null) bind(state.data);
            if (state.savedVersion > shownSaved) {
                shownSaved = state.savedVersion;
                note.setText("");
                AdminDialogs.snack(scroll, R.string.admin_saved);
            }
            if (state.deletedVersion > shownDeleted) {
                shownDeleted = state.deletedVersion;
                Toast.makeText(this, R.string.admin_deleted, Toast.LENGTH_SHORT).show();
                finish();
            }
            if (state.errorVersion > shownError) {
                shownError = state.errorVersion;
                AdminDialogs.snack(scroll, R.string.admin_save_error);
            }
        });
        viewModel.load(postId);
    }

    private void bind(AdminPostEditViewModel.Data data) {
        Post post = data.post;
        String author = data.authorName.isEmpty() ? getString(R.string.detail_member_fallback) : data.authorName;
        String when = post.getCreatedAt() == null ? "" : String.valueOf(DateUtils.getRelativeTimeSpanString(
                post.getCreatedAt().toDate().getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        ((TextView) findViewById(R.id.adminPostAuthor)).setText(getString(R.string.admin_author, author, when));
        ((TextView) findViewById(R.id.adminPostCommentsTitle)).setText(
                getString(R.string.admin_comments_count, data.comments.size()));
        comments.submit(data.comments, data.names);
        if (formBound) return;
        formBound = true;
        text.setText(post.getText());
        image.setText(post.getImageUrl());
        refreshPreview();
        PostCategory c = post.getCategory();
        categoryGroup.check(c == PostCategory.SHARING ? R.id.adminCategorySharing
                : c == PostCategory.QUESTION ? R.id.adminCategoryQuestion
                : c == PostCategory.PLANT_WIN ? R.id.adminCategoryWin
                : c == PostCategory.TIP ? R.id.adminCategoryTip : R.id.adminCategoryGeneral);
        // A post that shares a listing must stay in Sharing (Firestore rule).
        boolean locked = !post.getListingId().isEmpty();
        for (int i = 0; i < categoryGroup.getChildCount(); i++) categoryGroup.getChildAt(i).setEnabled(!locked);
    }

    private void refreshPreview() {
        String url = AdminDialogs.text(image);
        boolean valid = !url.isEmpty() && AdminValidation.imageUrl(url) == 0;
        imageLayout.setError(url.isEmpty() || valid ? null : getString(AdminValidation.imageUrl(url)));
        ImageView preview = findViewById(R.id.adminPostImagePreview);
        RemoteImageLoader.load(preview, valid ? url : "",
                getResources().getDisplayMetrics().widthPixels);
    }

    private PostCategory selectedCategory() {
        int id = categoryGroup.getCheckedChipId();
        return id == R.id.adminCategorySharing ? PostCategory.SHARING
                : id == R.id.adminCategoryQuestion ? PostCategory.QUESTION
                : id == R.id.adminCategoryWin ? PostCategory.PLANT_WIN
                : id == R.id.adminCategoryTip ? PostCategory.TIP : PostCategory.GENERAL;
    }

    private void confirmDeleteComment(Comment comment) {
        AdminDialogs.confirm(this, R.string.admin_delete_confirm_title, R.string.admin_delete_comment_body,
                R.string.admin_delete, () -> {
                    String n = AdminDialogs.note(this, noteLayout, note);
                    if (n != null) viewModel.deleteComment(comment.getCommentId(), n);
                });
    }

    private final class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.Holder> {
        private Map<String, String> names;
        private final DiffList<Comment> items = new DiffList<>(this, Comment::getCommentId, c -> Arrays.asList(
                c.getText(), c.getReplyToCommentId(), names == null ? null : names.get(c.getAuthorId())));

        void submit(List<Comment> values, Map<String, String> names) {
            this.names = names;
            items.submit(values);
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_comment, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Comment c = items.get(position);
            String name = names == null ? null : names.get(c.getAuthorId());
            h.author.setText(name == null || name.isEmpty() ? getString(R.string.detail_member_fallback) : name);
            h.text.setText(c.getText());
            h.itemView.setPaddingRelative(c.getReplyToCommentId().isEmpty() ? h.basePadding : h.basePadding * 3,
                    h.itemView.getPaddingTop(), h.itemView.getPaddingEnd(), h.itemView.getPaddingBottom());
            h.delete.setOnClickListener(v -> confirmDeleteComment(c));
        }

        @Override public int getItemCount() { return items.size(); }

        final class Holder extends RecyclerView.ViewHolder {
            final TextView author, text;
            final MaterialButton delete;
            final int basePadding;

            Holder(@NonNull View view) {
                super(view);
                author = view.findViewById(R.id.adminCommentAuthor);
                text = view.findViewById(R.id.adminCommentText);
                delete = view.findViewById(R.id.adminCommentDelete);
                basePadding = view.getPaddingStart();
            }
        }
    }
}
