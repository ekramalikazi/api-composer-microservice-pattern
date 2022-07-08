package com.samples.demo.repo;

import com.samples.demo.model.Post;
import com.samples.demo.model.Tweet;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface TweetRepository extends ReactiveCrudRepository<Tweet, Integer> {


}