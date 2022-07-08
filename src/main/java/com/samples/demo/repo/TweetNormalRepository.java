package com.samples.demo.repo;

import com.samples.demo.model.Tweet;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
public interface TweetNormalRepository extends PagingAndSortingRepository<Tweet, Integer> {


}