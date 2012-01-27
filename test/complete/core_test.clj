(ns complete.core-test
  (:use complete.core clojure.test))

(deftest completions-test
  (is '("alength" "alias" "all-ns" "alter" "alter-meta!" "alter-var-root")
      (completions "al" 'clojure.core))

  (is '("jio/make-input-stream" "jio/make-output-stream" "jio/make-parents" "jio/make-reader" "jio/make-writer")
      (completions "jio/make" 'clojure.core))

  (is '("complete.core" "complete.core-test")
      (completions "complete.core")))