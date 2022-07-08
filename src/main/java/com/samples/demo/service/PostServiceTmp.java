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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//@Service
@RequiredArgsConstructor
public class PostServiceTmp {

    private final PostRepository posts;

    private final TweetRepository tweets;
    //private final TweetNormalRepository tweetNormalRepository;

    public Flux<Result> findAll(long page, long size) {
        Flux<Post> allPosts = posts.findAll().skip(page * size)
                .take(size);

        Flux<Integer> postIds = allPosts.flatMap(post -> {
            return Flux.fromIterable(List.of(post.getId()));
        });

        Flux<Tweet> intersectTweets = tweets.findAllById(postIds);

        List<Integer> postIdStream = new ArrayList<>();
        allPosts.map(Post::getId)
                .collectList()
                .subscribe(postIdStream::addAll);

        //Iterable<Tweet> allTweetsByPostId = tweetNormalRepository.findAllById(postIdStream);
        /*Flux<Result> resultFlux = intersectTweets.zipWith(allPosts, (t, p) ->
                Result.builder()
                        .postId(p.getId())
                        .postTitle(p.getTitle())
                        .postContent(p.getContent())
                        .tweetId(t.getId())
                        .tweetTitle(t.getTitle())
                        .tweetDescription(t.getDescription())
                        .build()

        );*/

        /*Flux<Result> resultFlux = Flux.zip(intersectTweets, allPosts)
                .map(listTuple -> {
                    List<Result> resultingList = new ArrayList<>();
                    if (listTuple.getT1().getId().equals(listTuple.getT2().getId())) {
                        Result r = Result.builder()
                                .postId(listTuple.getT2().getId())
                                .postTitle(listTuple.getT2().getTitle())
                                .postContent(listTuple.getT2().getContent())
                                .tweetId(listTuple.getT1().getId())
                                .tweetTitle(listTuple.getT1().getTitle())
                                .tweetDescription(listTuple.getT1().getDescription())
                                .build();
                        resultingList.add(r);
                    }
                    return resultingList;
                }).flatMapIterable(list -> list);*/


        List<Tweet> tweetStream = new ArrayList<>();
        intersectTweets.collectList().subscribe(tweetStream::addAll);

        Map<Integer, Tweet> tweetsMap = tweetStream
                .stream()
                .collect(Collectors.toMap(tweet -> tweet.getId(), tweet -> tweet));

        /*Map<Integer, Tweet> tweetsMap = new HashMap<>();
        Iterator<Tweet> iterator = allTweetsByPostId.iterator();
        while (iterator.hasNext()) {
            Tweet t = iterator.next();
            tweetsMap.put(t.getId(), t);
        }*/

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
                    //.log()
                    .subscribe();
        }, FluxSink.OverflowStrategy.BUFFER);

        return objectFlux;

    }
}
