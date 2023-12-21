# Why Fabrikk?

## Why the name?

Fabrikk is the Norwegian word for 'Factory', and I work for a Norwegian company ([Ardoq](https://ardoq.com)). I'm Irish, so I did consider the Irish word 'Monarcha' but I figured I'd be forever correcting people's pronunciations! (it's pronounced something like  'MUN-ar-ka') I also like that I can shorten it to `fab` for a namespace alias, there's something I find pleasing about writing `fab/build` or `fab/one`.

## Why write it?&#x20;

In the dim past, before I fell in love with Clojure, I worked on a ruby-on-rails application. I'm now going steady with Clojure in my career, but from time to time my thoughts stray back to ruby. This is for one reason: the experience of writing tests. There were a number of reasons for this but in part I missed the wonderful [FactoryBot](https://thoughtbot.github.io/factory\_bot/summary.html) library.

Fabrikk is my attempt to scratch my own itch and write something with the same feature set. Factory bot wasn't without it's flaws; chief among them its tight coupling to rails ActiveRecord, so I've also tried to inject a bit of clojure magic (or at least I've made new, interesting mistakes rather than rehashing the sins of factory bot).

## Why not use another library?

There are a number of other options for this kind of thing, and depending on your use case (or aesthetic preferences) they may well be a better fit.

One thing to flag in particular is that Fabrikk has not been written with performance in mind. At. All. I don't really know how it would scale up to large data sets so this could be a factor to weigh if that's important to you.

### [Facai](https://github.com/lambdaisland/facai)/[Harvest](https://github.com/lambdaisland/harvest)

Here's where I address the elephant in the room; Fabrikk's basic api (create a factory, call build, vary output using `:with` and `:traits`) is basically identical to that of Facai/Harvest. However, Fabrikk differs pretty significantly in terms of implementation, and the way in which it handles persistence. Also, from what I can tell my longer term plans for Fabrikk go in a different direction.&#x20;

When I started writing Fabrikk, I was writing it with the aim of using it in the codebase of the company I work for, which has many complex entities with tangled relationships. Facai supported some of what I wanted to do, but would need to be changed significantly to support others, so I decided to start from scratch. Tl;DR [the curse of lisp](https://ericnormand.me/podcast/what-is-the-curse-of-lisp).

### [Specmonstah](https://github.com/reifyhealth/specmonstah/)&#x20;

Specmonstah is a really cool project, but I felt that it's more suited towards generating large data sets and didn't have quite as much flexibility as I wanted. I've also found the schema definition language it uses to be hard to learn/read. I'll admit that this could just be my own aesthetic preferences getting the better of me.&#x20;

### [Spec Generators](https://clojure.org/guides/spec#\_generators)

Spec generators are great for generating weird data to really test edge cases in your code, but I've always found it hard to get realistic domain entities out of them.&#x20;

### Pure Clojure

Clojure is a language where you can definitely just create the entities yourself as pure data. This is a perfectly viable thing to do, and I've done it a lot.&#x20;

However, I've found that you wind up either writing tons of repetitive objects (which makes it harder to see what's important in your tests) or writing build functions as you go that compose in ad-hoc ways. To my mind Fabrikk is the next step along this path, providing build functions that compose in a systematic way.



