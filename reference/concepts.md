# Concepts

## What is an entity?

This documentation makes heavy use of the word 'entity', but what is an entity? In general, entities are clojure **maps** that represent data in your application domain, e.g. a user with an id, name and email.&#x20;

Fabrikk also considers lists of entities to be entities in their own right, but this is generally an implementation detail.

## Templates

A template is a collection of keywords and associated values that is used to generate an entity. The values in a template can be either constants, or directives. The simplest template is a map e.g.:

```clojure
{:id (fab/sequence)
 :name "Jim Murphy"
 :email "Jim@example.com"}
```

However, in some cases the ordering of evaluation of values might be important, e.g. when using the `derive` directive. In this case, a template can be an ordered collection of maps and/or tuples, e.g.

```clojure
[{:id (fab/sequence)
  :name "Jim Murphy"}
  [:email (fab/derive :id #(str "User-" % "@example.com")]]
```

The tuples here are equivalent to a map with a single key in it, either can be used. The order of evaluation is dictated by the list, and is undefined within maps.&#x20;

In practice, Clojure maps below 8 keys are usually array maps that have a defined order of iteration, but it's best not to rely on this.&#x20;

### Compiled Template

Templates can come from the following sources in order of precedence:

* The main factory template
* Trait templates (in the same order they're specified in build-opts)
* `with` arguments in build-opts

These templates are compiled into a single _Compiled template_ before the entity is built. Compiled templates are built one template at a time, in the order of precedence, preserving the ordering of keys that are already in the compiled template. As a concrete example:

```clojure
;; These two templates
[[[:one "one"]
  [:two "two"]
  [:three "three"]]
 [[:four "four"]
  [:two 2]
  [:one 1]]]
  
;; Are equivalent to this one after compilation

[[:one 1]
 [:two 2]
 [:three "three"]
 [:four "four"]]
```

i.e. tuples in a compiled template can have their value overwritten, but their ordering is fixed once added.&#x20;

To build the entity, each key-value tuple is processed in order, the value is evaluated  (processing any directives as necessary), and is then assoc'ed into the result map under the key.&#x20;

## Build Options

These functions accept build options to vary the entity being created:

* `build`
* `build-list`
* `create`
* `create-list`
* `one`
* `many`

Build options have the following  structure, all keys are optional:

<table><thead><tr><th width="140">Option</th><th>Description</th></tr></thead><tbody><tr><td><code>:with</code></td><td>A template</td></tr><tr><td>:<code>traits</code></td><td>A list of trait keywords, the corresponding templates will be <a href="concepts.md#compiled-template">compiled</a> in the order they're referenced</td></tr><tr><td><code>:without</code></td><td>A list of keywords to be removed from the <a href="concepts.md#compiled-template">compiled template</a> before the entity is built</td></tr><tr><td><code>:as</code></td><td>A keyword or function. Overrides the primary-id of the entities factory, allowing you to control what value is used to reference the entity</td></tr></tbody></table>

## Output Options

These functions accept output options to vary the output from a factory:

* `build`
* `build-list`
* `create`
* `create-list`

Output as options have the following  structure, all keys are optional:

<table><thead><tr><th width="144">Option</th><th>Description</th></tr></thead><tbody><tr><td>output-as</td><td>Keyword, override the default output format</td></tr><tr><td>transform</td><td>A function that transforms the entity to be output (only supported for some values of <code>output-as</code>)</td></tr><tr><td>persist-with</td><td>Keyword, override the default persistence method when creating this entity. </td></tr></tbody></table>

The `output-as` option supports a wide variety of values:

<table><thead><tr><th width="169">Output Option</th><th width="186" data-type="checkbox">Supports transform</th><th>Description</th></tr></thead><tbody><tr><td><code>:meta</code></td><td>false</td><td>Default. Outputs the primary entity with the build graph stored in metadata</td></tr><tr><td>:<code>value</code></td><td>true</td><td>Outputs the primary entity only </td></tr><tr><td><code>:context</code></td><td>false</td><td>Outputs the build graph</td></tr><tr><td><code>:tuple</code></td><td>true</td><td>Outputs a 2 tuple of entity and build graph</td></tr><tr><td><code>:grouped</code></td><td>false</td><td>Outputs a map, with all dependent entities grouped by their factory id</td></tr><tr><td><code>:build-order</code></td><td>false</td><td>Outputs a list of entities in build order (reverse topological sort on the build graph)</td></tr></tbody></table>

## Build Context



