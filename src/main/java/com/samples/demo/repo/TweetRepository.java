package com.samples.demo.repo;

import com.samples.demo.model.Tweet;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TweetRepository extends ReactiveCrudRepository<Tweet, Integer> {


}