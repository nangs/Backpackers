package com.yoloo.android.backend.api;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiClass;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.users.User;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import com.yoloo.android.backend.Constants;
import com.yoloo.android.backend.authenticator.FacebookAuthenticator;
import com.yoloo.android.backend.authenticator.GoogleAuthenticator;
import com.yoloo.android.backend.authenticator.YolooAuthenticator;
import com.yoloo.android.backend.controller.CommentController;
import com.yoloo.android.backend.model.comment.Comment;
import com.yoloo.android.backend.model.like.Like;
import com.yoloo.android.backend.model.question.Question;
import com.yoloo.android.backend.util.ClassUtil;
import com.yoloo.android.backend.validator.Validator;
import com.yoloo.android.backend.validator.rule.comment.CommentCreateRule;
import com.yoloo.android.backend.validator.rule.common.AllowedToOperate;
import com.yoloo.android.backend.validator.rule.common.AuthenticationRule;
import com.yoloo.android.backend.validator.rule.common.IdValidationRule;
import com.yoloo.android.backend.validator.rule.common.NotFoundRule;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;

import static com.yoloo.android.backend.service.OfyHelper.ofy;

@Api(
        name = "yolooApi",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = Constants.API_OWNER,
                ownerName = Constants.API_OWNER,
                packagePath = Constants.API_PACKAGE_PATH
        )
)
@ApiClass(
        resource = "comments",
        clientIds = {
                Constants.ANDROID_CLIENT_ID,
                Constants.IOS_CLIENT_ID,
                Constants.WEB_CLIENT_ID},
        audiences = {Constants.AUDIENCE_ID},
        authenticators = {
                GoogleAuthenticator.class,
                FacebookAuthenticator.class,
                YolooAuthenticator.class
        }
)
public class CommentEndpoint {

    private static final Logger logger = Logger.getLogger(CommentEndpoint.class.getSimpleName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    /**
     * Inserts a new {@code Comment}.
     */
    @ApiMethod(
            name = "questions.comments.add",
            path = "questions/{question_id}/comments",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Comment add(@Named("question_id") final String questionId,
                       @Named("text") final String text,
                       final User user) throws ServiceException {

        Validator.builder()
                .addRule(new IdValidationRule(questionId))
                .addRule(new AuthenticationRule(user))
                .addRule(new CommentCreateRule(text))
                .addRule(new NotFoundRule(Question.class, questionId))
                .validate();

        return CommentController.newInstance().add(questionId, text, user);
    }

    /**
     * Deletes the specified {@code Comment}.
     *
     * @param questionId the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code Comment}
     */
    @ApiMethod(
            name = "questions.comments.remove",
            path = "questions/{question_id}/comments/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("question_id") final String questionId,
                       @Named("id") final String commentId,
                       final User user) throws ServiceException {

        // Validate.
        Validator.builder()
                .addRule(new IdValidationRule(questionId))
                .addRule(new IdValidationRule(commentId))
                .addRule(new AuthenticationRule(user))
                .addRule(new NotFoundRule(Question.class, questionId))
                .addRule(new NotFoundRule(Comment.class, commentId))
                .addRule(new AllowedToOperate(Comment.class, commentId, user, "delete"))
                .validate();

        CommentController.newInstance().remove(questionId, commentId, user);
    }

    /**
     * List all entities.
     *
     * @param cursor used for pagination to determine which page to return
     * @param limit  the maximum number of entries to return
     * @return a response that encapsulates the result list and the next page token/cursor
     */
    @ApiMethod(
            name = "questions.comments.list",
            path = "questions/{question_id}/comments",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<Comment> list(@Named("question_id") final String questionId,
                                            @Nullable @Named("cursor") String cursor,
                                            @Nullable @Named("limit") Integer limit,
                                            final User user) throws ServiceException {
        Validator.builder()
                .addRule(new IdValidationRule(questionId))
                .addRule(new AuthenticationRule(user))
                .addRule(new NotFoundRule(Question.class, questionId))
                .validate();

        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<Comment> query = ofy().load().type(Comment.class).ancestor(Key.create(questionId));
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }

        query = query.limit(limit);

        // load comment like keys of the current parentUserKey.
        List<Like<Comment>> likes = ofy().load()
                .type(ClassUtil.<Like<Comment>>castClass(Like.class))
                .ancestor(Key.create(user.getUserId()))
                .list();

        QueryResultIterator<Comment> queryIterator = query.iterator();
        List<Comment> commentList = new ArrayList<>(limit);
        while (queryIterator.hasNext()) {
            commentList.add(queryIterator.next());
        }

        // Set likes
        for (Like<Comment> like : likes) {
            for (Comment comment : commentList) {
                if (like.getLikeableEntityKey().equivalent(comment.getKey())) {
                    comment.setLiked(true);
                }
            }
        }

        return CollectionResponse.<Comment>builder()
                .setItems(commentList)
                .setNextPageToken(queryIterator.getCursor().toWebSafeString())
                .build();
    }
}