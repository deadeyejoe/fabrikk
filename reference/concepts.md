# Concepts

## What is an entity?

This documentation makes heavy use of the word 'entity', but what is an entity? In general, entities are clojure **maps** that represent data in your application domain, e.g. a user with an id, name and email.

Fabrikk also considers lists of entities to be entities in their own right, but this is generally an implementation detail.

This documentation frequently references the following classes of entities:

### Primary Entity

The entity that is currently being built, this term is generally only used when describing the build process.

### Dependent Entity

Dependent entities are ones that are required to exist before the Primary entity can be built. There may be many such entities, and those entities may themselves have dependent entities.&#x20;

Dependent entities are specified using the `one` and `many` directives, and by passing the output of a `build` or `create` call as a value in a `with` build option.

### Referring Entity

A referring entity is one that has dependent entities. It's generally used when talking about what happens when an entity is depended on.

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

The tuples here are equivalent to a map with a single key in it, either can be used. The order of evaluation is dictated by the list, and is undefined within maps.

In practice, Clojure maps below 8 keys are usually array maps that have a defined order of iteration, but it's best not to rely on this.

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

i.e. tuples in a compiled template can have their value overwritten, but their ordering is fixed once added.

To build the entity, each key-value tuple is processed in order, the value is evaluated (processing any directives as necessary), and is then assoc'ed into the result map under the key.

## Build Options

These functions accept build options to vary the entity being created:

* `build`
* `build-list`
* `create`
* `create-list`
* `one`
* `many`

Build options have the following structure, all keys are optional:

<table><thead><tr><th width="211">Option</th><th>Description</th></tr></thead><tbody><tr><td><code>:with</code></td><td>A template</td></tr><tr><td>:<code>traits</code></td><td>A list of trait keywords, the corresponding templates will be <a href="concepts.md#compiled-template">compiled</a> in the order they're referenced</td></tr><tr><td><code>:without</code></td><td>A list of keywords to be removed from the <a href="concepts.md#compiled-template">compiled template</a> before the entity is built</td></tr><tr><td><code>:associate-as</code></td><td>A keyword or function. Allows you to control what value is associated when this entity is referenced by another. Accepts the special values <code>:itself</code> or <code>:identity</code>,  which are equivalent to passing clojure.core/identity. See also <a data-mention href="concepts.md#associating">#associating</a>. </td></tr><tr><td><code>:persist-with</code></td><td>A keyword identifying a custom persistence method, allows you to override the configured default persistence method, does NOT change persistence for dependent entities, see <a data-mention href="../tutorial/persistence.md">persistence.md</a> </td></tr></tbody></table>

## Output Options

These functions accept output options to vary the output from a factory:

* `build`
* `build-list`
* `create`
* `create-list`

Output as options have the following structure, all keys are optional:

<table><thead><tr><th width="152.11627906976742">Option</th><th>Description</th></tr></thead><tbody><tr><td>output-as</td><td>Keyword, override the default output format</td></tr><tr><td>transform</td><td>A function that transforms the entity to be output (only supported for some values of <code>output-as</code>)</td></tr></tbody></table>

The `output-as` option supports a wide variety of values:

<table><thead><tr><th width="169">Output Option</th><th width="186" data-type="checkbox">Supports transform</th><th>Description</th></tr></thead><tbody><tr><td><code>:meta</code></td><td>false</td><td>Default. Outputs the primary entity with the build graph stored in metadata</td></tr><tr><td>:<code>value</code></td><td>true</td><td>Outputs the primary entity only</td></tr><tr><td><code>:context</code></td><td>false</td><td>Outputs the build graph</td></tr><tr><td><code>:tuple</code></td><td>true</td><td>Outputs a 2 tuple of entity and build graph</td></tr><tr><td><code>:grouped</code></td><td>false</td><td>Outputs a map, with all dependent entities grouped by their factory id</td></tr><tr><td><code>:build-order</code></td><td>false</td><td>Outputs a list of entities in build order (reverse topological sort on the build graph)</td></tr></tbody></table>

## Build Graph

This section gets into the weeds about the build graph that Fabrikk uses internally. Concepts here shouldn't be necessary for understanding the documentation, but may be useful for understanding the code.&#x20;

### Basic Structure

When you build an entity it forms a node in the build graph. When an entity depends on another entity, both entities are nodes in the build graph, and there is one edge between them for each key in the referring entity whose value depends on the dependent entity. Edges are labelled with the corresponding key, and the `:associate-as` value that is used to derive the value in the referring entity. As an example consider:

```clojure
(def user (fab/->factory
  {:primary-key :id
   :template {:id (fab/sequence)
              :name "Alice"}}))

(let [bob (fab/build user {:with {:name "Bob"}})]
  (fab/build user {:with {:parent bob
                          :parent-name (fab/derive [:parent] :name)}}))
;;=> {:id 2, :name "Alice", :parent 1, :parent-name "Bob"}
```

The build graph here has 2 nodes - users "Alice" and "Bob" - and 2 edges between those nodes. One edge is labelled `:parent` with `:associate-as` :id (the primary key of the user factory), and the other edge is labelled `:parent-name` with `:associate-as` :name.&#x20;

### Associating

When building a [#dependent-entity](concepts.md#dependent-entity "mention"), the act of inserting it into the build graph is referred to as '_Association**'**_. More concretely, when an entity is associated:

* A `key` is required, and an `:associate-as` option may be passed or inferred&#x20;
* If necessary the build graph of the dependent entity is merged into the build graph of the primary entity (only the first time this entity is associated)
* An edge is created from the primary entity to the dependent entity. This edge is labelled with the `key` and `:associate-as` option
* A value is derived from the dependent entity using `:associate-as`, and then assoc'ed into the primary entity under the `key`

If no `:associate-as` option is provided, then the factory's `:primary-key` is used if present, otherwise the identity function is used (i.e. the full dependent entity is assoc'ed into the primary entity).
