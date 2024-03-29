package com.naja.springblog.service;

import java.util.Set;

import com.naja.springblog.exception.ResourceNotFoundException;
import com.naja.springblog.exception.UnauthorizedException;
import com.naja.springblog.model.Comment;
import com.naja.springblog.model.Post;
import com.naja.springblog.model.RoleName;
import com.naja.springblog.model.User;
import com.naja.springblog.payload.CommentRequest;
import com.naja.springblog.projection.CommentResponse;
import com.naja.springblog.repository.CommentRepository;
import com.naja.springblog.repository.PostRepository;
import com.naja.springblog.repository.UserRepository;
import com.naja.springblog.security.UserPrincipal;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository,
            UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public Comment save(Long postId, CommentRequest commentRequest) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        Comment comment = new Comment();
        comment.addComment(commentRequest.getContent());
        comment.setPost(post);
        return commentRepository.save(comment);
    }

    public Comment addCommentToParentId(Long postId, Long parentId, CommentRequest commentRequest,
            UserPrincipal currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", postId));

        Comment comment = new Comment();

        User user = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", currentUser.getUsername()));

        comment.addComment(commentRequest.getContent());
        comment.setParentComment(parentComment);
        comment.setPost(post);
        comment.setUser(user);
        return commentRepository.save(comment);
    }

    public Comment update(Long commentId, CommentRequest commentRequest, UserPrincipal currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", commentId));

        if (comment.getUser().getId().equals(currentUser.getId())
                || currentUser.getAuthorities().contains(new SimpleGrantedAuthority(RoleName.ROLE_ADMIN.toString()))) {
            comment.addComment(commentRequest.getContent());
            return commentRepository.save(comment);
        } else {
            throw new UnauthorizedException("You don't have permission to update this post");
        }
    }

    public Set<CommentResponse> findById(Long id) {
        return commentRepository.findByParentCommentIsNullAndPostId(id);
    }

    public void deleteById(Long id, UserPrincipal currentUser) {

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        if (comment.getCreatedBy().equals(currentUser.getUsername())
                || currentUser.getAuthorities().contains(new SimpleGrantedAuthority(RoleName.ROLE_ADMIN.toString()))) {
            comment.deleteComment();
            commentRepository.save(comment);
        } else {
            throw new UnauthorizedException("You don't have permission to update this post");
        }
    }
}