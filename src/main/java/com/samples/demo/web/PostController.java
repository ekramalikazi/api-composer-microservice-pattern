package com.samples.demo.web;

import com.samples.demo.common.Result;
import com.samples.demo.model.Post;
import com.samples.demo.model.Tweet;
import com.samples.demo.repo.PostRepository;
import com.samples.demo.repo.TweetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/posts")
@RequiredArgsConstructor
@Slf4j
class PostController {

    private final PostRepository posts;

    private final TweetRepository tweets;

    @GetMapping
    public Flux<Post> all() {
        return this.posts.findAll();
    }

    @PostMapping
    public Mono<Post> create(@RequestBody Post post) {
        return this.posts.save(post);
    }

    @GetMapping("/{id}")
    public Mono<Post> get(@PathVariable("id") Integer id) {
        return this.posts.findById(id);
    }

    @PutMapping("/{id}")
    public Mono<Post> update(@PathVariable("id") Integer id, @RequestBody Post post) {
        return this.posts.findById(id)
                .map(p -> {
                    p.setTitle(post.getTitle());
                    p.setContent(post.getContent());

                    return p;
                })
                .flatMap(p -> this.posts.save(p));
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable("id") Integer id) {
        return this.posts.deleteById(id);
    }

    // Posts are Sent to the client as Server Sent Events
    @CrossOrigin(allowedHeaders = "*")
    @GetMapping(path = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Result> getAllSse(@RequestParam(value = "page", defaultValue = "0") long page,
                                       @RequestParam(value = "size", defaultValue = "100") long size) {

        long internalPageSize = size / 10;
        final long[] internalPageNumber = {page};
        final long[] count = {0};

        Map<Integer, Tweet>[] tweetsMap = new Map[1];
        Flux<Post> allPosts = Flux.defer(() -> {
            log.info("page = " + internalPageNumber[0] + " size = " + internalPageSize);
            Flux<Post> all = posts.findAll()
                    .skip(internalPageNumber[0] * internalPageSize)
                    .take(internalPageSize);

            Flux<Integer> postIds = all.flatMap(post -> {
                return Flux.fromIterable(List.of(post.getId()));
            });

            Flux<Tweet> intersectTweets = Flux.defer(() -> {
                return tweets.findAllById(postIds);
            });

            List<Tweet> tweetStream = new ArrayList<>();
            intersectTweets.collectList().subscribe(tweetStream::addAll);

            tweetsMap[0] = tweetStream
                    .stream()
                    .collect(Collectors.toMap(tweet -> tweet.getId(), tweet -> tweet));

            return all;
        });


        return Flux.create(emitter -> {
            allPosts.switchIfEmpty(Flux.empty())
                    .delayElements(Duration.ofMillis(100))
                    .doOnNext((p) -> {
                        if (tweetsMap[0].containsKey(p.getId())) {
                            Tweet t = tweetsMap[0].get(p.getId());
                            Result r = Result.builder()
                                    .postId(p.getId())
                                    .postTitle(p.getTitle())
                                    .postContent(p.getContent())
                                    .tweetId(t.getId())
                                    .tweetTitle(t.getTitle())
                                    .tweetDescription(t.getDescription())
                                    .build();
                            count[0]++;
                            emitter.next(r);
                        }
                    })
                    .doOnComplete(() -> {
                        internalPageNumber[0]++;
                        log.info("All the data are processed !!! " + internalPageNumber[0]);
                    })
                    .repeat(() -> internalPageNumber[0] * 10 <= 100 && count[0] <= 40)
                    .doOnTerminate(() -> {
                        log.info("All the data are processed Total Count = " + count[0]);
                        emitter.complete();
                    })
                    .subscribe();
        }, FluxSink.OverflowStrategy.BUFFER);

    }
}