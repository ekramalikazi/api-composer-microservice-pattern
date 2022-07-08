# API Composition Microservice Pattern

![Diagram](./img/API%20Composer.drawio.png)
## Intro
* Implementing queries in a microservice architecture.
* Different patterns for implementing query operations in a microservice architecture:
    * **_API composition pattern_**
        * simple
    * **_Command query responsibility segregation (CQRS) pattern_**
        * more powerful than the API composition pattern
        * complex
        * maintains one or more view databases whose sole purpose is to support queries
  

## Ways to implement API Composer Pattern

_**Considerations**_

* To avoid memory issues when loading large datasets, prefer implementing pagination

_**Async Style**_

_**Reactive Programming - Server Sent Events**_



## Benefits
  * simple
    * The API composition pattern, which gathers data from multiple services, 
      is the simplest way to implement queries and should be used whenever possible.
  * intuitive

## Drawbacks

  * **_Increased overhead_** 
    * overhead of invoking multiple services and querying multiple databases
    * involves multiple requests and database queries
    * more computing and network resources are required, increasing the cost of running the application
  * **_Risk of reduced availability_**
    * implementation of a query operation depends on other services, which implies its availability will be significantly less than that of a single service
    * example: if the availability of an individual service is 99.5%, then the availability of the API Composer which invokes other services, will be less than 99.5% or ~ 95%!
    * How to improve API Composer availability ?
        * cached data when a dependent service is unavailable (though it may be potentially stale)
        * to return incomplete data
        * example: For example, imagine that Dependent Service is temporarily unavailable. The API Composer for the query() operation could omit that service’s data from the response, 
          because the UI can still display other useful information.
  * **_Lack of transactional data consistency_**
    * Monolithic application typically executes a query operation using a single database transaction. 
      ACID transactions—subject to the fine print about isolation levels—ensure that an application has a consistent view of the data, even if it executes multiple database queries
    * The API composition pattern executes multiple database queries against multiple databases. There’s a risk, therefore, that a query operation will return inconsistent data.
    * example: For example, an Order retrieved from Order Service might be in the CANCELLED state, 
      whereas the corresponding Ticket retrieved from Kitchen Service might not yet have been cancelled. 
      The API composer must resolve this discrepancy, which increases the code complexity. 
      To make matters worse, an API composer might not always be able to detect inconsistent data, 
      and will return it to the client.
