package com.samples.demo.config;

import com.samples.demo.model.Post;
import com.samples.demo.model.Tweet;
import com.samples.demo.repo.PostRepository;
import com.samples.demo.repo.TweetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final PostRepository posts;

    private final TweetRepository tweets;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("start data initialization...");
        this.posts
                .saveAll(
                        List.of(
                                Post.builder().title("Post one").content("The content of post one").build(),
                                Post.builder().title("Post two").content("The content of post two").build()
                        )
                )
                .thenMany(
                        this.posts.findAll()
                )
                .subscribe((data) -> log.info("post:" + data),
                        (err) -> log.error("error" + err),
                        () -> log.info("initialization is done...")
                );

        this.tweets
                .saveAll(
                        List.of(
                                Tweet.builder().title("Tweet one").description("The description of Tweet one").build(),
                                Tweet.builder().title("Tweet two").description("The description of Tweet two").build()
                        )
                )
                .thenMany(
                        this.tweets.findAll()
                )
                .subscribe((data) -> log.info("tweet:" + data),
                        (err) -> log.error("error" + err),
                        () -> log.info("initialization is done...")
                );
    }
}