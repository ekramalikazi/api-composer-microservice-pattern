package com.samples.demo.repo;

import com.samples.demo.model.Post;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PostRepository extends ReactiveCrudRepository<Post, Integer> {

}