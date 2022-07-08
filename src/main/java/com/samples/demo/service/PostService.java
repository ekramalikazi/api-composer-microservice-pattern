package com.samples.demo.service;

import com.samples.demo.common.Result;
import com.samples.demo.model.Post;
import com.samples.demo.model.Tweet;
import com.samples.demo.repo.PostRepository;
import com.samples.demo.repo.TweetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository posts;

    private final TweetRepository tweets;

    public Flux<Post> findAll2(long page, long size) {
        System.out.println("page = " + page + " size = " + size);
        return posts.findAll()
                .skip(page * size)
                .take(size);
    }

    public Flux<Object> findAll3(long page, long size) {
        System.out.println("page = " + page + " size = " + size);
        Flux<Post> allPosts = posts.findAll()
                .skip(page * size)
                .take(size);

        Flux<Integer> postIds = allPosts.flatMap(post -> {
            return Flux.fromIterable(List.of(post.getId()));
        });

        Flux<Tweet> intersectTweets = tweets.findAllById(postIds);

        List<Integer> postIdStream = new ArrayList<>();
        allPosts.map(Post::getId)
                .collectList()
                .subscribe(postIdStream::addAll);

        List<Tweet> tweetStream = new ArrayList<>();
        intersectTweets.collectList().subscribe(tweetStream::addAll);

        Map<Integer, Tweet> tweetsMap = tweetStream
                .stream()
                .collect(Collectors.toMap(tweet -> tweet.getId(), tweet -> tweet));

        return Flux.just(allPosts, tweetsMap);

    }
    public Flux<Result> findAll(long page, long size) {

        Flux<Post> allPosts = posts.findAll()
                .skip(page * size)
                .take(size);

        Flux<Integer> postIds = allPosts.flatMap(post -> {
            return Flux.fromIterable(List.of(post.getId()));
        });

        Flux<Tweet> intersectTweets = tweets.findAllById(postIds);

        List<Integer> postIdStream = new ArrayList<>();
        allPosts.map(Post::getId)
                .collectList()
                .subscribe(postIdStream::addAll);

        List<Tweet> tweetStream = new ArrayList<>();
        intersectTweets.collectList().subscribe(tweetStream::addAll);

        Map<Integer, Tweet> tweetsMap = tweetStream
                .stream()
                .collect(Collectors.toMap(tweet -> tweet.getId(), tweet -> tweet));


        Flux<Result> objectFlux = Flux.create(emitter -> {
            allPosts.delayElements(Duration.ofMillis(100))
                    .doOnNext(p -> {
                        if (tweetsMap.containsKey(p.getId())) {
                            Tweet t = tweetsMap.get(p.getId());
                            Result r = Result.builder()
                                    .postId(p.getId())
                                    .postTitle(p.getTitle())
                                    .postContent(p.getContent())
                                    .tweetId(t.getId())
                                    .tweetTitle(t.getTitle())
                                    .tweetDescription(t.getDescription())
                                    .build();
                            emitter.next(r);
                        }
                    })
                    .doOnComplete(() -> {
                        emitter.complete();
                        System.out.println("All the data are processed !!!");
                    })
                    .subscribe();
        }, FluxSink.OverflowStrategy.BUFFER);

        return objectFlux;

    }
}
