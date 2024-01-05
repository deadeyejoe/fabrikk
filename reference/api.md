# API

## Factory Definition

### ->factory

```clojure
(->factory factory-id factory-description)
```

Accepts a keyword and a factory definition and returns a factory record. The keyword will be used as an id  to register the factory in a global directory internal to fabrikk. If a factory with the key already exists in the directory, it will be overwritten. Namespaced keywords are recommended.

Most of the API accepts either a factory record or the id, with the exception of the `persist!` multimethod which requires the id.

Factories are records that can be called as functions:

```clojure
(def user (->factory ....))
(user) ;;=> equivalent to (build user)
(user build-opts) ;; equivalent to (build user build-opts)
```

A factory definition has the following structure:

<table><thead><tr><th width="141">Property</th><th width="108" data-type="checkbox">Required?</th><th width="168">Inherit Behaviour</th><th>Description</th></tr></thead><tbody><tr><td>primary-key</td><td>false</td><td>Replace</td><td>A keyword that specifies a key in the template to identify the entity when referenced</td></tr><tr><td>template</td><td>true</td><td>Merge</td><td>A template specifying the default representation of the entity, see <a data-mention href="concepts.md">concepts.md</a></td></tr><tr><td>persistable</td><td>false</td><td>Replace</td><td>A boolean that controls whether the entity should be persisted in calls to <code>create</code> and <code>create-list</code>. Defaults to <code>true</code></td></tr><tr><td>traits</td><td>false</td><td>Merge</td><td>A map of keywords to templates, traits are identified by these keywords, see <a data-mention href="concepts.md">concepts.md</a> for more</td></tr><tr><td>transients</td><td>false</td><td>Replace</td><td>A template whose keys will be removed from the entity after it is built, see <a data-mention href="concepts.md">concepts.md</a> for more</td></tr><tr><td>before-build</td><td>false</td><td>Replace</td><td>A function that takes the compiled template (constructed from template, transients and any build options) and returns an updated template, see <a data-mention href="build-hooks.md">build-hooks.md</a> for more</td></tr><tr><td>after-build</td><td>false</td><td>Replace</td><td>A function that takes the current build context, and the value of the built entity (including transients), and returns an updated entity, see <a data-mention href="build-hooks.md">build-hooks.md</a> for more</td></tr></tbody></table>

### inherit

```clojure
(inherit parent-factory-or-id factory-id factory-description)
```

Accepts an existing factory (or id), a keyword and a factory description, and returns a new new factory that inherits from the existing factory. All keys listed in [#greater-than-factory](api.md#greater-than-factory "mention") may be specified, see the inherit behaviour column in the table for more.

A template must be specified in the inherited factory. This template is combined with the existing template following the behaviour described in [#compiled-template](concepts.md#compiled-template "mention")

## Building and Creation

### build

```clojure
(build factory-or-id)
(build factory-or-id build-opts)
(build factory-or-id build-opts output-opts)
```

Accepts a factory and some optional build and output options, and outputs an entity. See [#build-options](concepts.md#build-options "mention") and [#output-options](concepts.md#output-options "mention") for more

### build-list

```clojure
(build-list factory-or-id quantity)
(build-list factory-or-id quantity build-opts+)
(build-list factory-or-id quantity build-opts+ output-opts)
```

Accepts a factory and a quantity and builds that many entities. Build option can either be a single option map that is used to build each entity, or a list of option maps, each of which will be used to build one entity. If this list contains fewer than `quantity` options, the last one is repeated. For example:

```clojure
(build-list user 3 [{:with {:name "Joe"}}
                    {:with {:name "John"}}])
;; => [{:name "Joe" ....}
;;     {:name "John" ....}
;;     {:name "John" ....}
```

See [#build-options](concepts.md#build-options "mention") and [#output-options](concepts.md#output-options "mention") for more.

### create

```clojure
(create factory-or-id)
(create factory-or-id build-opts)
(create factory-or-id build-opts output-opts)
```

As for `build` but also persists each entity in the graph in build order. See [#build-options](concepts.md#build-options "mention") and [#output-options](concepts.md#output-options "mention") for more.

### create-list

```clojure
(create-list factory-or-id quantity)
(create-list factory-or-id quantity build-opts+)
(create-list factory-or-id quantity build-opts+ output-opts)
```

As for `build-list` but also persists each entity in the graph in build order. See [#build-options](concepts.md#build-options "mention") and [#output-options](concepts.md#output-options "mention") for more.

## Directives

### constant

```clojure
(constant x)
```

Returns a directive that evaluates to `x` during build. Useful if you don't want Fabrikk to try and interpret the value (e.g. if you want to specify a function as a value)

### sequence

```clojure
(sequence)
(sequence transform-fn)
(sequence transform-fn identifier)
```

Returns a directive that evaluates to an increasing series of integers, producing a new value each time the enclosing factory is invoked. Accepts an optional `transform-fn` that is called with the generated integer.

By default each call to `sequence` creates an 'series' of integers isolated to this specific key on this specific factory. If the optional `identifier` keyword is passed, then the sequence will be shared with each other `sequence` directive with that identifier.

Sequences persist for the lifetime of the program.

### one

```clojure
(one factory-or-id)
(one factory-or-id build-opts)
```

Returns a directive that will invoke the given factory at build time and add the resulting entity into the current build graph.

The entity is referenced via the key associated with this directive in the template.

Either the factory instance itself or its id can be used. Accepts the same build options as `build` etc. See [#build-options](concepts.md#build-options "mention") for more.

### many

```clojure
(many factory-or-id quantity)
(many factory-or-id quantity build-opts+)
```

Returns a directive that will invoke the given factory at build time, build `quantity` copies of that entity, and add the resulting list of entities into the current build graph.

The list of entities are referenced via the key associated with this directive in the template. Entities can be referenced within this list via their index.

Either the factory instance itself or its id can be used. Accepts the same build options as `build` etc. As is the case for `build-list`, either a single build option can be passed, or a list. See [#build-options](concepts.md#build-options "mention") for more.

### derive

```clojure
(derive key-or-path)
(derive key-or-path transform-fn)
```

Returns a directive that derives a value from the build graph at build time. If a keyword is passed, then the value will be derived from the current entity. If a path - i.e. a list of keywords - is passed, then the value will be derived from the entity at that path, relative to the current entity.

Accepts an optional `transform-fn` that accepts the derived value and returns a transformed value.

When deriving from an entity in the build graph using a path, if no `transform-fn` is specified the derived value will be the `primary-key` if the entities factory specifies one (see [#greater-than-factory](api.md#greater-than-factory "mention")), or the entity itself otherwise.

### associate-as

```clojure
(associate-as entity transform-fn)
```

Takes an `entity` and a `transform-fn` and overrides the associate-as value on that entity. This is sort of on an honorary directive, along with simply passing the entity in a `:with` template. See [#associating](concepts.md#associating "mention") for more

## Persistence

### persist!

```clojure
(defmethod persist! persistence-method [factory-id entity])
```

A multimethod that facilitates custom persistence methods. Takes a factory id, and an entity, and expects a persisted version of that entity to be returned. Changes made to the entity during persistence will be propagated through the build graph as necessary, depending on what other entities have references to the persisted one.

### set-default-persistence!

```clojure
(set-default-persistence! keyword)
```

Set the default persistence method. Defaults to built-in persistence.

### store

```clojure
@store
```

An atom that stores entities created using the built-in persistence.

The built-in persistence method collects built entities in lists keyed by their factory id. It does not modify them.

### reset-store!

```clojure
(reset-store!)
```

Empties the built in store.
