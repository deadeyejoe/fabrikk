# Creating Entities

So far we've been creating our entities as just data(tm), which is nice for writing unit tests, but how can fabrikk help us when we're writing tests that interact with our app's persistence layer? Say we want to write a bunch of data to our database to test some queries that we've written, or we want to write tests that make requests against our API. 

Fabrikk aims to support all of these use cases (and any more you can dream up), and it aims to do this with a single factory definition per-entity. It'll take a few tutorial articles to explore this area thoroughly, so let's get started by reviewing the current state of the factories we've been building:

- 
