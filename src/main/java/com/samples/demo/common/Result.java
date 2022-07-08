package com.samples.demo.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Result {

    int postId;
    int tweetId;
    String postTitle;
    String tweetTitle;
    String postContent;
    String tweetDescription;
}
