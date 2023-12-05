# Concepts

## Templates

A template is a collection of keywords and associated values that is used to generate an entity. The values in a template can be either constants, or directives. The simplest template is a map e.g.:

```
{:id (fab/sequence)
 :name "Jim Murphy"
 :email "Jim@example.com"}
```

However, in some cases the ordering of evaluation of values might be important, e.g. when using the `derive` directive. In this case, a template can be an ordered collection of maps and/or tuples, e.g.

```
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
  
;; Compile to:

[[:one 1]
 [:two 2]
 [:three "three"]
 [:four "four"]]
```

Thus tuples in a compiled template can have their value overwritten, but their ordering is fixed once added.&#x20;

To build the entity, each key-value tuple is processed in order, the value is evaluated  (processing any directives as necessary), and is then assoc'ed into the result map under the key.&#x20;

## Build Options

Many functions accept the same options to vary the entity being created

* `build`
* `build-list`
* `create`
* `create-list`
* `one`
* `many`

Build options have the following  structure, all options are optional

| Option     | Description                                                                                                                                 |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `:with`    | A template                                                                                                                                  |
| :`traits`  | A list of trait keywords, the corresponding templates will be [compiled](concepts.md#compiled-template) in the order they're referenced     |
| `:without` | A list of keywords to be removed from the [compiled template](concepts.md#compiled-template) before the entity is built                     |
| `:as`      | A keyword or function. Overrides the primary-id of the entities factory, allowing you to control what value is used to reference the entity |

## Output Options

Many functions accept the following options to vary the output from a factory

* `build`
* `build-list`
* `create`
* `create-list`

| Option       | Description                                                                                       |
| ------------ | ------------------------------------------------------------------------------------------------- |
| output-as    | Keyword, override the default output format                                                       |
| transform    | A function that transforms the entity to be output (not supported for some values of `output-as`) |
| persist-with | Keyword, override the default persistence method when creating this entity.                       |

The `output-as` option supports a wide variety of values:

| Output Option  | Supports transform | Description                                                                             |
| -------------- | ------------------ | --------------------------------------------------------------------------------------- |
| `:meta`        | No                 | Default. Outputs the primary entity with the build graph stored in metadata             |
| :`value`       | Yes                | Outputs the primary entity only                                                         |
| `:context`     | No                 | Outputs the build graph                                                                 |
| `:tuple`       | Yes                | Outputs a 2 tuple of entity and build graph                                             |
| `:grouped`     | No                 | Outputs a map, with all dependent entities grouped by their factory id                  |
| `:build-order` | No                 | Outputs a list of entities in build order (reverse topological sort on the build graph) |

## Build Context



