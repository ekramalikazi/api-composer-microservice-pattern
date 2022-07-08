package com.samples.demo.web;

import com.samples.demo.common.Result;
import com.samples.demo.model.Post;
import com.samples.demo.model.Tweet;
import com.samples.demo.repo.PostRepository;
import com.samples.demo.repo.TweetRepository;
import com.samples.demo.service.PostService;
import com.samples.demo.service.TweetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping(value = "/posts")
@RequiredArgsConstructor
@Slf4j
class PostController {

    private final PostRepository posts;

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
    @GetMapping(value = "/stream/22", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Post> streamAllTweets() {
        return posts.findAll();

    }

    private static final String[] WORDS = "The quick brown fox jumps over the lazy dog.".split(" ");

    @GetMapping(path = "/words", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> getWords() {
        return Flux
                .zip(Flux.just(WORDS), Flux.interval(Duration.ofSeconds(1)))
                .map(Tuple2::getT1);
    }

    @GetMapping(path = "/stream/working", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Post> working1(){
        return Flux.create(emitter ->{
            posts.findAll()
                    .doOnNext(data -> {
                        if (data.getId() % 2 == 0) {
                            emitter.next(data);
                        }
                    })
                    .doOnComplete( () -> {
                        emitter.complete();
                        System.out.println("All the data are processed !!!");
                    })
                    .subscribe();
        }, FluxSink.OverflowStrategy.LATEST);

    }

    @GetMapping(path = "/stream/pure", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Post> working2(){
        return posts.findAll()
                .flatMap(post -> Flux
                        .zip(Flux.interval(Duration.ofSeconds(3)),
                                Flux.fromStream(Stream.generate(() -> post))
                        )
                        .map(Tuple2::getT2)
                )
                .doOnComplete( () -> {
                    System.out.println("All the data are processed !!!");
                });

    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Post> working3(@RequestParam(value = "page", defaultValue = "0") long page,
                               @RequestParam(value = "size", defaultValue = "100") long size){
        log.info("calling sse... ");
        Flux<Post> all = posts.findAll().skip(page * size)
                .take(size);

        /*all.count().map(count -> {
            log.info("Counted values in this Flux: {}", count.longValue());
            return count;
        });*/

        return Flux.create(emitter -> {
                    all.delayElements(Duration.ofMillis(100))
                    .doOnNext(data -> {
                        if (data.getId() % 2 == 0) {
                            emitter.next(data);
                        }
                    })
                    .doOnComplete( () -> {
                        emitter.complete();
                        System.out.println("All the data are processed !!!");
                    })
                    //.log()
                    .subscribe();
        }, FluxSink.OverflowStrategy.BUFFER);

    }


    @Autowired
    PostService postService;

    private final TweetRepository tweets;

    private ExecutorService nonBlockingService = Executors
            .newCachedThreadPool();


    @GetMapping(path = "/stream/mixed", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Result> working4(@RequestParam(value = "page", defaultValue = "0") long page,
                               @RequestParam(value = "size", defaultValue = "100") long size) {

        log.info("calling mixed... ");

        Flux<Result> objectFlux = Flux.empty();

        long internalPageSize = size / 10;
        for (page = 0; page < internalPageSize; page++) {
            log.info("calling mixed... page = " + page);
            Flux<Post> allPosts = posts.findAll()
                    .skip(page * internalPageSize)
                    .take(internalPageSize);

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

            if (tweetsMap.size() == 0) {
                continue;
            }

            objectFlux = Flux.create(emitter -> {
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
        return objectFlux;
    }

    private final TweetService tweetService;

    @GetMapping(path = "/foo2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Result> getAllSomeWhatWorking2(@RequestParam(value = "page", defaultValue = "0") long page,
                                       @RequestParam(value = "size", defaultValue = "200") long size) {

        long internalPageSize = size / 10;
        final long[] internalPageNumber = {page};
        final long[] count = {0};

        Map<Integer, Tweet>[] tweetsMap = new Map[1];
        Flux<Post> allPosts = Flux.defer(() -> {
            Flux<Post> all2 = postService.findAll2(internalPageNumber[0], internalPageSize);
            Flux<Integer> postIds = all2.flatMap(post -> {
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

            return all2;
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
                        System.out.println("All the data are processed !!! " + internalPageNumber[0]);
                    })
                    .repeat(() -> internalPageNumber[0] * 10 <= 200 && count[0] <= 40)
                    .doOnTerminate(() -> {
                        System.out.println("All the data are processed Total Count = " + count[0]);
                        emitter.complete();
                    })
                    .subscribe();
        }, FluxSink.OverflowStrategy.BUFFER);

    }

    @GetMapping(path = "/foo", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Result> getAllSomeWhatWorking(@RequestParam(value = "page", defaultValue = "0") long page,
                        @RequestParam(value = "size", defaultValue = "100") long size) {

        long internalPageSize = size / 10;
        final long[] internalPageNumber = {page};

        Flux<Post> allPosts = Flux.defer(() -> {
            return postService.findAll2(internalPageNumber[0], internalPageSize);
        });

        return Flux.create(emitter -> {
            allPosts.switchIfEmpty(Flux.empty())
                    .delayElements(Duration.ofMillis(100))
                    .doOnNext((p) -> {
                        Tweet t = fetchTweet(p.getId());
                        if (t != null) {
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
                        internalPageNumber[0]++;
                        System.out.println("All the data are processed !!! " + internalPageNumber[0]);
                    })
                    .repeat(() -> internalPageNumber[0] <= 5)
                    .doOnTerminate(() -> {
                        emitter.complete();
                    })
                    .subscribe();
        }, FluxSink.OverflowStrategy.BUFFER);

        /*return Flux.create(emitter -> {
            postService.findAll2(internalPageNumber[0], internalPageSize)
                    .switchIfEmpty(Flux.empty())
                    .delayElements(Duration.ofMillis(100))
                    .doOnNext((p) -> {
                        Tweet t = fetchTweet(p.getId());
                        if (t != null) {
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
                        internalPageNumber[0]++;
                        System.out.println("All the data are processed !!! " + internalPageNumber[0]);
                    })
                    .repeat(() -> internalPageNumber[0] <= 5)
                    .doOnTerminate(() -> {
                        emitter.complete();
                    })
                    .subscribe();
            }, FluxSink.OverflowStrategy.BUFFER);*/
    }

    private Map<Integer, Tweet> fetchTweets(Flux<Integer> postIds) {
        Flux<Tweet> intersectTweets = tweets.findAllById(postIds);

        List<Tweet> tweetStream = new ArrayList<>();
        intersectTweets.collectList().subscribe(tweetStream::addAll);

        Map<Integer, Tweet> tweetsMap = tweetStream
                .stream()
                .collect(Collectors.toMap(tweet -> tweet.getId(), tweet -> tweet));

        return tweetsMap;

    }

    private Tweet fetchTweet(Integer postId) {
        Tweet tweet = Tweet.builder().id(postId)
                .description("d")
                .title("e")
                .build();

        return tweet;

    }

    private long getOffset(long page) {
        return page + 1;
    }

}