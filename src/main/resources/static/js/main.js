function LoadPostsData() {
    this.source = null;
    this.start = function () {
        let postsTable = document.getElementById("results");
        this.source = new EventSource("/posts/foo2");
        this.source.addEventListener("message", function (event) {
            // These events are JSON, so parsing.
            let r = JSON.parse(event.data);

            let rowElement = postsTable.getElementsByTagName("tbody")[0].insertRow(0);
            let cell0 = rowElement.insertCell(0);
            let cell1 = rowElement.insertCell(1);
            let cell2 = rowElement.insertCell(2);
            let cell3 = rowElement.insertCell(3);
            let cell4 = rowElement.insertCell(4);
            let cell5 = rowElement.insertCell(5);

            cell0.innerHTML = r.postId;
            cell1.innerHTML = r.postTitle;
            cell2.innerHTML = r.postContent;
            cell3.innerHTML = r.tweetId;
            cell4.innerHTML = r.tweetTitle;
            cell5.innerHTML = r.tweetDescription;
        });

        this.source.onerror = function () {
            this.close();
        };
    };

    this.stop = function () {
        this.source.close();
    };
}

results = new LoadPostsData();

/*
 * Register callbacks for starting and stopping the SSE controller.
 */
window.onload = function () {
    results.start();
};
window.onbeforeunload = function () {
    results.stop();
}