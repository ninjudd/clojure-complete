[![Reference Status](https://www.versioneye.com/clojure/clojure-complete:clojure-complete/reference_badge.svg?style=flat)](https://www.versioneye.com/clojure/clojure-complete:clojure-complete/references)

Clojure-complete is an symbol completion library for Clojure. The code is adapted from
[jochu/swank-clojure](http://github.com/jochu/swank-clojure).

## Examples

    (use 'complete.core)

    (completions "al" 'clojure.core)
    => ("alength" "alias" "all-ns" "alter" "alter-meta!" "alter-var-root")

    (completions "jio/make" 'clojure.core)
    => ("jio/make-input-stream" "jio/make-output-stream" "jio/make-parents" "jio/make-reader" "jio/make-writer")

    (completions "clojure.core")
    => ("clojure.core" "clojure.core.protocols" "clojure.core.ArrayChunk" "clojure.core.ArrayManager" "clojure.core.IVecImpl" "clojure.core.Vec" "clojure.core.VecNode" "clojure.core.VecSeq" "clojure.core.protocols.InternalReduce")

## Running Tests

    lein with-profile 1.2:1.3:1.4 test
