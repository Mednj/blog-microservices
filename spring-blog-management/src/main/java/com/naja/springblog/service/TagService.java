package com.naja.springblog.service;

import java.util.Set;

import com.naja.springblog.exception.ResourceAlreadyExistException;
import com.naja.springblog.exception.ResourceNotFoundException;
import com.naja.springblog.exception.UnauthorizedException;
import com.naja.springblog.model.RoleName;
import com.naja.springblog.model.Tag;
import com.naja.springblog.payload.TitleRequest;
import com.naja.springblog.projection.PostSummary;
import com.naja.springblog.projection.TagsOrCategoriesResponse;
import com.naja.springblog.repository.PostRepository;
import com.naja.springblog.repository.TagRepository;
import com.naja.springblog.security.UserPrincipal;
import com.naja.springblog.util.SlugUtil;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final PostRepository postRepository;

    public TagService(TagRepository tagRepository, PostRepository postRepository) {
        this.tagRepository = tagRepository;
        this.postRepository = postRepository;
    }

    public Tag save(TitleRequest titleRequest) {
        boolean existsTag = tagRepository.existsTagByTitleIgnoreCase(titleRequest.getTitle());
        if (existsTag) {
            throw new ResourceAlreadyExistException("Tag", "title", titleRequest.getTitle());
        }
        Tag tag = new Tag();
        tag.addTag(titleRequest.getTitle());
        tag.setSlug(SlugUtil.makeSlug(titleRequest.getTitle()));
        return tagRepository.save(tag);
    }

    public Tag update(Long id, TitleRequest titleRequest, UserPrincipal currentUser) {
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));

        if (tag.getCreatedBy().equals(currentUser.getUsername())
                || currentUser.getAuthorities().contains(new SimpleGrantedAuthority(RoleName.ROLE_ADMIN.toString()))) {

            boolean existsTag = tagRepository.existsTagByTitleIgnoreCase(titleRequest.getTitle());
            if (existsTag) {
                throw new ResourceNotFoundException("Tag", "title", titleRequest.getTitle());
            }
            tag.addTag(titleRequest.getTitle());
            tag.setSlug(SlugUtil.makeSlug(titleRequest.getTitle()));
            return tagRepository.save(tag);
        }
        throw new UnauthorizedException("You don't have permission to update this Tag");
    }

    public Page<PostSummary> findById(Long id, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return postRepository.findPostsByTagId(id, pageable);
    }

    public void deleteById(Long id, UserPrincipal currentUser) {

        Tag tag = tagRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));

        if (tag.getCreatedBy().equals(currentUser.getUsername())
                || currentUser.getAuthorities().contains(new SimpleGrantedAuthority(RoleName.ROLE_ADMIN.toString()))) {
            tagRepository.deleteById(id);
        } else {
            throw new UnauthorizedException("You don't have permission to delete this Tag");
        }
    }

    public Set<TagsOrCategoriesResponse> findTags() {
        return tagRepository.findTags();
    }

    public Set<TagsOrCategoriesResponse> findAllByTitleAndLimitBy10(String title) {
        return tagRepository.findAllByTitleAndLimitBy10(title);
    }
}